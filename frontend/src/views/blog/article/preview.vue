<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { resolveMarkdownAssets, resolveUploadUrl } from '@/utils/blogAssets'
import { fetchArticleDetail } from '@/api/blog/article'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const article = ref(null)

const articleId = computed(() => Number(route.params.id))

const renderedContent = computed(() => resolveMarkdownAssets(article.value?.content))

const statusLabel = computed(() => {
  const s = article.value?.status
  if (s === 1) return '已发布'
  if (s === 2) return 'AI 生成中'
  return '草稿'
})

async function loadArticle() {
  if (!articleId.value || Number.isNaN(articleId.value)) {
    article.value = null
    return
  }
  loading.value = true
  try {
    const res = await fetchArticleDetail(articleId.value)
    article.value = res.data || null
  } catch {
    article.value = null
  } finally {
    loading.value = false
  }
}

watch(articleId, loadArticle, { immediate: true })
</script>

<template>
  <div v-loading="loading" class="preview-page">
    <div class="toolbar">
      <el-button @click="router.back()">← 返回编辑</el-button>
      <el-tag v-if="article" :type="article.status === 1 ? 'success' : 'info'">{{ statusLabel }}</el-tag>
      <el-button
        v-if="article?.status === 1"
        type="primary"
        link
        @click="router.push(`/blog/${article.id}`)"
      >
        打开前台页面
      </el-button>
    </div>

    <el-empty v-if="!loading && !article" description="文章不存在或无权预览" />

    <article v-if="article" class="article-detail">
      <h1 class="article-title">{{ article.title }}</h1>
      <div class="meta">
        <span v-if="article.categoryName">{{ article.categoryName }}</span>
        <span>{{ article.viewCount ?? 0 }} 阅读</span>
        <span>{{ article.updateTime?.slice(0, 16) }}</span>
      </div>
      <div v-if="article.tagNames?.length" class="tags">
        <el-tag v-for="tag in article.tagNames" :key="tag" size="small">{{ tag }}</el-tag>
      </div>
      <img
        v-if="article.coverImage"
        :src="resolveUploadUrl(article.coverImage)"
        class="cover"
        alt=""
      />
      <MarkdownViewer :value="renderedContent" />
    </article>
  </div>
</template>

<style scoped>
.preview-page {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  min-height: calc(100vh - 120px);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.article-detail h1.article-title {
  margin: 16px 0 12px;
  font-size: 32px;
  line-height: 1.35;
  font-weight: 800;
  color: #0f172a;
}

.meta {
  display: flex;
  gap: 16px;
  color: #9ca3af;
  font-size: 14px;
  margin-bottom: 12px;
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 24px;
}
</style>
