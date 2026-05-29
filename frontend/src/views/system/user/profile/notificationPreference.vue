<template>
  <el-form label-width="140px" style="max-width: 520px">
    <el-form-item label="站内消息">
      <el-switch v-model="pref.enableInApp" :active-value="1" :inactive-value="0" />
    </el-form-item>
    <el-form-item label="邮件通知">
      <el-switch v-model="pref.enableEmail" :active-value="1" :inactive-value="0" />
    </el-form-item>
    <el-form-item label="评论通知">
      <el-switch v-model="pref.enableComment" :active-value="1" :inactive-value="0" />
    </el-form-item>
    <el-form-item label="回复通知">
      <el-switch v-model="pref.enableReply" :active-value="1" :inactive-value="0" />
    </el-form-item>
    <el-form-item label="系统通知">
      <el-switch v-model="pref.enableSystem" :active-value="1" :inactive-value="0" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { getNotificationPreference, updateNotificationPreference } from '@/api/blog/notification'

const { proxy } = getCurrentInstance()
const saving = ref(false)
const pref = ref({
  enableInApp: 1,
  enableEmail: 1,
  enableComment: 1,
  enableReply: 1,
  enableSystem: 1
})

function load() {
  getNotificationPreference().then(res => {
    if (res.data) {
      pref.value = { ...pref.value, ...res.data }
    }
  })
}

function save() {
  saving.value = true
  updateNotificationPreference(pref.value).then(() => {
    proxy.$modal.msgSuccess('保存成功')
  }).finally(() => {
    saving.value = false
  })
}

load()
</script>
