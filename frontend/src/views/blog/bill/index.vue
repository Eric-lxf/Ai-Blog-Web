<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="query" inline class="search-form">
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
        <el-button v-hasPermi="['blog:bill:recognize']" type="success" plain :icon="Camera" @click="openRecognize">AI 识别</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="消费日期" prop="billDate" width="110" />
      <el-table-column label="商户" prop="merchant" show-overflow-tooltip />
      <el-table-column label="类目" prop="category" width="110" />
      <el-table-column label="金额（元）" prop="amount" width="120" align="right">
        <template #default="{ row }">
          <span class="amount-text">{{ row.amount?.toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" prop="paymentMethod" width="110" />
      <el-table-column label="来源" prop="sourceName" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.source === 1 ? 'success' : 'info'" size="small">{{ row.sourceName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AI 置信度" prop="aiConfidence" width="100" align="center">
        <template #default="{ row }">
          <el-progress v-if="row.source === 1 && row.aiConfidence" :percentage="row.aiConfidence" :stroke-width="6" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="note" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['blog:bill:edit']" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button v-hasPermi="['blog:bill:remove']" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="query.pageNum"
      v-model:limit="query.pageSize"
      @pagination="loadList"
    />

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="formVisible" :title="formTitle" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="消费日期" prop="billDate">
          <el-date-picker v-model="form.billDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="商户名称" prop="merchant">
          <el-input v-model="form.merchant" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="消费类目" prop="category">
          <el-select v-model="form.category" placeholder="请选择" style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select v-model="form.paymentMethod" clearable placeholder="请选择" style="width:100%">
            <el-option label="微信支付" value="微信支付" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="银行卡" value="银行卡" />
            <el-option label="现金" value="现金" />
            <el-option label="其他" value="其他" />
          </el-select>
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

    <!-- AI 识别 Dialog（三步：上传→预览→确认保存） -->
    <el-dialog v-model="recognizeVisible" title="AI 识别账单" width="560px" append-to-body>
      <el-steps :active="recognizeStep" align-center class="mb20">
        <el-step title="上传图片" />
        <el-step title="AI 识别" />
        <el-step title="确认保存" />
      </el-steps>

      <!-- Step 0: 上传 -->
      <div v-if="recognizeStep === 0" class="step-body">
        <el-upload
          class="upload-area"
          drag
          :auto-upload="false"
          accept="image/*"
          :on-change="onFileChange"
          :show-file-list="false"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">拖拽图片到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 JPG / PNG，建议清晰原图（≤7MB）；本地图片会转为 Base64 后识别</div>
          </template>
        </el-upload>
        <div v-if="uploadPreviewUrl" class="preview-wrap">
          <img :src="uploadPreviewUrl" class="preview-img" alt="preview" />
        </div>
        <el-input v-model="recognizeImageUrl" placeholder="或粘贴可公网访问的图片 URL / data:image Base64" class="mt10" />
        <div class="step-actions">
          <el-button type="primary" :disabled="!recognizeImageUrl || recognizeImageUrl.startsWith('blob:')" @click="doRecognize">开始识别</el-button>
        </div>
      </div>

      <!-- Step 1: 识别中 -->
      <div v-if="recognizeStep === 1" class="step-body center">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <p class="mt10">AI 正在识别，请稍候…</p>
      </div>

      <!-- Step 2: 确认 -->
      <div v-if="recognizeStep === 2" class="step-body">
        <el-form :model="recognizeResult" label-width="90px">
          <el-form-item label="消费日期">
            <el-date-picker v-model="recognizeResult.billDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="商户名称">
            <el-input v-model="recognizeResult.merchant" />
          </el-form-item>
          <el-form-item label="消费类目">
            <el-select v-model="recognizeResult.category" style="width:100%">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="金额">
            <el-input-number v-model="recognizeResult.amount" :min="0.01" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="支付方式">
            <el-input v-model="recognizeResult.paymentMethod" />
          </el-form-item>
          <el-form-item label="AI 置信度">
            <el-progress :percentage="recognizeResult.aiConfidence || 0" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button v-if="recognizeStep === 2" @click="recognizeStep = 0">重新识别</el-button>
        <el-button v-if="recognizeStep === 2" type="primary" :loading="saving" @click="saveRecognized">保存账单</el-button>
        <el-button v-if="recognizeStep === 0" @click="recognizeVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Camera, Upload, Loading } from '@element-plus/icons-vue'
import { listBill, getBill, addBill, updateBill, deleteBill, recognizeBill } from '@/api/blog/bill'

const categories = ['餐饮食品', '购物消费', '交通出行', '水电燃气', '医疗健康', '健身娱乐', '服饰购物', '其他']

// ── 列表 ──────────────────────────────────────────────────────
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1, pageSize: 10,
  category: '', dateRange: null
})

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.category) params.category = query.category
    if (query.dateRange?.length === 2) {
      params.startDate = query.dateRange[0]
      params.endDate   = query.dateRange[1]
    }
    const res = await listBill(params)
    list.value  = res.rows  || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.category  = ''
  query.dateRange = null
  query.pageNum   = 1
  loadList()
}

