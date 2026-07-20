<script setup>
defineOptions({ name: 'MallAdminFrontCategory' })

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMallCategoryOptions } from '@/api/mall/category'
import {
  addMallFrontCategory,
  delMallFrontCategory,
  getMallFrontCategory,
  listMallFrontCategory,
  listMallFrontCategoryRels,
  replaceMallFrontCategoryRels,
  updateMallFrontCategory
} from '@/api/mall/frontCategory'

const loading = ref(false)
const categoryList = ref([])
const open = ref(false)
const relOpen = ref(false)
const title = ref('')
const formRef = ref()
const relSaving = ref(false)
const relFront = ref(null)
const selectedBackIds = ref([])
const leafOptions = ref([])
const queryParams = reactive({
  name: '',
  status: undefined,
  tree: true
})
const form = reactive({
  id: undefined,
  parentId: 0,
  name: '',
  sort: 0,
  status: '0',
  icon: '',
  remark: ''
})
const rules = {
  name: [{ required: true, message: '类目名称不能为空', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级类目', trigger: 'change' }]
}

const categoryOptions = computed(() => [
  { id: 0, name: '顶级类目', children: categoryList.value }
])

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

function flattenLeaves(nodes, acc = []) {
  for (const node of nodes || []) {
    const children = node.children || []
    if (!children.length) {
      acc.push({ id: node.id, name: node.name })
    } else {
      flattenLeaves(children, acc)
    }
  }
  return acc
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallFrontCategory(queryParams)
    const rows = normalizeRows(res)
    // tree=true 时后端已建树；否则前端组树
    if (queryParams.tree && rows.some(item => Array.isArray(item.children))) {
      categoryList.value = rows
    } else {
      categoryList.value = buildTree(rows)
    }
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parentId: 0,
    name: '',
    sort: 0,
    status: '0',
    icon: '',
    remark: ''
  })
  formRef.value?.clearValidate()
}

function handleQuery() {
  getList()
}

function resetQuery() {
  queryParams.name = ''
  queryParams.status = undefined
  getList()
}

function handleAdd(row) {
  resetForm()
  if (row?.id) {
    form.parentId = row.id
  }
  title.value = '新增前台类目'
  open.value = true
}

async function handleUpdate(row) {
  resetForm()
  const res = await getMallFrontCategory(row.id)
  Object.assign(form, res.data || row)
  title.value = '修改前台类目'
  open.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除前台类目“${row.name}”吗？`, '提示', { type: 'warning' })
  await delMallFrontCategory(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function submitForm() {
  await formRef.value.validate()
  if (form.id) {
    await updateMallFrontCategory(form)
  } else {
    await addMallFrontCategory(form)
  }
  ElMessage.success('保存成功')
  open.value = false
  getList()
}

async function loadLeafOptions() {
  const res = await listMallCategoryOptions()
  const tree = normalizeRows(res)
  leafOptions.value = flattenLeaves(tree)
}

async function handleRels(row) {
  relFront.value = row
  selectedBackIds.value = []
  await loadLeafOptions()
  const res = await listMallFrontCategoryRels(row.id)
  const rels = res.data || []
  selectedBackIds.value = rels.map(item => item.backCategoryId).filter(Boolean)
  relOpen.value = true
}

async function submitRels() {
  relSaving.value = true
  try {
    await replaceMallFrontCategoryRels(relFront.value.id, selectedBackIds.value)
    ElMessage.success('映射已保存')
    relOpen.value = false
  } finally {
    relSaving.value = false
  }
}

onMounted(getList)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>前台类目</span>
      </template>
      <el-form :inline="true" :model="queryParams" @submit.prevent="handleQuery">
        <el-form-item label="类目名称">
          <el-input v-model="queryParams.name" placeholder="请输入类目名称" clearable @keyup.enter="handleQuery" />
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
          <el-button type="primary" plain icon="Plus" v-hasPermi="['mall:frontCategory:add']" @click="handleAdd">
            新增
          </el-button>
        </el-col>
      </el-row>

      <el-table
        v-loading="loading"
        :data="categoryList"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="类目名称" min-width="180" />
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
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="['mall:frontCategory:add']" @click="handleAdd(row)">
              新增子类
            </el-button>
            <el-button link type="primary" v-hasPermi="['mall:frontCategory:edit']" @click="handleUpdate(row)">
              修改
            </el-button>
            <el-button link type="primary" v-hasPermi="['mall:frontCategory:edit']" @click="handleRels(row)">
              映射
            </el-button>
            <el-button link type="danger" v-hasPermi="['mall:frontCategory:remove']" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="open" :title="title" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级类目" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="categoryOptions"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入类目名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="图标">
          <image-upload v-model="form.icon" :limit="1" />
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

    <el-dialog v-model="relOpen" :title="`映射后台叶子 — ${relFront?.name || ''}`" width="560px" append-to-body>
      <el-select
        v-model="selectedBackIds"
        multiple
        filterable
        clearable
        placeholder="请选择后台叶子类目"
        style="width: 100%"
      >
        <el-option v-for="item in leafOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <template #footer>
        <el-button @click="relOpen = false">取消</el-button>
        <el-button type="primary" :loading="relSaving" @click="submitRels">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
