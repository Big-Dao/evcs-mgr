<template>
  <view class="page-container">
    <!-- 相机扫描区域 -->
    <view class="scan-area">
      <camera 
        v-if="showCamera"
        class="scan-camera"
        device-position="back"
        flash="auto"
        @error="onCameraError"
      />
      
      <!-- 扫描框 -->
      <view class="scan-frame">
        <view class="frame-corner top-left"></view>
        <view class="frame-corner top-right"></view>
        <view class="frame-corner bottom-left"></view>
        <view class="frame-corner bottom-right"></view>
        <view class="scan-line"></view>
      </view>
      
      <!-- 顶部操作栏 -->
      <view class="scan-header safe-area-top">
        <view class="back-btn" @click="goBack">
          <text>←</text>
        </view>
        <text class="scan-title">扫码充电</text>
        <view class="flash-btn" @click="toggleFlash">
          <text>{{ flashOn ? '🔦' : '💡' }}</text>
        </view>
      </view>
      
      <!-- 提示文字 -->
      <view class="scan-tip">
        <text>将二维码放入框内，即可自动扫描</text>
      </view>
    </view>
    
    <!-- 底部操作 -->
    <view class="bottom-actions safe-area-bottom">
      <view class="action-item" @click="scanQRCode">
        <view class="action-icon">📷</view>
        <text class="action-text">扫一扫</text>
      </view>
      <view class="action-item" @click="inputCode">
        <view class="action-icon">⌨️</view>
        <text class="action-text">手动输入</text>
      </view>
      <view class="action-item" @click="openAlbum">
        <view class="action-icon">🖼️</view>
        <text class="action-text">相册</text>
      </view>
    </view>
    
    <!-- 手动输入弹窗 -->
    <view class="input-modal" v-if="showInputModal" @click.self="closeInputModal">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">输入充电桩编号</text>
          <text class="modal-close" @click="closeInputModal">✕</text>
        </view>
        <view class="modal-body">
          <input
            class="code-input"
            type="text"
            v-model="inputChargerCode"
            placeholder="请输入充电桩编号"
            maxlength="20"
          />
        </view>
        <view class="modal-footer">
          <view class="btn btn-secondary" @click="closeInputModal">取消</view>
          <view class="btn btn-primary" @click="confirmInput">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore, useChargingStore } from '@/stores'
import { startCharging } from '@/api/order'
import { getChargerDetail } from '@/api/station'

const userStore = useUserStore()
const chargingStore = useChargingStore()

const showCamera = ref(true)
const flashOn = ref(false)
const showInputModal = ref(false)
const inputChargerCode = ref('')
const isProcessing = ref(false)

onMounted(() => {
  checkLogin()
})

onShow(() => {
  showCamera.value = true
})

onHide(() => {
  showCamera.value = false
})

onUnmounted(() => {
  showCamera.value = false
})

function checkLogin() {
  if (!userStore.isLoggedIn) {
    uni.redirectTo({ url: '/pages/login/index' })
  }
}

function goBack() {
  uni.navigateBack()
}

function toggleFlash() {
  flashOn.value = !flashOn.value
}

function onCameraError(e: unknown) {
  console.error('Camera error:', e)
  uni.showToast({
    title: '相机启动失败',
    icon: 'none'
  })
}

async function scanQRCode() {
  if (isProcessing.value) return
  
  try {
    const res = await uni.scanCode({
      scanType: ['qrCode'],
      onlyFromCamera: false
    })
    
    await handleScanResult(res.result)
  } catch (error) {
    console.error('Scan error:', error)
  }
}

async function openAlbum() {
  try {
    const res = await uni.scanCode({
      scanType: ['qrCode'],
      onlyFromCamera: false
    })
    
    await handleScanResult(res.result)
  } catch (error) {
    console.error('Album scan error:', error)
  }
}

function inputCode() {
  showInputModal.value = true
  inputChargerCode.value = ''
}

function closeInputModal() {
  showInputModal.value = false
}

async function confirmInput() {
  if (!inputChargerCode.value.trim()) {
    uni.showToast({
      title: '请输入充电桩编号',
      icon: 'none'
    })
    return
  }
  
  closeInputModal()
  await handleChargerCode(inputChargerCode.value.trim())
}

async function handleScanResult(result: string) {
  if (isProcessing.value) return
  
  console.log('Scan result:', result)
  
  // 解析二维码内容，提取充电桩编号
  let chargerCode = result
  
  // 如果是 URL 格式，提取参数
  if (result.includes('charger=')) {
    const match = result.match(/charger=([^&]+)/)
    if (match) {
      chargerCode = match[1]
    }
  } else if (result.includes('code=')) {
    const match = result.match(/code=([^&]+)/)
    if (match) {
      chargerCode = match[1]
    }
  }
  
  await handleChargerCode(chargerCode)
}

