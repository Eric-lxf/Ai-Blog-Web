import request from '@/utils/request'
import { getVisitorId } from '@/utils/blogVisitor'

const noToken = { isToken: false }

function publicHeaders() {
  return {
    isToken: false,
    'X-Blog-Visitor': getVisitorId()
  }
}

export function fetchPublicArticles(params) {
  return request({
    url: '/public/blog/articles',
    method: 'get',
    params,
    headers: publicHeaders(),
  })
}

export function fetchPublicArticleDetail(id) {
  return request({
    url: `/public/blog/articles/${id}`,
    method: 'get',
    headers: publicHeaders(),
  })
}

/** 上报前台访问（首页等） */
export function trackPublicVisit(pageType, articleId) {
  return request({
    url: '/public/blog/track',
    method: 'post',
    headers: publicHeaders(),
    data: {
      pageType,
      articleId,
      visitorId: getVisitorId()
    }
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
