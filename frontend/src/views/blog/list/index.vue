<script setup>
defineOptions({ name: 'BlogList' })

import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchArticlePage } from '@/api/blog/article'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
})

const statusMap = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已发布', type: 'success' },
  2: { label: 'AI生成中', type: 'warning' },
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchArticlePage(query.value)
    tableData.value = res.rows ?? res.data?.records ?? []
    total.value = res.total ?? res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.value.pageNum = 1
  loadData()
}

function goComments(row) {
  router.push({
    path: '/blog-ops/comment-manage',
    query: { articleId: String(row.id), articleTitle: row.title || '' },
  })
}

function previewPublic(row) {
  if (row.status === 1) {
    window.open(`/blog/${row.id}`, '_blank')
  }
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <span>博客列表</span>
    </template>

    <el-form :inline="true" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="标题搜索" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="AI生成中" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">
            {{ statusMap[row.status]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="阅读" width="80" align="center" />
      <el-table-column label="评论" width="100" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            v-hasPermi="['blog:comment:list']"
            @click="goComments(row)"
          >
            {{ row.commentCount ?? 0 }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            link
            type="primary"
            @click="previewPublic(row)"
          >
            前台
          </el-button>
          <el-button link type="primary" v-hasPermi="['blog:comment:list']" @click="goComments(row)">
            评论
          </el-button>
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
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
