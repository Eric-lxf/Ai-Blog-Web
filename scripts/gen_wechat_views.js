/**
 * Write wechat vue pages as UTF-8 (avoids Windows GBK save on source files).
 *
 * IMPORTANT: Do NOT edit frontend/src/views/wechat/*.vue directly in the IDE on Windows.
 * Always change scripts/gen_wechat_views*.js (use \\uXXXX for Chinese), then run:
 *   node scripts/gen_wechat_views.js
 *   node scripts/verify-wechat-encoding.js
 */
const fs = require('fs')
const path = require('path')

const root = path.join(__dirname, '../frontend/src/views/wechat')

function w(rel, content) {
  const file = path.join(root, rel)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, content, 'utf8')
  const s = fs.readFileSync(file, 'utf8')
  if (!s.includes('\u8d26\u53f7\u540d\u79f0') && !s.includes('\u8d26\u53f7ID') && !s.includes('\u5173\u952e\u8bcd')) {
    throw new Error('UTF-8 verify failed: ' + rel)
  }
  console.log('written', rel)
}

w('account/index.vue', `<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12">
      <template #title>
        \u5fae\u4fe1\u56de\u8c03\u5730\u5740\u793a\u4f8b\uff1a{{ callbackExample }}
      </template>
    </el-alert>

    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="\u8d26\u53f7\u540d\u79f0">
        <el-input v-model="queryParams.keyword" placeholder="\u8bf7\u8f93\u5165\u8d26\u53f7\u540d\u79f0" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="\u72b6\u6001">
        <el-select v-model="queryParams.status" placeholder="\u5168\u90e8" clearable style="width: 120px">
          <el-option label="\u542f\u7528" :value="1" />
          <el-option label="\u505c\u7528" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button>
        <el-button icon="Refresh" @click="resetQuery">\u91cd\u7f6e</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:account:add']" @click="openDialog()">\u65b0\u589e\u8d26\u53f7</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="\u8d26\u53f7\u540d\u79f0" prop="name" min-width="160" />
      <el-table-column label="AppID" prop="appId" min-width="180" show-overflow-tooltip />
      <el-table-column label="Token" prop="token" min-width="140" show-overflow-tooltip />
      <el-table-column label="\u72b6\u6001" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '\u542f\u7528' : '\u505c\u7528' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="\u66f4\u65b0\u65f6\u95f4" prop="updateTime" width="170" />
      <el-table-column label="\u64cd\u4f5c" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:account:edit']" @click="openDialog(row)">\u7f16\u8f91</el-button>
          <el-button link type="primary" v-hasPermi="['wechat:account:query']" @click="handleTest(row)">\u6d4b\u8bd5\u8fde\u63a5</el-button>
          <el-button link type="danger" v-hasPermi="['wechat:account:remove']" @click="handleDelete(row)">\u5220\u9664</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '\u7f16\u8f91\u516c\u4f17\u53f7\u8d26\u53f7' : '\u65b0\u589e\u516c\u4f17\u53f7\u8d26\u53f7'" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="\u8d26\u53f7\u540d\u79f0" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="AppID" prop="appId">
          <el-input v-model="form.appId" maxlength="64" />
        </el-form-item>
        <el-form-item label="AppSecret" prop="appSecret">
          <el-input v-model="form.appSecret" type="password" show-password maxlength="128" />
        </el-form-item>
        <el-form-item label="Token" prop="token">
          <el-input v-model="form.token" maxlength="64" />
        </el-form-item>
        <el-form-item label="AESKey">
          <el-input v-model="form.aesKey" maxlength="64" />
        </el-form-item>
        <el-form-item label="\u72b6\u6001">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="\u56de\u8c03\u5730\u5740">
          <el-text type="info">{{ callbackWithId }}</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">\u53d6\u6d88</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">\u4fdd\u5b58</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getWechatAccount, listWechatAccount, saveWechatAccount, deleteWechatAccount, testWechatAccount } from '@/api/wechat'

defineOptions({ name: 'WechatAccount' })

const { proxy } = getCurrentInstance()
const formRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const list = ref([])
const total = ref(0)
const callbackBase = \`\${window.location.origin}/prod-api/public/wechat/callback\`

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  status: undefined
})

const form = reactive({
  id: undefined,
  name: '',
  appId: '',
  appSecret: '',
  token: '',
  aesKey: '',
  enabled: 1
})

const rules = {
  name: [{ required: true, message: '\u8bf7\u8f93\u5165\u8d26\u53f7\u540d\u79f0', trigger: 'blur' }],
  appId: [{ required: true, message: '\u8bf7\u8f93\u5165 AppID', trigger: 'blur' }],
  appSecret: [{ required: true, message: '\u8bf7\u8f93\u5165 AppSecret', trigger: 'blur' }],
  token: [{ required: true, message: '\u8bf7\u8f93\u5165 Token', trigger: 'blur' }]
}

const callbackExample = computed(() => \`\${callbackBase}/{accountId}\`)
const callbackWithId = computed(() => \`\${callbackBase}/\${form.id || '{accountId}'}\`)

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    appId: '',
    appSecret: '',
    token: '',
    aesKey: '',
    enabled: 1
  })
  formRef.value?.clearValidate()
}

function getList() {
  loading.value = true
  listWechatAccount(queryParams.value).then(res => {
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
  queryParams.value = { pageNum: 1, pageSize: 10, keyword: undefined, status: undefined }
  getList()
}

function openDialog(row) {
  resetForm()
  dialogVisible.value = true
  if (row?.id) {
    getWechatAccount(row.id).then(res => {
      Object.assign(form, res.data || {})
    })
  }
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    saveWechatAccount(form).then(() => {
      proxy.$modal.msgSuccess('\u4fdd\u5b58\u6210\u529f')
      dialogVisible.value = false
      getList()
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(\`\u786e\u5b9a\u5220\u9664\u516c\u4f17\u53f7\u8d26\u53f7\u300c\${row.name}\u300d\u5417\uff1f\`).then(() => {
    return deleteWechatAccount(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('\u5220\u9664\u6210\u529f')
    getList()
  }).catch(() => {})
}

function handleTest(row) {
  testWechatAccount(row.id).then(() => {
    proxy.$modal.msgSuccess('\u8fde\u63a5\u6210\u529f')
  })
}

getList()
</script>
`)

require('./gen_wechat_views_pages.js')(w)
