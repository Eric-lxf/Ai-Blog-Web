<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号ID"><el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="关键词"><el-input v-model="queryParams.keyword" placeholder="OpenID/内容关键词" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" /><el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="方向" width="90"><template #default="{ row }"><el-tag :type="row.direction === 'in' ? 'success' : 'warning'">{{ row.direction === 'in' ? '接收' : '发送' }}</el-tag></template></el-table-column>
      <el-table-column label="OpenID" prop="openId" min-width="220" show-overflow-tooltip /><el-table-column label="消息类型" prop="messageType" width="110" />
      <el-table-column label="事件类型" prop="eventType" width="120" show-overflow-tooltip /><el-table-column label="内容" prop="content" min-width="260" show-overflow-tooltip />
      <el-table-column label="时间" prop="createTime" width="170" />
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>
<script setup>
import { listWechatMessage } from '@/api/wechat'
defineOptions({ name: 'WechatMessage' })
const loading = ref(false); const list = ref([]); const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
function getList() { loading.value = true; listWechatMessage(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined }; getList() }
getList()
</script>