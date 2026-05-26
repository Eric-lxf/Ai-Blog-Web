<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import EnhancedMarkdownEditor from '@/components/Editor/EnhancedMarkdownEditor.vue'
import AiSidebar from '@/components/AiSidebar.vue'
import { fetchAiStatus } from '@/api/blog/ai'
import { fetchArticleDetail, saveArticle } from '@/api/blog/article'
import { fetchCategories } from '@/api/blog/category'
import { fetchTags } from '@/api/blog/tag'
import { uploadImage } from '@/api/blog/upload'
import {
  clearLocalDraft,
  loadLocalDraft,
  useArticleDraft,
} from '@/composables/useArticleDraft'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const categories = ref([])
const allTags = ref([])
const selectedTags = ref([])
const draftEnabled = ref(true)
const aiVisible = ref(true)
const aiConfigured = ref(false)

const articleId = computed(() => {
  const q = route.query.id
  if (q === undefined || q === null || q === '') return undefined
  const n = Number(q)
  return Number.isFinite(n) ? n : undefined
})

const form = reactive({
  id: undefined,
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  categoryId: undefined,
  status: 0,
  tagIds: [],
  tagNames: [],
})

const { saving, lastSavedAt, saveError } = useArticleDraft(ref(form), { enabled: draftEnabled })

function buildTagPayload() {
  const tagIds = []
  const tagNames = []
  for (const name of selectedTags.value) {
    const found = allTags.value.find(t => t.name === name)
    if (found) {
      tagIds.push(found.id)
    } else {
      tagNames.push(name)
    }
  }
  return { tagIds, tagNames }
}

function unwrapList(res) {
  const d = res?.data
  if (Array.isArray(d)) return d
  return res?.rows ?? []
}

async function loadCategories() {
  const res = await fetchCategories()
  categories.value = unwrapList(res)
}

async function loadTags() {
  const res = await fetchTags()
  allTags.value = unwrapList(res)
}

function applyFormData(data) {
  form.id = data.id
  form.title = data.title || ''
  form.summary = data.summary || ''
  form.content = data.content || ''
  form.coverImage = data.coverImage || ''
  form.categoryId = data.categoryId
  form.status = data.status ?? 0
  selectedTags.value = data.tagNames || []
}

async function loadArticle() {
  if (!articleId.value) {
    const local = loadLocalDraft()
    if (local) {
      applyFormData(local)
    }
    return
  }
  loading.value = true
  try {
    const res = await fetchArticleDetail(articleId.value)
    const article = res.data
    if (!article) return
    applyFormData({
      id: article.id,
      title: article.title,
      summary: article.summary,
      content: article.content,
      coverImage: article.coverImage,
      categoryId: article.categoryId,
      status: article.status,
      tagNames: article.tagNames,
    })
    const local = loadLocalDraft(article.id)
    if (local && local.title && local.content) {
      try {
        await ElMessageBox.confirm('检测到本地未同步草稿，是否恢复？', '恢复草稿', {
          confirmButtonText: '恢复',
          cancelButtonText: '忽略',
        })
        applyFormData(local)
      } catch {
        /* 用户取消 */
      }
    }
  } finally {
    loading.value = false
  }
}

async function handleCoverUpload(file) {
  const url = await uploadImage(file)
  form.coverImage = url
  return false
}

async function handleSave(publish = false) {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和正文不能为空')
    return
  }
  draftEnabled.value = false
  loading.value = true
  try {
    const { tagIds, tagNames } = buildTagPayload()
    const res = await saveArticle({
      id: form.id,
      title: form.title,
      summary: form.summary,
      content: form.content,
      coverImage: form.coverImage,
      categoryId: form.categoryId,
      status: publish ? 1 : form.status,
      tagIds,
      tagNames,
    })
    form.id = res.data
    clearLocalDraft(form.id)
    ElMessage.success(publish ? '发布成功' : '保存成功')
    if (!articleId.value && form.id) {
      router.replace({ path: '/blog/article/edit', query: { id: String(form.id) } })
    }
    draftEnabled.value = true
  } finally {
    loading.value = false
    draftEnabled.value = true
  }
}

const savedHint = computed(() => {
  if (saveError.value) return saveError.value
  if (saving.value) return '正在自动保存...'
  if (lastSavedAt.value) {
    return `已自动保存 ${lastSavedAt.value.toLocaleTimeString()}`
  }
  return '编辑后将自动保存草稿'
})

watch(selectedTags, () => {
  const { tagIds, tagNames } = buildTagPayload()
  form.tagIds = tagIds
  form.tagNames = tagNames
})

function handleAiInsert(content) {
  const trimmed = content.trim()
  if (!trimmed) return
  form.content = form.content ? `${form.content}\n\n${trimmed}` : trimmed
  ElMessage.success('已插入到正文末尾')
}

async function loadAiStatus() {
  try {
    const res = await fetchAiStatus()
    aiConfigured.value = res.data?.configured ?? false
  } catch {
    aiConfigured.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadTags(), loadAiStatus()])
  await loadArticle()
})
</script>

<template>
  <div class="edit-layout">
    <el-card v-loading="loading" shadow="never" class="edit-card">
      <template #header>
        <div class="card-header">
          <span>{{ articleId ? '编辑文章' : '自己写' }}</span>
          <div class="header-actions">
            <span class="draft-hint">{{ savedHint }}</span>
            <el-button size="small" @click="aiVisible = !aiVisible">
              {{ aiVisible ? '隐藏 AI' : '显示 AI' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-form label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            placeholder="文章摘要"
          />
        </el-form-item>
        <el-form-item label="封面">
          <div class="cover-row">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :before-upload="handleCoverUpload"
            >
              <el-button :icon="Plus">上传封面</el-button>
            </el-upload>
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" alt="" />
          </div>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 200px">
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="selectedTags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入标签"
            style="width: 100%"
          >
            <el-option v-for="tag in allTags" :key="tag.id" :label="tag.name" :value="tag.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文" required>
          <EnhancedMarkdownEditor
            v-model="form.content"
            :article-title="form.title"
            :ai-configured="aiConfigured"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave(false)" v-hasPermi="['blog:article:edit']">
            保存草稿
          </el-button>
          <el-button type="success" @click="handleSave(true)" v-hasPermi="['blog:article:publish']">
            发布
          </el-button>
          <el-button @click="router.push('/blog/article')">返回列表</el-button>
          <el-button v-if="form.id" @click="router.push(`/blog/article/preview/${form.id}`)">
            预览
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <AiSidebar
      v-model:visible="aiVisible"
      :article-title="form.title"
      :article-content="form.content"
      @insert="handleAiInsert"
    />
  </div>
</template>

<style scoped>
.edit-layout {
  display: flex;
  gap: 0;
  align-items: flex-start;
}

.edit-card {
  flex: 1;
  min-width: 0;
  min-height: calc(100vh - 120px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.draft-hint {
  font-size: 13px;
  color: #6b7280;
}

.cover-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cover-preview {
  width: 120px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
</style>
