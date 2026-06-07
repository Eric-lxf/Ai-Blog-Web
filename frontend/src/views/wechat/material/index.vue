<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号ID">
        <el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 130px">
          <el-option label="待上传" :value="0" />
          <el-option label="草稿成功" :value="1" />
          <el-option label="失败" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="素材标题" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="作者" prop="author" width="120" />
      <el-table-column label="摘要" prop="digest" min-width="200" show-overflow-tooltip />
      <el-table-column label="mediaId" prop="mediaId" min-width="170" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'">
            {{ row.status === 1 ? '草稿成功' : row.status === 2 ? '失败' : '待上传' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button link type="danger" v-hasPermi="['wechat:material:remove']" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup>
import { deleteWechatMaterial, listWechatMaterial } from '@/api/wechat'

defineOptions({ name: 'WechatMaterial' })

const { proxy } = getCurrentInstance()
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

function getList() {
  loading.value = true
  listWechatMaterial(queryParams.value).then(res => {
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
  queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined }
  getList()
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除素材「${row.title}」吗？`).then(() => {
    return deleteWechatMaterial(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>
