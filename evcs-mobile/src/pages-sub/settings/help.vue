<template>
  <view class="page-container">
    <!-- 常见问题 -->
    <view class="faq-section">
      <view class="section-title">常见问题</view>
      
      <view 
        v-for="(faq, index) in faqs" 
        :key="index"
        class="faq-item card"
        @click="toggleFaq(index)"
      >
        <view class="faq-header">
          <text class="faq-question">{{ faq.question }}</text>
          <text class="faq-arrow" :class="{ expanded: faq.expanded }">›</text>
        </view>
        <view class="faq-answer" v-if="faq.expanded">
          <text>{{ faq.answer }}</text>
        </view>
      </view>
    </view>
    
    <!-- 联系客服 -->
    <view class="contact-section card">
      <view class="contact-title">需要更多帮助？</view>
      <view class="contact-actions">
        <view class="btn btn-primary" @click="callService">
          联系客服
        </view>
        <view class="btn btn-secondary" @click="goToFeedback">
          意见反馈
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface FAQ {
  question: string
  answer: string
  expanded: boolean
}

const faqs = ref<FAQ[]>([
  {
    question: '如何开始充电？',
    answer: '打开APP，点击首页的"扫码充电"，扫描充电桩上的二维码即可开始充电。也可以在充电站详情页选择具体的充电桩和充电枪。',
    expanded: false
  },
  {
    question: '如何结束充电？',
    answer: '在充电过程中，点击"结束充电"按钮即可停止充电。充电结束后会自动生成订单，您可以选择支付方式进行结算。',
    expanded: false
  },
  {
    question: '支持哪些支付方式？',
    answer: '我们支持微信支付、支付宝支付和账户余额支付。您可以在个人中心的"我的钱包"中进行充值。',
    expanded: false
  },
  {
    question: '充电过程中可以离开吗？',
    answer: '可以的，充电过程中您可以离开车辆。APP会实时显示充电进度，充电完成后会通过消息通知您。',
    expanded: false
  },
  {
    question: '如何查看充电记录？',
    answer: '点击底部导航栏的"订单"，即可查看所有充电记录和订单详情。',
    expanded: false
  },
  {
    question: '遇到充电故障怎么办？',
    answer: '如遇到充电故障，请先尝试重新扫码充电。如问题仍未解决，可以联系在线客服或拨打客服热线400-XXX-XXXX。',
    expanded: false
  }
])

function toggleFaq(index: number) {
  faqs.value[index].expanded = !faqs.value[index].expanded
}

function callService() {
  uni.makePhoneCall({
    phoneNumber: '400XXXXXXX'
  })
}

function goToFeedback() {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.faq-section {
  padding: $spacing-lg;
}

.section-title {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
  margin-bottom: $spacing-md;
}

.faq-item {
  margin-bottom: $spacing-sm;
  padding: $spacing-lg;
}

.faq-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .faq-question {
    flex: 1;
    font-size: $font-size-md;
    color: $text-color;
    font-weight: 500;
  }
  
  .faq-arrow {
    font-size: $font-size-xl;
    color: $text-color-tertiary;
    transition: transform 0.2s;
    
    &.expanded {
      transform: rotate(90deg);
    }
  }
}

.faq-answer {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color-light;
  
  text {
    font-size: $font-size-md;
    color: $text-color-secondary;
    line-height: 1.6;
  }
}

.contact-section {
  margin: $spacing-lg;
  text-align: center;
}

.contact-title {
  font-size: $font-size-lg;
  color: $text-color;
  margin-bottom: $spacing-lg;
}

.contact-actions {
  display: flex;
  gap: $spacing-md;
  
  .btn {
    flex: 1;
  }
}
</style>
