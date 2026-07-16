<script setup>
import { RouterLink } from 'vue-router'
import { resolveUploadUrl } from '@/utils/blogAssets'

defineProps({
  item: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <article class="article-item">
    <div class="body">
      <h2 class="title">
        <RouterLink :to="`/blog/${item.id}`">{{ item.title }}</RouterLink>
      </h2>
      <p class="summary">{{ item.summary || '暂无摘要' }}</p>
      <div class="meta">
        <span v-if="item.categoryName" class="meta-cat">{{ item.categoryName }}</span>
        <span>{{ item.viewCount ?? 0 }} 阅读</span>
        <span>{{ item.updateTime?.slice(0, 10) }}</span>
      </div>
      <div v-if="item.tagNames?.length" class="tags">
        <span v-for="tag in item.tagNames" :key="tag" class="tag">{{ tag }}</span>
      </div>
    </div>
    <img
      v-if="item.coverImage"
      :src="resolveUploadUrl(item.coverImage)"
      class="cover"
      width="120"
      height="75"
      loading="lazy"
      decoding="async"
      alt=""
    />
  </article>
</template>

<style scoped>
.article-item {
  display: flex;
  gap: 16px;
  padding: 18px 0;
  border-bottom: 1px solid #e5e7eb;
}

.body {
  flex: 1;
  min-width: 0;
}

.title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
}

.title a {
  color: #0f172a;
  text-decoration: none;
}

.title a:hover {
  color: #047857;
}

.summary {
  margin: 0 0 10px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #9ca3af;
}

.meta-cat {
  color: #059669;
}

.tags {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.cover {
  width: 120px;
  height: 75px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
  background: #f1f5f9;
}

@media (max-width: 640px) {
  .cover {
    display: none;
  }
}
</style>
