import request from '@/utils/request'

const baseUrl = '/mall/brand'

export function listMallBrand(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

/** 商品筛选/表单下拉（全量正常品牌） */
export function listMallBrandOptions() {
  return request({
    url: `${baseUrl}/options`,
    method: 'get'
  })
}

export function getMallBrand(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallBrand(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallBrand(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delMallBrand(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}
