<template>
  <div class="app-container">
    <div class="page-header">
      <span class="page-title">消费分析</span>
      <el-radio-group v-model="months" @change="loadAnalysis">
        <el-radio-button :value="3">近 3 个月</el-radio-button>
        <el-radio-button :value="6">近 6 个月</el-radio-button>
        <el-radio-button :value="12">近 12 个月</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading">
      <!-- KPI 卡片 -->
      <el-row :gutter="16" class="kpi-row">
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">总消费（元）</div>
            <div class="kpi-value primary">{{ summary.totalAmount?.toFixed(2) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">月均消费（元）</div>
            <div class="kpi-value success">{{ summary.monthlyAvg?.toFixed(2) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">账单笔数</div>
            <div class="kpi-value warning">{{ summary.billCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">最多消费类目</div>
            <div class="kpi-value danger">{{ summary.topCategory }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表区 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>分类占比</template>
            <div ref="pieRef" class="chart-box" />
          </el-card>
        </el-col>
        <el-col :span="16">
          <el-card shadow="never">
            <template #header>月度消费趋势</template>
            <div ref="lineRef" class="chart-box" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>各类目月度堆叠</template>
            <div ref="barRef" class="chart-box" />
          </el-card>
        </el-col>
      </el-row>

      <!-- AI 建议 -->
      <el-card shadow="never" class="advice-card" v-if="aiAdvice.length > 0">
        <template #header>
          <span>✨ AI 理财建议</span>
        </template>
        <el-row :gutter="12">
          <el-col v-for="(item, i) in aiAdvice" :key="i" :span="6">
            <el-alert
              :type="item.tone || 'info'"
              :title="item.title"
              :description="item.detail"
              show-icon
              :closable="false"
            />
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, DatasetComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getAnalysis } from '@/api/blog/bill'

echarts.use([
  PieChart, LineChart, BarChart,
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, DatasetComponent, CanvasRenderer
])

const loading  = ref(false)
const months   = ref(6)
const summary  = ref({ totalAmount: 0, monthlyAvg: 0, billCount: 0, topCategory: '—' })
const aiAdvice = ref([])

const pieRef  = ref(null)
const lineRef = ref(null)
const barRef  = ref(null)
let pieChart = null, lineChart = null, barChart = null

async function loadAnalysis() {
  loading.value = true
  try {
    const res = await getAnalysis(months.value)
    const data = res.data || {}
    summary.value  = data.summary  || { totalAmount: 0, monthlyAvg: 0, billCount: 0, topCategory: '—' }
    aiAdvice.value = data.aiAdvice || []
    await nextTick()
    renderPie(data.categoryPie  || [])
    renderLine(data.months      || [], data.monthlyTrend || [])
    renderBar(data.months       || [], data.monthlyTrend || [])
  } finally {
    loading.value = false
  }
}

function initChart(el, existingInstance) {
  if (existingInstance) existingInstance.dispose()
  return echarts.init(el)
}

function renderPie(data) {
  pieChart = initChart(pieRef.value, pieChart)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 元 ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: data.map(d => ({ name: d.name, value: d.value })),
      label: { show: false }
    }]
  })
}

function renderLine(monthList, series) {
  lineChart = initChart(lineRef.value, lineChart)
  const totalByMonth = monthList.map((_, i) =>
    series.reduce((sum, s) => sum + (Number(s.data[i]) || 0), 0)
  )
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: monthList },
    yAxis: { type: 'value', name: '元' },
    series: [{
      name: '月度总消费', type: 'line', smooth: true,
      areaStyle: { opacity: 0.2 }, data: totalByMonth
    }]
  })
}

function renderBar(monthList, series) {
  barChart = initChart(barRef.value, barChart)
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: {},
    xAxis: { type: 'category', data: monthList },
    yAxis: { type: 'value', name: '元' },
    series: series.map(s => ({
      name: s.name, type: 'bar', stack: 'total',
      data: s.data.map(v => Number(v) || 0)
    }))
  })
}

function resizeCharts() {
  pieChart?.resize()
  lineChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  loadAnalysis()
  window.addEventListener('resize', resizeCharts)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  pieChart?.dispose()
  lineChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title { font-size: 18px; font-weight: 600; }
.kpi-row    { margin-bottom: 16px; }
.kpi-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  text-align: center;
}
.kpi-label { color: #909399; font-size: 13px; margin-bottom: 8px; }
.kpi-value { font-size: 26px; font-weight: 700; }
.kpi-value.primary { color: #409eff; }
.kpi-value.success { color: #67c23a; }
.kpi-value.warning { color: #e6a23c; }
.kpi-value.danger  { color: #f56c6c; }
.chart-row  { margin-bottom: 16px; }
.chart-box  { height: 300px; }
.advice-card { margin-bottom: 16px; }
</style>