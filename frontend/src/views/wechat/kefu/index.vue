<template>
  <div class="app-container">
    <el-alert :closable="false" type="warning" class="mb12" title="客服消息仅能在用户 48 小时内主动发过消息后发送。认证服务号能力，个人号可能报 48001。" />
    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 720px">
        <el-form-item label="账号" prop="accountId">
          <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%" @change="handleAccountChange">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="OpenID" prop="openId">
          <el-input v-model="form.openId" placeholder="粉丝 OpenID" @blur="checkSession">
            <template #append><el-button @click="checkSession">检测会话</el-button></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="sessionInfo">
          <el-tag :type="sessionInfo.canSend ? 'success' : 'danger'">{{ sessionInfo.reason }}</el-tag>
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="600" show-word-limit placeholder="文本客服消息" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitLoading" v-hasPermi="['wechat:kefu:send']" @click="submitForm">发送</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { checkWechatKefuSession, listWechatAccountOptions, sendWechatKefuMessage } from '@/api/wechat'
defineOptions({ name: 'WechatKefu' })
const { proxy } = getCurrentInstance()
const formRef = ref(); const submitLoading = ref(false); const accountOptions = ref([]); const sessionInfo = ref(null)
const form = reactive({ accountId: undefined, openId: '', content: '' })
const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  openId: [{ required: true, message: '请输入 OpenID', trigger: 'blur' }],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }]
}
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function handleAccountChange() { sessionInfo.value = null }
function checkSession() {
  if (!form.accountId || !form.openId?.trim()) return
  checkWechatKefuSession(form.accountId, form.openId.trim()).then(res => { sessionInfo.value = res.data || {} })
}
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    sendWechatKefuMessage({ ...form, openId: form.openId.trim(), content: form.content.trim() })
      .then(() => { proxy.$modal.msgSuccess('发送成功'); form.content = ''; sessionInfo.value = null })
      .finally(() => { submitLoading.value = false })
  })
}
loadAccounts()
</script>