async function handleChargerCode(code: string) {
  if (isProcessing.value) return
  
  isProcessing.value = true
  uni.showLoading({ title: '查询中...' })
  
  try {
    // 根据编号查询充电桩
    const chargerRes = await getChargerDetail(Number(code))
    
    if (!chargerRes.data) {
      uni.showToast({
        title: '未找到该充电桩',
        icon: 'none'
      })
      return
    }
    
    const charger = chargerRes.data
    
    // 检查充电桩状态
    if (charger.status !== 'AVAILABLE') {
      uni.showToast({
        title: '该充电桩暂不可用',
        icon: 'none'
      })
      return
    }
    
    // 选择第一个可用的充电枪
    const availableConnector = charger.connectors.find(c => c.status === 'AVAILABLE')
    if (!availableConnector) {
      uni.showToast({
        title: '暂无可用充电枪',
        icon: 'none'
      })
      return
    }
    
    // 确认开始充电
    uni.showModal({
      title: '确认充电',
      content: `充电桩：${charger.name}\n电价：¥${charger.price.toFixed(2)}/度`,
      confirmText: '开始充电',
      success: async (modalRes) => {
        if (modalRes.confirm) {
          await doStartCharging(charger.stationId, charger.id, availableConnector.id)
        }
      }
    })
  } catch (error) {
    console.error('Handle charger code error:', error)
    uni.showToast({
      title: '查询失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
    isProcessing.value = false
  }
}

async function doStartCharging(stationId: number, chargerId: number, connectorId: number) {
  uni.showLoading({ title: '正在启动...' })
  
  try {
    const res = await startCharging({
      stationId,
      chargerId,
      connectorId
    })
    
    if (res.data) {
      chargingStore.setActiveOrder(res.data)
      
      uni.showToast({
        title: '充电启动成功',
        icon: 'success'
      })
      
      // 跳转到充电进行中页面
      setTimeout(() => {
        uni.redirectTo({ url: '/pages/charging/session' })
      }, 1500)
    }
  } catch (error) {
    console.error('Start charging error:', error)
    uni.showToast({
      title: '启动失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.page-container {
  height: 100vh;
  background-color: #000000;
  display: flex;
  flex-direction: column;
}

.scan-area {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scan-camera {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.scan-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-lg;
  z-index: 10;
}

.back-btn,
.flash-btn {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: #FFFFFF;
}

.scan-title {
  font-size: $font-size-lg;
  font-weight: 500;
  color: #FFFFFF;
}

.scan-frame {
  position: relative;
  width: 500rpx;
  height: 500rpx;
}

.frame-corner {
  position: absolute;
  width: 40rpx;
  height: 40rpx;
  border-color: $primary-color;
  border-style: solid;
  border-width: 0;
  
  &.top-left {
    top: 0;
    left: 0;
    border-top-width: 6rpx;
    border-left-width: 6rpx;
  }
  
  &.top-right {
    top: 0;
    right: 0;
    border-top-width: 6rpx;
    border-right-width: 6rpx;
  }
  
  &.bottom-left {
    bottom: 0;
    left: 0;
    border-bottom-width: 6rpx;
    border-left-width: 6rpx;
  }
  
  &.bottom-right {
    bottom: 0;
    right: 0;
    border-bottom-width: 6rpx;
    border-right-width: 6rpx;
  }
}

.scan-line {
  position: absolute;
  left: 20rpx;
  right: 20rpx;
  height: 4rpx;
  background: linear-gradient(90deg, transparent, $primary-color, transparent);
  animation: scan 2s linear infinite;
}

@keyframes scan {
  0% { top: 20rpx; }
  100% { top: calc(100% - 20rpx); }
}

.scan-tip {
  position: absolute;
  bottom: 200rpx;
  left: 0;
  right: 0;
  text-align: center;
  
  text {
    color: rgba(255, 255, 255, 0.7);
    font-size: $font-size-md;
  }
}

.bottom-actions {
  display: flex;
  justify-content: space-around;
  padding: $spacing-xl;
  background-color: rgba(0, 0, 0, 0.8);
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .action-icon {
    font-size: 56rpx;
    margin-bottom: $spacing-sm;
  }
  
  .action-text {
    font-size: $font-size-sm;
    color: #FFFFFF;
  }
}

.input-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: $mask-color;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 600rpx;
  background-color: $bg-color-white;
  border-radius: $radius-lg;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid $border-color-light;
  
  .modal-title {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
  }
  
  .modal-close {
    font-size: $font-size-xl;
    color: $text-color-tertiary;
    padding: $spacing-sm;
  }
}

.modal-body {
  padding: $spacing-xl;
}

.code-input {
  width: 100%;
  height: 88rpx;
  border: 2rpx solid $border-color;
  border-radius: $radius-md;
  padding: 0 $spacing-lg;
  font-size: $font-size-lg;
  
  &:focus {
    border-color: $primary-color;
  }
}

.modal-footer {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-lg;
  
  .btn {
    flex: 1;
  }
}
</style>
