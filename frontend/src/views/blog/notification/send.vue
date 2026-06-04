<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <span>发送系统通知</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 640px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="可选，如 /blog/1" />
        </el-form-item>
        <el-form-item label="接收用户">
          <el-input
            v-model="userIdsText"
            type="textarea"
            :rows="2"
            placeholder="留空表示全部正常用户；多个用户ID用英文逗号分隔"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">发送</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { sendSystemNotification } from '@/api/blog/notification'

defineOptions({ name: 'BlogNotificationSend' })

const { proxy } = getCurrentInstance()
const formRef = ref(null)
const submitting = ref(false)
const userIdsText = ref('')

const form = ref({
  title: '',
  content: '',
  linkUrl: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

function reset() {
  form.value = { title: '', content: '', linkUrl: '' }
  userIdsText.value = ''
  formRef.value?.resetFields()
}

function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    const payload = { ...form.value }
    const raw = userIdsText.value.trim()
    if (raw) {
      payload.userIds = raw.split(',').map(s => Number(s.trim())).filter(n => !Number.isNaN(n) && n > 0)
      if (!payload.userIds.length) {
        proxy.$modal.msgWarning('用户ID格式不正确')
        return
      }
    }
    submitting.value = true
    sendSystemNotification(payload).then(() => {
      proxy.$modal.msgSuccess('发送成功')
      reset()
    }).finally(() => {
      submitting.value = false
    })
  })
}
</script>
