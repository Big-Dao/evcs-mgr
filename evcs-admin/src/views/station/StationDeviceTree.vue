<template>
  <div class="station-device-tree">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>站点-桩-枪树形视图</span>
          <div class="actions">
            <el-input
              v-model="filterText"
              placeholder="筛选（站点/桩编码/名称）"
              clearable
              style="width: 260px"
            />
            <el-button type="primary" :loading="loading" @click="loadTree">刷新</el-button>
          </div>
        </div>
      </template>

      <el-tree
        ref="treeRef"
        v-loading="loading"
        :data="treeData"
        node-key="key"
        :props="treeProps"
        :filter-node-method="filterNode"
        default-expand-all
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getChargingStationTree } from '@/api/station'
import type { ChargingStationTreeDTO } from '@/api/station'
import type { ElTree } from 'element-plus'

type TreeNode = {
  key: string
  label: string
  raw?: unknown
  children?: TreeNode[]
}

const loading = ref(false)
const filterText = ref('')
const treeData = ref<TreeNode[]>([])
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)

const treeProps = computed(() => ({
  children: 'children',
  label: 'label'
}))

const buildTree = (data: ChargingStationTreeDTO[]): TreeNode[] => {
  return (data || []).map((station) => {
    const stationNode: TreeNode = {
      key: `station:${station.stationId}`,
      label: `站点 ${station.stationName}（${station.stationCode}）`,
      raw: station,
      children: (station.chargers || []).map((charger) => {
        const chargerNode: TreeNode = {
          key: `charger:${charger.chargerId}`,
          label: `充电桩 ${charger.chargerName || ''}（${charger.chargerCode}）`,
          raw: charger,
          children: (charger.connectors || []).map((connector) => ({
            key: `connector:${charger.chargerId}:${connector.connectorNo}`,
            label: `枪口 #${connector.connectorNo}（status=${connector.status}）`,
            raw: connector
          }))
        }
        return chargerNode
      })
    }
    return stationNode
  })
}

const loadTree = async () => {
  loading.value = true
  try {
    const resp = await getChargingStationTree()
    treeData.value = buildTree(resp.data || [])
  } catch (e) {
    console.error('加载树形数据失败:', e)
    ElMessage.error('加载树形数据失败')
  } finally {
    loading.value = false
  }
}

const filterNode = (value: string, data: TreeNode) => {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

onMounted(loadTree)
</script>

<style scoped>
.station-device-tree {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
