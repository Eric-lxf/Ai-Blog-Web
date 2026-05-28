<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchRecyclePage, purgeArticle, restoreArticle } from '@/api/blog/article'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})

const statusMap = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已发布', type: 'success' },
  2: { label: 'AI生成中', type: 'warning' },
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchRecyclePage(query.value)
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

async function handleRestore(id) {
  await ElMessageBox.confirm('确定恢复该文章吗？', '提示', { type: 'info' })
  await restoreArticle(id)
  ElMessage.success('恢复成功')
  loadData()
}

async function handlePurge(id) {
  await ElMessageBox.confirm('彻底删除后无法恢复，确定继续吗？', '警告', { type: 'warning' })
  await purgeArticle(id)
  ElMessage.success('已彻底删除')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <span>文章回收站</span>
    </template>

    <el-form :inline="true" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="标题搜索" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="删除前状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">
            {{ statusMap[row.status]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="删除时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['blog:article:restore']" @click="handleRestore(row.id)">
            恢复
          </el-button>
          <el-button link type="danger" v-hasPermi="['blog:article:purge']" @click="handlePurge(row.id)">
            彻底删除
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
