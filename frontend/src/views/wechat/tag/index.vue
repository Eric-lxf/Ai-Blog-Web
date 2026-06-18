<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12" title="标签数据以微信为准。新建/改名/删除会先调用微信 API，再同步本地镜像。" />
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签名">
        <el-input v-model="queryParams.keyword" clearable placeholder="标签名称" style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:tag:add']" @click="openDialog()">新增标签</el-button>
        <el-button type="success" plain icon="Download" v-hasPermi="['wechat:tag:sync']" :disabled="!queryParams.accountId" @click="handleSync">从微信同步</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="微信TagID" prop="wechatTagId" width="100" />
      <el-table-column label="标签名" prop="name" min-width="140" />
      <el-table-column label="粉丝数" prop="fanCount" width="90" />
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:tag:edit']" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" v-hasPermi="['wechat:tag:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑标签' : '新增标签'" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="accountId">
          <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签名" prop="name"><el-input v-model="form.name" maxlength="30" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { deleteWechatTag, listWechatAccountOptions, listWechatTag, saveWechatTag, syncWechatTag } from '@/api/wechat'
defineOptions({ name: 'WechatTag' })
const { proxy } = getCurrentInstance()
const loading = ref(false); const submitLoading = ref(false); const dialogVisible = ref(false)
const list = ref([]); const total = ref(0); const accountOptions = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
const form = reactive({ id: undefined, accountId: undefined, name: '' })
const formRef = ref()
const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  name: [{ required: true, message: '请输入标签名', trigger: 'blur' }]
}
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function getList() { loading.value = true; listWechatTag(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined }; getList() }
function openDialog(row) { Object.assign(form, { id: undefined, accountId: queryParams.value.accountId, name: '' }); if (row) Object.assign(form, row); dialogVisible.value = true }
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    saveWechatTag(form).then(() => { proxy.$modal.msgSuccess('保存成功'); dialogVisible.value = false; getList() }).finally(() => { submitLoading.value = false })
  })
}
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除标签「${row.name}」吗？`).then(() => deleteWechatTag(row.id)).then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {})
}
function handleSync() {
  if (!queryParams.value.accountId) { proxy.$modal.msgWarning('请先选择账号'); return }
  syncWechatTag(queryParams.value.accountId).then(res => {
    proxy.$modal.msgSuccess(`同步完成，共 ${res.data?.synced || 0} 个标签`)
    getList()
  })
}
Promise.all([loadAccounts()]).finally(() => getList())
</script>
