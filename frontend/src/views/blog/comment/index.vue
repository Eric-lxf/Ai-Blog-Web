<script setup>
defineOptions({ name: 'BlogCommentManage' })

import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditComments, deleteComment, fetchAdminComments } from '@/api/blog/comment'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedIds = ref([])

const filterArticleId = computed(() => {
  const id = route.query.articleId
  return id ? Number(id) : undefined
})

const filterArticleTitle = computed(() => String(route.query.articleTitle || ''))

const query = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  aiStatus: undefined,
  articleId: filterArticleId.value,
})

const statusMap = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已拒绝', type: 'danger' },
  3: { label: '已隐藏', type: 'info' },
  4: { label: '垃圾', type: 'danger' },
}

const aiStatusMap = {
  0: '未检测',
  1: '检测中',
  2: '通过',
  3: '疑似',
  4: '高风险',
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchAdminComments(query.value)
    tableData.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.value.pageNum = 1
  loadData()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

async function batchAudit(status) {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择评论')
    return
  }
  let rejectReason
  if (status === 2) {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝评论')
      rejectReason = value
    } catch {
      return
    }
  }
  await auditComments({ ids: selectedIds.value, status, rejectReason })
  ElMessage.success('操作成功')
  loadData()
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该评论吗？', '提示', { type: 'warning' })
  await deleteComment(id)
  ElMessage.success('删除成功')
  loadData()
}

function clearArticleFilter() {
  router.replace({ path: route.path })
}

watch(
  () => route.query.articleId,
  (id) => {
    query.value.articleId = id ? Number(id) : undefined
    query.value.pageNum = 1
    loadData()
  },
)

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <span>评论管理</span>
        <el-tag v-if="filterArticleId" closable type="info" @close="clearArticleFilter">
          文章：{{ filterArticleTitle || `#${filterArticleId}` }}
        </el-tag>
      </div>
    </template>

    <el-form :inline="true" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="评论内容" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="(item, key) in statusMap" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
      </el-form-item>
      <el-form-item label="AI状态">
        <el-select v-model="query.aiStatus" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="(label, key) in aiStatusMap" :key="key" :label="label" :value="Number(key)" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="success" v-hasPermi="['blog:comment:audit']" @click="batchAudit(1)">批量通过</el-button>
      <el-button type="warning" v-hasPermi="['blog:comment:audit']" @click="batchAudit(3)">批量隐藏</el-button>
      <el-button type="danger" v-hasPermi="['blog:comment:audit']" @click="batchAudit(4)">标记垃圾</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="articleId" label="文章ID" width="90" />
      <el-table-column prop="authorName" label="作者" width="120" />
      <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">{{ statusMap[row.status]?.label || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AI" width="120">
        <template #default="{ row }">
          <div>{{ aiStatusMap[row.aiStatus] || '-' }}</div>
          <div v-if="row.aiScore != null" class="sub">{{ row.aiScore }} 分</div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" v-hasPermi="['blog:comment:remove']" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>
  </el-card>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.sub {
  font-size: 12px;
  color: #94a3b8;
}
</style>
