<template>
  <view class="page-container">
    <!-- 头像 -->
    <view class="avatar-section" @click="changeAvatar">
      <image 
        class="avatar" 
        :src="avatar || '/static/images/avatar-default.png'"
        mode="aspectFill"
      />
      <text class="change-text">点击更换头像</text>
    </view>
    
    <!-- 个人信息 -->
    <view class="info-section card">
      <view class="info-item" @click="editNickname">
        <text class="info-label">昵称</text>
        <text class="info-value">{{ nickname || '未设置' }}</text>
        <text class="info-arrow">›</text>
      </view>
      <view class="info-item">
        <text class="info-label">手机号</text>
        <text class="info-value">{{ formatPhone(phone) }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">用户ID</text>
        <text class="info-value">{{ userId || '-' }}</text>
      </view>
    </view>
    
    <!-- 账号安全 -->
    <view class="info-section card">
      <view class="info-item" @click="changePhone">
        <text class="info-label">更换手机号</text>
        <text class="info-arrow">›</text>
      </view>
      <view class="info-item" @click="changePassword">
        <text class="info-label">修改密码</text>
        <text class="info-arrow">›</text>
      </view>
    </view>
    
    <!-- 昵称编辑弹窗 -->
    <view class="edit-modal" v-if="showNicknameModal" @click.self="closeNicknameModal">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">修改昵称</text>
        </view>
        <view class="modal-body">
          <input
            class="nickname-input"
            type="text"
            v-model="newNickname"
            placeholder="请输入昵称"
            maxlength="20"
          />
        </view>
        <view class="modal-footer">
          <view class="btn btn-secondary" @click="closeNicknameModal">取消</view>
          <view class="btn btn-primary" @click="saveNickname">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores'
import { updateUserInfo } from '@/api/auth'
import { formatPhone } from '@/utils/format'

const userStore = useUserStore()

const nickname = computed(() => userStore.nickname)
const phone = computed(() => userStore.phone)
const avatar = computed(() => userStore.avatar)
const userId = computed(() => userStore.userId)

const showNicknameModal = ref(false)
const newNickname = ref('')

function changeAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      // 上传头像
      uni.showLoading({ title: '上传中...' })
      
      // 这里应该调用上传接口
      // 模拟上传成功
      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({
          title: '头像更新成功',
          icon: 'success'
        })
        // 更新本地头像
        // userStore.setUserInfo({ ...userStore.userInfo, avatar: res.tempFilePaths[0] })
      }, 1000)
    }
  })
}

function editNickname() {
  newNickname.value = nickname.value
  showNicknameModal.value = true
}

function closeNicknameModal() {
  showNicknameModal.value = false
}

async function saveNickname() {
  if (!newNickname.value.trim()) {
    uni.showToast({
      title: '请输入昵称',
      icon: 'none'
    })
    return
  }
  
  uni.showLoading({ title: '保存中...' })
  
  try {
    await updateUserInfo({ nickname: newNickname.value })
    
    // 更新本地
    if (userStore.userInfo) {
      userStore.setUserInfo({ ...userStore.userInfo, nickname: newNickname.value })
    }
    
    closeNicknameModal()
    uni.showToast({
      title: '保存成功',
      icon: 'success'
    })
  } catch (error) {
    console.error('Save nickname error:', error)
    uni.showToast({
      title: '保存失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}

function changePhone() {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

function changePassword() {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-xxl;
  background-color: $bg-color-white;
  
  .avatar {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    border: 4rpx solid $border-color;
  }
  
  .change-text {
    margin-top: $spacing-md;
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}

.info-section {
  margin: $spacing-lg;
}

.info-item {
  display: flex;
  align-items: center;
  padding: $spacing-lg 0;
  border-bottom: 1px solid $border-color-light;
  
  &:last-child {
    border-bottom: none;
  }
  
  .info-label {
    font-size: $font-size-lg;
    color: $text-color;
  }
  
  .info-value {
    flex: 1;
    text-align: right;
    font-size: $font-size-lg;
    color: $text-color-secondary;
    margin-left: $spacing-md;
  }
  
  .info-arrow {
    font-size: $font-size-xl;
    color: $text-color-tertiary;
    margin-left: $spacing-sm;
  }
}

.edit-modal {
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
  padding: $spacing-lg;
  text-align: center;
  border-bottom: 1px solid $border-color-light;
  
  .modal-title {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
  }
}

.modal-body {
  padding: $spacing-xl;
}

.nickname-input {
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
