import request from '@/utils/request'

/** 分页查询账单列表 */
export function listBill(params) {
  return request({ url: '/blog/bill/list', method: 'get', params })
}

/** 获取单条账单详情 */
export function getBill(id) {
  return request({ url: `/blog/bill/${id}`, method: 'get' })
}

/** 新增账单 */
export function addBill(data) {
  return request({ url: '/blog/bill', method: 'post', data })
}

/** 修改账单 */
export function updateBill(data) {
  return request({ url: '/blog/bill', method: 'put', data })
}

/** 删除账单 */
export function deleteBill(id) {
  return request({ url: `/blog/bill/${id}`, method: 'delete' })
}

/** AI 识别账单图片（不写库，返回解析结果） */
export function recognizeBill(data) {
  return request({ url: '/blog/bill/recognize', method: 'post', data })
}

/** 获取消费分析数据，months 为近几个月（3/6/12） */
export function getAnalysis(months = 6) {
  return request({ url: '/blog/bill/analysis', method: 'get', params: { months } })
}