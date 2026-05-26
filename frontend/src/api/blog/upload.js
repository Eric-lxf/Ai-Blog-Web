import request from '@/utils/request'

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/blog/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  }).then(res => res.data?.url || '')
}
