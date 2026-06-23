<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="query" inline class="search-form">
      <el-form-item label="文件名">
        <el-input v-model="query.keyword" placeholder="搜索文件名" clearable style="width:220px" @keyup.enter="loadList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadList">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 上传区域 -->
    <el-card shadow="never" class="upload-card">
      <el-upload
        :action="uploadAction"
        :headers="uploadHeaders"
        :on-success="onUploadSuccess"
        :on-error="onUploadError"
        :before-upload="beforeUpload"
        :show-file-list="false"
        multiple
        drag
        class="upload-area"
      >
        <el-icon class="el-icon--upload" :size="40"><Upload /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">单文件最大 50MB，支持任意格式</div>
        </template>
      </el-upload>
    </el-card>

    <!-- 文件列表 -->
    <el-table v-loading="loading" :data="list" stripe class="mt16">
      <el-table-column label="文件名" prop="fileName" show-overflow-tooltip min-width="200">
        <template #default="{ row }">
          <el-icon class="file-icon"><Document /></el-icon>
          <span class="ml4">{{ row.fileName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="fileType" width="160" show-overflow-tooltip />
      <el-table-column label="大小" width="110" align="right">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="上传时间" prop="createTime" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="CopyDocument" @click="copyUrl(row.fileUrl)">复制链接</el-button>
          <el-button link type="success" :icon="Download" @click="downloadFile(row)">下载</el-button>
          <el-button v-hasPermi="['blog:file:remove']" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Delete, Upload, Document, CopyDocument, Download } from '@element-plus/icons-vue'
import { listFile, deleteFile } from '@/api/blog/file'
import { getToken } from '@/utils/auth'

const uploadAction = computed(() => `${import.meta.env.VITE_APP_BASE_API}/blog/file/upload`)
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))

function beforeUpload(file) {
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB')
    return false
  }
  return true
}

function onUploadSuccess(response) {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    loadList()
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

function onUploadError() {
  ElMessage.error('上传失败，请检查网络或重试')
}

// ── 列表 ──────────────────────────────────────────────────────
const loading = ref(false)
const list    = ref([])
const total   = ref(0)
const query   = reactive({ pageNum: 1, pageSize: 20, keyword: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await listFile({ pageNum: query.pageNum, pageSize: query.pageSize, keyword: query.keyword })
    list.value  = res.rows  || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.pageNum = 1
  loadList()
}

onMounted(loadList)

// ── 操作 ──────────────────────────────────────────────────────
function copyUrl(url) {
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

function downloadFile(row) {
  const a = document.createElement('a')
  a.href     = row.fileUrl
  a.download = row.fileName
  a.target   = '_blank'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除文件「${row.fileName}」？`, '提示', { type: 'warning' })
  await deleteFile(row.id)
  ElMessage.success('删除成功')
  loadList()
}

function formatSize(bytes) {
  if (!bytes) return '—'
  if (bytes < 1024)               return bytes + ' B'
  if (bytes < 1024 * 1024)        return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}
</script>

<style scoped lang="scss">
.upload-card { margin-bottom: 16px; }
.upload-area { width: 100%; }
.mt16 { margin-top: 16px; }
.ml4  { margin-left: 4px; }
.file-icon { vertical-align: middle; color: #909399; }
</style>