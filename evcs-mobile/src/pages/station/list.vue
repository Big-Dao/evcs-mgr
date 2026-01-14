<template>
  <view class="page-container">
    <!-- 搜索栏 -->
    <view class="search-header">
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          type="text"
          v-model="keyword"
          placeholder="搜索充电站名称/地址"
          confirm-type="search"
          :focus="isFocus"
          @confirm="handleSearch"
        />
        <text v-if="keyword" class="clear-icon" @click="clearKeyword">✕</text>
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-bar">
      <view 
        class="filter-item" 
        :class="{ active: activeFilter === 'distance' }"
        @click="setFilter('distance')"
      >
        <text>距离</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: activeFilter === 'price' }"
        @click="setFilter('price')"
      >
        <text>价格</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: activeFilter === 'available' }"
        @click="setFilter('available')"
      >
        <text>空闲</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: showType }"
        @click="toggleTypeFilter"
      >
        <text>类型</text>
        <text class="filter-arrow">▼</text>
      </view>
    </view>

    <!-- 类型筛选弹窗 -->
    <view v-if="showType" class="type-filter">
      <view 
        class="type-item" 
        :class="{ active: chargerType === '' }"
        @click="setChargerType('')"
      >
        全部
      </view>
      <view 
        class="type-item" 
        :class="{ active: chargerType === 'DC' }"
        @click="setChargerType('DC')"
      >
        快充
      </view>
      <view 
        class="type-item" 
        :class="{ active: chargerType === 'AC' }"
        @click="setChargerType('AC')"
      >
        慢充
      </view>
    </view>

    <!-- 站点列表 -->
    <scroll-view 
      class="station-scroll"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
    >
      <view v-if="loading && stations.length === 0" class="loading-wrapper">
        <text class="loading-text">加载中...</text>
      </view>
      
      <view v-else-if="stations.length === 0" class="empty-state">
        <text class="empty-text">未找到充电站</text>
        <view class="btn btn-outline btn-sm empty-action" @click="handleSearch">重新搜索</view>
      </view>
      
      <view v-else class="station-list">
        <view 
          v-for="station in stations" 
          :key="station.id" 
          class="station-card card"
          @click="goToDetail(station.id)"
        >
          <image 
            class="station-image" 
            :src="station.images?.[0] || '/static/images/station-default.png'"
            mode="aspectFill"
          />
          <view class="station-content">
            <view class="station-name ellipsis">{{ station.name }}</view>
            <view class="station-address ellipsis">{{ station.address }}</view>
            
            <view class="station-tags">
              <text class="tag tag-success" v-if="station.fastChargerCount">快充</text>
              <text class="tag tag-info" v-if="station.slowChargerCount">慢充</text>
            </view>
            
            <view class="station-footer">
              <view class="station-meta">
                <text class="station-distance">{{ formatDistance(station.distance || 0) }}</text>
                <text class="station-available">
                  空闲 <text class="text-success">{{ station.availableCount }}</text>/{{ station.chargerCount }}
                </text>
              </view>
              <view class="station-price">
                <text class="price-value">¥{{ station.minPrice?.toFixed(2) }}</text>
                <text class="price-unit">/度起</text>
              </view>
            </view>
          </view>
        </view>
        
        <view v-if="loading" class="loading-more">
          <text>加载中...</text>
        </view>
        
        <view v-if="noMore && stations.length > 0" class="no-more">
          <text>没有更多了</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useLocationStore } from '@/stores'
import { searchStations } from '@/api/station'
import { formatDistance } from '@/utils/format'
import type { Station, ChargerType } from '@/types'

const locationStore = useLocationStore()

const keyword = ref('')
const isFocus = ref(false)
const activeFilter = ref('distance')
const showType = ref(false)
const chargerType = ref<ChargerType | ''>('')

const stations = ref<Station[]>([])
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)
const currentPage = ref(1)
const pageSize = 10

onLoad((options) => {
  if (options?.focus === 'true') {
    isFocus.value = true
  }
})

onMounted(async () => {
  await locationStore.updateLocation()
  await loadStations()
})

