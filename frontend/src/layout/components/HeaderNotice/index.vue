<template>
  <div>
    <el-popover ref="noticePopover" placement="bottom-end" :width="340" trigger="manual" v-model:visible="noticeVisible" popper-class="notice-popover">
      <div class="notice-header">
        <el-tabs v-model="activeTab" class="notice-tabs" @tab-change="onTabChange">
          <el-tab-pane label="公告" name="notice" />
          <el-tab-pane name="message">
            <template #label>
              我的消息
              <span v-if="messageUnread > 0" class="tab-badge">{{ messageUnread > 99 ? '99+' : messageUnread }}</span>
            </template>
          </el-tab-pane>
        </el-tabs>
        <span class="notice-mark-all" @click="markAllRead">全部已读</span>
      </div>

      <div v-if="loading" class="notice-loading">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>

      <template v-else-if="activeTab === 'notice'">
        <div v-if="noticeList.length === 0" class="notice-empty">
          <el-icon style="font-size:24px;display:block;margin-bottom:6px;"><Postcard /></el-icon>
          暂无公告
        </div>
        <div v-else>
          <div v-for="item in noticeList" :key="item.noticeId" class="notice-item" :class="{ 'is-read': item.isRead }" @click="previewNotice(item)">
            <el-tag size="small" :type="item.noticeType === '1' ? 'warning' : 'success'" class="notice-tag">
              {{ item.noticeType === '1' ? '通知' : '公告' }}
            </el-tag>
            <span class="notice-item-title">{{ item.noticeTitle }}</span>
            <span class="notice-item-date">{{ item.createTime }}</span>
          </div>
        </div>
      </template>

      <template v-else>
        <div v-if="messageList.length === 0" class="notice-empty">暂无消息</div>
        <div v-else>
          <div v-for="item in messageList" :key="item.id" class="notice-item" :class="{ 'is-read': item.isRead }" @click="openMessage(item)">
            <el-tag size="small" :type="msgTagType(item.type)" class="notice-tag">{{ msgTypeLabel(item.type) }}</el-tag>
            <span class="notice-item-title">{{ item.title }}</span>
            <span class="notice-item-date">{{ item.createTime }}</span>
          </div>
        </div>
        <div class="notice-footer">
          <router-link to="/blog-ops/notification" @click="noticeVisible = false">消息中心</router-link>
        </div>
      </template>

      <template #reference>
        <div class="right-menu-item hover-effect notice-trigger" @mouseenter="onNoticeEnter" @mouseleave="onNoticeLeave">
          <svg-icon icon-class="bell" />
          <span v-if="totalUnread > 0" class="notice-badge">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
        </div>
      </template>
    </el-popover>

    <notice-detail-view ref="noticeViewRef" />
  </div>
</template>

<script setup>
import NoticeDetailView from './DetailView'
import { listNoticeTop, markNoticeRead, markNoticeReadAll } from '@/api/system/notice'
import {
  listNotifications,
  getNotificationUnreadCount,
  markNotificationRead,
  markNotificationReadAll
} from '@/api/blog/notification'

const router = useRouter()
const noticePopover = ref(null)
const activeTab = ref('notice')
const noticeList = ref([])
const messageList = ref([])
const noticeUnread = ref(0)
const messageUnread = ref(0)
const loading = ref(false)
const noticeVisible = ref(false)
const noticeLeaveTimer = ref(null)
const { proxy } = getCurrentInstance()

const totalUnread = computed(() => noticeUnread.value + messageUnread.value)

function msgTypeLabel(type) {
  const map = { COMMENT: '评论', REPLY: '回复', SYSTEM: '系统' }
  return map[type] || type
}

function msgTagType(type) {
  const map = { COMMENT: 'warning', REPLY: 'success', SYSTEM: 'info' }
  return map[type] || 'info'
}

function loadNoticeTop() {
  return listNoticeTop().then(res => {
    noticeList.value = res.data || []
    noticeUnread.value = res.unreadCount !== undefined
      ? res.unreadCount
      : noticeList.value.filter(n => !n.isRead).length
  })
}

function loadMessages() {
  return Promise.all([
    listNotifications({ pageNum: 1, pageSize: 5 }),
    getNotificationUnreadCount()
  ]).then(([listRes, countRes]) => {
    messageList.value = listRes.rows || []
    messageUnread.value = countRes.unreadCount ?? countRes.data?.unreadCount ?? 0
  })
}

function refreshAll() {
  loading.value = true
  Promise.all([loadNoticeTop(), loadMessages()]).finally(() => {
    loading.value = false
  })
}

