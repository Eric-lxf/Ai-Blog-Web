import request from '@/utils/request'

/** 分页查询 */
export function fetchArticlePage(query) {
  return request({
    url: '/blog/article',
    method: 'get',
    params: query,
  })
}

export function fetchArticleDetail(id) {
  return request({
    url: `/blog/article/${id}`,
    method: 'get',
  })
}

export function saveArticle(data) {
  return request({
    url: '/blog/article',
    method: 'post',
    data,
  })
}

/** 自动保存（草稿保持 0，已发布保持 1） */
export function autoSaveDraft(data) {
  return request({
    url: '/blog/article',
    method: 'post',
    data,
  })
}

export function deleteArticle(id) {
  return request({
    url: `/blog/article/${id}`,
    method: 'delete',
  })
}

export function fetchRecyclePage(query) {
  return request({
    url: '/blog/article/recycle',
    method: 'get',
    params: query,
  })
}

export function restoreArticle(id) {
  return request({
    url: `/blog/article/recycle/${id}/restore`,
    method: 'put',
  })
}

export function purgeArticle(id) {
  return request({
    url: `/blog/article/recycle/${id}`,
    method: 'delete',
  })
}
