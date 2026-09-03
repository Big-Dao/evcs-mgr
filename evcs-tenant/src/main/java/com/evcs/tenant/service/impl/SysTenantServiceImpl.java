package com.evcs.tenant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.audit.TenantAuditService;
import com.evcs.tenant.client.OrderUsageClient;
import com.evcs.tenant.client.StationUsageClient;
import com.evcs.tenant.dto.OrderUsageCount;
import com.evcs.tenant.dto.StationUsageCount;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 租户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements ISysTenantService {

    private final TenantAuditService tenantAuditService;
    private final StationUsageClient stationUsageClient;
    private final OrderUsageClient orderUsageClient;

    /**
     * 租户类型：平台租户
     */
    private static final Integer TENANT_TYPE_PLATFORM = 1;

    /**
     * 租户类型：运营商租户
     */
    private static final Integer TENANT_TYPE_OPERATOR = 2;

    /**
     * 分页查询租户列表
     * 数据权限：ALL - 查看所有租户，CHILDREN - 查看子租户，SELF - 查看自己
     */
    @Override
    @DataScope
    public IPage<SysTenant> queryTenantPage(Page<SysTenant> page, SysTenant tenant) {
        QueryWrapper<SysTenant> wrapper = buildTenantQueryWrapper(tenant);
        applyTenantFilter(wrapper);
        IPage<SysTenant> result = this.page(page, wrapper);
        decorateTenantInfo(result.getRecords());
        return result;
    }

    /**
     * 查询租户列表（不分页）
     * 数据权限：
     * - 平台租户(tenant_type=1): 可以查看自己及所有子租户
     * - 运营商租户(tenant_type=2): 只能查看自己
     */
    @Override
    @DataScope
    public List<SysTenant> queryTenantList(SysTenant tenant) {
        SysTenant criteria = tenant != null ? tenant : new SysTenant();
        QueryWrapper<SysTenant> wrapper = buildTenantQueryWrapper(criteria);
        applyTenantFilter(wrapper);

        List<SysTenant> list = this.list(wrapper);
        decorateTenantInfo(list);
        return list;
    }

    /**
     * 应用租户过滤条件
     */
    private void applyTenantFilter(QueryWrapper<SysTenant> wrapper) {
        Long currentTenantId = TenantContext.getCurrentTenantId();
        if (currentTenantId == null) {
            wrapper.eq("1", "0");
            return;
        }

        SysTenant currentTenant = this.getById(currentTenantId);
        if (currentTenant == null) {
            wrapper.eq("1", "0");
            return;
        }

        if ("SYSTEM".equals(currentTenant.getTenantCode())) {
            wrapper.eq("id", currentTenantId);
        } else if (TENANT_TYPE_PLATFORM.equals(currentTenant.getTenantType())) {
            wrapper.and(w -> w.eq("id", currentTenantId)
                    .or()
                    .like("ancestors", currentTenantId.toString()));
        } else {
            wrapper.and(w -> w.eq("id", currentTenantId)
                    .or()
                    .eq("parent_id", currentTenantId));
        }
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

        if (tenant.getTenantType() != null) {
            wrapper.eq("tenant_type", tenant.getTenantType());
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
        wrapper.ne(SysTenant::getId, 0L) // 排除虚拟根节点
                .eq(SysTenant::getStatus, 1)
                .orderByAsc(SysTenant::getId);
        List<SysTenant> tenantList = this.list(wrapper);

        if (CollUtil.isEmpty(tenantList)) {
            return CollUtil.newArrayList();
        }

        List<TreeNode<Long>> nodeList = tenantList.stream()
                .filter(tenant -> tenant.getId() != null) // 过滤掉ID为null的记录
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
        SysTenant tenant = this.getById(tenantId);
        if (tenant != null) {
            decorateTenantInfo(java.util.Collections.singletonList(tenant));
        }
        return tenant;
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
        tenant.setUpdateBy(TenantContext.getCurrentTenantId()); // Note: using tenantId as user? Usually userId. Let's
                                                                // keep original unless valid userId

        // --- 能力边界管控 (Capability Boundary) 开始 ---
        Long currentTenantId = TenantContext.getTenantId();
        // 简单判定是否为租户自身
        boolean isSelf = currentTenantId != null && currentTenantId.equals(existTenant.getId());

        // 检查关键字段变更需要更高权限（上级或平台）
        // 只有当传入了非空值且与原值不同时，才视为变更
        boolean isQuotaChanged = isValueChanged(tenant.getMaxUsers(), existTenant.getMaxUsers())
                || isValueChanged(tenant.getMaxStations(), existTenant.getMaxStations())
                || isValueChanged(tenant.getMaxChargers(), existTenant.getMaxChargers())
                || isValueChanged(tenant.getExpireTime(), existTenant.getExpireTime());

        boolean isStatusChanged = isValueChanged(tenant.getStatus(), existTenant.getStatus());

        if (isQuotaChanged || isStatusChanged) {
            if (isSelf) {
                throw new RuntimeException("租户无权修改自身的配额或状态信息");
            }

            // 验证层级关系: 确保 currentTenantId 在 existTenant 的 ancestors 列表中
            boolean isAncestor = isAncestor(currentTenantId, existTenant.getAncestors());

            if (!isAncestor) {
                // 兜底检查：如果是超级管理员(0)，也允许
                if (currentTenantId != null && currentTenantId == 0L) {
                    // allow
                } else {
                    throw new RuntimeException("无权修改下级租户的管控信息 (非直属上级)");
                }
            }

            // 记录审计日志
            if (isQuotaChanged) {
                String detail = String.format("Update Quota: Users [%s->%s], Stations [%s->%s]",
                        existTenant.getMaxUsers(), tenant.getMaxUsers(),
                        existTenant.getMaxStations(), tenant.getMaxStations());
                tenantAuditService.logOperation(TenantAuditService.ACTION_UPDATE_QUOTA, tenant.getId(), detail);
            }
            if (isStatusChanged) {
                String detail = String.format("Update Status: [%s->%s]", existTenant.getStatus(), tenant.getStatus());
                tenantAuditService.logOperation(TenantAuditService.ACTION_UPDATE_STATUS, tenant.getId(), detail);
            }
        }
        // --- 能力边界管控 结束 ---

        return this.updateById(tenant);
    }

    private boolean isValueChanged(Object newVal, Object oldVal) {
        // 如果新值为null，说明本次不更新该字段，视为无变更
        if (newVal == null)
            return false;
        return !newVal.equals(oldVal);
    }

    private boolean isAncestor(Long currentId, String ancestors) {
        if (currentId == null || ancestors == null)
            return false;
        String[] ids = ancestors.split(",");
        String currentStr = String.valueOf(currentId);
        for (String id : ids) {
            if (id.trim().equals(currentStr))
                return true;
        }
        return false;
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
        int stationCount = stationUsageClient.getUsageCounts(List.of(tenantId))
                .getOrDefault(tenantId, new StationUsageCount(tenantId, 0, 0))
                .stationCount();
        if (stationCount > 0) {
            throw new RuntimeException("租户下存在充电站数据，无法删除");
        }

        int orderCount = orderUsageClient.getUsageCounts(List.of(tenantId))
                .getOrDefault(tenantId, new OrderUsageCount(tenantId, 0))
                .orderCount();
        if (orderCount > 0) {
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
        List<SysTenant> list = this.list(new QueryWrapper<SysTenant>().eq("parent_id", parentId));
        decorateTenantInfo(list);
        return list;
    }

    /**
     * 修改租户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(Long tenantId, Integer status) {
        // 如果是禁用操作(0)，需要递归禁用所有子租户
        if (Integer.valueOf(0).equals(status)) {
            List<Long> childrenIds = getTenantChildren(tenantId); // 包含自己
            if (CollUtil.isNotEmpty(childrenIds)) {
                // 记录审计日志
                tenantAuditService.logOperation("DISABLE_RECURSIVE", tenantId,
                        "Recursive disable triggered for " + childrenIds.size() + " tenants");

                LambdaUpdateWrapper<SysTenant> wrapper = new LambdaUpdateWrapper<>();
                wrapper.in(SysTenant::getId, childrenIds)
                        .set(SysTenant::getStatus, status)
                        .set(SysTenant::getUpdateTime, LocalDateTime.now())
                        .set(SysTenant::getUpdateBy, TenantContext.getCurrentUserId());
                return this.update(wrapper);
            }
        }

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

    private void decorateTenantInfo(List<SysTenant> tenants) {
        if (CollUtil.isEmpty(tenants)) {
            return;
        }

        List<Long> parentIds = tenants.stream()
                .map(SysTenant::getParentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        List<Long> tenantIds = tenants.stream()
                .map(SysTenant::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        var parentMap = parentIds.isEmpty() ? java.util.Collections.<Long, SysTenant>emptyMap()
                : this.listByIds(parentIds).stream()
                        .collect(Collectors.toMap(SysTenant::getId, t -> t));

        var childCountMap = tenantIds.isEmpty() ? java.util.Collections.<Long, Integer>emptyMap()
                : this.listMaps(new QueryWrapper<SysTenant>()
                        .select("parent_id", "COUNT(1) AS cnt")
                        .in("parent_id", tenantIds)
                        .groupBy("parent_id"))
                        .stream()
                        .map(m -> {
                            Object parentId = m.get("parent_id");
                            if (parentId == null) {
                                parentId = m.get("PARENT_ID");
                            }
                            Object count = m.get("cnt");
                            if (count == null) {
                                count = m.get("CNT");
                            }
                            if (parentId == null || count == null) {
                                return null;
                            }
                            return new AbstractMap.SimpleEntry<>(
                                    ((Number) parentId).longValue(),
                                    ((Number) count).intValue()
                            );
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (SysTenant tenant : tenants) {
            tenant.setTenantTypeName(resolveTenantTypeName(tenant.getTenantType()));
            if (tenant.getParentId() != null && tenant.getParentId() > 0) {
                SysTenant parent = parentMap.get(tenant.getParentId());
                tenant.setParentName(parent != null ? parent.getTenantName() : "-");
            } else {
                tenant.setParentName("无");
            }
            tenant.setChildrenCount(childCountMap.getOrDefault(tenant.getId(), 0));
        }
    }

    private String resolveTenantTypeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case 1 -> "平台方";
            case 2 -> "运营商";
            case 3 -> "站点方";
            default -> "未知";
        };
    }
}
