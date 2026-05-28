<script setup>
defineOptions({ name: 'BlogCommentSensitive' })

import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addSensitiveWord, deleteSensitiveWords, fetchSensitiveWords, updateSensitiveWord } from '@/api/blog/sensitive'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const form = reactive({
  id: undefined,
  word: '',
  matchMode: 'contains',
  action: 'block',
  replaceText: '***',
  status: 1,
})

async function loadData() {
  loading.value = true
  try {
    const res = await fetchSensitiveWords({})
    tableData.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      word: '',
      matchMode: 'contains',
      action: 'block',
      replaceText: '***',
      status: 1,
    })
  }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.word.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  if (form.id) {
    await updateSensitiveWord(form)
  } else {
    await addSensitiveWord(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除敏感词「${row.word}」吗？`, '提示', { type: 'warning' })
  await deleteSensitiveWords(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <span>敏感词管理</span>
        <el-button type="primary" v-hasPermi="['blog:sensitive:add']" @click="openDialog()">新增</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="tableData" stripe>
      <el-table-column prop="word" label="敏感词" min-width="140" />
      <el-table-column prop="matchMode" label="匹配模式" width="100">
        <template #default="{ row }">{{ row.matchMode === 'exact' ? '精确' : '包含' }}</template>
      </el-table-column>
      <el-table-column prop="action" label="动作" width="100">
        <template #default="{ row }">
          {{ row.action === 'block' ? '拦截' : row.action === 'replace' ? '替换' : '转审核' }}
        </template>
      </el-table-column>
      <el-table-column prop="replaceText" label="替换文本" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['blog:sensitive:edit']" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" v-hasPermi="['blog:sensitive:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑敏感词' : '新增敏感词'" width="480px">
    <el-form label-width="90px">
      <el-form-item label="敏感词">
        <el-input v-model="form.word" maxlength="100" />
      </el-form-item>
      <el-form-item label="匹配模式">
        <el-select v-model="form.matchMode">
          <el-option label="包含" value="contains" />
          <el-option label="精确" value="exact" />
        </el-select>
      </el-form-item>
      <el-form-item label="动作">
        <el-select v-model="form.action">
          <el-option label="拦截" value="block" />
          <el-option label="替换" value="replace" />
          <el-option label="转审核" value="review" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.action === 'replace'" label="替换为">
        <el-input v-model="form.replaceText" maxlength="100" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
