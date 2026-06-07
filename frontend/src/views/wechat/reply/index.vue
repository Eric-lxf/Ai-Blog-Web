<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="关键词/内容" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:reply:add']" @click="openDialog()">新增规则</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ replyTypeLabel(row.replyType) }}</template>
      </el-table-column>
      <el-table-column label="关键词" prop="keyword" min-width="130" show-overflow-tooltip />
      <el-table-column label="内容" prop="content" min-width="220" show-overflow-tooltip />
      <el-table-column label="匹配方式" width="100">
        <template #default="{ row }">{{ row.matchType === 2 ? '全等' : '包含' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:reply:edit']" @click="openDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑自动回复' : '新增自动回复'" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="accountId">
          <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="回复类型" prop="replyType">
          <el-select v-model="form.replyType" style="width: 100%">
            <el-option label="关键词回复" value="keyword" />
            <el-option label="默认回复" value="default" />
            <el-option label="关注回复" value="subscribe" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.replyType === 'keyword'" label="关键词" prop="keyword">
          <el-input v-model="form.keyword" maxlength="100" />
        </el-form-item>
        <el-form-item label="回复内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="匹配方式" v-if="form.replyType === 'keyword'">
          <el-radio-group v-model="form.matchType">
            <el-radio :label="1">包含匹配</el-radio>
            <el-radio :label="2">全等匹配</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
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
import { listWechatAccount, listWechatReply, saveWechatReply } from '@/api/wechat'

defineOptions({ name: 'WechatReply' })

const { proxy } = getCurrentInstance()
const formRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const list = ref([])
const total = ref(0)
const accountOptions = ref([])

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  accountId: undefined,
  status: undefined,
  keyword: undefined
})

const form = reactive({
  id: undefined,
  accountId: undefined,
  replyType: 'keyword',
  keyword: '',
  content: '',
  enabled: 1,
  matchType: 1
})

const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  replyType: [{ required: true, message: '请选择回复类型', trigger: 'change' }],
  keyword: [{
    validator: (_, value, callback) => {
      if (form.replyType === 'keyword' && !value?.trim()) {
        callback(new Error('请输入关键词'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }],
  content: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

function replyTypeLabel(type) {
  const map = { keyword: '关键词', default: '默认回复', subscribe: '关注回复' }
  return map[type] || type
}

function loadAccounts() {
  return listWechatAccount({ pageNum: 1, pageSize: 1000 }).then(res => {
    accountOptions.value = res.rows || []
  })
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    accountId: undefined,
    replyType: 'keyword',
    keyword: '',
    content: '',
    enabled: 1,
    matchType: 1
  })
  formRef.value?.clearValidate()
}

function getList() {
  loading.value = true
  listWechatReply(queryParams.value).then(res => {
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

function openDialog(row) {
  resetForm()
  if (row) {
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

function submitForm() {
  if (form.replyType !== 'keyword') {
    form.keyword = ''
    form.matchType = 1
  }
  formRef.value.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    saveWechatReply(form).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      dialogVisible.value = false
      getList()
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

Promise.all([loadAccounts()]).finally(() => {
  getList()
})
</script>
