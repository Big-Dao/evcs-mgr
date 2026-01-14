<template>
  <view class="empty-state">
    <text class="empty-icon">{{ icon }}</text>
    <text class="empty-title">{{ title }}</text>
    <text class="empty-desc" v-if="description">{{ description }}</text>
    <view class="empty-action" v-if="actionText">
      <view class="btn btn-primary" @click="handleAction">
        {{ actionText }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
interface Props {
  icon?: string
  title?: string
  description?: string
  actionText?: string
}

withDefaults(defineProps<Props>(), {
  icon: '📭',
  title: '暂无数据'
})

const emit = defineEmits<{
  (e: 'action'): void
}>()

function handleAction() {
  emit('action')
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx $spacing-lg;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: $spacing-lg;
}

.empty-title {
  font-size: $font-size-lg;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
}

.empty-desc {
  font-size: $font-size-md;
  color: $text-color-tertiary;
  text-align: center;
  margin-bottom: $spacing-lg;
}

.empty-action {
  margin-top: $spacing-md;
  
  .btn {
    min-width: 240rpx;
  }
}
</style>
