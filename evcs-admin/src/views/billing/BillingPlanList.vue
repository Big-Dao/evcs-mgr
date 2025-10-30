<template>
  <div class="billing-plan-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>计费方案列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增计费方案
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="方案名称">
          <el-input v-model="searchForm.name" placeholder="请输入方案名称" clearable />
        </el-form-item>
        <el-form-item label="计费类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable>
            <el-option label="分时计费" value="TOU" />
            <el-option label="固定计费" value="FIXED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="planId" label="方案ID" width="80" />
        <el-table-column prop="planCode" label="方案编码" width="120" />
        <el-table-column prop="planName" label="方案名称" />
        <el-table-column prop="planType" label="计费类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.planType === 'TOU' ? 'success' : 'warning'" size="small">
              {{ row.planType === 'TOU' ? '分时计费' : '固定计费' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="segments" label="时段数" width="100" />
        <el-table-column prop="stationCount" label="应用站点" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="handlePreview(row)">预览</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" title="计费方案预览 - 24小时时间轴" width="80%">
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
            class="segment"
            :style="{
              width: segment.width + '%',
              backgroundColor: segment.color
            }"
          >
            <div class="segment-info">
              <div class="segment-time">{{ segment.startTime }} - {{ segment.endTime }}</div>
              <div class="segment-price">¥{{ segment.price }}/kWh</div>
              <div class="segment-type">{{ segment.type }}</div>
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
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const searchForm = reactive({
  name: '',
  type: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<any[]>([])
const previewVisible = ref(false)
const previewSegments = ref<any[]>([])

const loadBillingPlans = async () => {
  try {
    const { getBillingPlanPage } = await import('@/api/billing')
    const response = await getBillingPlanPage({
      current: pagination.currentPage,
      size: pagination.pageSize
    })
    
    if (response.code === 200 && response.data) {
      const page = response.data
      tableData.value = (page.records || []).map((plan: any) => ({
        planId: plan.id,
        planCode: plan.planCode || `BP${String(plan.id).padStart(3, '0')}`,
        planName: plan.planName,
        planType: plan.planType || 'TOU',
        segments: 0, // TODO: Backend needs to provide segment count
        stationCount: 0, // TODO: Backend needs to provide station count
        status: plan.status
      }))
      pagination.total = page.total || 0
    }
  } catch (error) {
    console.error('加载计费方案列表失败:', error)
    ElMessage.error('加载计费方案列表失败')
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadBillingPlans()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.type = ''
  loadBillingPlans()
}

const handleAdd = () => {
  router.push('/billing-plans/new')
}

const handleView = (row: any) => {
  ElMessage.info('查看计费方案: ' + row.planName)
}

const handleEdit = (row: any) => {
  router.push(`/billing-plans/${row.planId}/edit`)
}

const handlePreview = async (row: any) => {
  try {
    const { getBillingPlanSegments } = await import('@/api/billing')
    const response = await getBillingPlanSegments(row.planId)
    
    if (response.code === 200 && response.data) {
      const segments = response.data
      
      // Convert segments to preview format
      previewSegments.value = segments.map((seg: any) => {
        const startHour = parseInt(seg.startTime.split(':')[0])
        const endHour = parseInt(seg.endTime.split(':')[0])
        const duration = endHour - startHour
        const width = (duration / 24) * 100
        
        // Determine color based on price
        const totalPrice = seg.electricityPrice + seg.servicePrice
        let color = '#409eff' // 平时段
        let type = '平时段'
        if (totalPrice >= 1.2) {
          color = '#f56c6c' // 尖峰时段
          type = '尖峰时段'
        } else if (totalPrice >= 0.9) {
          color = '#e6a23c' // 高峰时段
          type = '高峰时段'
        } else if (totalPrice <= 0.6) {
          color = '#67c23a' // 低谷时段
          type = '低谷时段'
        }
        
        return {
          startTime: seg.startTime,
          endTime: seg.endTime,
          price: totalPrice,
          type,
          width,
          color
        }
      })
    }
    
    previewVisible.value = true
  } catch (error) {
    console.error('加载计费方案分段失败:', error)
    ElMessage.warning('该计费方案暂无分段数据')
    previewVisible.value = false
  }
}

const handleDelete = async (row: any) => {
  ElMessageBox.confirm('确定要删除该计费方案吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const { deleteBillingPlan } = await import('@/api/billing')
      const response = await deleteBillingPlan(row.planId)
      if (response.code === 200) {
        ElMessage.success('删除成功')
        loadBillingPlans()
      } else {
        ElMessage.error(response.message || '删除失败')
      }
    } catch (error) {
      console.error('删除计费方案失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // User cancelled
  })
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadBillingPlans()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadBillingPlans()
}

onMounted(() => {
  loadBillingPlans()
})
</script>

<style scoped>
.billing-plan-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
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

.segment {
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #fff;
  transition: all 0.3s;
  cursor: pointer;
}

.segment:hover {
  opacity: 0.8;
  transform: scaleY(1.05);
}

.segment-info {
  color: #fff;
  text-align: center;
  font-size: 12px;
}

.segment-time {
  font-weight: bold;
  margin-bottom: 5px;
}

.segment-price {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 5px;
}

.segment-type {
  font-size: 11px;
  opacity: 0.9;
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
