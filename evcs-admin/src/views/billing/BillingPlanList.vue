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
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchForm = reactive({
  name: '',
  type: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 30
})

const tableData = ref([
  {
    planId: 1,
    planCode: 'BP001',
    planName: '标准分时计费方案',
    planType: 'TOU',
    segments: 4,
    stationCount: 12,
    status: 1
  },
  {
    planId: 2,
    planCode: 'BP002',
    planName: '固定计费方案',
    planType: 'FIXED',
    segments: 1,
    stationCount: 5,
    status: 1
  }
])

const previewVisible = ref(false)
const previewSegments = ref([
  {
    startTime: '00:00',
    endTime: '08:00',
    price: 0.5,
    type: '低谷时段',
    width: 33.33,
    color: '#67c23a'
  },
  {
    startTime: '08:00',
    endTime: '12:00',
    price: 0.9,
    type: '高峰时段',
    width: 16.67,
    color: '#e6a23c'
  },
  {
    startTime: '12:00',
    endTime: '18:00',
    price: 0.7,
    type: '平时段',
    width: 25,
    color: '#409eff'
  },
  {
    startTime: '18:00',
    endTime: '22:00',
    price: 1.2,
    type: '尖峰时段',
    width: 16.67,
    color: '#f56c6c'
  },
  {
    startTime: '22:00',
    endTime: '24:00',
    price: 0.5,
    type: '低谷时段',
    width: 8.33,
    color: '#67c23a'
  }
])

const handleSearch = () => {
  pagination.currentPage = 1
  ElMessage.success('查询成功')
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.type = ''
}

const handleAdd = () => {
  ElMessage.info('新增计费方案功能')
}

const handleView = (row: any) => {
  ElMessage.info('查看计费方案: ' + row.planName)
}

const handleEdit = (row: any) => {
  ElMessage.info('编辑计费方案: ' + row.planName)
}

const handlePreview = (_row: any) => {
  previewVisible.value = true
}

const handleDelete = (_row: any) => {
  ElMessageBox.confirm('确定要删除该计费方案吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  })
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
}
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
