<script setup>
defineOptions({ name: 'MallAdminPayment' })

import { onMounted, reactive, ref } from 'vue'
import { getMallAdminPayment, listMallAdminPayment } from '@/api/mall/payment'

const loading = ref(false)
const detailLoading = ref(false)
const paymentList = ref([])
const total = ref(0)
const drawerOpen = ref(false)
const currentPayment = ref(null)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  payNo: '',
  orderNo: '',
  channel: undefined,
  status: undefined
})
const statusMap = {
  INIT: { label: '初始化', type: 'info' },
  PAYING: { label: '支付中', type: 'warning' },
  SUCCESS: { label: '成功', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
  CLOSED: { label: '关闭', type: 'info' }
}

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallAdminPayment(queryParams)
    paymentList.value = normalizeRows(res)
    total.value = res.total ?? res.data?.total ?? paymentList.value.length
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
    payNo: '',
    orderNo: '',
    channel: undefined,
    status: undefined
  })
  getList()
}

async function openDetail(row) {
  drawerOpen.value = true
  detailLoading.value = true
  try {
    const res = await getMallAdminPayment(row.id)
    currentPayment.value = res.data || row
  } finally {
    detailLoading.value = false
  }
}

onMounted(getList)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryParams" @submit.prevent="handleQuery">
        <el-form-item label="支付单号">
          <el-input v-model="queryParams.payNo" placeholder="请输入支付单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="queryParams.channel" placeholder="全部" clearable style="width: 130px">
            <el-option label="MOCK" value="MOCK" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="初始化" value="INIT" />
            <el-option label="支付中" value="PAYING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="paymentList" stripe>
        <el-table-column prop="payNo" label="支付单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="channel" label="渠道" width="100" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="channelTradeNo" label="渠道流水号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="paidTime" label="支付时间" width="180" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="['mall:payment:query']" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="drawerOpen" title="支付单详情" size="640px" append-to-body>
      <div v-loading="detailLoading">
        <el-empty v-if="!detailLoading && !currentPayment" description="暂无支付单详情" />
        <template v-if="currentPayment">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="支付单号">{{ currentPayment.payNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMap[currentPayment.status]?.type || 'info'">
                {{ statusMap[currentPayment.status]?.label || currentPayment.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单号">{{ currentPayment.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="订单ID">{{ currentPayment.orderId }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ currentPayment.userId }}</el-descriptions-item>
            <el-descriptions-item label="渠道">{{ currentPayment.channel }}</el-descriptions-item>
            <el-descriptions-item label="金额">¥{{ Number(currentPayment.amount || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="渠道流水号">{{ currentPayment.channelTradeNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentPayment.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="过期时间">{{ currentPayment.expireTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ currentPayment.paidTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-card class="detail-card" shadow="never">
            <template #header>回调原文</template>
            <pre class="notify-raw">{{ currentPayment.notifyRaw || '暂无回调内容' }}</pre>
          </el-card>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.detail-card {
  margin-top: 16px;
}

.notify-raw {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: #606266;
}
</style>
