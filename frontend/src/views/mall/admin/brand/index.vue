<script setup>
defineOptions({ name: 'MallAdminBrand' })

import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addMallBrand, delMallBrand, getMallBrand, listMallBrand, updateMallBrand } from '@/api/mall/brand'
import { resolveUploadUrl } from '@/utils/blogAssets'

const loading = ref(false)
const brandList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref('')
const formRef = ref()
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: undefined
})
const form = reactive({
  id: undefined,
  name: '',
  logo: '',
  sort: 0,
  status: '0',
  remark: ''
})
const rules = {
  name: [{ required: true, message: '品牌名称不能为空', trigger: 'blur' }]
}

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallBrand(queryParams)
    brandList.value = normalizeRows(res)
    total.value = res.total ?? res.data?.total ?? brandList.value.length
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    logo: '',
    sort: 0,
    status: '0',
    remark: ''
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
    status: undefined
  })
  getList()
}

function handleAdd() {
  resetForm()
  title.value = '新增品牌'
  open.value = true
}

async function handleUpdate(row) {
  resetForm()
  const res = await getMallBrand(row.id)
  Object.assign(form, res.data || row)
  title.value = '修改品牌'
  open.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除品牌“${row.name}”吗？`, '提示', { type: 'warning' })
  await delMallBrand(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function submitForm() {
  await formRef.value.validate()
  if (form.id) {
    await updateMallBrand(form)
  } else {
    await addMallBrand(form)
  }
  ElMessage.success('保存成功')
  open.value = false
  getList()
}

onMounted(getList)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryParams" @submit.prevent="handleQuery">
        <el-form-item label="品牌名称">
          <el-input v-model="queryParams.name" placeholder="请输入品牌名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" v-hasPermi="['mall:brand:add']" @click="handleAdd">
            新增
          </el-button>
        </el-col>
      </el-row>

      <el-table v-loading="loading" :data="brandList" stripe>
        <el-table-column label="Logo" width="90" align="center">
          <template #default="{ row }">
            <el-avatar v-if="row.logo" shape="square" :size="42" :src="resolveUploadUrl(row.logo)" />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="品牌名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="['mall:brand:edit']" @click="handleUpdate(row)">修改</el-button>
            <el-button link type="danger" v-hasPermi="['mall:brand:remove']" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="open" :title="title" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="品牌名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入品牌名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="Logo">
          <image-upload v-model="form.logo" :limit="1" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.text-muted {
  color: #909399;
}
</style>
