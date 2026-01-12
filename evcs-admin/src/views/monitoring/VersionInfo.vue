<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, CopyDocument } from '@element-plus/icons-vue'
import {
  getFrontendVersionInfo,
  type FrontendVersionInfo
} from '@/api/version'
import {
  getSystemOverview,
  getServiceVersions,
  type ServiceHealth,
  type ServiceVersion
} from '@/api/monitoring'

type ComponentKey = 'gateway' | 'auth' | 'tenant' | 'station' | 'order' | 'payment'

interface ComponentRow {
  key: ComponentKey
  name: string
  serviceName: string
  healthStatus?: string
  version?: ServiceVersion
  error?: string
}

const loading = reactive({
  frontend: false,
  components: false
})

const frontend = ref<FrontendVersionInfo | null>(null)

const rows = ref<ComponentRow[]>([
  { key: 'gateway', name: 'evcs-gateway', serviceName: 'evcs-gateway' },
  { key: 'auth', name: 'evcs-auth', serviceName: 'evcs-auth' },
  { key: 'tenant', name: 'evcs-tenant', serviceName: 'evcs-tenant' },
  { key: 'station', name: 'evcs-station', serviceName: 'evcs-station' },
  { key: 'order', name: 'evcs-order', serviceName: 'evcs-order' },
  { key: 'payment', name: 'evcs-payment', serviceName: 'evcs-payment' }
])

const safeJson = (v: unknown) => {
  try {
    return JSON.stringify(v, null, 2)
  } catch {
    return String(v)
  }
}

const pickBestVersion = (versions: ServiceVersion[]): ServiceVersion | undefined => {
  if (!versions.length) return undefined
  return versions.find(v => v.reachable) || versions[0]
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    console.error('copy failed:', e)
    ElMessage.error('复制失败')
  }
}

const loadFrontend = async () => {
  loading.frontend = true
  try {
    frontend.value = await getFrontendVersionInfo()
  } catch (e: any) {
    console.error('load frontend version failed:', e)
    ElMessage.warning('加载前端版本信息失败')
    frontend.value = null
  } finally {
    loading.frontend = false
  }
}

const loadComponents = async () => {
  loading.components = true
  try {
    const [overviewResp, versionsResp] = await Promise.all([
      getSystemOverview(),
      getServiceVersions()
    ])

    const healthList: ServiceHealth[] = overviewResp.data?.services || []
    const versionList: ServiceVersion[] = versionsResp.data || []

    rows.value.forEach((row) => {
      row.error = undefined
      row.healthStatus = undefined
      row.version = undefined

      const health = healthList.find((h) => h.serviceName === row.serviceName)
      row.healthStatus = health?.status || 'UNKNOWN'

      const candidates = versionList.filter((v) => v.serviceName === row.serviceName)
      const picked = pickBestVersion(candidates)

      if (!picked) {
        row.error = 'NO_VERSION_INFO'
        return
      }

      row.version = picked
      if (!picked.reachable) {
        row.error = picked.error || 'UNREACHABLE'
      }
    })
  } finally {
    loading.components = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadFrontend(), loadComponents()])
}

const tableData = computed(() => {
  return rows.value.map((r) => {
    const v = r.version

    return {
      key: r.key,
      name: r.name,
      healthStatus: r.healthStatus || 'UNKNOWN',
      imageTag: v?.imageTag || '-',
      registry: v?.registry || '-',
      buildVersion: v?.buildVersion || '-',
      gitCommit: v?.gitCommit || '-',
      buildTime: v?.buildTime || '-',
      error: r.error || ''
    }
  })
})

onMounted(() => {
  refreshAll()
})
</script>

<template>
  <div class="version-info">
    <div class="page-header">
      <div class="title">运行版本信息</div>
      <el-button :icon="Refresh" @click="refreshAll" :loading="loading.frontend || loading.components">
        刷新
      </el-button>
    </div>

    <el-card class="block" shadow="never">
      <template #header>
        <div class="card-header">
          <span>前端（admin-frontend）</span>
          <el-button
            v-if="frontend"
            link
            type="primary"
            :icon="CopyDocument"
            @click="copyToClipboard(JSON.stringify(frontend, null, 2))"
          >
            复制
          </el-button>
        </div>
      </template>

      <div v-loading="loading.frontend" class="kv">
        <div class="kv-row"><span class="k">commit</span><span class="v">{{ frontend?.commit || '-' }}</span></div>
        <div class="kv-row"><span class="k">branch</span><span class="v">{{ frontend?.branch || '-' }}</span></div>
        <div class="kv-row"><span class="k">buildTime</span><span class="v">{{ frontend?.buildTime || '-' }}</span></div>
        <div class="kv-row"><span class="k">buildNumber</span><span class="v">{{ frontend?.buildNumber ?? '-' }}</span></div>
      </div>
    </el-card>

    <el-card class="block" shadow="never">
      <template #header>
        <div class="card-header">
          <span>后端组件（通过 Monitoring 聚合接口）</span>
        </div>
      </template>

      <el-table v-loading="loading.components" :data="tableData" size="small" style="width: 100%">
        <el-table-column prop="name" label="组件" min-width="160" />
        <el-table-column prop="healthStatus" label="健康" width="120">
          <template #default="{ row }">
            <el-tag
              v-if="row.healthStatus === 'UP'"
              type="success"
              effect="plain"
            >
              UP
            </el-tag>
            <el-tag
              v-else-if="row.healthStatus === 'DOWN'"
              type="danger"
              effect="plain"
            >
              DOWN
            </el-tag>
            <el-tag v-else type="warning" effect="plain">{{ row.healthStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="imageTag" label="imageTag" width="110" />
        <el-table-column prop="buildVersion" label="build.version" min-width="140" />
        <el-table-column prop="gitCommit" label="git.commit" min-width="140" />
        <el-table-column prop="buildTime" label="build.time" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :icon="CopyDocument"
              @click="copyToClipboard(safeJson(rows.find(r => r.key === row.key)?.version || { error: row.error }))"
            >
              复制详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="hint">
        <div>说明：</div>
        <div>1) 该页面只调用聚合接口：/api/monitoring/overview 与 /api/monitoring/versions。</div>
        <div>2) 如果 build/git 信息为空，请在对应服务开启 Actuator info 的 build/git 贡献项。</div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.version-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.block {
  border-radius: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kv {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.kv-row {
  display: flex;
  gap: 12px;
}

.k {
  width: 100px;
  color: #606266;
}

.v {
  flex: 1;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  line-height: 18px;
}
</style>
