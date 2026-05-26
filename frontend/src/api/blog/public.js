import request from '@/utils/request'

const noToken = { isToken: false }

export function fetchPublicArticles(params) {
  return request({
    url: '/public/blog/articles',
    method: 'get',
    params,
    headers: noToken,
  })
}

export function fetchPublicArticleDetail(id) {
  return request({
    url: `/public/blog/articles/${id}`,
    method: 'get',
    headers: noToken,
  })
}

/** 前台分类（免登录） */
export function fetchPublicCategories() {
  return request({
    url: '/blog/category',
    method: 'get',
    headers: noToken,
  })
}
