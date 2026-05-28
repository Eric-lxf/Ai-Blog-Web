import request from '@/utils/request'

const publicHeaders = { isToken: false }

export function fetchPublicComments(articleId, params) {
  return request({
    url: `/public/blog/articles/${articleId}/comments`,
    method: 'get',
    params,
    headers: publicHeaders,
  })
}

export function postComment(articleId, data) {
  return request({
    url: `/public/blog/articles/${articleId}/comments`,
    method: 'post',
    data,
  })
}

export function replyComment(commentId, data) {
  return request({
    url: `/public/blog/comments/${commentId}/reply`,
    method: 'post',
    data,
  })
}

export function toggleCommentLike(commentId) {
  return request({
    url: `/public/blog/comments/${commentId}/like`,
    method: 'post',
  })
}

export function reportComment(commentId, data) {
  return request({
    url: `/public/blog/comments/${commentId}/report`,
    method: 'post',
    data,
  })
}

export function fetchAdminComments(params) {
  return request({
    url: '/blog/comment',
    method: 'get',
    params,
  })
}

export function auditComments(data) {
  return request({
    url: '/blog/comment/audit',
    method: 'put',
    data,
  })
}

export function deleteComment(id) {
  return request({
    url: `/blog/comment/${id}`,
    method: 'delete',
  })
}

export function fetchCommentReports(params) {
  return request({
    url: '/blog/comment/report',
    method: 'get',
    params,
  })
}

export function handleCommentReport(id, data) {
  return request({
    url: `/blog/comment/report/${id}/handle`,
    method: 'put',
    data,
  })
}
