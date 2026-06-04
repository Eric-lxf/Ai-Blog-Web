<script setup>
defineOptions({ name: 'BlogDashboard' })

import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { QuestionFilled } from '@element-plus/icons-vue'
import { fetchAnalyticsDashboard } from '@/api/blog/analytics'

const loading = ref(false)
const days = ref(7)
const summary = ref({
  pv: 0,
  uv: 0,
  readCount: 0,
  likeCount: 0,
  commentCount: 0,
  newUsers: 0,
  totalReadCount: 0
})
const trend = ref([])
const hotArticles = ref([])
const sources = ref([])
const regions = ref([])

const trendChartRef = ref(null)
const sourceChartRef = ref(null)
const regionChartRef = ref(null)
let trendChart
let sourceChart
let regionChart

const kpiCards = [
  { key: 'pv', label: 'PV', tip: '页面浏览量' },
  { key: 'uv', label: 'UV', tip: '独立访客数' },
  { key: 'readCount', label: '阅读量', tip: '统计周期内文章阅读次数' },
  { key: 'likeCount', label: '点赞量', tip: '评论点赞次数' },
  { key: 'commentCount', label: '评论量', tip: '已通过评论数' },
  { key: 'newUsers', label: '用户增长', tip: '新注册用户' }
]

async function loadData() {
  loading.value = true
  try {
    const res = await fetchAnalyticsDashboard(days.value)
    const data = res.data || {}
    summary.value = { ...summary.value, ...(data.summary || {}) }
    trend.value = data.trend || []
    hotArticles.value = data.hotArticles || []
    sources.value = data.sources || []
    regions.value = data.regions || []
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  renderTrendChart()
  sourceChart = renderPieChart(sourceChartRef.value, sourceChart, sources.value, '访问来源')
  regionChart = renderPieChart(regionChartRef.value, regionChart, regions.value, '地域分布')
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const labels = trend.value.map(i => i.label)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['PV', 'UV', '评论', '点赞', '新用户'] },
    grid: { left: 48, right: 24, bottom: 32, top: 40 },
    xAxis: { type: 'category', data: labels, boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: 'PV', type: 'line', smooth: true, data: trend.value.map(i => i.pv ?? 0) },
      { name: 'UV', type: 'line', smooth: true, data: trend.value.map(i => i.uv ?? 0) },
      { name: '评论', type: 'line', smooth: true, data: trend.value.map(i => i.comments ?? 0) },
      { name: '点赞', type: 'line', smooth: true, data: trend.value.map(i => i.likes ?? 0) },
      { name: '新用户', type: 'line', smooth: true, data: trend.value.map(i => i.newUsers ?? 0) }
    ]
  })
}

function renderPieChart(el, instance, list, title) {
  if (!el) return instance
  let chart = instance
  if (!chart) {
    chart = echarts.init(el)
  }
  chart.setOption({
    title: { text: title, left: 'center', top: 8, textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left', top: 36 },
    series: [{
      type: 'pie',
      radius: ['36%', '62%'],
      center: ['58%', '55%'],
      data: (list || []).map(i => ({ name: i.name, value: i.count }))
    }]
  })
  return chart
}

function handleResize() {
  trendChart?.resize()
  sourceChart?.resize()
  regionChart?.resize()
}

watch(days, loadData)

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  sourceChart?.dispose()
  regionChart?.dispose()
})
</script>

<template>
  <div class="app-container dashboard">
    <div class="toolbar">
      <span class="title">数据统计</span>
      <el-radio-group v-model="days" size="small">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
      </el-radio-group>
    </div>

    <el-row v-loading="loading" :gutter="16" class="kpi-row">
      <el-col v-for="item in kpiCards" :key="item.key" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">
            {{ item.label }}
            <el-tooltip :content="item.tip" placement="top">
              <el-icon class="kpi-tip"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="kpi-value">{{ summary[item.key] ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      class="total-read"
      :title="`累计文章阅读（view_count 总和）：${summary.totalReadCount ?? 0}`"
      type="info"
      :closable="false"
      show-icon
    />

    <el-card shadow="never" class="chart-card">
      <template #header>访问与互动趋势</template>
      <div ref="trendChartRef" class="chart-box" />
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <div ref="sourceChartRef" class="chart-box chart-box-pie" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <div ref="regionChartRef" class="chart-box chart-box-pie" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>热门文章（周期内 PV 优先）</template>
      <el-table :data="hotArticles" stripe>
        <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="周期 PV" prop="periodPv" width="100" align="center" />
        <el-table-column label="总阅读" prop="viewCount" width="100" align="center" />
        <el-table-column label="评论" prop="commentCount" width="90" align="center" />
        <el-table-column label="点赞" prop="likeCount" width="90" align="center" />
      </el-table>
      <el-empty v-if="!loading && hotArticles.length === 0" description="暂无已发布文章或访问数据" />
    </el-card>
  </div>
</template>

<style scoped>
.dashboard .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dashboard .title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.kpi-row {
  margin-bottom: 16px;
}

.kpi-card {
  margin-bottom: 16px;
}

.kpi-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #64748b;
}

.kpi-tip {
  cursor: help;
  font-size: 14px;
}

.kpi-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.total-read {
  margin-bottom: 16px;
}

.chart-card {
  margin-bottom: 16px;
}

.chart-box {
  height: 360px;
}

.chart-box-pie {
  height: 320px;
}
</style>
