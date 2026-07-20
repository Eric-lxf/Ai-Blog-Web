import request from '@/utils/request'

const baseUrl = '/mall/category'

export function listMallCategory(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

/** 商品筛选/表单下拉（树形） */
export function listMallCategoryOptions() {
  return request({
    url: `${baseUrl}/options`,
    method: 'get'
  })
}

export function getMallCategory(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallCategory(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallCategory(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delMallCategory(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

export function listMallCategoryAttrs(id) {
  return request({
    url: `${baseUrl}/${id}/attrs`,
    method: 'get'
  })
}

/** body 必须为 { items: [...] } */
export function replaceMallCategoryAttrs(id, items) {
  return request({
    url: `${baseUrl}/${id}/attrs`,
    method: 'put',
    data: { items }
  })
}
