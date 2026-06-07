<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号ID">
        <el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" placeholder="账号ID" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="待处理" :value="0" />
          <el-option label="草稿成功" :value="1" />
          <el-option label="发布中" :value="2" />
          <el-option label="已发布" :value="3" />
          <el-option label="失败" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="文章ID/消息ID" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="记录ID" prop="id" width="90" />
      <el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="文章ID" prop="articleId" width="90" />
      <el-table-column label="素材ID" prop="materialId" width="90" />
      <el-table-column label="推送模式" prop="publishMode" width="140">
        <template #default="{ row }">
          {{ row.publishMode === 'draft_and_publish' ? '草稿并发布' : '仅草稿' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="任务号" prop="msgId" min-width="130" show-overflow-tooltip />
      <el-table-column label="错误信息" prop="errorMessage" min-width="180" show-overflow-tooltip />
      <el-table-column label="更新时间" prop="updateTime" width="170" />
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup>
import { listWechatPublish } from '@/api/wechat'

defineOptions({ name: 'WechatPublish' })

const loading = ref(false)
const list = ref([])
const total = ref(0)
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  accountId: undefined,
  status: undefined,
  keyword: undefined
})

function statusLabel(status) {
  const map = { 0: '待处理', 1: '草稿成功', 2: '发布中', 3: '已发布', 4: '失败' }
  return map[status] || String(status ?? '')
}

function statusTag(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}

function getList() {
  loading.value = true
  listWechatPublish(queryParams.value).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    accountId: undefined,
    status: undefined,
    keyword: undefined
  }
  getList()
}

getList()
</script>
