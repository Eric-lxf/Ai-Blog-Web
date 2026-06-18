<template>
  <div class="app-container">
    <el-alert :closable="false" type="info" class="mb12" title="粉丝来源：认证服务号/认证订阅号可全量拉取；未认证个人号会报 48001，系统自动改从消息日志补全回调记录。关注/取关事件也会自动入库。" />
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 220px" @change="loadTags">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="queryParams.tagId" clearable filterable placeholder="全部标签" style="width: 180px">
          <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态"><el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 120px"><el-option label="已关注" :value="1" /><el-option label="未关注" :value="0" /></el-select></el-form-item>
      <el-form-item label="关键词"><el-input v-model="queryParams.keyword" placeholder="昵称/OpenID/备注" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" plain icon="Download" :loading="syncLoading" v-hasPermi="['wechat:fans:list']" @click="handleSync">拉取粉丝</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column label="ID" prop="id" width="80" /><el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="昵称" prop="nickname" min-width="120" show-overflow-tooltip />
      <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="标签" min-width="160"><template #default="{ row }"><el-tag v-for="name in (row.tagNames || [])" :key="name" size="small" class="mr4">{{ name }}</el-tag></template></el-table-column>
      <el-table-column label="OpenID" prop="openId" min-width="200" show-overflow-tooltip />
      <el-table-column label="关注状态" width="100"><template #default="{ row }"><el-tag :type="row.subscribeStatus === 1 ? 'success' : 'info'">{{ row.subscribeStatus === 1 ? '已关注' : '未关注' }}</el-tag></template></el-table-column>
      <el-table-column label="关注时间" prop="subscribeTime" width="170" /><el-table-column label="更新时间" prop="updateTime" width="170" />
    </el-table>
    <div class="mt8" v-if="selectedRows.length">
      <el-select v-model="markTagId" filterable placeholder="选择标签" style="width: 200px" class="mr8">
        <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="primary" plain v-hasPermi="['wechat:tag:mark']" @click="handleMark(true)">批量打标</el-button>
      <el-button type="warning" plain v-hasPermi="['wechat:tag:mark']" @click="handleMark(false)">批量取消</el-button>
    </div>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>
<script setup>
import { listWechatAccountOptions, listWechatFans, listWechatTagOptions, markWechatTag, syncWechatFans } from '@/api/wechat'
defineOptions({ name: 'WechatFans' })
const { proxy } = getCurrentInstance()
const loading = ref(false); const syncLoading = ref(false); const list = ref([]); const total = ref(0)
const accountOptions = ref([]); const tagOptions = ref([]); const selectedRows = ref([]); const markTagId = ref(undefined)
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, tagId: undefined, status: undefined, keyword: undefined })
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function loadTags() {
  tagOptions.value = []
  if (!queryParams.value.accountId) return
  listWechatTagOptions(queryParams.value.accountId).then(res => { tagOptions.value = res.data || [] })
}
function getList() { loading.value = true; listWechatFans(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, tagId: undefined, status: undefined, keyword: undefined }; tagOptions.value = []; getList() }
function handleSelectionChange(rows) { selectedRows.value = rows }
function handleSync() {
  if (!queryParams.value.accountId) { proxy.$modal.msgWarning('请先选择要同步的账号'); return }
  syncLoading.value = true
  syncWechatFans(queryParams.value.accountId).then(res => {
    const data = res.data || {}
    const source = data.source === 'message_log' ? '消息日志补全' : '微信接口'
    const msg = `同步完成（${source}）：共 ${data.total || 0} 人，写入 ${data.synced || 0} 条`
    if (data.warning) { proxy.$modal.msgWarning(data.warning + '\n' + msg) } else { proxy.$modal.msgSuccess(msg) }
    getList()
  }).finally(() => { syncLoading.value = false })
}
function handleMark(mark) {
  if (!queryParams.value.accountId) { proxy.$modal.msgWarning('请先选择账号'); return }
  if (!markTagId.value) { proxy.$modal.msgWarning('请选择标签'); return }
  if (!selectedRows.value.length) { proxy.$modal.msgWarning('请先勾选粉丝'); return }
  markWechatTag({
    accountId: queryParams.value.accountId,
    tagId: markTagId.value,
    mark,
    openIds: selectedRows.value.map(row => row.openId)
  }).then(() => { proxy.$modal.msgSuccess(mark ? '打标成功' : '取消打标成功'); getList() })
}
Promise.all([loadAccounts()]).finally(() => getList())
</script>
<style scoped>.mr4 { margin-right: 4px; }.mr8 { margin-right: 8px; }.mt8 { margin-top: 8px; }</style>
