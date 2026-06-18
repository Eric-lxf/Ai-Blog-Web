<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12" title="关闭「功能开关」后，除本页外的所有微信公众号管理 API 将被拦截；微信服务器回调不受影响。" />
    <el-card v-loading="loading">
      <template #header><span>微信公众号模块配置</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" style="max-width: 640px">
        <el-form-item label="功能开关" prop="enabled">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="关闭" />
        </el-form-item>
        <el-form-item label="默认账号 ID" prop="defaultAccountId">
          <el-input v-model="form.defaultAccountId" placeholder="留空则各页面需手动选择账号" maxlength="20" />
        </el-form-item>
        <el-form-item label="回调密文模式" prop="callbackEncrypt">
          <el-switch v-model="form.callbackEncrypt" active-text="兼容/安全模式" inactive-text="明文模式" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitLoading" v-hasPermi="['wechat:config:edit']" @click="submitForm">保存配置</el-button>
          <el-button icon="Refresh" @click="loadConfig">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { getWechatModuleConfig, saveWechatModuleConfig } from '@/api/wechat'
defineOptions({ name: 'WechatConfig' })
const { proxy } = getCurrentInstance()
const formRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const form = reactive({ enabled: false, defaultAccountId: '', callbackEncrypt: false })
const rules = {
  enabled: [{ required: true, message: '请选择功能开关', trigger: 'change' }],
  callbackEncrypt: [{ required: true, message: '请选择回调模式', trigger: 'change' }]
}
function loadConfig() {
  loading.value = true
  getWechatModuleConfig().then(res => {
    const data = res.data || {}
    form.enabled = !!data.enabled
    form.defaultAccountId = data.defaultAccountId || ''
    form.callbackEncrypt = !!data.callbackEncrypt
  }).finally(() => { loading.value = false })
}
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    saveWechatModuleConfig(form).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      loadConfig()
    }).finally(() => { submitLoading.value = false })
  })
}
loadConfig()
</script>
