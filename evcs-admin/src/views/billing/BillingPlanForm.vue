<template>
  <div class="billing-plan-form">
    <el-page-header @back="handleBack" :content="isEdit ? '编辑计费方案' : '新增计费方案'" />

    <el-card style="margin-top: 20px;">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="方案编码" prop="planCode">
              <el-input v-model="formData.planCode" placeholder="请输入方案编码" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方案名称" prop="planName">
              <el-input v-model="formData.planName" placeholder="请输入方案名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计费类型" prop="planType">
              <el-radio-group v-model="formData.planType" @change="handleTypeChange">
                <el-radio label="TOU">分时计费</el-radio>
                <el-radio label="FIXED">固定计费</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应用充电站" prop="stationIds">
              <el-select v-model="formData.stationIds" multiple placeholder="请选择充电站" style="width: 100%;">
                <el-option
                  v-for="station in stations"
                  :key="station.id"
                  :label="station.name"
                  :value="station.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="方案描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入方案描述" />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>计费时段配置</span>
          <el-button type="primary" size="small" @click="handleAddSegment" :disabled="formData.planType === 'FIXED' && segments.length >= 1">
            <el-icon><Plus /></el-icon>
            添加时段
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="formData.planType === 'FIXED'"
        title="固定计费模式只需配置一个时段（全天有效）"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <div class="segments-editor">
        <div v-for="(segment, index) in segments" :key="segment.id" class="segment-item">
          <div class="segment-header">
            <span class="segment-title">时段 {{ index + 1 }}</span>
            <el-button link type="danger" size="small" @click="handleRemoveSegment(index)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>

          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="开始时间">
                <el-time-select
                  v-model="segment.startTime"
                  :start="'00:00'"
                  :step="'00:15'"
                  :end="'23:45'"
                  placeholder="开始时间"
                  :disabled="formData.planType === 'FIXED'"
                />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="结束时间">
                <el-time-select
                  v-model="segment.endTime"
                  :start="'00:00'"
                  :step="'00:15'"
                  :end="'24:00'"
                  placeholder="结束时间"
                  :disabled="formData.planType === 'FIXED'"
                />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="电费单价(元/kWh)">
                <el-input-number v-model="segment.electricityPrice" :precision="2" :step="0.1" :min="0" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="服务费单价(元/kWh)">
                <el-input-number v-model="segment.servicePrice" :precision="2" :step="0.1" :min="0" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="时段类型">
            <el-select v-model="segment.segmentType" placeholder="请选择时段类型" style="width: 200px;">
              <el-option label="尖峰时段" value="PEAK" />
              <el-option label="高峰时段" value="HIGH" />
              <el-option label="平时段" value="NORMAL" />
              <el-option label="低谷时段" value="VALLEY" />
            </el-select>
          </el-form-item>
        </div>

        <el-empty v-if="segments.length === 0" description="暂无时段配置，请点击添加时段按钮" :image-size="100" />
      </div>
    </el-card>

    <el-card style="margin-top: 20px;" v-if="segments.length > 0">
      <template #header>
        <span>24小时计费预览</span>
      </template>

      <div class="timeline-preview">
        <div class="timeline-header">
          <span class="time-label">00:00</span>
          <span class="time-label">06:00</span>
          <span class="time-label">12:00</span>
          <span class="time-label">18:00</span>
          <span class="time-label">24:00</span>
        </div>
        <div class="timeline-segments">
          <div
            v-for="(segment, index) in previewSegments"
            :key="index"
            class="preview-segment"
            :style="{
              width: segment.width + '%',
              backgroundColor: getSegmentColor(segment.segmentType)
            }"
            :title="`${segment.startTime} - ${segment.endTime}\n电费: ¥${segment.electricityPrice}/kWh\n服务费: ¥${segment.servicePrice}/kWh`"
          >
            <div class="segment-info">
              <div class="segment-time">{{ segment.startTime }}-{{ segment.endTime }}</div>
              <div class="segment-price">
                ¥{{ (segment.electricityPrice + segment.servicePrice).toFixed(2) }}/kWh
              </div>
            </div>
          </div>
        </div>
        <div class="legend">
          <div class="legend-item">
            <span class="legend-color" style="background: #f56c6c;"></span>
            <span>尖峰时段</span>
          </div>
          <div class="legend-item">
            <span class="legend-color" style="background: #e6a23c;"></span>
            <span>高峰时段</span>
          </div>
          <div class="legend-item">
            <span class="legend-color" style="background: #409eff;"></span>
            <span>平时段</span>
          </div>
          <div class="legend-item">
            <span class="legend-color" style="background: #67c23a;"></span>
            <span>低谷时段</span>
          </div>
        </div>
      </div>
    </el-card>

    <div style="margin-top: 20px; text-align: center;">
      <el-button @click="handleBack">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()

