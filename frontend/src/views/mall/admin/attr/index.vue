<script setup>
defineOptions({ name: 'MallAdminAttr' })

import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addMallAttr,
  delMallAttr,
  getMallAttr,
  listMallAttr,
  listMallAttrOptions,
  replaceMallAttrOptions,
  updateMallAttr
} from '@/api/mall/attr'

const loading = ref(false)
const attrList = ref([])
const total = ref(0)
const open = ref(false)
const optionsOpen = ref(false)
const title = ref('')
const formRef = ref()
const optionsAttr = ref(null)
const optionsRows = ref([])
const optionsSaving = ref(false)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: undefined,
  inputType: undefined
})
const form = reactive({
  id: undefined,
  name: '',
  inputType: 'text',
  sort: 0,
  status: '0',
  remark: ''
})
const rules = {
  name: [{ required: true, message: '属性名称不能为空', trigger: 'blur' }],
  inputType: [{ required: true, message: '请选择录入类型', trigger: 'change' }]
}

const inputTypeLabel = {
  text: '文本',
  select: '单选',
  multi: '多选'
}

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallAttr(queryParams)
    attrList.value = normalizeRows(res)
    total.value = res.total ?? res.data?.total ?? attrList.value.length
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    inputType: 'text',
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
    status: undefined,
    inputType: undefined
  })
  getList()
}

function handleAdd() {
  resetForm()
  title.value = '新增属性'
  open.value = true
}

async function handleUpdate(row) {
  resetForm()
  const res = await getMallAttr(row.id)
  Object.assign(form, res.data || row)
  title.value = '修改属性'
  open.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除属性“${row.name}”吗？`, '提示', { type: 'warning' })
  await delMallAttr(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function submitForm() {
  await formRef.value.validate()
  if (form.id) {
    await updateMallAttr(form)
  } else {
    await addMallAttr(form)
  }
  ElMessage.success('保存成功')
  open.value = false
  getList()
}

async function handleOptions(row) {
  optionsAttr.value = row
  const res = await listMallAttrOptions(row.id)
  const list = res.data || []
  optionsRows.value = list.map(item => ({
    id: item.id,
    value: item.value,
    sort: item.sort ?? 0,
    status: item.status ?? '0'
  }))
  if (!optionsRows.value.length) {
    optionsRows.value.push({ value: '', sort: 0, status: '0' })
  }
  optionsOpen.value = true
}

function addOptionRow() {
  optionsRows.value.push({
    value: '',
    sort: optionsRows.value.length,
    status: '0'
  })
}

function removeOptionRow(index) {
  optionsRows.value.splice(index, 1)
}

async function submitOptions() {
  const options = optionsRows.value
    .map((row, index) => ({
      value: (row.value || '').trim(),
      sort: row.sort ?? index,
      status: row.status || '0'
    }))
    .filter(row => row.value)
  if (!options.length) {
    ElMessage.warning('请至少填写一个选项值')
    return
  }
  optionsSaving.value = true
  try {
    await replaceMallAttrOptions(optionsAttr.value.id, options)
    ElMessage.success('选项已保存')
    optionsOpen.value = false
  } finally {
    optionsSaving.value = false
  }
}

onMounted(getList)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryParams" @submit.prevent="handleQuery">
        <el-form-item label="属性名称">
          <el-input v-model="queryParams.name" placeholder="请输入属性名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="录入类型">
          <el-select v-model="queryParams.inputType" placeholder="全部" clearable style="width: 140px">
            <el-option label="文本" value="text" />
            <el-option label="单选" value="select" />
            <el-option label="多选" value="multi" />
          </el-select>
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
          <el-button type="primary" plain icon="Plus" v-hasPermi="['mall:attr:add']" @click="handleAdd">
            新增
          </el-button>
        </el-col>
      </el-row>

      <el-table v-loading="loading" :data="attrList" stripe>
        <el-table-column prop="name" label="属性名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="录入类型" width="110" align="center">
          <template #default="{ row }">
            {{ inputTypeLabel[row.inputType] || row.inputType }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="['mall:attr:edit']" @click="handleUpdate(row)">修改</el-button>
            <el-button
              link
              type="primary"
              v-hasPermi="['mall:attr:edit']"
              :disabled="row.inputType === 'text'"
              @click="handleOptions(row)"
            >
              选项
            </el-button>
            <el-button link type="danger" v-hasPermi="['mall:attr:remove']" @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="属性名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入属性名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="录入类型" prop="inputType">
          <el-radio-group v-model="form.inputType">
            <el-radio value="text">文本</el-radio>
            <el-radio value="select">单选</el-radio>
            <el-radio value="multi">多选</el-radio>
          </el-radio-group>
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

    <el-drawer v-model="optionsOpen" :title="`维护选项 — ${optionsAttr?.name || ''}`" size="520px" append-to-body>
      <el-table :data="optionsRows" border>
        <el-table-column label="选项值" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.value" maxlength="128" placeholder="选项值" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="110" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.sort" :min="0" controls-position="right" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-select v-model="row.status" style="width: 90px">
              <el-option label="正常" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeOptionRow($index)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="options-actions">
        <el-button icon="Plus" @click="addOptionRow">添加行</el-button>
        <el-button type="primary" :loading="optionsSaving" @click="submitOptions">保存选项</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.options-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