async function loadStations(isRefresh = false) {
  if (loading.value) return
  
  if (isRefresh) {
    currentPage.value = 1
    noMore.value = false
  }
  
  loading.value = true
  
  try {
    const location = locationStore.currentLocation
    const res = await searchStations({
      keyword: keyword.value || undefined,
      latitude: location?.latitude,
      longitude: location?.longitude,
      chargerType: chargerType.value || undefined,
      current: currentPage.value,
      size: pageSize
    })
    
    if (res.data) {
      const newStations = res.data.records || []
      if (isRefresh) {
        stations.value = newStations
      } else {
        stations.value = [...stations.value, ...newStations]
      }
      
      if (newStations.length < pageSize) {
        noMore.value = true
      }
    }
  } catch (error) {
    console.error('Failed to load stations:', error)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function handleSearch() {
  loadStations(true)
}

function clearKeyword() {
  keyword.value = ''
  loadStations(true)
}

function setFilter(filter: string) {
  activeFilter.value = filter
  showType.value = false
  // 根据筛选条件重新排序
  loadStations(true)
}

function toggleTypeFilter() {
  showType.value = !showType.value
}

function setChargerType(type: ChargerType | '') {
  chargerType.value = type
  showType.value = false
  loadStations(true)
}

async function onRefresh() {
  refreshing.value = true
  await loadStations(true)
}

async function onLoadMore() {
  if (noMore.value || loading.value) return
  currentPage.value++
  await loadStations()
}

function goToDetail(stationId: number) {
  uni.navigateTo({ url: `/pages/station/detail?id=${stationId}` })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.search-header {
  padding: $spacing-md $spacing-lg;
  background-color: $bg-color-white;
}

.search-bar {
  display: flex;
  align-items: center;
  height: 72rpx;
  background-color: $fill-color-light;
  border-radius: $radius-full;
  padding: 0 $spacing-lg;
  
  .search-icon {
    font-size: $font-size-lg;
    margin-right: $spacing-sm;
  }
  
  .search-input {
    flex: 1;
    font-size: $font-size-md;
    color: $text-color;
  }
  
  .clear-icon {
    font-size: $font-size-md;
    color: $text-color-tertiary;
    padding: $spacing-sm;
  }
}

.filter-bar {
  display: flex;
  background-color: $bg-color-white;
  border-bottom: 1px solid $border-color-light;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80rpx;
  font-size: $font-size-md;
  color: $text-color-secondary;
  
  &.active {
    color: $primary-color;
    font-weight: 500;
  }
  
  .filter-arrow {
    font-size: $font-size-xs;
    margin-left: $spacing-xs;
  }
}

.type-filter {
  display: flex;
  background-color: $bg-color-white;
  padding: $spacing-md $spacing-lg;
  gap: $spacing-md;
  border-bottom: 1px solid $border-color-light;
}

.type-item {
  padding: $spacing-sm $spacing-lg;
  background-color: $fill-color-light;
  border-radius: $radius-full;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  
  &.active {
    background-color: rgba($primary-color, 0.1);
    color: $primary-color;
  }
}

.station-scroll {
  flex: 1;
  height: calc(100vh - 280rpx);
}

.station-list {
  padding: $spacing-md $spacing-lg;
}

.station-card {
  display: flex;
  margin-bottom: $spacing-md;
  padding: $spacing-md;
}

.station-image {
  width: 180rpx;
  height: 180rpx;
  border-radius: $radius-md;
  flex-shrink: 0;
}

.station-content {
  flex: 1;
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
  min-width: 0;
  
  .station-name {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
    margin-bottom: $spacing-xs;
  }
  
  .station-address {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
    margin-bottom: $spacing-sm;
  }
}

.station-tags {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.station-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: auto;
}

.station-meta {
  display: flex;
  flex-direction: column;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  
  .station-distance {
    margin-bottom: $spacing-xs;
  }
}

.station-price {
  display: flex;
  align-items: baseline;
  
  .price-value {
    font-size: $font-size-xl;
    font-weight: 600;
    color: $primary-color;
  }
  
  .price-unit {
    font-size: $font-size-xs;
    color: $text-color-tertiary;
  }
}

.loading-more,
.no-more {
  text-align: center;
  padding: $spacing-lg;
  color: $text-color-tertiary;
  font-size: $font-size-sm;
}
</style>
