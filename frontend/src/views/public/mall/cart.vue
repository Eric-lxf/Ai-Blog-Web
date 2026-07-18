<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMallCart, removeMallCartItem, updateMallCartItem } from '@/api/mall/cart'
import { resolveUploadUrl } from '@/utils/blogAssets'

const router = useRouter()
const loading = ref(false)
const cartList = ref([])

const checkedItems = computed(() => cartList.value.filter(item => item.checked === '1' || item.checked === true))
const checkedIds = computed(() => checkedItems.value.map(item => item.id))
const totalAmount = computed(() => checkedItems.value.reduce((sum, item) => sum + Number(item.price || item.skuPrice || 0) * Number(item.quantity || 0), 0))

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

function itemImage(item) {
  return resolveUploadUrl(item.image || item.mainImage || item.spuImage || '')
}

function specsText(item) {
  return item.skuSpecs || item.specsJson || item.skuName || '默认规格'
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallCart()
    cartList.value = normalizeRows(res).map(item => ({
      ...item,
      checked: item.checked ?? '1'
    }))
  } finally {
    loading.value = false
  }
}

async function updateItem(row) {
  await updateMallCartItem(row.id, {
    quantity: row.quantity,
    checked: row.checked
  })
}

async function removeItem(row) {
  await ElMessageBox.confirm('确认从购物车移除该商品吗？', '提示', { type: 'warning' })
  await removeMallCartItem(row.id)
  ElMessage.success('已移除')
  getList()
}

function checkout() {
  if (!checkedIds.value.length) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  router.push({ path: '/mall/checkout', query: { cartIds: checkedIds.value.join(',') } })
}

onMounted(getList)
</script>

<template>
  <div class="cart-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <strong>购物车</strong>
          <RouterLink to="/mall/list">继续购物</RouterLink>
        </div>
      </template>

      <el-table v-loading="loading" :data="cartList">
        <el-table-column width="56" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.checked" true-label="1" false-label="0" @change="updateItem(row)" />
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="goods">
              <div class="thumb">
                <img v-if="itemImage(row)" :src="itemImage(row)" alt="" />
                <span v-else>N</span>
              </div>
              <div>
                <div class="name">{{ row.spuName || row.productName || row.name }}</div>
                <div class="muted">{{ specsText(row) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.price || row.skuPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="160" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="row.stock || 999" size="small" @change="updateItem(row)" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="right">
          <template #default="{ row }">¥{{ (Number(row.price || row.skuPrice || 0) * Number(row.quantity || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeItem(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && cartList.length === 0" description="购物车还是空的" />

      <div class="cart-footer">
        <span>已选 {{ checkedItems.length }} 件</span>
        <div>
          <span class="amount">合计：¥{{ totalAmount.toFixed(2) }}</span>
          <el-button type="primary" size="large" @click="checkout">去结算</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.header,
.cart-footer,
.goods {
  display: flex;
  align-items: center;
}

.header,
.cart-footer {
  justify-content: space-between;
}

.goods {
  gap: 12px;
}

.thumb {
  width: 62px;
  height: 62px;
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
  color: #111827;
  font-weight: 700;
}

.muted {
  margin-top: 4px;
  color: #6b7280;
}

.cart-footer {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #e5e7eb;
}

.amount {
  margin-right: 16px;
  color: #dc2626;
  font-size: 20px;
  font-weight: 800;
}
</style>
