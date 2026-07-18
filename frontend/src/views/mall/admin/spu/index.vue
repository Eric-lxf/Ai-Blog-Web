<script setup>
defineOptions({ name: 'MallAdminSpu' })

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMallBrandOptions } from '@/api/mall/brand'
import { listMallCategoryOptions } from '@/api/mall/category'
import {
  addMallSpu,
  delMallSpu,
  getMallSpu,
  listMallSpu,
  updateMallSpu,
  updateMallSpuStatus
} from '@/api/mall/spu'
import { resolveUploadUrl } from '@/utils/blogAssets'

const loading = ref(false)
const saving = ref(false)
const spuList = ref([])
const categoryList = ref([])
const brandList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref('')
const formRef = ref()
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  categoryId: undefined,
  brandId: undefined,
  status: undefined
})
const form = reactive({
  id: undefined,
  name: '',
  subtitle: '',
  categoryId: undefined,
  brandId: undefined,
  mainImage: '',
  detailHtml: '',
  status: 'DRAFT',
  sort: 0,
  remark: '',
  skus: []
})
const rules = {
  name: [{ required: true, message: '商品名称不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类目', trigger: 'change' }]
}
const statusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  ON: { label: '上架', type: 'success' },
  OFF: { label: '下架', type: 'warning' }
}

const categoryOptions = computed(() => categoryList.value)

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

function buildTree(list, parentId = 0) {
  return list
    .filter(item => Number(item.parentId ?? 0) === Number(parentId))
    .sort((a, b) => Number(a.sort ?? 0) - Number(b.sort ?? 0))
    .map(item => ({
      ...item,
      children: buildTree(list, item.id)
    }))
}

function normalizeTree(rows) {
  if (rows.some(item => Array.isArray(item.children))) {
    return rows
  }
  return buildTree(rows)
}

function createSku() {
  return {
    id: undefined,
    skuCode: '',
    specsJson: '{}',
    price: 0,
    stock: 0,
    status: '0'
  }
}

function priceText(row) {
  const skus = row.skus || row.skuList || []
  if (!skus.length) return '-'
  const prices = skus.map(item => Number(item.price || 0))
  const min = Math.min(...prices).toFixed(2)
  const max = Math.max(...prices).toFixed(2)
  return min === max ? `¥${min}` : `¥${min} - ¥${max}`
}

async function loadOptions() {
  // 类目/品牌各自加载，避免一侧失败导致两侧下拉都空
  try {
    const categoryRes = await listMallCategoryOptions()
    categoryList.value = normalizeTree(normalizeRows(categoryRes))
  } catch (e) {
    console.error('加载类目下拉失败', e)
    categoryList.value = []
  }
  try {
    const brandRes = await listMallBrandOptions()
    brandList.value = normalizeRows(brandRes)
  } catch (e) {
    console.error('加载品牌下拉失败', e)
    brandList.value = []
  }
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallSpu(queryParams)
    spuList.value = normalizeRows(res)
    total.value = res.total ?? res.data?.total ?? spuList.value.length
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    subtitle: '',
    categoryId: undefined,
    brandId: undefined,
    mainImage: '',
    detailHtml: '',
    status: 'DRAFT',
    sort: 0,
    remark: '',
    skus: [createSku()]
  })
  formRef.value?.clearValidate()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 10,
    name: '',
    categoryId: undefined,
    brandId: undefined,
    status: undefined
  })
  getList()
}

function handleAdd() {
  resetForm()
  title.value = '新增商品'
  open.value = true
}

async function handleUpdate(row) {
  resetForm()
  const res = await getMallSpu(row.id)
  const detail = res.data || row
  Object.assign(form, {
    ...detail,
    skus: (detail.skus || detail.skuList || []).map(item => ({ ...createSku(), ...item }))
  })
  if (!form.skus.length) {
    form.skus.push(createSku())
  }
  title.value = '修改商品'
  open.value = true
}

function addSkuRow() {
  form.skus.push(createSku())
}

function removeSkuRow(index) {
  if (form.skus.length === 1) {
    ElMessage.warning('至少保留一个 SKU')
    return
  }
  form.skus.splice(index, 1)
}

function buildPayload() {
  return {
    ...form,
    skus: form.skus.map(item => ({
      ...item,
      price: Number(item.price || 0),
      stock: Number(item.stock || 0)
    }))
  }
}

