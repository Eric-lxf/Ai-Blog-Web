<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyMallOrder } from '@/api/mall/order'
import { resolveUploadUrl } from '@/utils/blogAssets'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref(null)

const orderId = computed(() => Number(route.params.id))
const items = computed(() => order.value?.items || order.value?.orderItems || [])
const address = computed(() => parseAddress(order.value?.addressSnapshot))
const statusMap = {
  PENDING_PAY: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'primary' },
  SHIPPED: { label: '已发货', type: 'success' },
  COMPLETED: { label: '已完成', type: 'info' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

function parseAddress(value) {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return { detail: value }
  }
}

function itemImage(item) {
  return resolveUploadUrl(item.image || item.mainImage || '')
}

async function loadOrder() {
  if (!orderId.value) return
  loading.value = true
  try {
    const res = await getMyMallOrder(orderId.value)
    order.value = res.data || null
  } finally {
    loading.value = false
  }
}

watch(orderId, loadOrder, { immediate: true })
</script>

<template>
  <div v-loading="loading">
    <el-button link type="primary" @click="router.push('/mall/orders')">返回订单列表</el-button>
    <el-empty v-if="!loading && !order" description="订单不存在" />

    <template v-if="order">
      <el-card shadow="never" class="detail-card">
        <template #header>
          <div class="header">
            <strong>订单 {{ order.orderNo }}</strong>
            <el-tag :type="statusMap[order.status]?.type || 'info'">{{ statusMap[order.status]?.label || order.status }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="应付金额">¥{{ Number(order.payAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ order.shipTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ order.completeTime || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="order.status === 'PENDING_PAY'" class="actions">
          <el-button type="primary" @click="router.push(`/mall/pay/${order.id}`)">继续支付</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="detail-card">
        <template #header><strong>收货地址</strong></template>
        <div v-if="address">
          <div>{{ address.receiver || address.receiverName || address.name }} {{ address.mobile || address.receiverMobile }}</div>
          <div class="muted">{{ address.province }} {{ address.city }} {{ address.district }} {{ address.detail || address.detailAddress || address.address }}</div>
        </div>
        <el-empty v-else description="无地址快照" :image-size="72" />
      </el-card>

      <el-card shadow="never" class="detail-card">
        <template #header><strong>商品明细</strong></template>
        <el-table :data="items">
          <el-table-column label="商品" min-width="260">
            <template #default="{ row }">
              <div class="goods">
                <div class="thumb">
                  <img v-if="itemImage(row)" :src="itemImage(row)" alt="" />
                  <span v-else>N</span>
                </div>
                <div>
                  <div class="name">{{ row.spuName }}</div>
                  <div class="muted">{{ row.skuSpecs || row.specsJson || '默认规格' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">¥{{ Number(row.price || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="90" align="center" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.detail-card {
  margin-top: 16px;
}

.header,
.goods {
  display: flex;
  align-items: center;
}

.header {
  justify-content: space-between;
}

.goods {
  gap: 12px;
}

.thumb {
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 10px;
  color: #94a3b8;
  background: #eef2ff;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name {
  font-weight: 700;
}

.muted {
  margin-top: 4px;
  color: #6b7280;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
