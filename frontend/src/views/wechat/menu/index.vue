<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="账号">
        <el-select v-model="queryParams.accountId" clearable filterable placeholder="全部账号" style="width: 220px">
          <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="菜单 JSON 关键字" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" v-hasPermi="['wechat:menu:add']" @click="openDialog()">新增菜单</el-button>
        <el-button type="info" plain icon="View" v-hasPermi="['wechat:menu:query']" :disabled="!queryParams.accountId" @click="handleQueryWechat">查询微信菜单</el-button>
        <el-button type="success" plain icon="Download" v-hasPermi="['wechat:menu:sync']" :disabled="!queryParams.accountId" @click="handleSyncWechat">同步到本地</el-button>
        <el-button type="warning" plain icon="Delete" v-hasPermi="['wechat:menu:publish']" :disabled="!queryParams.accountId" @click="handleDeleteWechat">删除微信菜单</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="账号ID" prop="accountId" width="90" />
      <el-table-column label="账号名称" min-width="140">
        <template #default="{ row }">{{ accountNameMap[row.accountId] || '-' }}</template>
      </el-table-column>
      <el-table-column label="已发布" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublished === 1 ? 'success' : 'info'">{{ row.isPublished === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="菜单JSON" prop="menuJson" min-width="260" show-overflow-tooltip />
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" width="220" align="center">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['wechat:menu:edit']" @click="openDialog(row)">编辑</el-button>
          <el-button link type="success" v-hasPermi="['wechat:menu:publish']" @click="handlePublish(row)">发布</el-button>
          <el-button link type="danger" v-hasPermi="['wechat:menu:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="860px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="accountId">
          <el-select v-model="form.accountId" filterable placeholder="请选择账号" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="编辑方式">
          <el-radio-group v-model="editMode" @change="handleEditModeChange">
            <el-radio-button label="visual">可视化</el-radio-button>
            <el-radio-button label="json">JSON</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="editMode === 'visual'">
          <div v-for="(btn, index) in visualButtons" :key="index" class="menu-block mb12">
            <el-card shadow="never">
              <template #header>
                <div class="menu-block-header">
                  <span>主菜单 {{ index + 1 }}</span>
                  <el-button v-if="visualButtons.length > 1" link type="danger" @click="removeTopButton(index)">删除</el-button>
                </div>
              </template>
              <el-form-item label="名称" label-width="70px">
                <el-input v-model="btn.name" maxlength="16" placeholder="最多 16 字符" />
              </el-form-item>
              <el-form-item label="类型" label-width="70px">
                <el-select v-model="btn.menuType" style="width: 180px">
                  <el-option label="点击事件 (click)" value="click" />
                  <el-option label="跳转链接 (view)" value="view" />
                  <el-option label="子菜单" value="submenu" />
                </el-select>
              </el-form-item>
              <template v-if="btn.menuType === 'click'">
                <el-form-item label="Key" label-width="70px"><el-input v-model="btn.key" maxlength="128" placeholder="事件 KEY" /></el-form-item>
              </template>
              <template v-else-if="btn.menuType === 'view'">
                <el-form-item label="URL" label-width="70px"><el-input v-model="btn.url" placeholder="https://..." /></el-form-item>
              </template>
              <template v-else>
                <div v-for="(sub, subIndex) in btn.subButtons" :key="subIndex" class="submenu-block">
                  <el-divider content-position="left">子菜单 {{ subIndex + 1 }}</el-divider>
                  <el-form-item label="名称" label-width="70px"><el-input v-model="sub.name" maxlength="16" /></el-form-item>
                  <el-form-item label="类型" label-width="70px">
                    <el-select v-model="sub.type" style="width: 180px">
                      <el-option label="点击 (click)" value="click" />
                      <el-option label="链接 (view)" value="view" />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-if="sub.type === 'click'" label="Key" label-width="70px"><el-input v-model="sub.key" maxlength="128" /></el-form-item>
                  <el-form-item v-else label="URL" label-width="70px"><el-input v-model="sub.url" placeholder="https://..." /></el-form-item>
                  <el-button v-if="btn.subButtons.length > 1" link type="danger" @click="removeSubButton(index, subIndex)">删除子菜单</el-button>
                </div>
                <el-button v-if="btn.subButtons.length < 5" type="primary" plain size="small" @click="addSubButton(index)">添加子菜单</el-button>
              </template>
            </el-card>
          </div>
          <el-button v-if="visualButtons.length < 3" type="primary" plain icon="Plus" @click="addTopButton">添加主菜单</el-button>
        </template>
        <el-form-item v-else label="菜单JSON" prop="menuJson">
          <el-input v-model="form.menuJson" type="textarea" :rows="14" placeholder='按钮数组，例如：[{"type":"click","name":"今日歌曲","key":"V1001_TODAY_MUSIC"}]' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="wechatMenuVisible" title="微信当前菜单" width="760px" append-to-body>
      <el-input v-model="wechatMenuJson" type="textarea" :rows="18" readonly />
      <template #footer>
        <el-button v-hasPermi="['wechat:menu:sync']" type="success" :disabled="!queryParams.accountId" @click="handleSyncWechat">同步到本地</el-button>
        <el-button type="primary" @click="wechatMenuVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { deleteWechatMenu, deleteWechatMenuFromWechat, getWechatMenuFromWechat, listWechatAccountOptions, listWechatMenu, publishWechatMenu, saveWechatMenu, syncWechatMenuFromWechat } from '@/api/wechat'
