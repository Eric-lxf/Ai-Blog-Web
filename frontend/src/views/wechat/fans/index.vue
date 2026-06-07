<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号ID"><el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 120px"><el-option label="已关注" :value="1" /><el-option label="未关注" :value="0" /></el-select></el-form-item>
      <el-form-item label="关键词"><el-input v-model="queryParams.keyword" placeholder="昵称/OpenID" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" /><el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="昵称" prop="nickname" min-width="140" show-overflow-tooltip /><el-table-column label="OpenID" prop="openId" min-width="220" show-overflow-tooltip />
      <el-table-column label="UnionID" prop="unionId" min-width="180" show-overflow-tooltip />
      <el-table-column label="关注状态" width="100"><template #default="{ row }"><el-tag :type="row.subscribeStatus === 1 ? 'success' : 'info'">{{ row.subscribeStatus === 1 ? '已关注' : '未关注' }}</el-tag></template></el-table-column>
      <el-table-column label="关注时间" prop="subscribeTime" width="170" /><el-table-column label="更新时间" prop="updateTime" width="170" />
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>
<script setup>
import { listWechatFans } from '@/api/wechat'
defineOptions({ name: 'WechatFans' })
const loading = ref(false); const list = ref([]); const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined })
function getList() { loading.value = true; listWechatFans(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined }; getList() }
getList()
</script>