import request from '@/utils/request'

export function listNotifications(query) {
  return request({
    url: '/blog/notification/list',
    method: 'get',
    params: query
  })
}

export function getNotificationUnreadCount() {
  return request({
    url: '/blog/notification/unread-count',
    method: 'get'
  })
}

export function markNotificationRead(id) {
  return request({
    url: `/blog/notification/${id}/read`,
    method: 'post'
  })
}

export function markNotificationReadAll() {
  return request({
    url: '/blog/notification/read-all',
    method: 'post'
  })
}

export function getNotificationPreference() {
  return request({
    url: '/blog/notification/preference',
    method: 'get'
  })
}

export function updateNotificationPreference(data) {
  return request({
    url: '/blog/notification/preference',
    method: 'put',
    data
  })
}

export function sendSystemNotification(data) {
  return request({
    url: '/blog/notification/admin/send',
    method: 'post',
    data
  })
}
