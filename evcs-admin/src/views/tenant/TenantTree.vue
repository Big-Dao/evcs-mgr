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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const treeRef = ref()
const expandAll = ref(true)

const treeProps = {
  label: 'tenantName',
  children: 'children'
}

const treeData = ref([
  {
    id: 1,
    tenantCode: 'T001',
    tenantName: '总部平台',
    tenantType: 'PLATFORM',
    status: 1,
    children: [
      {
        id: 2,
        tenantCode: 'T002',
        tenantName: '华东运营商',
        tenantType: 'OPERATOR',
        status: 1,
        children: [
          {
            id: 4,
            tenantCode: 'T004',
            tenantName: '上海充电站',
            tenantType: 'STATION',
            status: 1
          },
          {
            id: 5,
            tenantCode: 'T005',
            tenantName: '杭州充电站',
            tenantType: 'STATION',
            status: 1
          },
          {
            id: 6,
            tenantCode: 'T006',
            tenantName: '南京充电站',
            tenantType: 'STATION',
            status: 1
          }
        ]
      },
      {
        id: 3,
        tenantCode: 'T003',
        tenantName: '华南运营商',
        tenantType: 'OPERATOR',
        status: 1,
        children: [
          {
            id: 7,
            tenantCode: 'T007',
            tenantName: '深圳充电站',
            tenantType: 'STATION',
            status: 1
          },
          {
            id: 8,
            tenantCode: 'T008',
            tenantName: '广州充电站',
            tenantType: 'STATION',
            status: 1
          }
        ]
      }
    ]
  }
])

const getTenantTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    PLATFORM: '平台方',
    OPERATOR: '运营商',
    STATION: '站点方'
  }
  return typeMap[type] || type
}

const getTenantTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    PLATFORM: 'danger',
    OPERATOR: 'warning',
    STATION: 'success'
  }
  return tagMap[type] || ''
}

const getNodeIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    PLATFORM: 'OfficeBuilding',
    OPERATOR: 'ShoppingCart',
    STATION: 'Location'
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
