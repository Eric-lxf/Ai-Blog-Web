<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="永久素材" name="asset">
        <el-form :inline="true" :model="assetQuery" class="mb8">
          <el-form-item label="账号">
            <el-select v-model="assetQuery.accountId" clearable filterable placeholder="全部账号" style="width: 200px">
              <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadAssets">搜索</el-button>
            <el-button type="primary" plain icon="Upload" v-hasPermi="['wechat:material:add']" @click="openUploadDialog">上传素材</el-button>
            <el-button type="info" plain icon="View" v-hasPermi="['wechat:material:query']" :disabled="!assetQuery.accountId" @click="loadWechatMaterials">查微信素材库</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="assetLoading" :data="assetList">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="名称" prop="name" min-width="140" />
          <el-table-column label="类型" prop="mediaType" width="90" />
          <el-table-column label="media_id" prop="mediaId" min-width="180" show-overflow-tooltip />
          <el-table-column label="URL" prop="url" min-width="200" show-overflow-tooltip />
          <el-table-column label="上传时间" prop="createTime" width="170" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button link type="danger" v-hasPermi="['wechat:material:remove']" @click="handleDeleteAsset(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="assetTotal > 0" :total="assetTotal" v-model:page="assetQuery.pageNum" v-model:limit="assetQuery.pageSize" @pagination="loadAssets" />
      </el-tab-pane>

      <el-tab-pane label="推送图文" name="article">
        <el-form :inline="true" :model="queryParams" class="mb8">
          <el-form-item label="账号ID"><el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" /></el-form-item>
          <el-form-item label="状态"><el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 130px"><el-option label="待上传" :value="0" /><el-option label="草稿成功" :value="1" /><el-option label="失败" :value="2" /></el-select></el-form-item>
          <el-form-item label="关键词"><el-input v-model="queryParams.keyword" placeholder="素材标题" clearable style="width: 220px" @keyup.enter="getList" /></el-form-item>
          <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="list">
          <el-table-column label="ID" prop="id" width="80" /><el-table-column label="账号ID" prop="accountId" width="90" />
          <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip /><el-table-column label="作者" prop="author" width="120" />
          <el-table-column label="摘要" prop="digest" min-width="200" show-overflow-tooltip /><el-table-column label="mediaId" prop="mediaId" min-width="170" show-overflow-tooltip />
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'">{{ row.status === 1 ? '草稿成功' : row.status === 2 ? '失败' : '待上传' }}</el-tag></template></el-table-column>
          <el-table-column label="更新时间" prop="updateTime" width="170" />
          <el-table-column label="操作" width="120" align="center"><template #default="{ row }"><el-button link type="danger" v-hasPermi="['wechat:material:remove']" @click="handleDelete(row)">删除</el-button></template></el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="uploadVisible" title="上传永久素材" width="520px" append-to-body>
      <el-form ref="uploadRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="账号" prop="accountId">
          <el-select v-model="uploadForm.accountId" filterable placeholder="请选择账号" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="uploadForm.name" maxlength="100" /></el-form-item>
        <el-form-item label="类型" prop="mediaType">
          <el-select v-model="uploadForm.mediaType" style="width: 100%">
            <el-option label="封面(thumb)" value="thumb" />
            <el-option label="图片(image)" value="image" />
            <el-option label="正文图(content)" value="content" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件" prop="file">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleFileChange" :on-remove="handleFileRemove">
            <el-button type="primary">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="wechatMaterialVisible" title="微信素材库" width="760px" append-to-body>
      <el-input v-model="wechatMaterialJson" type="textarea" :rows="18" readonly />
    </el-dialog>
  </div>
</template>
<script setup>
import {
  batchGetWechatMaterial,
  deleteWechatMaterial,
  deleteWechatMediaAsset,
  listWechatAccountOptions,
  listWechatMaterial,
  listWechatMediaAsset,
  uploadWechatMediaAsset
} from '@/api/wechat'

defineOptions({ name: 'WechatMaterial' })
const { proxy } = getCurrentInstance()
const activeTab = ref('asset')
const loading = ref(false)
const assetLoading = ref(false)
const uploadLoading = ref(false)
const uploadVisible = ref(false)
const wechatMaterialVisible = ref(false)
const wechatMaterialJson = ref('')
const list = ref([])
const assetList = ref([])
const total = ref(0)
const assetTotal = ref(0)
const accountOptions = ref([])
const uploadRef = ref()
const uploadFile = ref(null)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined })
const assetQuery = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
const uploadForm = reactive({ accountId: undefined, name: '', mediaType: 'thumb' })
const uploadRules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  mediaType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function loadAccounts() {
  return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] })
}
function getList() {
  loading.value = true
  listWechatMaterial(queryParams.value).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}
function loadAssets() {
  assetLoading.value = true
  listWechatMediaAsset(assetQuery.value).then(res => {
    assetList.value = res.rows || []
    assetTotal.value = res.total || 0
  }).finally(() => { assetLoading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined }; getList() }
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除素材「${row.title}」吗？`).then(() => deleteWechatMaterial(row.id))
    .then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {})
}
function handleDeleteAsset(row) {
  proxy.$modal.confirm(`确认删除素材「${row.name}」吗？`).then(() => deleteWechatMediaAsset(row.id))
    .then(() => { proxy.$modal.msgSuccess('删除成功'); loadAssets() }).catch(() => {})
}
function openUploadDialog() {
  Object.assign(uploadForm, { accountId: assetQuery.accountId, name: '', mediaType: 'thumb' })
  uploadFile.value = null
  uploadVisible.value = true
  uploadRef.value?.clearValidate()
}
function handleFileChange(file) { uploadFile.value = file.raw }
function handleFileRemove() { uploadFile.value = null }
function submitUpload() {
  uploadRef.value.validate(valid => {
    if (!valid) return
    if (!uploadFile.value) { proxy.$modal.msgWarning('请选择文件'); return }
    uploadLoading.value = true
    const formData = new FormData()
    formData.append('accountId', uploadForm.accountId)
    formData.append('name', uploadForm.name)
    formData.append('mediaType', uploadForm.mediaType)
    formData.append('file', uploadFile.value)
    uploadWechatMediaAsset(formData).then(() => {
      proxy.$modal.msgSuccess('上传成功')
      uploadVisible.value = false
      loadAssets()
    }).finally(() => { uploadLoading.value = false })
  })
}
function loadWechatMaterials() {
  batchGetWechatMaterial({ accountId: assetQuery.accountId, type: 'image', offset: 0, count: 20 }).then(res => {
    wechatMaterialJson.value = JSON.stringify(res.data || {}, null, 2)
    wechatMaterialVisible.value = true
  })
}

loadAccounts().finally(() => { getList(); loadAssets() })
</script>
