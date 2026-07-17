<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="query" inline class="search-form">
      <el-form-item label="交易对方">
        <el-input v-model="query.merchant" clearable placeholder="通行宝等" style="width:150px" />
      </el-form-item>
      <el-form-item label="收/支">
        <el-select v-model="query.direction" clearable placeholder="全部" style="width:110px">
          <el-option label="支出" value="支出" />
          <el-option label="收入" value="收入" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="消费类目">
        <el-select v-model="query.category" clearable placeholder="全部类目" style="width:150px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadList">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button v-hasPermi="['blog:bill:add']" type="primary" plain :icon="Plus" @click="openAdd">新增账单</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button v-hasPermi="['blog:bill:recognize']" type="success" plain :icon="Camera" @click="openRecognize">AI 识别明细</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:bill:remove']"
          type="danger"
          plain
          :icon="Delete"
          :disabled="!selectedListRows.length"
          @click="handleBatchDelete"
        >批量删除</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格：对齐微信交易明细列 -->
    <el-table v-loading="loading" :data="list" stripe row-key="id" @selection-change="onListSelectionChange">
      <el-table-column type="selection" width="42" reserve-selection />
      <el-table-column label="交易时间" width="170">
        <template #default="{ row }">
          {{ formatTradeTime(row) }}
        </template>
      </el-table-column>
      <el-table-column label="交易类型" prop="tradeType" width="100" show-overflow-tooltip />
      <el-table-column label="收/支" prop="direction" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="row.direction === '收入' ? 'success' : (row.direction === '支出' ? 'warning' : 'info')" size="small">
            {{ row.direction || '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交易方式" prop="paymentMethod" min-width="140" show-overflow-tooltip />
      <el-table-column label="金额（元）" prop="amount" width="110" align="right">
        <template #default="{ row }">
          <span class="amount-text" :class="{ income: row.direction === '收入' }">{{ Number(row.amount || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交易对方" prop="merchant" min-width="120" show-overflow-tooltip />
      <el-table-column label="类目" prop="category" width="100" />
      <el-table-column label="交易单号" prop="tradeNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="商户单号" prop="merchantOrderNo" min-width="140" show-overflow-tooltip />
      <el-table-column label="来源" prop="sourceName" width="88" align="center">
        <template #default="{ row }">
          <el-tag :type="row.source === 1 ? 'success' : 'info'" size="small">{{ row.sourceName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['blog:bill:edit']" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button v-hasPermi="['blog:bill:remove']" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="query.pageNum"
      v-model:limit="query.pageSize"
      @pagination="loadList"
    />

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="交易时间" prop="tradeTime">
          <el-date-picker v-model="form.tradeTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" @change="onTradeTimeChange" />
        </el-form-item>
        <el-form-item label="消费日期" prop="billDate">
          <el-date-picker v-model="form.billDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="交易类型" prop="tradeType">
          <el-select v-model="form.tradeType" clearable allow-create filterable placeholder="商户消费/转账" style="width:100%">
            <el-option label="商户消费" value="商户消费" />
            <el-option label="转账" value="转账" />
            <el-option label="红包" value="红包" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="收/支" prop="direction">
          <el-select v-model="form.direction" style="width:100%">
            <el-option label="支出" value="支出" />
            <el-option label="收入" value="收入" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易对方" prop="merchant">
          <el-input v-model="form.merchant" placeholder="如：通行宝" />
        </el-form-item>
        <el-form-item label="消费类目" prop="category">
          <el-select v-model="form.category" placeholder="请选择" style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="交易方式" prop="paymentMethod">
          <el-input v-model="form.paymentMethod" placeholder="如：招商银行信用卡(1683)" />
        </el-form-item>
        <el-form-item label="交易单号" prop="tradeNo">
          <el-input v-model="form.tradeNo" />
        </el-form-item>
        <el-form-item label="商户单号" prop="merchantOrderNo">
          <el-input v-model="form.merchantOrderNo" />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="form.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- AI 识别 -->
    <el-dialog v-model="recognizeVisible" title="AI 识别交易明细" width="1200px" append-to-body>
      <el-steps :active="recognizeStep" align-center class="mb20">
        <el-step title="上传文件" />
        <el-step title="识别解析" />
        <el-step title="确认保存" />
      </el-steps>

      <div v-if="recognizeStep === 0" class="step-body">
        <el-upload
          class="upload-area"
          drag
          :auto-upload="false"
          accept="image/*,.pdf,.xls,.xlsx,application/pdf,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          :on-change="onFileChange"
          :show-file-list="false"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">拖拽微信明细截图 / PDF / Excel 到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">
              图片走视觉模型；PDF（微信明细证明等文字版）优先本地抽表，扫图 PDF 再按页 OCR；Excel 本地解析。单文件 ≤15MB
            </div>
          </template>
        </el-upload>
        <div v-if="uploadPreviewUrl" class="preview-wrap">
          <img :src="uploadPreviewUrl" class="preview-img" alt="preview" />
        </div>
        <div v-else-if="uploadFileName" class="preview-wrap file-name-tip">已选择：{{ uploadFileName }}</div>
        <el-input v-model="recognizeImageUrl" placeholder="或粘贴可公网访问的图片 URL / data:image Base64（仅图片）" class="mt10" />
        <div class="step-actions">
          <el-button type="primary" :disabled="!canStartRecognize" @click="doRecognize">开始识别</el-button>
        </div>
      </div>

      <div v-if="recognizeStep === 1" class="step-body center">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <p class="mt10">{{ recognizeLoadingText }}</p>
      </div>

      <div v-if="recognizeStep === 2" class="step-body">
        <div class="recognize-toolbar">
          <span>共识别 {{ recognizeResults.length }} 笔，已选 {{ selectedRecognizeIndexes.length }} 笔</span>
          <div>
            <el-button link type="primary" @click="selectAllRecognize">全选</el-button>
            <el-button link @click="clearRecognizeSelection">清空选择</el-button>
            <el-button link type="danger" :disabled="!selectedRecognizeIndexes.length" @click="removeSelectedRecognize">删除选中</el-button>
          </div>
        </div>
        <el-table
          ref="recognizeTableRef"
          :data="recognizeResults"
          border
          size="small"
          max-height="460"
          row-key="_key"
          @selection-change="onRecognizeSelectionChange"
        >
          <el-table-column type="selection" width="42" />
          <el-table-column label="交易时间" width="168">
            <template #default="{ row }">
              <el-date-picker v-model="row.tradeTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" size="small" style="width:150px" @change="() => syncBillDate(row)" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-input v-model="row.tradeType" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="收/支" width="90">
            <template #default="{ row }">
              <el-select v-model="row.direction" size="small" style="width:76px">
                <el-option label="支出" value="支出" />
                <el-option label="收入" value="收入" />
                <el-option label="其他" value="其他" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="交易对方" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.merchant" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="类目" width="120">
            <template #default="{ row }">
              <el-select v-model="row.category" size="small" style="width:104px">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :min="0.01" :precision="2" size="small" controls-position="right" style="width:100px" />
            </template>
          </el-table-column>
          <el-table-column label="交易方式" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.paymentMethod" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="交易单号" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.tradeNo" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="商户单号" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.merchantOrderNo" size="small" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button v-if="recognizeStep === 2" @click="recognizeStep = 0">重新识别</el-button>
        <el-button
          v-if="recognizeStep === 2"
          type="danger"
          plain
          :disabled="!selectedRecognizeIndexes.length"
          @click="removeSelectedRecognize"
        >删除选中</el-button>
        <el-button v-if="recognizeStep === 2" type="primary" :loading="saving" :disabled="!selectedRecognizeIndexes.length" @click="saveRecognized">
          保存选中（{{ selectedRecognizeIndexes.length }}）
        </el-button>
        <el-button v-if="recognizeStep === 0" @click="recognizeVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Camera, Upload, Loading } from '@element-plus/icons-vue'
import { listBill, getBill, addBill, updateBill, deleteBill, deleteBills, recognizeBill, recognizeBillFile } from '@/api/blog/bill'

const categories = ['餐饮食品', '购物消费', '交通出行', '水电燃气', '医疗健康', '健身娱乐', '服饰购物', '其他']

const loading = ref(false)
const list = ref([])
const total = ref(0)
const selectedListRows = ref([])
const query = reactive({
  pageNum: 1, pageSize: 10,
  merchant: '', direction: '', category: '', dateRange: null
})

function formatTradeTime(row) {
  if (row.tradeTime) return String(row.tradeTime).replace('T', ' ').slice(0, 19)
  return row.billDate || '—'
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.merchant) params.merchant = query.merchant
    if (query.direction) params.direction = query.direction
    if (query.category) params.category = query.category
    if (query.dateRange?.length === 2) {
      params.startDate = query.dateRange[0]
      params.endDate = query.dateRange[1]
    }
    const res = await listBill(params)
    list.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.merchant = ''
  query.direction = ''
  query.category = ''
  query.dateRange = null
  query.pageNum = 1
  loadList()
}

onMounted(loadList)

const formVisible = ref(false)
const formTitle = ref('新增账单')
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({
  id: null, tradeNo: '', billDate: '', tradeTime: '', tradeType: '商户消费',
  direction: '支出', merchant: '', category: '', amount: null,
  paymentMethod: '', merchantOrderNo: '', note: '', source: 0
})
const form = reactive(emptyForm())
const rules = {
  billDate: [{ required: true, message: '请选择消费日期', trigger: 'change' }],
  category: [{ required: true, message: '请选择消费类目', trigger: 'change' }],
  amount: [{ required: true, message: '请填写金额', trigger: 'blur' }]
}

function onTradeTimeChange(val) {
  if (val && String(val).length >= 10) {
    form.billDate = String(val).slice(0, 10)
  }
}

function openAdd() {
  Object.assign(form, emptyForm())
  formTitle.value = '新增账单'
  formVisible.value = true
}

async function openEdit(row) {
  const res = await getBill(row.id)
  Object.assign(form, emptyForm(), res.data)
  if (form.tradeTime) form.tradeTime = String(form.tradeTime).replace('T', ' ').slice(0, 19)
  formTitle.value = '编辑账单'
  formVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateBill(form)
      ElMessage.success('修改成功')
    } else {
      await addBill(form)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

function onListSelectionChange(rows) {
  selectedListRows.value = rows || []
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该账单记录？', '提示', { type: 'warning' })
  await deleteBill(row.id)
  ElMessage.success('删除成功')
  selectedListRows.value = selectedListRows.value.filter((r) => r.id !== row.id)
  loadList()
}

async function handleBatchDelete() {
  const rows = selectedListRows.value
  if (!rows.length) {
    ElMessage.warning('请先勾选要删除的账单')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${rows.length} 笔账单？`, '批量删除', { type: 'warning' })
  await deleteBills(rows.map((r) => r.id))
  ElMessage.success(`已删除 ${rows.length} 笔`)
  selectedListRows.value = []
  loadList()
}

const recognizeVisible = ref(false)
const recognizeStep = ref(0)
const recognizeImageUrl = ref('')
const uploadPreviewUrl = ref('')
const uploadFile = ref(null)
const uploadFileName = ref('')
const recognizeLoadingText = ref('正在识别全部明细…')
const recognizeResults = ref([])
const selectedRecognizeIndexes = ref([])
const recognizeTableRef = ref(null)
const persistImageUrl = ref(null)

const canStartRecognize = computed(() => {
  if (uploadFile.value) return true
  const url = (recognizeImageUrl.value || '').trim()
  return !!url && !url.startsWith('blob:') && !url.startsWith('file:')
})

function openRecognize() {
  recognizeStep.value = 0
  recognizeImageUrl.value = ''
  recognizeResults.value = []
  selectedRecognizeIndexes.value = []
  persistImageUrl.value = null
  uploadFile.value = null
  uploadFileName.value = ''
  recognizeLoadingText.value = '正在识别全部明细…'
  if (uploadPreviewUrl.value?.startsWith('blob:')) URL.revokeObjectURL(uploadPreviewUrl.value)
  uploadPreviewUrl.value = ''
  recognizeVisible.value = true
}

function fileExt(name = '') {
  const i = name.lastIndexOf('.')
  return i >= 0 ? name.slice(i + 1).toLowerCase() : ''
}

function isExcelFile(file) {
  const ext = fileExt(file.name)
  return ext === 'xls' || ext === 'xlsx'
    || (file.type || '').includes('sheet') || (file.type || '').includes('excel')
}

function isPdfFile(file) {
  return fileExt(file.name) === 'pdf' || (file.type || '').includes('pdf')
}

function isImageFile(file) {
  return (file.type || '').startsWith('image/')
    || ['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(fileExt(file.name))
}

async function onFileChange(file) {
  const raw = file.raw
  if (!raw) return
  if (raw.size > 15 * 1024 * 1024) {
    ElMessage.warning('文件过大，请压缩至 15MB 以内后再识别')
    return
  }
  if (!isImageFile(raw) && !isPdfFile(raw) && !isExcelFile(raw)) {
    ElMessage.warning('仅支持图片（JPG/PNG/WEBP）、PDF、Excel（xls/xlsx）')
    return
  }
  if (uploadPreviewUrl.value?.startsWith('blob:')) URL.revokeObjectURL(uploadPreviewUrl.value)
  uploadPreviewUrl.value = ''
  recognizeImageUrl.value = ''
  uploadFile.value = raw
  uploadFileName.value = raw.name || '未命名文件'
  // 图片仍生成预览；PDF/Excel 仅显示文件名，识别走 multipart
  if (isImageFile(raw)) {
    uploadPreviewUrl.value = URL.createObjectURL(raw)
  }
}

function syncBillDate(row) {
  if (row.tradeTime && String(row.tradeTime).length >= 10) {
    row.billDate = String(row.tradeTime).slice(0, 10)
  }
}

function normalizeRecognizeList(data) {
  const rows = Array.isArray(data) ? data : (data ? [data] : [])
  return rows.map((row, index) => {
    let tradeTime = row.tradeTime ? String(row.tradeTime).replace('T', ' ').slice(0, 19) : ''
    let billDate = row.billDate || (tradeTime ? tradeTime.slice(0, 10) : '')
    return {
      _key: index,
      tradeNo: row.tradeNo || '',
      billDate,
      tradeTime,
      tradeType: row.tradeType || '',
      direction: row.direction || '支出',
      merchant: row.merchant || '',
      category: row.category || '其他',
      amount: row.amount != null ? Number(row.amount) : null,
      paymentMethod: row.paymentMethod || '',
      merchantOrderNo: row.merchantOrderNo || '',
      note: row.note || '',
      aiConfidence: row.aiConfidence ?? null,
      source: 1
    }
  })
}

function onRecognizeSelectionChange(rows) {
  selectedRecognizeIndexes.value = rows.map((row) => recognizeResults.value.indexOf(row)).filter((i) => i >= 0)
}

async function selectAllRecognize() {
  await nextTick()
  recognizeTableRef.value?.toggleAllSelection?.()
}

function clearRecognizeSelection() {
  recognizeTableRef.value?.clearSelection?.()
  selectedRecognizeIndexes.value = []
}

async function removeSelectedRecognize() {
  const indexes = [...selectedRecognizeIndexes.value].sort((a, b) => b - a)
  if (!indexes.length) {
    ElMessage.warning('请先勾选要删除的识别结果')
    return
  }
  await ElMessageBox.confirm(`确认从识别结果中移除选中的 ${indexes.length} 笔？`, '删除选中', { type: 'warning' })
  const remain = recognizeResults.value.filter((_, i) => !indexes.includes(i))
  recognizeResults.value = remain.map((row, index) => ({ ...row, _key: index }))
  selectedRecognizeIndexes.value = []
  await nextTick()
  recognizeTableRef.value?.clearSelection?.()
  ElMessage.success(`已移除 ${indexes.length} 笔`)
}

async function doRecognize() {
  const file = uploadFile.value
  const imageUrl = (recognizeImageUrl.value || '').trim()
  if (!file && (!imageUrl || imageUrl.startsWith('blob:') || imageUrl.startsWith('file:'))) {
    ElMessage.warning('请上传图片 / PDF / Excel，或粘贴可公网访问的图片 URL')
    return
  }
  recognizeStep.value = 1
  try {
    let res
    if (file) {
      if (isExcelFile(file)) {
        recognizeLoadingText.value = '正在解析 Excel 账单明细…'
      } else if (isPdfFile(file)) {
        recognizeLoadingText.value = '正在解析 PDF 账单明细（文字版本地抽表，必要时按页识别）…'
      } else {
        recognizeLoadingText.value = '正在用账单视觉模型识别全部明细…'
      }
      res = await recognizeBillFile(file)
      persistImageUrl.value = null
    } else {
      recognizeLoadingText.value = '正在用账单视觉模型识别全部明细…'
      res = await recognizeBill({ imageUrl })
      persistImageUrl.value = imageUrl.startsWith('data:') ? null : imageUrl
    }
    recognizeResults.value = normalizeRecognizeList(res.data)
    selectedRecognizeIndexes.value = []
    recognizeStep.value = 2
    await nextTick()
    recognizeTableRef.value?.toggleAllSelection?.()
    if (recognizeResults.value.length <= 1 && file && !isExcelFile(file)) {
      ElMessage.warning('仅识别到 1 笔。若原文件有多行，请确认账单识别视觉模型为 qwen3.5-ocr，并重试清晰截图/PDF')
    }
  } catch (e) {
    recognizeStep.value = 0
  }
}

async function saveRecognized() {
  const selected = selectedRecognizeIndexes.value.map((i) => recognizeResults.value[i]).filter(Boolean)
  if (!selected.length) {
    ElMessage.warning('请至少选择一笔账单')
    return
  }
  const invalid = selected.find((row) => !row.billDate || !row.category || row.amount == null || row.amount <= 0)
  if (invalid) {
    ElMessage.warning('选中明细存在未填日期/类目/金额，请补全后再保存')
    return
  }
  saving.value = true
  try {
    let ok = 0
    for (const row of selected) {
      await addBill({
        tradeNo: row.tradeNo,
        billDate: row.billDate,
        tradeTime: row.tradeTime || null,
        tradeType: row.tradeType,
        direction: row.direction || '支出',
        merchant: row.merchant,
        category: row.category,
        amount: row.amount,
        paymentMethod: row.paymentMethod,
        merchantOrderNo: row.merchantOrderNo,
        note: row.note,
        aiConfidence: row.aiConfidence,
        imageUrl: persistImageUrl.value,
        source: 1
      })
      ok += 1
    }
    ElMessage.success(`已保存 ${ok} 笔账单`)
    recognizeVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.amount-text { font-weight: 600; color: #e6a23c; }
.amount-text.income { color: #67c23a; }
.upload-area { width: 100%; }
.preview-wrap { margin-top: 12px; text-align: center; }
.preview-img  { max-height: 200px; border-radius: 6px; }
.file-name-tip { color: var(--el-text-color-secondary); font-size: 13px; }
.step-body    { padding: 8px 0; }
.step-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
.center       { display: flex; flex-direction: column; align-items: center; padding: 32px 0; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.mt10 { margin-top: 10px; }
.mb20 { margin-bottom: 20px; }
.recognize-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
