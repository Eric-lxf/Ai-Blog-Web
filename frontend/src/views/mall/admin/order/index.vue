<script setup>
defineOptions({ name: 'MallAdminOrder' })

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { completeMallOrder, getMallAdminOrder, listMallAdminOrder, shipMallOrder } from '@/api/mall/order'

const loading = ref(false)
const detailLoading = ref(false)
const orderList = ref([])
const total = ref(0)
const drawerOpen = ref(false)
const currentOrder = ref(null)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  status: undefined
})
const statusMap = {
  PENDING_PAY: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'primary' },
  SHIPPED: { label: '已发货', type: 'success' },
  COMPLETED: { label: '已完成', type: 'info' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

const currentItems = computed(() => currentOrder.value?.items || currentOrder.value?.orderItems || [])
const currentAddress = computed(() => parseAddress(currentOrder.value?.addressSnapshot))

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

function parseAddress(value) {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return { detailAddress: value }
  }
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallAdminOrder(queryParams)
    orderList.value = normalizeRows(res)
    total.value = res.total ?? res.data?.total ?? orderList.value.length
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 10,
    orderNo: '',
    status: undefined
  })
  getList()
}

async function openDetail(row) {
  drawerOpen.value = true
  detailLoading.value = true
  try {
    const res = await getMallAdminOrder(row.id)
    currentOrder.value = res.data || row
  } finally {
    detailLoading.value = false
  }
}

async function handleShip(row) {
  const { value } = await ElMessageBox.prompt('请输入物流单号或发货备注', '订单发货', {
    inputPlaceholder: '如 SF123456789',
    confirmButtonText: '发货',
    cancelButtonText: '取消'
  })
  await shipMallOrder(row.id, { trackingNo: value, remark: value })
  ElMessage.success('发货成功')
  getList()
  if (currentOrder.value?.id === row.id) {
    openDetail(row)
  }
}

async function handleComplete(row) {
  await ElMessageBox.confirm(`确认将订单 ${row.orderNo} 标记为已完成吗？`, '提示', { type: 'warning' })
  await completeMallOrder(row.id)
  ElMessage.success('订单已完成')
  getList()
  if (currentOrder.value?.id === row.id) {
    openDetail(row)
  }
}

onMounted(getList)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryParams" @submit.prevent="handleQuery">
        <el-form-item label="订单号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="待发货" value="PAID" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="orderList" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应付金额" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.payAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="商品金额" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.goodsAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column prop="payTime" label="支付时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="['mall:order:query']" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'PAID'"
              link
              type="success"
              v-hasPermi="['mall:order:ship']"
              @click="handleShip(row)"
            >
              发货
            </el-button>
            <el-button
              v-if="row.status === 'SHIPPED'"
              link
              type="warning"
              v-hasPermi="['mall:order:complete']"
              @click="handleComplete(row)"
            >
              完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-drawer v-model="drawerOpen" title="订单详情" size="720px" append-to-body>
      <div v-loading="detailLoading">
        <el-empty v-if="!detailLoading && !currentOrder" description="暂无订单详情" />
        <template v-if="currentOrder">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMap[currentOrder.status]?.type || 'info'">
                {{ statusMap[currentOrder.status]?.label || currentOrder.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
            <el-descriptions-item label="应付金额">¥{{ Number(currentOrder.payAmount || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="商品金额">¥{{ Number(currentOrder.goodsAmount || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="运费">¥{{ Number(currentOrder.freightAmount || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ currentOrder.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发货时间">{{ currentOrder.shipTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ currentOrder.completeTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-card class="detail-card" shadow="never">
            <template #header>收货地址</template>
            <div v-if="currentAddress">
              <div>{{ currentAddress.receiverName || currentAddress.name }} {{ currentAddress.receiverMobile || currentAddress.mobile }}</div>
              <div class="muted">
                {{ currentAddress.province }} {{ currentAddress.city }} {{ currentAddress.district }}
                {{ currentAddress.detailAddress || currentAddress.address }}
              </div>
            </div>
            <el-empty v-else description="无地址快照" :image-size="64" />
          </el-card>

          <el-card class="detail-card" shadow="never">
            <template #header>商品明细</template>
            <el-table :data="currentItems" border>
              <el-table-column prop="spuName" label="商品" min-width="180" show-overflow-tooltip />
              <el-table-column prop="skuSpecs" label="规格" min-width="150" show-overflow-tooltip />
              <el-table-column prop="skuCode" label="SKU编码" min-width="130" show-overflow-tooltip />
              <el-table-column label="单价" width="100" align="right">
                <template #default="{ row }">¥{{ Number(row.price || 0).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="80" align="center" />
            </el-table>
          </el-card>

          <div class="drawer-actions">
            <el-button
              v-if="currentOrder.status === 'PAID'"
              type="success"
              v-hasPermi="['mall:order:ship']"
              @click="handleShip(currentOrder)"
            >
              发货
            </el-button>
            <el-button
              v-if="currentOrder.status === 'SHIPPED'"
              type="warning"
              v-hasPermi="['mall:order:complete']"
              @click="handleComplete(currentOrder)"
            >
              完成订单
            </el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.detail-card {
  margin-top: 16px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.muted {
  color: #606266;
  margin-top: 6px;
}
</style>
