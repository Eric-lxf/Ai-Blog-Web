<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addMallCart } from '@/api/mall/cart'
import { getPublicMallSpu } from '@/api/mall/public'
import { resolveUploadUrl } from '@/utils/blogAssets'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const adding = ref(false)
const product = ref(null)
const selectedSkuId = ref(undefined)
const quantity = ref(1)

const productId = computed(() => Number(route.params.id))
const skus = computed(() => product.value?.skus || product.value?.skuList || [])
const selectedSku = computed(() => skus.value.find(item => item.id === selectedSkuId.value))
const mainImage = computed(() => resolveUploadUrl(product.value?.mainImage || product.value?.images?.[0]?.url || ''))
const descAttrValues = computed(() => {
  const list = product.value?.attrValues || []
  return list.filter(item => !item.attrType || item.attrType === 'DESC')
})

function specsText(sku) {
  if (!sku?.specsJson) return '默认规格'
  try {
    const specs = JSON.parse(sku.specsJson)
    return Object.entries(specs).map(([key, value]) => `${key}: ${value}`).join(' / ')
  } catch {
    return sku.specsJson
  }
}

async function loadProduct() {
  if (!productId.value) return
  loading.value = true
  try {
    const res = await getPublicMallSpu(productId.value)
    product.value = res.data || null
    selectedSkuId.value = skus.value.find(item => item.status !== '1')?.id
  } finally {
    loading.value = false
  }
}

async function addCart() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择 SKU')
    return
  }
  adding.value = true
  try {
    await addMallCart({
      skuId: selectedSku.value.id,
      quantity: quantity.value
    })
    ElMessage.success('已加入购物车')
  } finally {
    adding.value = false
  }
}

function buyNow() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择 SKU')
    return
  }
  addCart().then(() => router.push('/mall/cart'))
}

watch(productId, loadProduct, { immediate: true })
</script>

<template>
  <div v-loading="loading">
    <el-button link type="primary" @click="router.push('/mall/list')">返回商品列表</el-button>
    <el-empty v-if="!loading && !product" description="商品不存在或未上架" />

    <template v-if="product">
      <section class="detail-card">
        <div class="gallery">
          <img v-if="mainImage" :src="mainImage" alt="" />
          <div v-else class="image-placeholder">NovaMall</div>
        </div>
        <div class="summary">
          <el-tag type="success">上架商品</el-tag>
          <h1>{{ product.name }}</h1>
          <p class="subtitle">{{ product.subtitle || product.categoryName || 'NovaMall 精选商品' }}</p>
          <div class="price">
            {{ selectedSku ? `¥${Number(selectedSku.price || 0).toFixed(2)}` : '请选择规格' }}
          </div>

          <div class="sku-block">
            <span class="label">规格</span>
            <el-radio-group v-model="selectedSkuId">
              <el-radio-button v-for="sku in skus" :key="sku.id" :value="sku.id" :disabled="sku.status === '1'">
                {{ specsText(sku) }}
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="sku-block">
            <span class="label">数量</span>
            <el-input-number v-model="quantity" :min="1" :max="selectedSku?.stock || 999" />
            <span class="stock">库存 {{ selectedSku?.stock ?? '-' }}</span>
          </div>

          <div class="actions">
            <el-button size="large" :loading="adding" @click="addCart">加入购物车</el-button>
            <el-button size="large" type="primary" :loading="adding" @click="buyNow">立即购买</el-button>
          </div>
        </div>
      </section>

      <section v-if="descAttrValues.length" class="detail-attrs">
        <h2>商品参数</h2>
        <table class="attr-table">
          <tbody>
            <tr v-for="item in descAttrValues" :key="item.attrId || item.attrName">
              <th>{{ item.attrName }}</th>
              <td>{{ item.value || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="detail-html">
        <h2>商品详情</h2>
        <div v-if="product.detailHtml" v-html="product.detailHtml" />
        <el-empty v-else description="暂无商品详情" :image-size="80" />
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-card {
  display: grid;
  grid-template-columns: 46% minmax(0, 1fr);
  gap: 28px;
  margin-top: 12px;
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.gallery {
  min-height: 420px;
  border-radius: 16px;
  overflow: hidden;
  background: #eef2ff;
}

.gallery img,
.image-placeholder {
  width: 100%;
  height: 100%;
  min-height: 420px;
  object-fit: cover;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 28px;
  font-weight: 800;
}

.summary h1 {
  margin: 16px 0 8px;
  font-size: 32px;
}

.subtitle {
  color: #6b7280;
}

.price {
  margin: 22px 0;
  padding: 18px;
  border-radius: 14px;
  color: #dc2626;
  font-size: 30px;
  font-weight: 800;
  background: #fff1f2;
}

.sku-block {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0;
  flex-wrap: wrap;
}

.label {
  min-width: 48px;
  color: #606266;
  font-weight: 700;
}

.stock {
  color: #909399;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.detail-html,
.detail-attrs {
  margin-top: 24px;
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.detail-attrs h2,
.detail-html h2 {
  margin: 0 0 16px;
  font-size: 20px;
}

.attr-table {
  width: 100%;
  border-collapse: collapse;
}

.attr-table th,
.attr-table td {
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  text-align: left;
  vertical-align: top;
}

.attr-table th {
  width: 160px;
  color: #606266;
  background: #f8fafc;
  font-weight: 600;
}

@media (max-width: 900px) {
  .detail-card {
    grid-template-columns: 1fr;
  }
}
</style>