onMounted(loadList)

// ── 新增/编辑 ─────────────────────────────────────────────────
const formVisible = ref(false)
const formTitle   = ref('新增账单')
const saving      = ref(false)
const formRef     = ref(null)
const form        = reactive({
  id: null, billDate: '', merchant: '', category: '',
  amount: null, paymentMethod: '', note: '', source: 0
})
const rules = {
  billDate: [{ required: true, message: '请选择消费日期', trigger: 'change' }],
  category: [{ required: true, message: '请选择消费类目', trigger: 'change' }],
  amount:   [{ required: true, message: '请填写金额',   trigger: 'blur'   }]
}

function openAdd() {
  Object.assign(form, { id: null, billDate: '', merchant: '', category: '', amount: null, paymentMethod: '', note: '', source: 0 })
  formTitle.value  = '新增账单'
  formVisible.value = true
}

async function openEdit(row) {
  const res = await getBill(row.id)
  Object.assign(form, res.data)
  formTitle.value  = '编辑账单'
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

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该账单记录？', '提示', { type: 'warning' })
  await deleteBill(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ── AI 识别 ───────────────────────────────────────────────────
const recognizeVisible  = ref(false)
const recognizeStep     = ref(0)
const recognizeImageUrl = ref('')
const uploadPreviewUrl  = ref('')
const recognizeResult   = reactive({})

function openRecognize() {
  recognizeStep.value     = 0
  recognizeImageUrl.value = ''
  if (uploadPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(uploadPreviewUrl.value)
  }
  uploadPreviewUrl.value  = ''
  recognizeVisible.value  = true
}

function onFileChange(file) {
  const raw = file.raw
  if (!raw) return
  if (raw.size > 7 * 1024 * 1024) {
    ElMessage.warning('图片过大，请压缩至 7MB 以内后再识别')
    return
  }
  if (uploadPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(uploadPreviewUrl.value)
  }
  uploadPreviewUrl.value = URL.createObjectURL(raw)
  // 远端 OCR（如 qwen3.5-ocr）无法访问浏览器 blob: URL，必须传 Base64 data URL
  recognizeImageUrl.value = ''
  const reader = new FileReader()
  reader.onload = () => {
    recognizeImageUrl.value = String(reader.result || '')
  }
  reader.onerror = () => {
    ElMessage.error('读取图片失败，请重试')
    recognizeImageUrl.value = ''
  }
  reader.readAsDataURL(raw)
}

async function doRecognize() {
  const imageUrl = (recognizeImageUrl.value || '').trim()
  if (!imageUrl || imageUrl.startsWith('blob:') || imageUrl.startsWith('file:')) {
    ElMessage.warning('请上传本地图片，或粘贴可公网访问的图片 URL')
    return
  }
  recognizeStep.value = 1
  try {
    const res = await recognizeBill({ imageUrl })
    Object.assign(recognizeResult, res.data)
    // 不把超大 Base64 写入账单记录，仅保留可持久化的 http(s) URL
    recognizeResult.imageUrl = imageUrl.startsWith('data:') ? null : imageUrl
    recognizeStep.value = 2
  } catch (e) {
    // 全局拦截器已对业务错误弹过提示，这里只回退步骤
    recognizeStep.value = 0
  }
}

async function saveRecognized() {
  saving.value = true
  try {
    await addBill({ ...recognizeResult, source: 1 })
    ElMessage.success('账单已保存')
    recognizeVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.amount-text { font-weight: 600; color: #e6a23c; }
.upload-area { width: 100%; }
.preview-wrap { margin-top: 12px; text-align: center; }
.preview-img  { max-height: 200px; border-radius: 6px; }
.step-body    { padding: 8px 0; }
.step-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
.center       { display: flex; flex-direction: column; align-items: center; padding: 32px 0; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.mt10 { margin-top: 10px; }
.mb20 { margin-bottom: 20px; }
</style>