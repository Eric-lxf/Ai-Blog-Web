import request from '@/utils/request'

export function fetchAnalyticsDashboard(days = 7) {
  return request({
    url: '/blog/analytics/dashboard',
    method: 'get',
    params: { days }
  })
}
