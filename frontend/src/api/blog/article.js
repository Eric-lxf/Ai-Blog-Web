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

/** 自动保存草稿 */
export function autoSaveDraft(data) {
  return request({
    url: '/blog/article',
    method: 'post',
    data: { ...data, status: 0 },
  })
}

export function deleteArticle(id) {
  return request({
    url: `/blog/article/${id}`,
    method: 'delete',
  })
}