const isEdit = ref(!!route.params.id)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  planCode: '',
  planName: '',
  planType: 'TOU',
  stationIds: [],
  description: '',
  status: 1
})

const rules: FormRules = {
  planCode: [{ required: true, message: '请输入方案编码', trigger: 'blur' }],
  planName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  planType: [{ required: true, message: '请选择计费类型', trigger: 'change' }]
}

const stations = ref<any[]>([])

// 加载充电站列表
const loadStations = async () => {
  try {
    const { getStationList } = await import('@/api/station')
    const response = await getStationList({ current: 1, size: 100 })
    if (response.code === 200 && response.data) {
      const records = (response.data as any).records || response.data
      stations.value = Array.isArray(records) ? records.map((s: any) => ({
        id: s.id,
        name: s.stationName
      })) : []
    }
  } catch (error) {
    console.error('加载充电站列表失败:', error)
  }
}

// 在组件挂载时加载充电站列表
import { onMounted } from 'vue'
onMounted(() => {
  loadStations()
})

let segmentIdCounter = 1

const segments = ref([
  {
    id: segmentIdCounter++,
    startTime: '00:00',
    endTime: '08:00',
    electricityPrice: 0.5,
    servicePrice: 0.3,
    segmentType: 'VALLEY'
  }
])

const previewSegments = computed(() => {
  if (formData.planType === 'FIXED' && segments.value.length > 0) {
    const seg = segments.value[0]
    return [{
      ...seg,
      startTime: '00:00',
      endTime: '24:00',
      width: 100
    }]
  }

  return segments.value.map(seg => {
    const start = timeToMinutes(seg.startTime)
    const end = timeToMinutes(seg.endTime)
    const duration = end - start
    const width = (duration / 1440) * 100 // 1440 = 24 * 60
    return {
      ...seg,
      width
    }
  })
})

const timeToMinutes = (time: string) => {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

const getSegmentColor = (type: string) => {
  const colorMap: Record<string, string> = {
    PEAK: '#f56c6c',
    HIGH: '#e6a23c',
    NORMAL: '#409eff',
    VALLEY: '#67c23a'
  }
  return colorMap[type] || '#909399'
}

const handleTypeChange = () => {
  if (formData.planType === 'FIXED') {
    segments.value = [{
      id: segmentIdCounter++,
      startTime: '00:00',
      endTime: '24:00',
      electricityPrice: 0.8,
      servicePrice: 0.4,
      segmentType: 'NORMAL'
    }]
  } else {
    segments.value = []
  }
}

const handleAddSegment = () => {
  const lastSegment = segments.value[segments.value.length - 1]
  const newStartTime = lastSegment ? lastSegment.endTime : '00:00'

  segments.value.push({
    id: segmentIdCounter++,
    startTime: newStartTime,
    endTime: '24:00',
    electricityPrice: 0.8,
    servicePrice: 0.4,
    segmentType: 'NORMAL'
  })
}

const handleRemoveSegment = (index: number) => {
  segments.value.splice(index, 1)
}

const handleBack = () => {
  router.back()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate((valid) => {
    if (valid) {
      if (segments.value.length === 0) {
        ElMessage.warning('请至少添加一个计费时段')
        return
      }

      submitting.value = true
      setTimeout(() => {
        ElMessage.success('保存成功')
        submitting.value = false
        router.back()
      }, 1000)
    }
  })
}
</script>

<style scoped>
.billing-plan-form {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.segments-editor {
  padding: 10px 0;
}

.segment-item {
  padding: 20px;
  margin-bottom: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.segment-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.timeline-preview {
  padding: 20px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.timeline-segments {
  display: flex;
  height: 100px;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 20px;
}

.preview-segment {
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #fff;
  transition: all 0.3s;
  cursor: pointer;
}

.preview-segment:hover {
  opacity: 0.8;
  transform: scaleY(1.05);
}

.segment-info {
  color: #fff;
  text-align: center;
  font-size: 12px;
  padding: 5px;
}

.segment-time {
  font-weight: bold;
  margin-bottom: 5px;
}

.segment-price {
  font-size: 14px;
  font-weight: bold;
}

.legend {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color {
  width: 20px;
  height: 20px;
  border-radius: 4px;
}
</style>
