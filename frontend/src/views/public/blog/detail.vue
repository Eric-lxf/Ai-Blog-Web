<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { resolveMarkdownAssets, resolveUploadUrl } from '@/utils/blogAssets'
import { fetchPublicArticleDetail } from '@/api/blog/public'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const article = ref(null)

const articleId = computed(() => Number(route.params.id))

const renderedContent = computed(() => resolveMarkdownAssets(article.value?.content))

async function loadArticle() {
  if (!articleId.value || Number.isNaN(articleId.value)) {
    article.value = null
    return
  }
  loading.value = true
  try {
    const res = await fetchPublicArticleDetail(articleId.value)
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
  <div v-loading="loading">
    <el-button link type="primary" @click="router.push('/blog')">← 返回列表</el-button>

    <el-empty v-if="!loading && !article" description="文章不存在或未发布" />

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
      <img v-if="article.coverImage" :src="resolveUploadUrl(article.coverImage)" class="cover" alt="" />
      <MarkdownViewer :value="renderedContent" />
    </article>
  </div>
</template>

<style scoped>
.article-detail h1.article-title {
  margin: 16px 0 12px;
  font-size: 32px;
  line-height: 1.35;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
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
