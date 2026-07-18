<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelMyMallOrder, listMyMallOrder } from '@/api/mall/order'

const router = useRouter()
const loading = ref(false)
const orderList = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined
})
const statusMap = {
  PENDING_PAY: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'primary' },
  SHIPPED: { label: '已发货', type: 'success' },
  COMPLETED: { label: '已完成', type: 'info' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

async function getList() {
  loading.value = true
  try {
    const res = await listMyMallOrder(query)
    orderList.value = normalizeRows(res)
    total.value = res.total ?? res.data?.total ?? orderList.value.length
  } finally {
    loading.value = false
  }
}

function changeStatus(status) {
  query.status = status
  query.pageNum = 1
  getList()
}

async function cancelOrder(row) {
  await ElMessageBox.confirm(`确认取消订单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
  await cancelMyMallOrder(row.id, { cancelReason: '用户取消' })
  ElMessage.success('订单已取消')
  getList()
}

onMounted(getList)
</script>

<template>
  <div class="orders-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <strong>我的订单</strong>
          <RouterLink to="/mall/list">继续购物</RouterLink>
        </div>
      </template>

      <div class="tabs">
        <el-button :type="!query.status ? 'primary' : 'default'" @click="changeStatus(undefined)">全部</el-button>
        <el-button :type="query.status === 'PENDING_PAY' ? 'primary' : 'default'" @click="changeStatus('PENDING_PAY')">待支付</el-button>
        <el-button :type="query.status === 'PAID' ? 'primary' : 'default'" @click="changeStatus('PAID')">待发货</el-button>
        <el-button :type="query.status === 'SHIPPED' ? 'primary' : 'default'" @click="changeStatus('SHIPPED')">已发货</el-button>
        <el-button :type="query.status === 'COMPLETED' ? 'primary' : 'default'" @click="changeStatus('COMPLETED')">已完成</el-button>
      </div>

      <el-table v-loading="loading" :data="orderList">
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应付金额" width="130" align="right">
          <template #default="{ row }">¥{{ Number(row.payAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/mall/orders/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_PAY'" link type="success" @click="router.push(`/mall/pay/${row.id}`)">去支付</el-button>
            <el-button v-if="row.status === 'PENDING_PAY'" link type="danger" @click="cancelOrder(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && orderList.length === 0" description="暂无订单" />

      <div class="pager" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="getList"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