async function submitForm() {
  await formRef.value.validate()
  const invalidSku = form.skus.find(item => !item.skuCode || Number(item.price) < 0 || Number(item.stock) < 0)
  if (invalidSku) {
    ElMessage.warning('请完整填写 SKU 编码、价格和库存')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateMallSpu(buildPayload())
    } else {
      await addMallSpu(buildPayload())
    }
    ElMessage.success('保存成功')
    open.value = false
    getList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除商品“${row.name}”吗？`, '提示', { type: 'warning' })
  await delMallSpu(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function handleChangeStatus(row, status) {
  await updateMallSpuStatus(row.id, status)
  ElMessage.success(status === 'ON' ? '已上架' : '已下架')
  getList()
}

onMounted(async () => {
  await loadOptions()
  await getList()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryParams" @submit.prevent="handleQuery">
        <el-form-item label="商品名称">
          <el-input v-model="queryParams.name" placeholder="请输入商品名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="类目">
          <el-tree-select
            v-model="queryParams.categoryId"
            :data="categoryOptions"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            clearable
            check-strictly
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="queryParams.brandId" placeholder="全部" clearable filterable style="width: 160px">
            <el-option v-for="item in brandList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="上架" value="ON" />
            <el-option label="下架" value="OFF" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" v-hasPermi="['mall:spu:add']" @click="handleAdd">
            新增
          </el-button>
        </el-col>
      </el-row>

      <el-table v-loading="loading" :data="spuList" stripe>
        <el-table-column label="主图" width="88" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.mainImage"
              :src="resolveUploadUrl(row.mainImage)"
              fit="cover"
              class="spu-image"
              :preview-src-list="[resolveUploadUrl(row.mainImage)]"
            />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="spu-name">{{ row.name }}</div>
            <div v-if="row.subtitle" class="spu-subtitle">{{ row.subtitle }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类目" width="140" show-overflow-tooltip />
        <el-table-column prop="brandName" label="品牌" width="120" show-overflow-tooltip />
        <el-table-column label="价格" width="150">
          <template #default="{ row }">{{ priceText(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="['mall:spu:edit']" @click="handleUpdate(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 'ON'"
              link
              type="success"
              v-hasPermi="['mall:spu:publish']"
              @click="handleChangeStatus(row, 'ON')"
            >
              上架
            </el-button>
            <el-button
              v-else
              link
              type="warning"
              v-hasPermi="['mall:spu:publish']"
              @click="handleChangeStatus(row, 'OFF')"
            >
              下架
            </el-button>
            <el-button link type="danger" v-hasPermi="['mall:spu:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog v-model="open" :title="title" width="920px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题">
              <el-input v-model="form.subtitle" placeholder="请输入副标题" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类目" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryOptions"
                node-key="id"
                :props="{ label: 'name', children: 'children' }"
                check-strictly
                filterable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-select v-model="form.brandId" placeholder="请选择品牌" clearable filterable style="width: 100%">
                <el-option v-for="item in brandList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="上架" value="ON" />
                <el-option label="下架" value="OFF" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主图">
              <image-upload v-model="form.mainImage" :limit="1" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详情HTML">
              <el-input v-model="form.detailHtml" type="textarea" :rows="4" placeholder="请输入商品详情 HTML" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="sku-header">
          <span>SKU 信息</span>
          <el-button link type="primary" @click="addSkuRow">新增 SKU</el-button>
        </div>
        <el-table :data="form.skus" border>
          <el-table-column label="SKU编码" min-width="170">
            <template #default="{ row }">
              <el-input v-model="row.skuCode" placeholder="如 DEMO-SKU-BLACK" />
            </template>
          </el-table-column>
          <el-table-column label="规格JSON" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.specsJson" placeholder='{"颜色":"黑色"}' />
            </template>
          </el-table-column>
          <el-table-column label="价格" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" :precision="0" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-select v-model="row.status">
                <el-option label="启用" value="0" />
                <el-option label="停用" value="1" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeSkuRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.spu-image {
  width: 52px;
  height: 52px;
  border-radius: 6px;
}

.spu-name {
  font-weight: 600;
  color: #303133;
}

.spu-subtitle,
.text-muted {
  color: #909399;
  font-size: 12px;
}

.sku-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
  font-weight: 600;
}
</style>
