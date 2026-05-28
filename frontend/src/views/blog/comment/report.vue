<script setup>
defineOptions({ name: 'BlogCommentReport' })

import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchCommentReports, handleCommentReport } from '@/api/blog/comment'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = ref({
  pageNum: 1,
  pageSize: 10,
  status: 0,
})

async function loadData() {
  loading.value = true
  try {
    const res = await fetchCommentReports(query.value)
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

async function handle(row, commentStatus) {
  const { value } = await ElMessageBox.prompt('处理备注（可选）', '处理举报', {
    inputValue: commentStatus === 4 ? '举报成立，标记垃圾' : '举报成立，隐藏评论',
  })
  await handleCommentReport(row.id, { commentStatus, handleRemark: value })
  ElMessage.success('处理成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <span>举报处理</span>
    </template>

    <el-form :inline="true" @submit.prevent="handleSearch">
      <el-form-item label="状态">
        <el-select v-model="query.status" style="width: 120px">
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe>
      <el-table-column prop="articleTitle" label="文章" min-width="160" show-overflow-tooltip />
      <el-table-column prop="commentContent" label="评论内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="reason" label="举报原因" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="举报时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="warning" v-hasPermi="['blog:comment:report:handle']" @click="handle(row, 3)">隐藏</el-button>
            <el-button link type="danger" v-hasPermi="['blog:comment:report:handle']" @click="handle(row, 4)">标记垃圾</el-button>
          </template>
          <span v-else class="handled">{{ row.handleBy }} · {{ row.handleTime?.slice(0, 16) }}</span>
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

.handled {
  color: #94a3b8;
  font-size: 13px;
}
</style>
