<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMallAddress } from '@/api/mall/address'
import { listMallCart } from '@/api/mall/cart'
import { createMallOrder } from '@/api/mall/order'
import { resolveUploadUrl } from '@/utils/blogAssets'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const cartList = ref([])
const addressList = ref([])
const selectedAddressId = ref(undefined)
const remark = ref('')

const cartIds = computed(() => String(route.query.cartIds || '').split(',').filter(Boolean).map(Number))
const items = computed(() => cartList.value.filter(item => !cartIds.value.length || cartIds.value.includes(Number(item.id))))
const totalAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.price || item.skuPrice || 0) * Number(item.quantity || 0), 0))

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

function itemImage(item) {
  return resolveUploadUrl(item.image || item.mainImage || item.spuImage || '')
}

function addressText(item) {
  return `${item.province || ''}${item.city || ''}${item.district || ''}${item.detail || item.detailAddress || ''}`
}

async function loadData() {
  loading.value = true
  try {
    const [cartRes, addressRes] = await Promise.all([
      listMallCart(),
      listMallAddress()
    ])
    cartList.value = normalizeRows(cartRes)
    addressList.value = normalizeRows(addressRes)
    selectedAddressId.value = addressList.value.find(item => item.isDefault === '1' || item.defaultFlag === '1')?.id || addressList.value[0]?.id
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  if (!items.value.length) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  if (!selectedAddressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  submitting.value = true
  try {
    const res = await createMallOrder({
      addressId: selectedAddressId.value,
      cartIds: items.value.map(item => item.id),
      remark: remark.value
    })
    const order = res.data || {}
    const orderId = order.id || order.orderId
    ElMessage.success('订单创建成功')
    router.push({ path: `/mall/pay/${orderId}`, query: { orderNo: order.orderNo } })
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="checkout-page">
    <el-card shadow="never" class="checkout-card">
      <template #header>
        <div class="header">
          <strong>确认收货地址</strong>
          <RouterLink to="/mall/address">管理地址</RouterLink>
        </div>
      </template>

      <el-empty v-if="!loading && addressList.length === 0" description="暂无收货地址，请先新增" />
      <el-radio-group v-else v-model="selectedAddressId" class="address-list">
        <el-radio v-for="item in addressList" :key="item.id" :value="item.id" border class="address-item">
          <div>
            <strong>{{ item.receiver || item.receiverName }} {{ item.mobile }}</strong>
            <el-tag v-if="item.isDefault === '1' || item.defaultFlag === '1'" size="small" type="success">默认</el-tag>
          </div>
          <p>{{ addressText(item) }}</p>
        </el-radio>
      </el-radio-group>
    </el-card>

    <el-card shadow="never" class="checkout-card">
      <template #header><strong>确认商品</strong></template>
      <el-table :data="items">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="goods">
              <div class="thumb">
                <img v-if="itemImage(row)" :src="itemImage(row)" alt="" />
                <span v-else>N</span>
              </div>
              <div>
                <div class="name">{{ row.spuName || row.productName || row.name }}</div>
                <div class="muted">{{ row.skuSpecs || row.specsJson || '默认规格' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.price || row.skuPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" align="center" />
        <el-table-column label="小计" width="120" align="right">
          <template #default="{ row }">¥{{ (Number(row.price || row.skuPrice || 0) * Number(row.quantity || 0)).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <el-form label-width="80px" class="remark-form">
        <el-form-item label="买家备注">
          <el-input v-model="remark" type="textarea" placeholder="选填" />
        </el-form-item>
      </el-form>
      <div class="submit-bar">
        <span>应付金额：<strong>¥{{ totalAmount.toFixed(2) }}</strong></span>
        <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.checkout-card + .checkout-card {
  margin-top: 18px;
}

.header,
.goods,
.submit-bar {
  display: flex;
  align-items: center;
}

.header,
.submit-bar {
  justify-content: space-between;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.address-item {
  height: auto;
  padding: 12px;
  white-space: normal;
}

.address-item p {
  margin: 6px 0 0;
  color: #6b7280;
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

.remark-form {
  margin-top: 18px;
}

.submit-bar {
  padding-top: 18px;
  border-top: 1px solid #e5e7eb;
}

.submit-bar strong {
  color: #dc2626;
  font-size: 22px;
}

@media (max-width: 800px) {
  .address-list {
    grid-template-columns: 1fr;
  }
}
</style>
