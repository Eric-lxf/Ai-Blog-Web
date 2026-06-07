module.exports = function wPages(w) {
  w('menu/index.vue', `<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="\u8d26\u53f7">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="\u5168\u90e8\u8d26\u53f7" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u5173\u952e\u8bcd">
        <el-input v-model="queryParams.keyword" placeholder="\u83dc\u5355 JSON \u5173\u952e\u5b57" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button>
        <el-button icon="Refresh" @click="resetQuery">\u91cd\u7f6e</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:menu:add']" @click="openDialog()">\u65b0\u589e\u83dc\u5355</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="\u8d26\u53f7ID" prop="accountId" width="90" />
      <el-table-column label="\u8d26\u53f7\u540d\u79f0" min-width="140">
        <template #default="{ row }">{{ accountNameMap[row.accountId] || '-' }}</template>
      </el-table-column>
      <el-table-column label="\u5df2\u53d1\u5e03" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublished === 1 ? 'success' : 'info'">{{ row.isPublished === 1 ? '\u662f' : '\u5426' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="\u83dc\u5355JSON" prop="menuJson" min-width="260" show-overflow-tooltip />
      <el-table-column label="\u66f4\u65b0\u65f6\u95f4" prop="updateTime" width="170" />
      <el-table-column label="\u64cd\u4f5c" width="170" align="center">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:menu:edit']" @click="openDialog(row)">\u7f16\u8f91</el-button>
          <el-button link type="success" v-hasPermi="['wechat:menu:publish']" @click="handlePublish(row)">\u53d1\u5e03</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="dialogVisible" :title="form.id ? '\u7f16\u8f91\u83dc\u5355' : '\u65b0\u589e\u83dc\u5355'" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="\u8d26\u53f7" prop="accountId">
          <el-select v-model="form.accountId" filterable placeholder="\u8bf7\u9009\u62e9\u8d26\u53f7" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="\u83dc\u5355JSON" prop="menuJson">
          <el-input v-model="form.menuJson" type="textarea" :rows="14" placeholder="\u8bf7\u8f93\u5165\u5b8c\u6574\u7684\u83dc\u5355 JSON" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">\u53d6\u6d88</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">\u4fdd\u5b58</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { listWechatAccountOptions, listWechatMenu, publishWechatMenu, saveWechatMenu } from '@/api/wechat'
defineOptions({ name: 'WechatMenu' })
const { proxy } = getCurrentInstance()
const formRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const list = ref([])
const total = ref(0)
const accountOptions = ref([])
const accountNameMap = computed(() => { const map = {}; accountOptions.value.forEach(item => { map[item.id] = item.name }); return map })
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
const form = reactive({ id: undefined, accountId: undefined, menuJson: '' })
const rules = {
  accountId: [{ required: true, message: '\u8bf7\u9009\u62e9\u8d26\u53f7', trigger: 'change' }],
  menuJson: [{ required: true, message: '\u8bf7\u8f93\u5165\u83dc\u5355 JSON', trigger: 'blur' }]
}
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function resetForm() { Object.assign(form, { id: undefined, accountId: undefined, menuJson: '{"button":[]}' }); formRef.value?.clearValidate() }
function getList() { loading.value = true; listWechatMenu(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined }; getList() }
function openDialog(row) { resetForm(); if (row) Object.assign(form, row); dialogVisible.value = true }
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    try { JSON.parse(form.menuJson) } catch { proxy.$modal.msgError('\u83dc\u5355 JSON \u683c\u5f0f\u9519\u8bef'); return }
    submitLoading.value = true
    saveWechatMenu(form).then(() => { proxy.$modal.msgSuccess('\u4fdd\u5b58\u6210\u529f'); dialogVisible.value = false; getList() }).finally(() => { submitLoading.value = false })
  })
}
function handlePublish(row) {
  proxy.$modal.confirm('\u786e\u8ba4\u53d1\u5e03\u8be5\u83dc\u5355\u5230\u5fae\u4fe1\u5417\uff1f').then(() => publishWechatMenu(row.id)).then(() => { proxy.$modal.msgSuccess('\u53d1\u5e03\u6210\u529f'); getList() }).catch(() => {})
}
Promise.all([loadAccounts()]).finally(() => getList())
</script>`)

  w('reply/index.vue', `<template>
  <div class="app-container">
    <el-alert :closable="false" type="warning" class="mb12" title="\u89c4\u5219\u901a\u8fc7\u670d\u52a1\u5668\u56de\u8c03\u751f\u6548\uff0c\u4e0d\u4f1a\u5199\u5165\u5fae\u4fe1\u516c\u4f17\u53f7\u540e\u53f0\u7684\u300c\u81ea\u52a8\u56de\u590d\u300d\u9875\u9762\u3002\u8bf7\u5728\u516c\u4f17\u53f7\u914d\u7f6e\u4e2d\u5f00\u542f\u5f00\u53d1\u8005\u6a21\u5f0f\uff0c\u5e76\u586b\u5199\u8d26\u53f7\u7ba1\u7406\u9875\u5c55\u793a\u7684\u56de\u8c03 URL\uff08\u542b accountId\uff09\u3002" />
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="\u8d26\u53f7">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="\u5168\u90e8\u8d26\u53f7" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u72b6\u6001">
        <el-select v-model="queryParams.status" clearable placeholder="\u5168\u90e8" style="width: 120px">
          <el-option label="\u542f\u7528" :value="1" /><el-option label="\u505c\u7528" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u5173\u952e\u8bcd">
        <el-input v-model="queryParams.keyword" placeholder="\u5173\u952e\u8bcd/\u5185\u5bb9" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button>
        <el-button icon="Refresh" @click="resetQuery">\u91cd\u7f6e</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:reply:add']" @click="openDialog()">\u65b0\u589e\u89c4\u5219</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="\u8d26\u53f7ID" prop="accountId" width="90" />
      <el-table-column label="\u7c7b\u578b" width="110"><template #default="{ row }">{{ replyTypeLabel(row.replyType) }}</template></el-table-column>
      <el-table-column label="\u5173\u952e\u8bcd" prop="keyword" min-width="130" show-overflow-tooltip />
      <el-table-column label="\u5185\u5bb9" prop="content" min-width="220" show-overflow-tooltip />
      <el-table-column label="\u5339\u914d\u65b9\u5f0f" width="100"><template #default="{ row }">{{ row.matchType === 2 ? '\u5168\u7b49' : '\u5305\u542b' }}</template></el-table-column>
      <el-table-column label="\u72b6\u6001" width="90" align="center"><template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '\u542f\u7528' : '\u505c\u7528' }}</el-tag></template></el-table-column>
      <el-table-column label="\u66f4\u65b0\u65f6\u95f4" prop="updateTime" width="170" />
      <el-table-column label="\u64cd\u4f5c" width="90" align="center"><template #default="{ row }"><el-button link type="primary" v-hasPermi="['wechat:reply:edit']" @click="openDialog(row)">\u7f16\u8f91</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="dialogVisible" :title="form.id ? '\u7f16\u8f91\u81ea\u52a8\u56de\u590d' : '\u65b0\u589e\u81ea\u52a8\u56de\u590d'" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="\u8d26\u53f7" prop="accountId"><el-select v-model="form.accountId" filterable placeholder="\u8bf7\u9009\u62e9\u8d26\u53f7" style="width: 100%"><el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="\u56de\u590d\u7c7b\u578b" prop="replyType"><el-select v-model="form.replyType" style="width: 100%"><el-option label="\u5173\u952e\u8bcd\u56de\u590d" value="keyword" /><el-option label="\u9ed8\u8ba4\u56de\u590d" value="default" /><el-option label="\u5173\u6ce8\u56de\u590d" value="subscribe" /></el-select></el-form-item>
        <el-form-item v-if="form.replyType === 'keyword'" label="\u5173\u952e\u8bcd" prop="keyword"><el-input v-model="form.keyword" maxlength="100" /></el-form-item>
        <el-form-item label="\u56de\u590d\u5185\u5bb9" prop="content"><el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit /></el-form-item>
        <el-form-item label="\u5339\u914d\u65b9\u5f0f" v-if="form.replyType === 'keyword'"><el-radio-group v-model="form.matchType"><el-radio :label="1">\u5305\u542b\u5339\u914d</el-radio><el-radio :label="2">\u5168\u7b49\u5339\u914d</el-radio></el-radio-group></el-form-item>
        <el-form-item label="\u72b6\u6001"><el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">\u53d6\u6d88</el-button><el-button type="primary" :loading="submitLoading" @click="submitForm">\u4fdd\u5b58</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { listWechatAccountOptions, listWechatReply, saveWechatReply } from '@/api/wechat'
defineOptions({ name: 'WechatReply' })
const { proxy } = getCurrentInstance()
const formRef = ref(); const loading = ref(false); const submitLoading = ref(false); const dialogVisible = ref(false)
const list = ref([]); const total = ref(0); const accountOptions = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined })
const form = reactive({ id: undefined, accountId: undefined, replyType: 'keyword', keyword: '', content: '', enabled: 1, matchType: 1 })
const rules = {
  accountId: [{ required: true, message: '\u8bf7\u9009\u62e9\u8d26\u53f7', trigger: 'change' }],
  replyType: [{ required: true, message: '\u8bf7\u9009\u62e9\u56de\u590d\u7c7b\u578b', trigger: 'change' }],
  keyword: [{ validator: (_, value, callback) => { if (form.replyType === 'keyword' && !value?.trim()) { callback(new Error('\u8bf7\u8f93\u5165\u5173\u952e\u8bcd')); return } callback() }, trigger: 'blur' }],
  content: [{ required: true, message: '\u8bf7\u8f93\u5165\u56de\u590d\u5185\u5bb9', trigger: 'blur' }]
}
function replyTypeLabel(type) { const map = { keyword: '\u5173\u952e\u8bcd', default: '\u9ed8\u8ba4\u56de\u590d', subscribe: '\u5173\u6ce8\u56de\u590d' }; return map[type] || type }
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function resetForm() { Object.assign(form, { id: undefined, accountId: undefined, replyType: 'keyword', keyword: '', content: '', enabled: 1, matchType: 1 }); formRef.value?.clearValidate() }
function getList() { loading.value = true; listWechatReply(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined }; getList() }
function openDialog(row) { resetForm(); if (row) Object.assign(form, row); dialogVisible.value = true }
function submitForm() { if (form.replyType !== 'keyword') { form.keyword = ''; form.matchType = 1 }; formRef.value.validate(valid => { if (!valid) return; submitLoading.value = true; saveWechatReply(form).then(() => { proxy.$modal.msgSuccess('\u4fdd\u5b58\u6210\u529f'); dialogVisible.value = false; getList() }).finally(() => { submitLoading.value = false }) }) }
Promise.all([loadAccounts()]).finally(() => getList())
</script>`)

  w('publish/index.vue', `<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="\u8d26\u53f7ID"><el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" placeholder="\u8d26\u53f7ID" /></el-form-item>
      <el-form-item label="\u72b6\u6001"><el-select v-model="queryParams.status" placeholder="\u5168\u90e8" clearable style="width: 140px">
        <el-option label="\u5f85\u5904\u7406" :value="0" /><el-option label="\u8349\u7a3f\u6210\u529f" :value="1" /><el-option label="\u53d1\u5e03\u4e2d" :value="2" /><el-option label="\u5df2\u53d1\u5e03" :value="3" /><el-option label="\u5931\u8d25" :value="4" />
      </el-select></el-form-item>
      <el-form-item label="\u5173\u952e\u8bcd"><el-input v-model="queryParams.keyword" placeholder="\u6587\u7ae0ID/\u6d88\u606fID" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button><el-button icon="Refresh" @click="resetQuery">\u91cd\u7f6e</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="\u8bb0\u5f55ID" prop="id" width="90" />
      <el-table-column label="\u8d26\u53f7ID" prop="accountId" width="90" />
      <el-table-column label="\u6587\u7ae0ID" prop="articleId" width="90" />
      <el-table-column label="\u7d20\u6750ID" prop="materialId" width="90" />
      <el-table-column label="\u63a8\u9001\u6a21\u5f0f" prop="publishMode" width="140"><template #default="{ row }">{{ row.publishMode === 'draft_and_publish' ? '\u8349\u7a3f\u5e76\u53d1\u5e03' : '\u4ec5\u8349\u7a3f' }}</template></el-table-column>
      <el-table-column label="\u72b6\u6001" width="110"><template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="\u4efb\u52a1\u53f7" prop="msgId" min-width="130" show-overflow-tooltip />
      <el-table-column label="\u9519\u8bef\u4fe1\u606f" prop="errorMessage" min-width="180" show-overflow-tooltip />
      <el-table-column label="\u66f4\u65b0\u65f6\u95f4" prop="updateTime" width="170" />
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>
<script setup>
import { listWechatPublish } from '@/api/wechat'
defineOptions({ name: 'WechatPublish' })
const loading = ref(false); const list = ref([]); const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined })
function statusLabel(status) { const map = { 0: '\u5f85\u5904\u7406', 1: '\u8349\u7a3f\u6210\u529f', 2: '\u53d1\u5e03\u4e2d', 3: '\u5df2\u53d1\u5e03', 4: '\u5931\u8d25' }; return map[status] || String(status ?? '') }
function statusTag(status) { const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'success', 4: 'danger' }; return map[status] || 'info' }
function getList() { loading.value = true; listWechatPublish(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined }; getList() }
getList()
</script>`)

  w('material/index.vue', `<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="\u8d26\u53f7ID"><el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="\u72b6\u6001"><el-select v-model="queryParams.status" clearable placeholder="\u5168\u90e8" style="width: 130px"><el-option label="\u5f85\u4e0a\u4f20" :value="0" /><el-option label="\u8349\u7a3f\u6210\u529f" :value="1" /><el-option label="\u5931\u8d25" :value="2" /></el-select></el-form-item>
      <el-form-item label="\u5173\u952e\u8bcd"><el-input v-model="queryParams.keyword" placeholder="\u7d20\u6750\u6807\u9898" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button><el-button icon="Refresh" @click="resetQuery">\u91cd\u7f6e</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" /><el-table-column label="\u8d26\u53f7ID" prop="accountId" width="90" />
      <el-table-column label="\u6807\u9898" prop="title" min-width="180" show-overflow-tooltip /><el-table-column label="\u4f5c\u8005" prop="author" width="120" />
      <el-table-column label="\u6458\u8981" prop="digest" min-width="200" show-overflow-tooltip /><el-table-column label="mediaId" prop="mediaId" min-width="170" show-overflow-tooltip />
      <el-table-column label="\u72b6\u6001" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'">{{ row.status === 1 ? '\u8349\u7a3f\u6210\u529f' : row.status === 2 ? '\u5931\u8d25' : '\u5f85\u4e0a\u4f20' }}</el-tag></template></el-table-column>
      <el-table-column label="\u66f4\u65b0\u65f6\u95f4" prop="updateTime" width="170" />
      <el-table-column label="\u64cd\u4f5c" width="120" align="center"><template #default="{ row }"><el-button link type="danger" v-hasPermi="['wechat:material:remove']" @click="handleDelete(row)">\u5220\u9664</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>
<script setup>
import { deleteWechatMaterial, listWechatMaterial } from '@/api/wechat'
defineOptions({ name: 'WechatMaterial' })
const { proxy } = getCurrentInstance(); const loading = ref(false); const list = ref([]); const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined })
function getList() { loading.value = true; listWechatMaterial(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined }; getList() }
function handleDelete(row) { proxy.$modal.confirm(\`\u786e\u8ba4\u5220\u9664\u7d20\u6750\u300c\${row.title}\u300d\u5417\uff1f\`).then(() => deleteWechatMaterial(row.id)).then(() => { proxy.$modal.msgSuccess('\u5220\u9664\u6210\u529f'); getList() }).catch(() => {}) }
getList()
</script>`)

  w('fans/index.vue', `<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12" title="\u7c89\u4e1d\u5217\u8868\u6765\u81ea\u5fae\u4fe1\u63a5\u53e3\u540c\u6b65\u6216\u5173\u6ce8/\u53d6\u5173\u56de\u8c03\u3002\u9996\u6b21\u8bf7\u9009\u62e9\u8d26\u53f7\u540e\u70b9\u300c\u62c9\u53d6\u7c89\u4e1d\u300d\u3002\u5fae\u4fe1\u5df2\u9650\u5236\u6635\u79f0\u62c9\u53d6\uff0c\u65b0\u7c89\u4e1d\u6635\u79f0\u53ef\u80fd\u4e3a\u7a7a\u3002" />
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="\u8d26\u53f7">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="\u5168\u90e8\u8d26\u53f7" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u72b6\u6001"><el-select v-model="queryParams.status" clearable placeholder="\u5168\u90e8" style="width: 120px"><el-option label="\u5df2\u5173\u6ce8" :value="1" /><el-option label="\u672a\u5173\u6ce8" :value="0" /></el-select></el-form-item>
      <el-form-item label="\u5173\u952e\u8bcd"><el-input v-model="queryParams.keyword" placeholder="\u6635\u79f0/OpenID" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button>
        <el-button icon="Refresh" @click="resetQuery">\u91cd\u7f6e</el-button>
        <el-button type="success" plain icon="Download" :loading="syncLoading" v-hasPermi="['wechat:fans:list']" @click="handleSync">\u62c9\u53d6\u7c89\u4e1d</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" /><el-table-column label="\u8d26\u53f7ID" prop="accountId" width="90" />
      <el-table-column label="\u6635\u79f0" prop="nickname" min-width="140" show-overflow-tooltip /><el-table-column label="OpenID" prop="openId" min-width="220" show-overflow-tooltip />
      <el-table-column label="UnionID" prop="unionId" min-width="180" show-overflow-tooltip />
      <el-table-column label="\u5173\u6ce8\u72b6\u6001" width="100"><template #default="{ row }"><el-tag :type="row.subscribeStatus === 1 ? 'success' : 'info'">{{ row.subscribeStatus === 1 ? '\u5df2\u5173\u6ce8' : '\u672a\u5173\u6ce8' }}</el-tag></template></el-table-column>
      <el-table-column label="\u5173\u6ce8\u65f6\u95f4" prop="subscribeTime" width="170" /><el-table-column label="\u66f4\u65b0\u65f6\u95f4" prop="updateTime" width="170" />
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>
<script setup>
import { listWechatAccountOptions, listWechatFans, syncWechatFans } from '@/api/wechat'
defineOptions({ name: 'WechatFans' })
const { proxy } = getCurrentInstance()
const loading = ref(false); const syncLoading = ref(false); const list = ref([]); const total = ref(0); const accountOptions = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined })
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function getList() { loading.value = true; listWechatFans(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, status: undefined, keyword: undefined }; getList() }
function handleSync() {
  if (!queryParams.value.accountId) { proxy.$modal.msgWarning('\u8bf7\u5148\u9009\u62e9\u8981\u540c\u6b65\u7684\u8d26\u53f7'); return }
  syncLoading.value = true
  syncWechatFans(queryParams.value.accountId).then(res => {
    const data = res.data || {}
    proxy.$modal.msgSuccess(\`\u540c\u6b65\u5b8c\u6210\uff1a\u5171 \${data.total || 0} \u4eba\uff0c\u5199\u5165 \${data.synced || 0} \u6761\`)
    getList()
  }).finally(() => { syncLoading.value = false })
}
Promise.all([loadAccounts()]).finally(() => getList())
</script>`)

  w('message/index.vue', `<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="\u8d26\u53f7ID"><el-input-number v-model="queryParams.accountId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="\u5173\u952e\u8bcd"><el-input v-model="queryParams.keyword" placeholder="OpenID/\u5185\u5bb9\u5173\u952e\u8bcd" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button><el-button icon="Refresh" @click="resetQuery">\u91cd\u7f6e</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" /><el-table-column label="\u8d26\u53f7ID" prop="accountId" width="90" />
      <el-table-column label="\u65b9\u5411" width="90"><template #default="{ row }"><el-tag :type="row.direction === 'in' ? 'success' : 'warning'">{{ row.direction === 'in' ? '\u63a5\u6536' : '\u53d1\u9001' }}</el-tag></template></el-table-column>
      <el-table-column label="OpenID" prop="openId" min-width="220" show-overflow-tooltip /><el-table-column label="\u6d88\u606f\u7c7b\u578b" prop="messageType" width="110" />
      <el-table-column label="\u4e8b\u4ef6\u7c7b\u578b" prop="eventType" width="120" show-overflow-tooltip /><el-table-column label="\u5185\u5bb9" prop="content" min-width="260" show-overflow-tooltip />
      <el-table-column label="\u65f6\u95f4" prop="createTime" width="170" />
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>
<script setup>
import { listWechatMessage } from '@/api/wechat'
defineOptions({ name: 'WechatMessage' })
const loading = ref(false); const list = ref([]); const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
function getList() { loading.value = true; listWechatMessage(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined }; getList() }
getList()
</script>`)
}
