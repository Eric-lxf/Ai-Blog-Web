<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { resolveUploadUrl } from '@/utils/blogAssets'
import { fetchPublicArticles, fetchPublicCategories } from '@/api/blog/public'

const router = useRouter()
const loading = ref(false)
const articles = ref([])
const categories = ref([])
const total = ref(0)
const query = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: undefined,
})

async function loadCategories() {
  const res = await fetchPublicCategories()
  const d = res?.data
  categories.value = Array.isArray(d) ? d : res?.rows ?? []
}

async function loadArticles() {
  loading.value = true
  try {
    const res = await fetchPublicArticles(query.value)
    articles.value = res.rows ?? res.data?.records ?? []
    total.value = res.total ?? res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/blog/${id}`)
}

onMounted(async () => {
  await loadCategories()
  await loadArticles()
})
</script>

<template>
  <div v-loading="loading">
    <section class="hero">
      <h1 class="blog-heading">技术博客</h1>
      <p>记录学习与思考，支持 Markdown 与 Mermaid 图表</p>
    </section>

    <el-form :inline="true" class="filters" @submit.prevent="loadArticles">
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="搜索标题或摘要" clearable />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 140px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadArticles">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-empty v-if="!loading && articles.length === 0" description="暂无已发布文章" />

    <article
      v-for="item in articles"
      :key="item.id"
      class="article-card"
      @click="goDetail(item.id)"
    >
      <img v-if="item.coverImage" :src="resolveUploadUrl(item.coverImage)" class="cover" alt="" />
      <div class="body">
        <h2 class="article-title">{{ item.title }}</h2>
        <p class="summary">{{ item.summary || '暂无摘要' }}</p>
        <div class="meta">
          <span v-if="item.categoryName">{{ item.categoryName }}</span>
          <span>{{ item.viewCount ?? 0 }} 阅读</span>
          <span>{{ item.updateTime?.slice(0, 10) }}</span>
        </div>
        <div v-if="item.tagNames?.length" class="tags">
          <el-tag v-for="tag in item.tagNames" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
        </div>
      </div>
    </article>

    <div v-if="total > query.pageSize" class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadArticles"
      />
    </div>
  </div>
</template>

<style scoped>
.hero {
  margin-bottom: 24px;
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #0f172a;
  font-weight: 800;
}

.hero p {
  margin: 0;
  color: #6b7280;
}

.filters {
  margin-bottom: 16px;
}

.article-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.article-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.cover {
  width: 160px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.body h2.article-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.4;
}

.article-card:hover .article-title {
  color: #047857;
}

.summary {
  margin: 0 0 12px;
  color: #6b7280;
  line-height: 1.6;
}

.meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #9ca3af;
}

.tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
