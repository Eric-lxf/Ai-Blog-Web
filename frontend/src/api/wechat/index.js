import request from '@/utils/request'

export function listWechatAccount(query) {
  return request({
    url: '/wechat/account',
    method: 'get',
    params: query
  })
}

export function listWechatAccountOptions() {
  return request({
    url: '/wechat/account/options',
    method: 'get'
  })
}

export function getWechatAccount(id) {
  return request({
    url: `/wechat/account/${id}`,
    method: 'get'
  })
}

export function saveWechatAccount(data) {
  return request({
    url: '/wechat/account',
    method: 'post',
    data
  })
}

export function deleteWechatAccount(id) {
  return request({
    url: `/wechat/account/${id}`,
    method: 'delete'
  })
}

export function testWechatAccount(id) {
  return request({
    url: `/wechat/account/${id}/test`,
    method: 'post'
  })
}

export function listWechatPublish(query) {
  return request({
    url: '/wechat/publish',
    method: 'get',
    params: query
  })
}

export function pushWechatArticle(data) {
  return request({
    url: '/wechat/push',
    method: 'post',
    data
  })
}

export function submitWechatPublishRecord(id) {
  return request({
    url: `/wechat/publish/${id}/submit`,
    method: 'post'
  })
}

export function syncWechatPublishStatus(id) {
  return request({
    url: `/wechat/publish/${id}/sync-status`,
    method: 'post'
  })
}

export function batchGetWechatPublished(data) {
  return request({
    url: '/wechat/publish/wechat/batchget',
    method: 'post',
    data
  })
}

export function deleteWechatPublished(data) {
  return request({
    url: '/wechat/publish/wechat/delete',
    method: 'post',
    data
  })
}

export function getWechatPublishStatus(data) {
  return request({
    url: '/wechat/publish/wechat/status',
    method: 'post',
    data
  })
}

export function getWechatPublishedArticle(data) {
  return request({
    url: '/wechat/publish/wechat/article',
    method: 'post',
    data
  })
}

export function submitWechatDraft(data) {
  return request({
    url: '/wechat/publish/wechat/submit',
    method: 'post',
    data
  })
}

export function listWechatMaterial(query) {
  return request({
    url: '/wechat/material',
    method: 'get',
    params: query
  })
}

export function listWechatMediaAsset(query) {
  return request({
    url: '/wechat/material/assets',
    method: 'get',
    params: query
  })
}

