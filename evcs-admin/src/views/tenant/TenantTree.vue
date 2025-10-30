<template>
  <div class="tenant-tree">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租户层级结构</span>
          <el-button-group>
            <el-button :type="expandAll ? 'primary' : ''" @click="handleExpandAll">
              <el-icon><FolderOpened /></el-icon>
              全部展开
            </el-button>
            <el-button :type="!expandAll ? 'primary' : ''" @click="handleCollapseAll">
              <el-icon><Folder /></el-icon>
              全部折叠
            </el-button>
          </el-button-group>
        </div>
      </template>

      <div v-loading="loading">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          :default-expand-all="expandAll"
          node-key="id"
          :expand-on-click-node="false"
        >
        <template #default="{ node, data }">
          <div class="tree-node">
            <div class="node-content">
              <el-icon :size="18" style="margin-right: 8px;">
                <component :is="getNodeIcon(data.tenantType)" />
              </el-icon>
              <span class="node-label">{{ node.label }}</span>
              <el-tag :type="getTenantTypeTag(data.tenantType)" size="small" style="margin-left: 10px;">
                {{ getTenantTypeName(data.tenantType) }}
              </el-tag>
              <el-tag :type="data.status === 1 ? 'success' : 'danger'" size="small" style="margin-left: 5px;">
                {{ data.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </div>
            <div class="node-actions">
              <el-button link type="primary" size="small" @click="handleView(data)">查看</el-button>
              <el-button link type="primary" size="small" @click="handleEdit(data)">编辑</el-button>
              <el-button link type="success" size="small" @click="handleAddChild(data)">添加子租户</el-button>
            </div>
          </div>
        </template>
      </el-tree>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTenantTree } from '@/api/tenant'

const treeRef = ref()
const expandAll = ref(true)
const loading = ref(false)

const treeProps = {
  label: 'tenantName',
  children: 'children'
}

const treeData = ref<any[]>([])

// 加载租户树数据
const loadTenantTree = async () => {
  try {
    loading.value = true
    const response = await getTenantTree()
    
    if (response.code === 200 && response.data) {
      // 后端返回的是平铺的租户列表，需要构建树形结构
      const tenants = Array.isArray(response.data) ? response.data : []
      treeData.value = buildTree(tenants)
    } else {
      ElMessage.error(response.message || '加载租户树失败')
    }
  } catch (error) {
    console.error('加载租户树失败:', error)
    ElMessage.error('加载租户树失败')
  } finally {
    loading.value = false
  }
}

// 构建树形结构
const buildTree = (tenants: any[]) => {
  if (!tenants || tenants.length === 0) return []
  
  // 创建映射表
  const map = new Map()
  tenants.forEach(tenant => {
    map.set(tenant.tenantId, { 
      ...tenant, 
      id: tenant.tenantId,
      children: [] 
    })
  })
  
  // 构建树形结构
  const tree: any[] = []
  tenants.forEach(tenant => {
    const node = map.get(tenant.tenantId)
    if (tenant.parentId && map.has(tenant.parentId)) {
      // 有父节点，添加到父节点的 children
      const parent = map.get(tenant.parentId)
      parent.children.push(node)
    } else {
      // 没有父节点或父节点不在当前列表中，作为根节点
      tree.push(node)
    }
  })
  
  return tree
}

onMounted(() => {
  loadTenantTree()
})

const getTenantTypeName = (type: number) => {
  const typeMap: Record<number, string> = {
    1: '平台方',
    2: '运营商'
  }
  return typeMap[type] || '未知'
}

const getTenantTypeTag = (type: number) => {
  const tagMap: Record<number, string> = {
    1: 'danger',
    2: 'warning'
  }
  return tagMap[type] || ''
}

const getNodeIcon = (type: number) => {
  const iconMap: Record<number, string> = {
    1: 'OfficeBuilding',
    2: 'ShoppingCart'
  }
  return iconMap[type] || 'Folder'
}

const handleExpandAll = () => {
  expandAll.value = true
  const nodes = treeRef.value?.store._getAllNodes()
  nodes?.forEach((node: any) => {
    node.expanded = true
  })
}

const handleCollapseAll = () => {
  expandAll.value = false
  const nodes = treeRef.value?.store._getAllNodes()
  nodes?.forEach((node: any) => {
    node.expanded = false
  })
}

const handleView = (data: any) => {
  ElMessage.info('查看租户: ' + data.tenantName)
}

const handleEdit = (data: any) => {
  ElMessage.info('编辑租户: ' + data.tenantName)
}

const handleAddChild = (data: any) => {
  ElMessage.info('为 ' + data.tenantName + ' 添加子租户')
}
</script>

<style scoped>
.tenant-tree {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
}

.node-content {
  display: flex;
  align-items: center;
  flex: 1;
}

.node-label {
  font-size: 14px;
  font-weight: 500;
}

.node-actions {
  display: flex;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.3s;
}

.tree-node:hover .node-actions {
  opacity: 1;
}
</style>
