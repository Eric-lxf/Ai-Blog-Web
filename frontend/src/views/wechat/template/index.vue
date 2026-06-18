<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card header="模板列表">
          <el-form :inline="true" class="mb8">
            <el-form-item label="账号">
              <el-select v-model="accountId" filterable placeholder="请选择账号" style="width: 220px" @change="loadTemplates">
                <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button icon="Refresh" @click="loadTemplates">刷新</el-button></el-form-item>
          </el-form>
          <el-table v-loading="loading" :data="templates" height="420" highlight-current-row @current-change="handleSelect">
            <el-table-column label="标题" prop="title" min-width="140" show-overflow-tooltip />
            <el-table-column label="模板ID" prop="templateId" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card header="发送模板消息">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="账号" prop="accountId">
              <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%">
                <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="OpenID" prop="openId"><el-input v-model="form.openId" /></el-form-item>
            <el-form-item label="模板ID" prop="templateId"><el-input v-model="form.templateId" /></el-form-item>
            <el-form-item label="跳转链接"><el-input v-model="form.url" placeholder="可选" /></el-form-item>
            <el-form-item label="模板内容" v-if="selectedTemplate"><el-input :model-value="selectedTemplate.content" type="textarea" :rows="4" readonly /></el-form-item>
            <el-form-item label="data JSON" prop="dataJson">
              <el-input v-model="form.dataJson" type="textarea" :rows="8" placeholder='{"keyword1":{"value":"内容","color":"#173177"}}' />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitLoading" v-hasPermi="['wechat:template:send']" @click="submitForm">发送</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { listWechatAccountOptions, listWechatTemplate, sendWechatTemplate } from '@/api/wechat'
defineOptions({ name: 'WechatTemplate' })
const { proxy } = getCurrentInstance()
const formRef = ref(); const loading = ref(false); const submitLoading = ref(false)
const accountOptions = ref([]); const accountId = ref(undefined); const templates = ref([]); const selectedTemplate = ref(null)
const form = reactive({ accountId: undefined, openId: '', templateId: '', url: '', dataJson: '{}' })
const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  openId: [{ required: true, message: '请输入 OpenID', trigger: 'blur' }],
  templateId: [{ required: true, message: '请输入模板 ID', trigger: 'blur' }],
  dataJson: [{ required: true, message: '请输入 data JSON', trigger: 'blur' }]
}
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function loadTemplates() {
  if (!accountId.value) { templates.value = []; return }
  loading.value = true
  listWechatTemplate(accountId.value).then(res => { templates.value = res.data || [] }).finally(() => { loading.value = false })
}
function handleSelect(row) {
  selectedTemplate.value = row
  if (row) { form.templateId = row.templateId; form.accountId = accountId.value }
}
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    let data
    try { data = JSON.parse(form.dataJson) } catch { proxy.$modal.msgError('data JSON 格式错误'); return }
    submitLoading.value = true
    sendWechatTemplate({ accountId: form.accountId, openId: form.openId.trim(), templateId: form.templateId.trim(), url: form.url, data })
      .then(() => proxy.$modal.msgSuccess('发送成功')).finally(() => { submitLoading.value = false })
  })
}
loadAccounts()
</script>
