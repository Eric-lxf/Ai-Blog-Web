<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12" title="群发支持文本与图文(mpnews)。按标签发送需先在「用户标签」同步标签；图文 media_id 来自草稿箱/素材。" />
    <el-row :gutter="16" class="mb12">
      <el-col :span="24">
        <el-card header="新建群发">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="账号" prop="accountId">
                  <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%" @change="loadTags">
                    <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="消息类型" prop="msgType">
                  <el-select v-model="form.msgType" style="width: 100%">
                    <el-option label="文本" value="text" /><el-option label="图文(mpnews)" value="mpnews" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="发送范围">
                  <el-radio-group v-model="form.isToAll">
                    <el-radio :label="true">全部粉丝</el-radio>
                    <el-radio :label="false">按标签</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8" v-if="!form.isToAll">
                <el-form-item label="微信标签" prop="wechatTagId">
                  <el-select v-model="form.wechatTagId" filterable placeholder="选择标签" style="width: 100%">
                    <el-option v-for="item in tagOptions" :key="item.wechatTagId" :label="`${item.name} (${item.wechatTagId})`" :value="item.wechatTagId" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="16" v-if="form.msgType === 'text'">
                <el-form-item label="文本内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
              </el-col>
              <el-col :span="16" v-else>
                <el-form-item label="media_id" prop="mediaId"><el-input v-model="form.mediaId" placeholder="图文 media_id" /></el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="warning" plain v-hasPermi="['wechat:mass:preview']" @click="openPreview">预览</el-button>
              <el-button type="primary" :loading="submitLoading" v-hasPermi="['wechat:mass:send']" @click="submitForm">发送群发</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
    <el-card header="群发记录">
      <el-form :inline="true" class="mb8">
        <el-form-item label="账号">
          <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 220px">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="list">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="类型" prop="msgType" width="90" />
        <el-table-column label="范围" width="100"><template #default="{ row }">{{ row.isToAll === 1 ? '全部' : `标签${row.wechatTagId}` }}</template></el-table-column>
        <el-table-column label="状态" prop="status" width="90" />
        <el-table-column label="msg_id" prop="msgId" width="120" />
        <el-table-column label="内容/media" min-width="200" show-overflow-tooltip><template #default="{ row }">{{ row.content || row.mediaId }}</template></el-table-column>
        <el-table-column label="时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }"><el-button link type="primary" v-hasPermi="['wechat:mass:list']" :disabled="!row.msgId" @click="handleSync(row)">查状态</el-button></template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>
    <el-dialog v-model="previewVisible" title="群发预览" width="520px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="预览 OpenID"><el-input v-model="previewOpenId" placeholder="接收预览的 OpenID" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="previewVisible = false">取消</el-button>
        <el-button type="primary" :loading="previewLoading" @click="submitPreview">发送预览</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { listWechatAccountOptions, listWechatMass, listWechatTagOptions, previewWechatMass, sendWechatMass, syncWechatMassStatus } from '@/api/wechat'
defineOptions({ name: 'WechatMass' })
const { proxy } = getCurrentInstance()
const formRef = ref(); const loading = ref(false); const submitLoading = ref(false); const previewLoading = ref(false)
const previewVisible = ref(false); const previewOpenId = ref('')
const accountOptions = ref([]); const tagOptions = ref([]); const list = ref([]); const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined })
const form = reactive({ accountId: undefined, msgType: 'text', content: '', mediaId: '', isToAll: true, wechatTagId: undefined })
const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  msgType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function loadTags() {
  tagOptions.value = []
  if (!form.accountId) return
  listWechatTagOptions(form.accountId).then(res => { tagOptions.value = res.data || [] })
}
function getList() { loading.value = true; listWechatMass(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function openPreview() {
  if (!form.accountId) { proxy.$modal.msgWarning('请先选择账号'); return }
  previewOpenId.value = ''; previewVisible.value = true
}
function submitPreview() {
  if (!previewOpenId.value?.trim()) { proxy.$modal.msgWarning('请输入预览 OpenID'); return }
  previewLoading.value = true
  previewWechatMass({ accountId: form.accountId, openId: previewOpenId.value.trim(), msgType: form.msgType, content: form.content, mediaId: form.mediaId })
    .then(() => { proxy.$modal.msgSuccess('预览已发送'); previewVisible.value = false }).finally(() => { previewLoading.value = false })
}
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    if (form.msgType === 'text' && !form.content?.trim()) { proxy.$modal.msgError('请输入文本内容'); return }
    if (form.msgType === 'mpnews' && !form.mediaId?.trim()) { proxy.$modal.msgError('请输入 media_id'); return }
    if (!form.isToAll && !form.wechatTagId) { proxy.$modal.msgError('请选择标签'); return }
    proxy.$modal.confirm('确认发送群发消息吗？').then(() => {
      submitLoading.value = true
      return sendWechatMass({ ...form, content: form.content?.trim(), mediaId: form.mediaId?.trim() })
    }).then(() => { proxy.$modal.msgSuccess('群发已提交'); getList() }).finally(() => { submitLoading.value = false }).catch(() => {})
  })
}
function handleSync(row) {
  syncWechatMassStatus(row.id).then(res => {
    proxy.$modal.msgSuccess('状态：' + JSON.stringify(res.data || {}))
    getList()
  })
}
Promise.all([loadAccounts()]).finally(() => getList())
</script>
