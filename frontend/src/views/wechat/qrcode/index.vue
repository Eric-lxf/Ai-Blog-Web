<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12" title="带参数二维码仅认证服务号可用。扫码后通过服务器回调接收 SCAN / 关注事件，可在「自动回复」中配置扫码回复（关键词填场景值）。" />
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="渠道名/场景值" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:qrcode:add']" @click="openDialog()">生成二维码</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="渠道名称" prop="name" min-width="120" />
      <el-table-column label="账号" width="120">
        <template #default="{ row }">{{ accountNameMap[row.accountId] || row.accountId }}</template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ row.qrType === 'permanent' ? '永久' : '临时' }}</template>
      </el-table-column>
      <el-table-column label="场景值" min-width="120">
        <template #default="{ row }">{{ row.sceneType === 'int' ? row.sceneId : row.sceneStr }}</template>
      </el-table-column>
      <el-table-column label="扫码次数" prop="scanCount" width="90" align="center" />
      <el-table-column label="过期时间" width="170">
        <template #default="{ row }">{{ row.expireTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="二维码" width="100" align="center">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" :preview-src-list="[row.imageUrl]" fit="contain" style="width: 48px; height: 48px" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row)">查看</el-button>
          <el-button link type="danger" v-hasPermi="['wechat:qrcode:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="dialogVisible" title="生成带参数二维码" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="账号" prop="accountId">
          <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="如：线下活动A" />
        </el-form-item>
        <el-form-item label="二维码类型" prop="qrType">
          <el-radio-group v-model="form.qrType">
            <el-radio label="temp">临时（最长30天）</el-radio>
            <el-radio label="permanent">永久（最多10万个）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="场景类型" prop="sceneType">
          <el-radio-group v-model="form.sceneType">
            <el-radio label="str">字符串</el-radio>
            <el-radio label="int">整型</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.sceneType === 'str'" label="场景字符串" prop="sceneStr">
          <el-input v-model="form.sceneStr" maxlength="64" placeholder="1-64字符，如 channel_offline_a" />
        </el-form-item>
        <el-form-item v-else label="场景整型" prop="sceneId">
          <el-input-number v-model="form.sceneId" :min="1" :max="100000" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.qrType === 'temp'" label="有效期(秒)" prop="expireSeconds">
          <el-input-number v-model="form.expireSeconds" :min="60" :max="2592000" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="二维码详情" width="520px" append-to-body>
      <div v-if="previewRow" class="preview-wrap">
        <img v-if="previewRow.imageUrl" :src="previewRow.imageUrl" alt="qrcode" class="preview-image" />
        <p>场景值：{{ previewRow.sceneType === 'int' ? previewRow.sceneId : previewRow.sceneStr }}</p>
        <p>扫码次数：{{ previewRow.scanCount || 0 }}</p>
        <el-link v-if="previewRow.imageUrl" :href="previewRow.imageUrl" target="_blank" type="primary">下载二维码图片</el-link>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { createWechatQrcode, deleteWechatQrcode, listWechatAccountOptions, listWechatQrcode } from '@/api/wechat'

defineOptions({ name: 'WechatQrcode' })

const { proxy } = getCurrentInstance()
const formRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const previewRow = ref(null)
const list = ref([])
const total = ref(0)
const accountOptions = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
const form = reactive({
  accountId: undefined,
  name: '',
  qrType: 'temp',
  sceneType: 'str',
  sceneId: 1,
  sceneStr: '',
  expireSeconds: 604800,
  remark: ''
})
const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  name: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  qrType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  sceneType: [{ required: true, message: '请选择场景类型', trigger: 'change' }],
  sceneStr: [{ validator: (_, value, callback) => {
    if (form.sceneType === 'str' && !value?.trim()) callback(new Error('请输入场景字符串'))
    else callback()
  }, trigger: 'blur' }],
  sceneId: [{ validator: (_, value, callback) => {
    if (form.sceneType === 'int' && (value == null || value < 1)) callback(new Error('请输入场景整型值'))
    else callback()
  }, trigger: 'change' }]
}

const accountNameMap = computed(() => {
  const map = {}
  accountOptions.value.forEach(item => { map[item.id] = item.name })
  return map
})

function loadAccounts() {
  return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] })
}
function getList() {
  loading.value = true
  listWechatQrcode(queryParams.value).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined }
  getList()
}
function resetForm() {
  Object.assign(form, {
    accountId: undefined,
    name: '',
    qrType: 'temp',
    sceneType: 'str',
    sceneId: 1,
    sceneStr: '',
    expireSeconds: 604800,
    remark: ''
  })
  formRef.value?.clearValidate()
}
function openDialog() {
  resetForm()
  dialogVisible.value = true
}
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    const payload = {
      accountId: form.accountId,
      name: form.name,
      qrType: form.qrType,
      sceneType: form.sceneType,
      sceneId: form.sceneType === 'int' ? form.sceneId : undefined,
      sceneStr: form.sceneType === 'str' ? form.sceneStr : undefined,
      expireSeconds: form.qrType === 'temp' ? form.expireSeconds : undefined,
      remark: form.remark
    }
    createWechatQrcode(payload).then(() => {
      proxy.$modal.msgSuccess('二维码已生成')
      dialogVisible.value = false
      getList()
    }).finally(() => { submitLoading.value = false })
  })
}
function openPreview(row) {
  previewRow.value = row
  previewVisible.value = true
}
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除渠道「${row.name}」的本地记录吗？微信侧已生成的码无法撤回。`).then(() => deleteWechatQrcode(row.id))
    .then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {})
}

loadAccounts().finally(() => getList())
</script>
<style scoped>
.preview-wrap { text-align: center; }
.preview-image { width: 220px; height: 220px; object-fit: contain; margin-bottom: 12px; }
</style>