defineOptions({ name: 'WechatMenu' })
const { proxy } = getCurrentInstance()
const formRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const wechatMenuVisible = ref(false)
const wechatMenuJson = ref('')
const editMode = ref('visual')
const list = ref([])
const total = ref(0)
const accountOptions = ref([])
const accountNameMap = computed(() => { const map = {}; accountOptions.value.forEach(item => { map[item.id] = item.name }); return map })
const queryParams = ref({ pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined })
const form = reactive({ id: undefined, accountId: undefined, menuJson: '' })
const visualButtons = ref([])
const rules = {
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  menuJson: [{ required: true, message: '请输入菜单 JSON', trigger: 'blur' }]
}
const defaultMenuJson = '[{"type":"click","name":"示例菜单","key":"DEMO_KEY"}]'

function createEmptySubButton() {
  return { type: 'click', name: '', key: '', url: '' }
}
function createEmptyTopButton() {
  return { menuType: 'click', name: '', key: '', url: '', subButtons: [createEmptySubButton()] }
}
function addTopButton() {
  if (visualButtons.value.length >= 3) return
  visualButtons.value.push(createEmptyTopButton())
}
function removeTopButton(index) {
  visualButtons.value.splice(index, 1)
}
function addSubButton(topIndex) {
  const btn = visualButtons.value[topIndex]
  if (!btn || btn.subButtons.length >= 5) return
  btn.subButtons.push(createEmptySubButton())
}
function removeSubButton(topIndex, subIndex) {
  visualButtons.value[topIndex].subButtons.splice(subIndex, 1)
}
function buildMenuJsonFromVisual() {
  const buttons = visualButtons.value.filter(btn => btn.name?.trim()).map(btn => {
    if (btn.menuType === 'submenu') {
      return {
        name: btn.name.trim(),
        sub_button: btn.subButtons.filter(sub => sub.name?.trim()).map(sub => {
          const item = { type: sub.type, name: sub.name.trim() }
          if (sub.type === 'click') item.key = sub.key?.trim() || ''
          else item.url = sub.url?.trim() || ''
          return item
        })
      }
    }
    const item = { type: btn.menuType, name: btn.name.trim() }
    if (btn.menuType === 'click') item.key = btn.key?.trim() || ''
    else item.url = btn.url?.trim() || ''
    return item
  })
  return JSON.stringify(buttons, null, 2)
}
function loadVisualFromJson(json) {
  try {
    const parsed = JSON.parse(json || '[]')
    const buttons = Array.isArray(parsed) ? parsed : (parsed.button || [])
    if (!Array.isArray(buttons) || buttons.length === 0) {
      visualButtons.value = [createEmptyTopButton()]
      return
    }
    visualButtons.value = buttons.map(btn => {
      if (btn.sub_button?.length) {
        return {
          menuType: 'submenu',
          name: btn.name || '',
          key: '',
          url: '',
          subButtons: btn.sub_button.map(sub => ({
            type: sub.type || 'click',
            name: sub.name || '',
            key: sub.key || '',
            url: sub.url || ''
          }))
        }
      }
      return {
        menuType: btn.type || 'click',
        name: btn.name || '',
        key: btn.key || '',
        url: btn.url || '',
        subButtons: [createEmptySubButton()]
      }
    })
  } catch {
    visualButtons.value = [createEmptyTopButton()]
  }
}
function handleEditModeChange(mode) {
  if (mode === 'visual') {
    loadVisualFromJson(form.menuJson || defaultMenuJson)
  } else {
    form.menuJson = buildMenuJsonFromVisual()
  }
}
function loadAccounts() { return listWechatAccountOptions().then(res => { accountOptions.value = res.data || [] }) }
function resetForm() {
  Object.assign(form, { id: undefined, accountId: undefined, menuJson: defaultMenuJson })
  editMode.value = 'visual'
  loadVisualFromJson(defaultMenuJson)
  formRef.value?.clearValidate()
}
function getList() { loading.value = true; listWechatMenu(queryParams.value).then(res => { list.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, accountId: undefined, keyword: undefined }; getList() }
function openDialog(row) {
  resetForm()
  if (row) {
    Object.assign(form, row)
    loadVisualFromJson(form.menuJson || defaultMenuJson)
  }
  dialogVisible.value = true
}
function submitForm() {
  if (editMode.value === 'visual') {
    form.menuJson = buildMenuJsonFromVisual()
    if (!form.menuJson || form.menuJson === '[]') {
      proxy.$modal.msgError('请至少配置一个有效的主菜单')
      return
    }
  }
  formRef.value.validate(valid => {
    if (!valid) return
    try { JSON.parse(form.menuJson) } catch { proxy.$modal.msgError('菜单 JSON 格式错误'); return }
    submitLoading.value = true
    saveWechatMenu(form).then(() => { proxy.$modal.msgSuccess('保存成功'); dialogVisible.value = false; getList() }).finally(() => { submitLoading.value = false })
  })
}
function handlePublish(row) {
  proxy.$modal.confirm('确认发布该菜单到微信吗？').then(() => publishWechatMenu(row.id)).then(() => { proxy.$modal.msgSuccess('发布成功'); getList() }).catch(() => {})
}
function handleDelete(row) {
  proxy.$modal.confirm('确认删除该本地菜单记录吗？').then(() => deleteWechatMenu(row.id)).then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {})
}
function handleQueryWechat() {
  if (!queryParams.value.accountId) {
    proxy.$modal.msgWarning('请先选择账号')
    return
  }
  getWechatMenuFromWechat(queryParams.value.accountId).then(res => {
    wechatMenuJson.value = JSON.stringify(res.data || {}, null, 2)
    wechatMenuVisible.value = true
  })
}
function handleSyncWechat() {
  if (!queryParams.value.accountId) {
    proxy.$modal.msgWarning('请先选择账号')
    return
  }
  proxy.$modal.confirm('确认将微信端当前菜单同步到本地吗？若已有记录将覆盖 menuJson。').then(() => syncWechatMenuFromWechat(queryParams.value.accountId))
    .then(() => { proxy.$modal.msgSuccess('同步成功'); wechatMenuVisible.value = false; getList() }).catch(() => {})
}
function handleDeleteWechat() {
  if (!queryParams.value.accountId) {
    proxy.$modal.msgWarning('请先选择账号')
    return
  }
  proxy.$modal.confirm('确认删除该账号在微信端的自定义菜单吗？').then(() => deleteWechatMenuFromWechat(queryParams.value.accountId))
    .then(() => proxy.$modal.msgSuccess('微信菜单已删除')).catch(() => {})
}
Promise.all([loadAccounts()]).finally(() => getList())
</script>
<style scoped>
.menu-block-header { display: flex; justify-content: space-between; align-items: center; }
.submenu-block { margin-left: 12px; padding-left: 12px; border-left: 2px solid var(--el-border-color-light); }
.mb12 { margin-bottom: 12px; }
</style>
