<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="mb8">
      <el-form-item label="账号">
        <el-select v-model="query.accountId" filterable placeholder="请选择账号" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" :disabled="!query.accountId" @click="loadDrafts">查询草稿</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:draft:add']" @click="openDialog()">新增草稿</el-button>
        <span v-if="draftCount !== null" class="draft-count">草稿总数：{{ draftCount }}</span>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="draftList">
      <el-table-column label="media_id" prop="media_id" min-width="200" show-overflow-tooltip />
      <el-table-column label="标题" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ firstTitle(row) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.update_time) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:draft:list']" @click="handleView(row)">详情</el-button>
          <el-button link type="primary" v-hasPermi="['wechat:draft:edit']" @click="openDialog(row)">编辑</el-button>
          <el-button link type="success" v-hasPermi="['wechat:publish:push']" @click="handlePublish(row)">发布</el-button>
          <el-button link type="danger" v-hasPermi="['wechat:draft:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="draftTotal > 0" :total="draftTotal" v-model:page="pageNum" v-model:limit="pageSize" :page-sizes="[10, 20]" @pagination="loadDrafts" />

    <el-dialog v-model="dialogVisible" :title="form.mediaId ? '编辑草稿' : '新增草稿'" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="120" /></el-form-item>
        <el-form-item label="作者"><el-input v-model="form.author" maxlength="32" /></el-form-item>
        <el-form-item label="封面 media_id" prop="thumbMediaId"><el-input v-model="form.thumbMediaId" placeholder="从素材管理上传封面后复制" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.digest" type="textarea" :rows="2" maxlength="128" /></el-form-item>
        <el-form-item label="正文 HTML" prop="content"><el-input v-model="form.content" type="textarea" :rows="12" /></el-form-item>
        <el-form-item label="原文链接"><el-input v-model="form.contentSourceUrl" placeholder="https://" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="草稿详情" width="760px" append-to-body>
      <el-input v-model="detailJson" type="textarea" :rows="20" readonly />
    </el-dialog>
  </div>
</template>
<script setup>
import {
  batchGetWechatDraft,
  countWechatDraft,
  deleteWechatDraft,
  getWechatDraft,
  listWechatAccountOptions,
  saveWechatDraft,
  submitWechatDraft,
  updateWechatDraft
} from '@/api/wechat'

defineOptions({ name: 'WechatDraft' })
const { proxy } = getCurrentInstance()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailJson = ref('')
const draftList = ref([])
const draftTotal = ref(0)
const draftCount = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const accountOptions = ref([])
const formRef = ref()
const query = ref({ accountId: undefined })
const form = reactive({
  mediaId: '',
  title: '',
  author: '',
  digest: '',
  content: '',
  thumbMediaId: '',
  contentSourceUrl: ''
})
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  thumbMediaId: [{ required: true, message: '请输入封面 media_id', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
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
    if (!query.value.accountId && accountOptions.value.length) {
      query.value.accountId = accountOptions.value[0].id
    }
  })
}
function loadDrafts() {
  if (!query.value.accountId) return
  loading.value = true
  const offset = (pageNum.value - 1) * pageSize.value
  Promise.all([
    batchGetWechatDraft({ accountId: query.value.accountId, offset, count: pageSize.value, noContent: 1 }),
    countWechatDraft(query.value.accountId)
  ]).then(([listRes, countRes]) => {
    const data = listRes.data || {}
    draftList.value = data.item || []
    draftTotal.value = data.total_count || 0
    draftCount.value = countRes.data?.total_count ?? countRes.data?.draft_count ?? draftTotal.value
  }).finally(() => { loading.value = false })
}
function resetForm() {
  Object.assign(form, { mediaId: '', title: '', author: '', digest: '', content: '', thumbMediaId: '', contentSourceUrl: '' })
  formRef.value?.clearValidate()
}
async function openDialog(row) {
  resetForm()
  if (row) {
    const res = await getWechatDraft({ accountId: query.value.accountId, mediaId: row.media_id })
    const item = res.data?.news_item?.[0] || {}
    Object.assign(form, {
      mediaId: row.media_id,
      title: item.title || '',
      author: item.author || '',
      digest: item.digest || '',
      content: item.content || '',
      thumbMediaId: item.thumb_media_id || '',
      contentSourceUrl: item.content_source_url || ''
    })
  }
  dialogVisible.value = true
}
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid || !query.value.accountId) return
    submitLoading.value = true
    const payload = {
      accountId: query.value.accountId,
      title: form.title,
      author: form.author,
      digest: form.digest,
      content: form.content,
      thumbMediaId: form.thumbMediaId,
      contentSourceUrl: form.contentSourceUrl
    }
    const req = form.mediaId
      ? updateWechatDraft({ ...payload, mediaId: form.mediaId, index: 0 })
      : saveWechatDraft(payload)
    req.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      dialogVisible.value = false
      loadDrafts()
    }).finally(() => { submitLoading.value = false })
  })
}
function handleView(row) {
  getWechatDraft({ accountId: query.value.accountId, mediaId: row.media_id }).then(res => {
    detailJson.value = JSON.stringify(res.data || {}, null, 2)
    detailVisible.value = true
  })
}
function handlePublish(row) {
  proxy.$modal.confirm('确认发布该草稿吗？').then(() => submitWechatDraft({
    accountId: query.value.accountId,
    mediaId: row.media_id
  })).then(() => proxy.$modal.msgSuccess('已提交发布')).catch(() => {})
}
function handleDelete(row) {
  proxy.$modal.confirm('删除后不可恢复，确认删除该草稿吗？').then(() => deleteWechatDraft({
    accountId: query.value.accountId,
    mediaId: row.media_id
  })).then(() => { proxy.$modal.msgSuccess('删除成功'); loadDrafts() }).catch(() => {})
}

loadAccounts().finally(() => loadDrafts())
</script>
<style scoped>
.draft-count { margin-left: 12px; color: var(--el-text-color-secondary); }
</style>
