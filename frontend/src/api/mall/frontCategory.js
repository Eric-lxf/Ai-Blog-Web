import request from '@/utils/request'

const baseUrl = '/mall/front-category'

export function listMallFrontCategory(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

export function listMallFrontCategoryOptions() {
  return request({
    url: `${baseUrl}/options`,
    method: 'get'
  })
}

export function getMallFrontCategory(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallFrontCategory(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallFrontCategory(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delMallFrontCategory(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

export function listMallFrontCategoryRels(id) {
  return request({
    url: `${baseUrl}/${id}/rels`,
    method: 'get'
  })
}

/** body 必须为 { backCategoryIds: [...] }，不可传裸数组 */
export function replaceMallFrontCategoryRels(id, backCategoryIds) {
  return request({
    url: `${baseUrl}/${id}/rels`,
    method: 'put',
    data: { backCategoryIds }
  })
}