export function uploadWechatMediaAsset(data) {
  return request({
    url: '/wechat/material/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteWechatMediaAsset(id) {
  return request({
    url: `/wechat/material/assets/${id}`,
    method: 'delete'
  })
}

export function batchGetWechatMaterial(data) {
  return request({
    url: '/wechat/material/wechat/batchget',
    method: 'post',
    data
  })
}

export function deleteWechatMaterialFromWechat(data) {
  return request({
    url: '/wechat/material/wechat/delete',
    method: 'post',
    data
  })
}

export function batchGetWechatDraft(data) {
  return request({
    url: '/wechat/draft/batchget',
    method: 'post',
    data
  })
}

export function getWechatDraft(data) {
  return request({
    url: '/wechat/draft/get',
    method: 'post',
    data
  })
}

export function countWechatDraft(accountId) {
  return request({
    url: '/wechat/draft/count',
    method: 'get',
    params: { accountId }
  })
}

export function saveWechatDraft(data) {
  return request({
    url: '/wechat/draft',
    method: 'post',
    data
  })
}

export function updateWechatDraft(data) {
  return request({
    url: '/wechat/draft/update',
    method: 'post',
    data
  })
}

export function deleteWechatDraft(data) {
  return request({
    url: '/wechat/draft/delete',
    method: 'post',
    data
  })
}

export function deleteWechatMaterial(id) {
  return request({
    url: `/wechat/material/${id}`,
    method: 'delete'
  })
}

export function listWechatMenu(query) {
  return request({
    url: '/wechat/menu',
    method: 'get',
    params: query
  })
}

export function saveWechatMenu(data) {
  return request({
    url: '/wechat/menu',
    method: 'post',
    data
  })
}

export function publishWechatMenu(id) {
  return request({
    url: `/wechat/menu/${id}/publish`,
    method: 'post'
  })
}

export function getWechatMenuFromWechat(accountId) {
  return request({
    url: `/wechat/menu/wechat/${accountId}`,
    method: 'get'
  })
}

export function deleteWechatMenuFromWechat(accountId) {
  return request({
    url: `/wechat/menu/wechat/${accountId}`,
    method: 'delete'
  })
}

export function deleteWechatMenu(id) {
  return request({
    url: `/wechat/menu/${id}`,
    method: 'delete'
  })
}

export function syncWechatMenuFromWechat(accountId) {
  return request({
    url: `/wechat/menu/sync/${accountId}`,
    method: 'post'
  })
}

export function listWechatReply(query) {
  return request({
    url: '/wechat/reply',
    method: 'get',
    params: query
  })
}

export function saveWechatReply(data) {
  return request({
    url: '/wechat/reply',
    method: 'post',
    data
  })
}

export function deleteWechatReply(id) {
  return request({
    url: `/wechat/reply/${id}`,
    method: 'delete'
  })
}

export function getWechatModuleConfig() {
  return request({
    url: '/wechat/config',
    method: 'get'
  })
}

export function saveWechatModuleConfig(data) {
  return request({
    url: '/wechat/config',
    method: 'post',
    data
  })
}

export function listWechatFans(query) {
  return request({
    url: '/wechat/fans',
    method: 'get',
    params: query
  })
}

export function syncWechatFans(accountId) {
  return request({
    url: '/wechat/fans/sync',
    method: 'post',
    params: { accountId }
  })
}

export function listWechatMessage(query) {
  return request({
    url: '/wechat/message',
    method: 'get',
    params: query
  })
}

export function listWechatQrcode(query) {
  return request({
    url: '/wechat/qrcode',
    method: 'get',
    params: query
  })
}

export function createWechatQrcode(data) {
  return request({
    url: '/wechat/qrcode',
    method: 'post',
    data
  })
}

export function deleteWechatQrcode(id) {
  return request({
    url: `/wechat/qrcode/${id}`,
    method: 'delete'
  })
}

export function listWechatTag(query) {
  return request({ url: '/wechat/tag', method: 'get', params: query })
}

export function listWechatTagOptions(accountId) {
  return request({ url: '/wechat/tag/options', method: 'get', params: { accountId } })
}

export function saveWechatTag(data) {
  return request({ url: '/wechat/tag', method: 'post', data })
}

export function deleteWechatTag(id) {
  return request({ url: `/wechat/tag/${id}`, method: 'delete' })
}

export function syncWechatTag(accountId) {
  return request({ url: '/wechat/tag/sync', method: 'post', params: { accountId } })
}

export function markWechatTag(data) {
  return request({ url: '/wechat/tag/mark', method: 'post', data })
}

export function checkWechatKefuSession(accountId, openId) {
  return request({ url: '/wechat/kefu/session', method: 'get', params: { accountId, openId } })
}

export function sendWechatKefuMessage(data) {
  return request({ url: '/wechat/kefu/send', method: 'post', data })
}

export function listWechatTemplate(accountId) {
  return request({ url: '/wechat/template/list', method: 'get', params: { accountId } })
}

export function sendWechatTemplate(data) {
  return request({ url: '/wechat/template/send', method: 'post', data })
}

export function listWechatMass(query) {
  return request({ url: '/wechat/mass', method: 'get', params: query })
}

export function previewWechatMass(data) {
  return request({ url: '/wechat/mass/preview', method: 'post', data })
}

export function sendWechatMass(data) {
  return request({ url: '/wechat/mass/send', method: 'post', data })
}

export function syncWechatMassStatus(id) {
  return request({ url: `/wechat/mass/${id}/sync-status`, method: 'post' })
}
