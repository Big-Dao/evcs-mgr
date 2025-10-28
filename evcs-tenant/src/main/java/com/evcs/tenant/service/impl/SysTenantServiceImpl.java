package com.evcs.tenant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 租户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements ISysTenantService {

    /**
     * 分页查询租户列表
     * 数据权限：ALL - 查看所有租户，CHILDREN - 查看子租户，SELF - 查看自己
     */
    @Override
    @DataScope
    public IPage<SysTenant> queryTenantPage(Page<SysTenant> page, SysTenant tenant) {
        QueryWrapper<SysTenant> wrapper = buildTenantQueryWrapper(tenant);
        return this.page(page, wrapper);
    }

    /**
     * 查询租户列表（不分页）
     * 数据权限：ALL - 查看所有租户，CHILDREN - 查看子租户，SELF - 查看自己
     */
    @Override
    @DataScope
    public List<SysTenant> queryTenantList(SysTenant tenant) {
        QueryWrapper<SysTenant> wrapper = buildTenantQueryWrapper(tenant);
        return this.list(wrapper);
    }
    
    /**
     * 构建租户查询条件
     */
    private QueryWrapper<SysTenant> buildTenantQueryWrapper(SysTenant tenant) {
        QueryWrapper<SysTenant> wrapper = new QueryWrapper<>();
        
        // 根据租户名称查询
        if (StrUtil.isNotBlank(tenant.getTenantName())) {
            wrapper.like("tenant_name", tenant.getTenantName());
        }
        
        // 根据租户编码查询
        if (StrUtil.isNotBlank(tenant.getTenantCode())) {
            wrapper.like("tenant_code", tenant.getTenantCode());
        }
        
        // 根据状态查询
        if (tenant.getStatus() != null) {
            wrapper.eq("status", tenant.getStatus());
        }
        
        // 排序
        wrapper.orderByAsc("tenant_id");
        
        return wrapper;
    }

    /**
     * 获取租户树形结构
     * 数据权限：ALL - 查看所有租户，CHILDREN - 查看子租户，SELF - 查看自己
     */
    @Override
    @DataScope
    public List<Tree<Long>> getTenantTree() {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SysTenant::getId, 0L)  // 排除虚拟根节点
               .eq(SysTenant::getStatus, 1)
               .orderByAsc(SysTenant::getId);
        List<SysTenant> tenantList = this.list(wrapper);
        
        if (CollUtil.isEmpty(tenantList)) {
            return CollUtil.newArrayList();
        }
        
        List<TreeNode<Long>> nodeList = tenantList.stream()
                .filter(tenant -> tenant.getId() != null)  // 过滤掉ID为null的记录
                .map(tenant -> {
                    TreeNode<Long> node = new TreeNode<>();
                    node.setId(tenant.getId());
                    node.setParentId(tenant.getParentId());
                    node.setName(tenant.getTenantName());
                    node.setExtra(BeanUtil.beanToMap(tenant));
                    return node;
                })
                .collect(Collectors.toList());
        
        return TreeUtil.build(nodeList, 0L);
    }

    /**
     * 根据ID查询租户详情
     * 注意：此方法不做数据权限检查，调用方需自行验证权限
     */
    @Override
    public SysTenant getTenantById(Long tenantId) {
        return this.getById(tenantId);
    }

    /**
     * 新增租户
     * 只有管理员或父租户可以创建子租户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTenant(SysTenant tenant) {
        // 检查租户编码是否重复
        long count = this.count(new QueryWrapper<SysTenant>()
                .eq("tenant_code", tenant.getTenantCode()));
        if (count > 0) {
            throw new RuntimeException("租户编码已存在");
        }
        
        // 设置父级信息
        if (tenant.getParentId() != null && tenant.getParentId() > 0) {
            SysTenant parentTenant = this.getById(tenant.getParentId());
            if (parentTenant == null) {
                throw new RuntimeException("父租户不存在");
            }
            
            // 构建祖级列表
            String ancestors = parentTenant.getAncestors() + "," + parentTenant.getId();
            tenant.setAncestors(ancestors);
        } else {
            tenant.setParentId(0L);
            tenant.setAncestors("0");
        }
        
        // 设置创建信息
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setCreateBy(TenantContext.getCurrentTenantId());
        
        return this.save(tenant);
    }

    /**
     * 更新租户信息
     * 注意：此方法不做数据权限检查，调用方需自行验证权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTenant(SysTenant tenant) {
        // 检查租户是否存在
        SysTenant existTenant = this.getById(tenant.getId());
        if (existTenant == null) {
            throw new RuntimeException("租户不存在");
        }
        
        // 检查租户编码是否重复（排除自己）
        long count = this.count(new QueryWrapper<SysTenant>()
                .eq("tenant_code", tenant.getTenantCode())
                .ne("id", tenant.getId()));
        if (count > 0) {
            throw new RuntimeException("租户编码已存在");
        }
        
        // 不允许修改父级关系（需要单独的移动方法）
        tenant.setParentId(null);
        tenant.setAncestors(null);
        
        // 设置更新信息
        tenant.setUpdateTime(LocalDateTime.now());
        tenant.setUpdateBy(TenantContext.getCurrentTenantId());
        
        return this.updateById(tenant);
    }

    /**
     * 删除租户
     * 注意：此方法不做数据权限检查，调用方需自行验证权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTenant(Long tenantId) {
        // 检查是否有子租户
        long childCount = this.count(new QueryWrapper<SysTenant>()
                .eq("parent_id", tenantId));
        if (childCount > 0) {
            throw new RuntimeException("存在子租户，无法删除");
        }
        
        // 检查租户下是否有业务数据
        // 注意：这里使用原生SQL直接查询，因为切换租户上下文可能导致问题
        Long stationCount = baseMapper.countByTenantId("evcs_station", tenantId);
        if (stationCount != null && stationCount > 0) {
            throw new RuntimeException("租户下存在充电站数据，无法删除");
        }
        
        Long orderCount = baseMapper.countByTenantId("evcs_charging_order", tenantId);
        if (orderCount != null && orderCount > 0) {
            throw new RuntimeException("租户下存在订单数据，无法删除");
        }
        
        return this.removeById(tenantId);
    }

    /**
     * 移动租户到新的父级下
     * 需要重新计算祖级列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveTenant(Long tenantId, Long newParentId) {
        SysTenant tenant = this.getById(tenantId);
        if (tenant == null) {
            throw new RuntimeException("租户不存在");
        }
        
        // 不能移动到自己的子级下
        if (isChildTenant(newParentId, tenantId)) {
            throw new RuntimeException("不能移动到自己的子级下");
        }
        
        String newAncestors;
        if (newParentId == null || newParentId == 0) {
            newAncestors = "0";
            newParentId = 0L;
        } else {
            SysTenant newParent = this.getById(newParentId);
            if (newParent == null) {
                throw new RuntimeException("新父租户不存在");
            }
            newAncestors = newParent.getAncestors() + "," + newParent.getId();
        }
        
        // 更新当前租户
        tenant.setParentId(newParentId);
        tenant.setAncestors(newAncestors);
        tenant.setUpdateTime(LocalDateTime.now());
        tenant.setUpdateBy(TenantContext.getCurrentTenantId());
        
        boolean result = this.updateById(tenant);
        
        // 更新所有子级的祖级列表
        if (result) {
            updateChildrenAncestors(tenant);
        }
        
        return result;
    }

    /**
     * 检查是否为子租户
     */
    private boolean isChildTenant(Long parentId, Long childId) {
        if (parentId == null || childId == null) {
            return false;
        }
        
        SysTenant childTenant = this.getById(parentId);
        if (childTenant == null) {
            return false;
        }
        
        String[] ancestors = childTenant.getAncestors().split(",");
        return Arrays.asList(ancestors).contains(childId.toString());
    }

    /**
     * 递归更新子级的祖级列表
     */
    private void updateChildrenAncestors(SysTenant tenant) {
        List<SysTenant> children = this.list(new QueryWrapper<SysTenant>()
                .eq("parent_id", tenant.getId()));
        
        for (SysTenant child : children) {
            String newAncestors = tenant.getAncestors() + "," + tenant.getId();
            child.setAncestors(newAncestors);
            child.setUpdateTime(LocalDateTime.now());
            child.setUpdateBy(TenantContext.getCurrentTenantId());
            
            this.updateById(child);
            
            // 递归更新子级
            updateChildrenAncestors(child);
        }
    }

    /**
     * 获取租户的所有子租户ID列表（包含自己）
     */
    @Override
    public List<Long> getTenantChildren(Long tenantId) {
        if (tenantId == null) {
            return CollUtil.newArrayList();
        }
        
        List<SysTenant> allTenants = this.list();
        List<Long> children = CollUtil.newArrayList(tenantId);
        
        // 递归查找所有子租户
        findChildren(allTenants, tenantId, children);
        
        return children;
    }

    /**
     * 根据父租户ID查询直接子租户列表
     */
    @Override
    public List<SysTenant> getSubTenants(Long parentId) {
        return this.list(new QueryWrapper<SysTenant>().eq("parent_id", parentId));
    }

    /**
     * 修改租户状态
     */
    @Override
    public boolean changeStatus(Long tenantId, Integer status) {
        SysTenant tenant = new SysTenant();
        tenant.setId(tenantId);
        tenant.setStatus(status);
        tenant.setUpdateTime(LocalDateTime.now());
        tenant.setUpdateBy(TenantContext.getCurrentUserId());
        return this.updateById(tenant);
    }

    /**
     * 递归查找子租户
     */
    private void findChildren(List<SysTenant> allTenants, Long parentId, List<Long> children) {
        for (SysTenant tenant : allTenants) {
            if (Objects.equals(tenant.getParentId(), parentId)) {
                children.add(tenant.getId());
                findChildren(allTenants, tenant.getId(), children);
            }
        }
    }

    /**
     * 检查租户编码是否存在
     */
    @Override
    public boolean checkTenantCodeExists(String tenantCode, Long excludeId) {
        QueryWrapper<SysTenant> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_code", tenantCode);
        
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        
        return this.count(wrapper) > 0;
    }
}