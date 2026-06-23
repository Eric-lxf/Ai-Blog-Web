import request from '@/utils/request'

/** 上传文件，返回文件记录（含 fileUrl） */
export function uploadFile(formData) {
  return request({
    url: '/blog/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 分页查询文件列表 */
export function listFile(params) {
  return request({ url: '/blog/file/list', method: 'get', params })
}

/** 删除文件 */
export function deleteFile(id) {
  return request({ url: `/blog/file/${id}`, method: 'delete' })
}