function onTabChange() {
  if (activeTab.value === 'message' && messageList.value.length === 0) {
    loadMessages()
  }
}

onMounted(() => {
  refreshAll()
  setInterval(() => {
    if (document.hidden) return
    loadNoticeTop()
    getNotificationUnreadCount().then(res => {
      messageUnread.value = res.unreadCount ?? res.data?.unreadCount ?? 0
    }).catch(() => {})
  }, 60000)
})

function onNoticeEnter() {
  clearTimeout(noticeLeaveTimer.value)
  noticeVisible.value = true
  refreshAll()
  nextTick(() => {
    const popper = noticePopover.value?.popperRef?.contentRef
    if (popper && !popper._noticeBound) {
      popper._noticeBound = true
      popper.addEventListener('mouseenter', () => clearTimeout(noticeLeaveTimer.value))
      popper.addEventListener('mouseleave', () => {
        noticeLeaveTimer.value = setTimeout(() => { noticeVisible.value = false }, 100)
      })
    }
  })
}

function onNoticeLeave() {
  noticeLeaveTimer.value = setTimeout(() => { noticeVisible.value = false }, 150)
}

function previewNotice(item) {
  if (!item.isRead) {
    markNoticeRead(item.noticeId).catch(() => {})
    const idx = noticeList.value.indexOf(item)
    if (idx !== -1) noticeList.value[idx] = { ...item, isRead: true }
    noticeUnread.value = Math.max(0, noticeUnread.value - 1)
  }
  proxy.$refs.noticeViewRef.open(item.noticeId)
}

function openMessage(item) {
  if (!item.isRead) {
    markNotificationRead(item.id).catch(() => {})
    item.isRead = true
    messageUnread.value = Math.max(0, messageUnread.value - 1)
  }
  if (item.linkUrl) {
    noticeVisible.value = false
    router.push(item.linkUrl)
  }
}

function markAllRead() {
  if (activeTab.value === 'notice') {
    const ids = noticeList.value.map(n => n.noticeId).join(',')
    if (!ids) return
    markNoticeReadAll(ids).catch(() => {})
    noticeList.value = noticeList.value.map(n => ({ ...n, isRead: true }))
    noticeUnread.value = 0
  } else {
    markNotificationReadAll().catch(() => {})
    messageList.value = messageList.value.map(n => ({ ...n, isRead: true }))
    messageUnread.value = 0
  }
}
</script>

<style lang="scss" scoped>
.notice-trigger {
  position: relative;
  transform: translateX(-6px);
  .svg-icon { width: 1.2em; height: 1.2em; vertical-align: -0.2em; }
  .notice-badge {
    position: absolute;
    top: 7px;
    right: -3px;
    background: #f56c6c;
    color: #fff;
    border-radius: 10px;
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    padding: 0 4px;
    min-width: 16px;
    text-align: center;
    white-space: nowrap;
    pointer-events: none;
  }
}
.notice-popover { padding: 0 !important; }
.notice-popover .notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 0 8px;
  background: #f7f9fb;
  border-bottom: 1px solid #eee;
}
.notice-tabs {
  flex: 1;
  :deep(.el-tabs__header) { margin: 0; }
  :deep(.el-tabs__nav-wrap::after) { display: none; }
}
.tab-badge {
  margin-left: 4px;
  background: #f56c6c;
  color: #fff;
  border-radius: 8px;
  font-size: 10px;
  padding: 0 4px;
  line-height: 14px;
}
.notice-popover .notice-mark-all {
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: normal;
  cursor: pointer;
  flex-shrink: 0;
  padding-right: 6px;
}
.notice-popover .notice-mark-all:hover { color: #2b7cc1; }
.notice-popover .notice-loading,
.notice-popover .notice-empty {
  padding: 24px;
  text-align: center;
  color: #bbb;
  font-size: 12px;
  line-height: 1.8;
}
.notice-popover .notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}
.notice-popover .notice-item:last-child { border-bottom: none; }
.notice-popover .notice-item:hover { background: #f7f9fb; }
.notice-popover .notice-item.is-read .notice-tag,
.notice-popover .notice-item.is-read .notice-item-title,
.notice-popover .notice-item.is-read .notice-item-date { opacity: 0.45; filter: grayscale(1); color: #999; }
.notice-popover .notice-tag { flex-shrink: 0; }
.notice-popover .notice-item-title {
  flex: 1;
  font-size: 12px;
  color: #333;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.notice-popover .notice-item-date {
  flex-shrink: 0;
  font-size: 11px;
  color: #bbb;
}
.notice-footer {
  padding: 8px 14px;
  text-align: center;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  a { color: var(--el-color-primary); text-decoration: none; }
}
</style>
