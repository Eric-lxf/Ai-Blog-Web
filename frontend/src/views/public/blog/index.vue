<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchPublicArticles, fetchPublicCategories } from '@/api/blog/public'
import BlogCategoryNav from '@/components/blog/BlogCategoryNav.vue'
import BlogArticleItem from '@/components/blog/BlogArticleItem.vue'

const loading = ref(false)
const loadingMore = ref(false)
const articles = ref([])
const categories = ref([])
const total = ref(0)
const keywordInput = ref('')
const query = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: undefined,
  sort: 'latest',
})

const hasMore = computed(() => articles.value.length < total.value)

async function loadCategories() {
  const res = await fetchPublicCategories()
  const d = res?.data
  categories.value = Array.isArray(d) ? d : res?.rows ?? []
}

async function fetchPage({ append } = { append: false }) {
  if (append) {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await fetchPublicArticles(query.value)
    const rows = res.rows ?? res.data?.records ?? []
    total.value = res.total ?? res.data?.total ?? 0
    if (append) {
      articles.value = articles.value.concat(rows)
    } else {
      articles.value = rows
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function resetAndLoad() {
  query.value.pageNum = 1
  return fetchPage({ append: false })
}

function onCategoryChange(id) {
  query.value.categoryId = id
  resetAndLoad()
}

function onTabChange(sort) {
  if (query.value.sort === sort) return
  query.value.sort = sort
  resetAndLoad()
}

function onSearch() {
  query.value.keyword = keywordInput.value.trim()
  resetAndLoad()
}

function loadMore() {
  if (!hasMore.value || loadingMore.value || loading.value) return
  query.value.pageNum += 1
  fetchPage({ append: true })
}

onMounted(async () => {
  await loadCategories()
  await fetchPage({ append: false })
})
</script>

<template>
  <div class="blog-home">
    <aside class="sidebar">
      <BlogCategoryNav
        :categories="categories"
        :model-value="query.categoryId"
        @update:model-value="onCategoryChange"
      />
    </aside>

    <section class="feed" v-loading="loading">
      <header class="feed-toolbar">
        <div class="tabs" role="tablist">
          <button
            type="button"
            role="tab"
            class="tab"
            :class="{ active: query.sort === 'latest' }"
            :aria-selected="query.sort === 'latest'"
            @click="onTabChange('latest')"
          >
            最新
          </button>
          <button
            type="button"
            role="tab"
            class="tab"
            :class="{ active: query.sort === 'hot' }"
            :aria-selected="query.sort === 'hot'"
            @click="onTabChange('hot')"
          >
            热门
          </button>
        </div>
        <form class="search" @submit.prevent="onSearch">
          <el-input
            v-model="keywordInput"
            placeholder="搜索标题或摘要"
            clearable
            @clear="onSearch"
          />
          <el-button type="primary" native-type="submit">搜索</el-button>
        </form>
      </header>

      <el-empty v-if="!loading && articles.length === 0" description="暂无已发布文章" />

      <div v-else class="article-list">
        <BlogArticleItem v-for="item in articles" :key="item.id" :item="item" />
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button :loading="loadingMore" :disabled="loading" @click="loadMore">
          加载更多
        </el-button>
      </div>
      <p v-else-if="!loading && articles.length > 0" class="end-hint">没有更多了</p>
    </section>
  </div>
</template>

<style scoped>
.blog-home {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.sidebar {
  position: sticky;
  top: 16px;
}

.feed {
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 20px 24px;
}

.feed-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 12px 0 8px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 4px;
}

.tabs {
  display: flex;
  gap: 4px;
}

.tab {
  border: none;
  background: transparent;
  padding: 8px 14px;
  font-size: 15px;
  color: #6b7280;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -9px;
}

.tab:hover {
  color: #0f172a;
}

.tab.active {
  color: #047857;
  font-weight: 700;
  border-bottom-color: #059669;
}

.search {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 220px;
  flex: 1;
  max-width: 360px;
  justify-content: flex-end;
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.end-hint {
  text-align: center;
  margin: 20px 0 0;
  font-size: 13px;
  color: #9ca3af;
}

@media (max-width: 959px) {
  .blog-home {
    flex-direction: column;
    gap: 16px;
  }

  .sidebar {
    position: static;
    width: 100%;
  }

  .feed {
    padding: 8px 14px 20px;
  }

  .feed-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search {
    max-width: none;
  }
}
</style>
