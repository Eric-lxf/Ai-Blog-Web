<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmMockPayment, createMallPayment } from '@/api/mall/payment'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const confirming = ref(false)
const payment = ref(null)

const orderId = computed(() => route.params.orderId || route.query.orderId)

async function loadPayment() {
  if (!orderId.value) return
  loading.value = true
  try {
    const res = await createMallPayment({
      orderId: orderId.value,
      channel: 'MOCK'
    })
    payment.value = res.data || null
  } finally {
    loading.value = false
  }
}

async function confirmPay() {
  if (!payment.value && !orderId.value) {
    ElMessage.warning('缺少支付信息')
    return
  }
  confirming.value = true
  try {
    await confirmMockPayment({
      paymentId: payment.value?.id,
      payNo: payment.value?.payNo,
      orderId: orderId.value
    })
    ElMessage.success('支付成功')
    router.push(orderId.value ? `/mall/orders/${orderId.value}` : '/mall/orders')
  } finally {
    confirming.value = false
  }
}

onMounted(loadPayment)
</script>

<template>
  <div class="pay-page" v-loading="loading">
    <el-card shadow="never" class="pay-card">
      <template #header><strong>订单支付</strong></template>
      <el-result icon="info" title="MOCK 支付" sub-title="Phase 1 使用模拟支付确认按钮完成支付链路">
        <template #extra>
          <div class="payment-info">
            <p>订单ID：{{ orderId || '-' }}</p>
            <p>订单号：{{ route.query.orderNo || payment?.orderNo || '-' }}</p>
            <p>支付单号：{{ payment?.payNo || '待创建' }}</p>
            <p>支付金额：<strong>¥{{ Number(payment?.amount || 0).toFixed(2) }}</strong></p>
            <p>支付渠道：{{ payment?.channel || 'MOCK' }}</p>
          </div>
          <el-button type="primary" size="large" :loading="confirming" @click="confirmPay">
            MOCK 确认支付
          </el-button>
          <el-button size="large" @click="router.push('/mall/orders')">查看订单</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<style scoped>
.pay-card {
  max-width: 720px;
  margin: 0 auto;
}

.payment-info {
  min-width: 320px;
  margin: 0 auto 18px;
  padding: 18px;
  border-radius: 14px;
  text-align: left;
  background: #f8fafc;
}

.payment-info p {
  margin: 8px 0;
}

.payment-info strong {
  color: #dc2626;
  font-size: 22px;
}
</style>
