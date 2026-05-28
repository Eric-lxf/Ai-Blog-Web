<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import {
  fetchPublicComments,
  postComment,
  replyComment,
  reportComment,
  toggleCommentLike,
} from '@/api/blog/comment'

const props = defineProps({
  articleId: { type: Number, required: true },
})

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const comments = ref([])
const total = ref(0)
const sort = ref('hot')
const pageNum = ref(1)
const pageSize = ref(10)
const replyTarget = ref(null)

const form = reactive({
  content: '',
  guestName: '',
})

const isLoggedIn = computed(() => !!getToken())
const displayName = computed(() => userStore.nickName || userStore.name || '')

async function loadComments() {
  if (!props.articleId) return
  loading.value = true
  try {
    const res = await fetchPublicComments(props.articleId, {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sort: sort.value,
    })
    comments.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!form.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  if (!isLoggedIn.value && !form.guestName.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  submitting.value = true
  try {
    const payload = {
      content: form.content.trim(),
      guestName: isLoggedIn.value ? undefined : form.guestName.trim(),
    }
    if (replyTarget.value) {
      await replyComment(replyTarget.value.id, payload)
    } else {
      await postComment(props.articleId, payload)
    }
    ElMessage.success('评论已提交，审核通过后展示')
    form.content = ''
    replyTarget.value = null
  } finally {
    submitting.value = false
  }
}

async function handleLike(comment) {
  try {
    const res = await toggleCommentLike(comment.id)
    comment.liked = res.data?.liked ?? !comment.liked
    comment.likeCount += comment.liked ? 1 : -1
  } catch {
    /* handled by interceptor */
  }
}

function startReply(comment) {
  replyTarget.value = comment
}

function cancelReply() {
  replyTarget.value = null
}

async function handleReport(comment) {
  try {
    const { value } = await ElMessageBox.prompt('请填写举报原因', '举报评论', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
    })
    if (value?.trim()) {
      await reportComment(comment.id, { reason: value.trim() })
      ElMessage.success('举报已提交')
    }
  } catch {
    /* cancelled */
  }
}

watch(() => props.articleId, () => {
  pageNum.value = 1
  loadComments()
}, { immediate: true })

watch(sort, () => {
  pageNum.value = 1
  loadComments()
})

onMounted(() => {
  if (isLoggedIn.value && displayName.value) {
    form.guestName = displayName.value
  }
})
</script>

<template>
  <section class="comment-section">
    <div class="section-header">
      <h3>评论 ({{ total }})</h3>
      <el-radio-group v-model="sort" size="small">
        <el-radio-button value="hot">最热</el-radio-button>
        <el-radio-button value="new">最新</el-radio-button>
      </el-radio-group>
    </div>

    <div class="comment-form">
      <div v-if="replyTarget" class="reply-hint">
        回复 @{{ replyTarget.authorName }}
        <el-button link type="primary" @click="cancelReply">取消</el-button>
      </div>
      <el-input v-if="!isLoggedIn" v-model="form.guestName" placeholder="昵称" maxlength="64" class="name-input" />
      <el-input
        v-model="form.content"
        type="textarea"
        :rows="3"
        maxlength="2000"
        show-word-limit
        placeholder="写下你的评论..."
      />
      <el-button type="primary" :loading="submitting" @click="submitComment">发表评论</el-button>
    </div>

    <div v-loading="loading" class="comment-list">
      <el-empty v-if="!loading && comments.length === 0" description="暂无评论，来抢沙发吧" />
      <div v-for="item in comments" :key="item.id" class="comment-item">
        <div class="comment-main">
          <div class="author">{{ item.authorName || '访客' }}</div>
          <div class="content">{{ item.content }}</div>
          <div class="actions">
            <span class="time">{{ item.createTime?.slice(0, 16) }}</span>
            <el-button link @click="handleLike(item)">{{ item.liked ? '已赞' : '点赞' }} ({{ item.likeCount || 0 }})</el-button>
            <el-button link @click="startReply(item)">回复</el-button>
            <el-button link type="danger" @click="handleReport(item)">举报</el-button>
          </div>
        </div>
        <div v-if="item.children?.length" class="comment-children">
          <div v-for="child in item.children" :key="child.id" class="comment-child">
            <div class="author">{{ child.authorName || '访客' }}</div>
            <div class="content">{{ child.content }}</div>
            <div class="actions">
              <span class="time">{{ child.createTime?.slice(0, 16) }}</span>
              <el-button link @click="handleLike(child)">{{ child.liked ? '已赞' : '点赞' }} ({{ child.likeCount || 0 }})</el-button>
              <el-button link @click="startReply(child)">回复</el-button>
              <el-button link type="danger" @click="handleReport(child)">举报</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadComments"
      />
    </div>
  </section>
</template>

<style scoped>
.comment-section {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.name-input {
  max-width: 240px;
}

.reply-hint {
  font-size: 13px;
  color: #64748b;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}

.author {
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.content {
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 13px;
}

.time {
  color: #94a3b8;
  margin-right: 8px;
}

.comment-children {
  margin-top: 12px;
  margin-left: 24px;
  padding-left: 16px;
  border-left: 2px solid #e2e8f0;
}

.comment-child {
  padding: 10px 0;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
