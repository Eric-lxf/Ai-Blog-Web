<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12">
      <template #title>
        微信回调地址示例：{{ callbackExample }}
      </template>
    </el-alert>

    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号名称">
        <el-input v-model="queryParams.keyword" placeholder="请输入账号名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:account:add']" @click="openDialog()">新增账号</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="账号名称" prop="name" min-width="160" />
      <el-table-column label="AppID" prop="appId" min-width="180" show-overflow-tooltip />
      <el-table-column label="Token" prop="token" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:account:edit']" @click="openDialog(row)">编辑</el-button>
          <el-button link type="primary" v-hasPermi="['wechat:account:query']" @click="handleTest(row)">测试连接</el-button>
          <el-button link type="danger" v-hasPermi="['wechat:account:remove']" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公众号账号' : '新增公众号账号'" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="账号名称" prop="name">
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
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="回调地址">
          <el-text type="info">{{ callbackWithId }}</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
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
const callbackBase = `${window.location.origin}/prod-api/public/wechat/callback`

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
  name: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  appId: [{ required: true, message: '请输入 AppID', trigger: 'blur' }],
  appSecret: [{ required: true, message: '请输入 AppSecret', trigger: 'blur' }],
  token: [{ required: true, message: '请输入 Token', trigger: 'blur' }]
}

const callbackExample = computed(() => `${callbackBase}/{accountId}`)
const callbackWithId = computed(() => `${callbackBase}/${form.id || '{accountId}'}`)

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
      proxy.$modal.msgSuccess('保存成功')
      dialogVisible.value = false
      getList()
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确定删除公众号账号「${row.name}」吗？`).then(() => {
    return deleteWechatAccount(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleTest(row) {
  testWechatAccount(row.id).then(() => {
    proxy.$modal.msgSuccess('连接成功')
  })
}

getList()
</script>
