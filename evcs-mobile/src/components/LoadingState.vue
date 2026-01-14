<template>
  <view class="loading-state">
    <view class="loading-spinner"></view>
    <text class="loading-text" v-if="text">{{ text }}</text>
  </view>
</template>

<script setup lang="ts">
interface Props {
  text?: string
}

withDefaults(defineProps<Props>(), {
  text: '加载中...'
})
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx;
}

.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid $border-color;
  border-top-color: $primary-color;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  margin-top: $spacing-md;
  font-size: $font-size-md;
  color: $text-color-tertiary;
}
</style>
