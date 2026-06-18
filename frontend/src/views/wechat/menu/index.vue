<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="菜单 JSON 关键字" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:menu:add']" @click="openDialog()">新增菜单</el-button>
        <el-button type="info" plain icon="View" v-hasPermi="['wechat:menu:query']" :disabled="!queryParams.accountId" @click="handleQueryWechat">查询微信菜单</el-button>
        <el-button type="warning" plain icon="Delete" v-hasPermi="['wechat:menu:publish']" :disabled="!queryParams.accountId" @click="handleDeleteWechat">删除微信菜单</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="账号名称" min-width="140">
        <template #default="{ row }">{{ accountNameMap[row.accountId] || '-' }}</template>
      </el-table-column>
      <el-table-column label="已发布" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublished === 1 ? 'success' : 'info'">{{ row.isPublished === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="菜单JSON" prop="menuJson" min-width="260" show-overflow-tooltip />
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" width="220" align="center">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:menu:edit']" @click="openDialog(row)">编辑</el-button>
          <el-button link type="success" v-hasPermi="['wechat:menu:publish']" @click="handlePublish(row)">发布</el-button>
          <el-button link type="danger" v-hasPermi="['wechat:menu:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="accountId">
          <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单JSON" prop="menuJson">
          <el-input v-model="form.menuJson" type="textarea" :rows="14" placeholder='按钮数组，例如：[{"type":"click","name":"今日歌曲","key":"V1001_TODAY_MUSIC"}]' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="wechatMenuVisible" title="微信当前菜单" width="760px" append-to-body>
      <el-input v-model="wechatMenuJson" type="textarea" :rows="18" readonly />
      <template #footer>
        <el-button type="primary" @click="wechatMenuVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { deleteWechatMenu, deleteWechatMenuFromWechat, getWechatMenuFromWechat, listWechatAccountOptions, listWechatMenu, publishWechatMenu, saveWechatMenu } from '@/api/wechat'
defineOptions({ name: 'WechatMenu' })
const { proxy } = getCurrentInstance()
const formRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const wechatMenuVisible = ref(false)
const wechatMenuJson = ref('')
const list = ref([])
const total = ref(0)
const accountOptions = ref([])
const accountNameMap = computed(() => { const map = {}; accountOptions.value.forEach(item => { map[item.id] = item.name }); return map })
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
const form = reactive({ id: undefined, accountId: undefined, menuJson: '' })
const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  menuJson: [{ required: true, message: '请输入菜单 JSON', trigger: 'blur' }]
}
const defaultMenuJson = '[{"type":"click","name":"示例菜单","key":"DEMO_KEY"}]'
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function resetForm() { Object.assign(form, { id: undefined, accountId: undefined, menuJson: defaultMenuJson }); formRef.value?.clearValidate() }
function getList() { loading.value = true; listWechatMenu(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined }; getList() }
function openDialog(row) { resetForm(); if (row) Object.assign(form, row); dialogVisible.value = true }
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    try { JSON.parse(form.menuJson) } catch { proxy.$modal.msgError('菜单 JSON 格式错误'); return }
    submitLoading.value = true
    saveWechatMenu(form).then(() => { proxy.$modal.msgSuccess('保存成功'); dialogVisible.value = false; getList() }).finally(() => { submitLoading.value = false })
  })
}
function handlePublish(row) {
  proxy.$modal.confirm('确认发布该菜单到微信吗？').then(() => publishWechatMenu(row.id)).then(() => { proxy.$modal.msgSuccess('发布成功'); getList() }).catch(() => {})
}
function handleDelete(row) {
  proxy.$modal.confirm('确认删除该本地菜单记录吗？').then(() => deleteWechatMenu(row.id)).then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {})
}
function handleQueryWechat() {
  if (!queryParams.value.accountId) {
    proxy.$modal.msgWarning('请先选择账号')
    return
  }
  getWechatMenuFromWechat(queryParams.value.accountId).then(res => {
    wechatMenuJson.value = JSON.stringify(res.data || {}, null, 2)
    wechatMenuVisible.value = true
  })
}
function handleDeleteWechat() {
  if (!queryParams.value.accountId) {
    proxy.$modal.msgWarning('请先选择账号')
    return
  }
  proxy.$modal.confirm('确认删除该账号在微信端的自定义菜单吗？').then(() => deleteWechatMenuFromWechat(queryParams.value.accountId))
    .then(() => proxy.$modal.msgSuccess('微信菜单已删除')).catch(() => {})
}
Promise.all([loadAccounts()]).finally(() => getList())
</script>
