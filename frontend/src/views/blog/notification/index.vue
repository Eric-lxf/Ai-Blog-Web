<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="类型">
        <el-select v-model="queryParams.type" clearable placeholder="全部" style="width: 120px">
          <el-option label="评论" value="COMMENT" />
          <el-option label="回复" value="REPLY" />
          <el-option label="系统" value="SYSTEM" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.isRead" clearable placeholder="全部" style="width: 120px">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" plain @click="handleReadAll">全部已读</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="内容" prop="content" min-width="220" show-overflow-tooltip />
      <el-table-column label="时间" prop="createTime" width="170" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isRead" type="info" size="small">已读</el-tag>
          <el-tag v-else type="danger" size="small">未读</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openLink(row)">查看</el-button>
          <el-button v-if="!row.isRead" link type="primary" @click="markRead(row)">标为已读</el-button>
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

<script setup name="BlogNotification">
import { listNotifications, markNotificationRead, markNotificationReadAll } from '@/api/blog/notification'

const { proxy } = getCurrentInstance()
const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  type: undefined,
  isRead: undefined
})

function typeLabel(type) {
  const map = { COMMENT: '评论', REPLY: '回复', SYSTEM: '系统' }
  return map[type] || type
}

function typeTag(type) {
  const map = { COMMENT: 'warning', REPLY: 'success', SYSTEM: '' }
  return map[type] || 'info'
}

function getList() {
  loading.value = true
  listNotifications(queryParams.value).then(res => {
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
  queryParams.value = { pageNum: 1, pageSize: 10, type: undefined, isRead: undefined }
  getList()
}

function markRead(row) {
  markNotificationRead(row.id).then(() => {
    row.isRead = true
    proxy.$modal.msgSuccess('已标记为已读')
  })
}

function handleReadAll() {
  markNotificationReadAll().then(() => {
    proxy.$modal.msgSuccess('已全部标记为已读')
    getList()
  })
}

function openLink(row) {
  if (!row.isRead) {
    markNotificationRead(row.id).catch(() => {})
  }
  if (row.linkUrl) {
    router.push(row.linkUrl)
  }
}

getList()
</script>
