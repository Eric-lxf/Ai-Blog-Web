<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="本地推送记录" name="local">
        <el-form :inline="true" :model="queryParams" class="mb8">
          <el-form-item label="账号">
            <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 200px">
              <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
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
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="list">
          <el-table-column label="记录ID" prop="id" width="90" />
          <el-table-column label="账号" width="120">
            <template #default="{ row }">{{ accountNameMap[row.accountId] || row.accountId }}</template>
          </el-table-column>
          <el-table-column label="文章ID" prop="articleId" width="90" />
          <el-table-column label="推送模式" prop="publishMode" width="130">
            <template #default="{ row }">{{ row.publishMode === 'draft_and_publish' ? '草稿并发布' : '仅草稿' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="任务/文章ID" prop="msgId" min-width="140" show-overflow-tooltip />
          <el-table-column label="错误信息" prop="errorMessage" min-width="160" show-overflow-tooltip />
          <el-table-column label="更新时间" prop="updateTime" width="170" />
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" link type="primary" v-hasPermi="['wechat:publish:push']" @click="handleSubmitRecord(row)">发布草稿</el-button>
              <el-button v-if="row.status === 2" link type="primary" v-hasPermi="['wechat:publish:query']" @click="handleSyncStatus(row)">同步状态</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <el-tab-pane label="微信已发布消息" name="wechat">
        <el-form :inline="true" :model="wechatQuery" class="mb8">
          <el-form-item label="账号">
            <el-select v-model="wechatQuery.accountId" filterable placeholder="请选择账号" style="width: 220px">
              <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" :disabled="!wechatQuery.accountId" @click="loadWechatList">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="wechatLoading" :data="wechatList">
          <el-table-column label="article_id" prop="article_id" min-width="180" show-overflow-tooltip />
          <el-table-column label="标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ firstTitle(row) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatTime(row.update_time) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" v-hasPermi="['wechat:publish:list']" @click="handleViewArticle(row)">详情</el-button>
              <el-button link type="danger" v-hasPermi="['wechat:publish:remove']" @click="handleDeletePublished(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="wechatTotal > 0"
          :total="wechatTotal"
          v-model:page="wechatPage"
          v-model:limit="wechatPageSize"
          :page-sizes="[10, 20]"
          @pagination="loadWechatList"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="articleVisible" title="已发布图文详情" width="760px" append-to-body>
      <el-input v-model="articleJson" type="textarea" :rows="20" readonly />
      <template #footer>
        <el-button type="primary" @click="articleVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="发布状态" width="640px" append-to-body>
      <el-input v-model="statusJson" type="textarea" :rows="14" readonly />
      <template #footer>
        <el-button type="primary" @click="statusVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import {
  batchGetWechatPublished,
  deleteWechatPublished,
  getWechatPublishedArticle,
  listWechatAccountOptions,
  listWechatPublish,
  submitWechatPublishRecord,
  syncWechatPublishStatus
} from '@/api/wechat'

defineOptions({ name: 'WechatPublish' })

const { proxy } = getCurrentInstance()
const activeTab = ref('local')
const loading = ref(false)
const wechatLoading = ref(false)
const list = ref([])
const total = ref(0)
const wechatList = ref([])
const wechatTotal = ref(0)
const wechatPage = ref(1)
const wechatPageSize = ref(10)
const accountOptions = ref([])
const articleVisible = ref(false)
const statusVisible = ref(false)
const articleJson = ref('')
const statusJson = ref('')

const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, status: undefined })
const wechatQuery = ref({ accountId: undefined })

const accountNameMap = computed(() => {
  const map = {}
  accountOptions.value.forEach(item => { map[item.id] = item.name })
  return map
})

function statusLabel(status) {
  const map = { 0: '待处理', 1: '草稿成功', 2: '发布中', 3: '已发布', 4: '失败' }
  return map[status] || String(status ?? '')
}
function statusTag(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}
function firstTitle(row) {
  const items = row?.content?.news_item
  return Array.isArray(items) && items.length ? items[0].title : '-'
}
function formatTime(ts) {
  if (!ts) return '-'
  const date = new Date(Number(ts) * 1000)
  return Number.isNaN(date.getTime()) ? String(ts) : date.toLocaleString()
}

function loadAccounts() {
  return listWechatAccountOptions().then(res => {
    accountOptions.value = res.data || []
    if (!wechatQuery.value.accountId && accountOptions.value.length) {
      wechatQuery.value.accountId = accountOptions.value[0].id
    }
  })
}
function getList() {
  loading.value = true
  listWechatPublish(queryParams.value).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined }
  getList()
}
function loadWechatList() {
  if (!wechatQuery.value.accountId) return
  wechatLoading.value = true
  const offset = (wechatPage.value - 1) * wechatPageSize.value
  batchGetWechatPublished({
    accountId: wechatQuery.value.accountId,
    offset,
    count: wechatPageSize.value,
    noContent: 1
  }).then(res => {
    const data = res.data || {}
    wechatList.value = data.item || []
    wechatTotal.value = data.total_count || 0
  }).finally(() => { wechatLoading.value = false })
}
function handleSubmitRecord(row) {
  proxy.$modal.confirm('确认将该草稿提交发布到微信吗？').then(() => submitWechatPublishRecord(row.id))
    .then(res => {
      statusJson.value = JSON.stringify(res.data || {}, null, 2)
      statusVisible.value = true
      proxy.$modal.msgSuccess('已提交发布')
      getList()
    }).catch(() => {})
}
function handleSyncStatus(row) {
  syncWechatPublishStatus(row.id).then(res => {
    statusJson.value = JSON.stringify(res.data || {}, null, 2)
    statusVisible.value = true
    getList()
  })
}
function handleViewArticle(row) {
  getWechatPublishedArticle({ accountId: wechatQuery.value.accountId, articleId: row.article_id }).then(res => {
    articleJson.value = JSON.stringify(res.data || {}, null, 2)
    articleVisible.value = true
  })
}
function handleDeletePublished(row) {
  proxy.$modal.confirm('删除后不可恢复，确认删除该已发布文章吗？').then(() => deleteWechatPublished({
    accountId: wechatQuery.value.accountId,
    articleId: row.article_id
  })).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadWechatList()
  }).catch(() => {})
}

loadAccounts().finally(() => getList())
</script>
