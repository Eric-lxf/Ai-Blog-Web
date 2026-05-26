import request from '@/utils/request'

export function fetchCategories() {
  return request({
    url: '/blog/category',
    method: 'get',
  })
}
