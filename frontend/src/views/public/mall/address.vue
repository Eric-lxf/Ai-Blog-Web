<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addMallAddress,
  listMallAddress,
  removeMallAddress,
  setDefaultMallAddress,
  updateMallAddress
} from '@/api/mall/address'

const loading = ref(false)
const saving = ref(false)
const addressList = ref([])
const open = ref(false)
const title = ref('')
const formRef = ref()
const form = reactive({
  id: undefined,
  receiver: '',
  mobile: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: '0'
})
const rules = {
  receiver: [{ required: true, message: '收货人不能为空', trigger: 'blur' }],
  mobile: [{ required: true, message: '手机号不能为空', trigger: 'blur' }],
  province: [{ required: true, message: '省份不能为空', trigger: 'blur' }],
  city: [{ required: true, message: '城市不能为空', trigger: 'blur' }],
  district: [{ required: true, message: '区县不能为空', trigger: 'blur' }],
  detail: [{ required: true, message: '详细地址不能为空', trigger: 'blur' }]
}

function normalizeRows(res) {
  return res.rows || res.data?.records || res.data || []
}

function isDefaultAddress(item) {
  return item.isDefault === '1' || item.defaultFlag === '1'
}

function addressText(item) {
  return `${item.province || ''}${item.city || ''}${item.district || ''}${item.detail || item.detailAddress || ''}`
}

async function getList() {
  loading.value = true
  try {
    const res = await listMallAddress()
    addressList.value = normalizeRows(res)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    receiver: '',
    mobile: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: '0'
  })
  formRef.value?.clearValidate()
}

function handleAdd() {
  resetForm()
  title.value = '新增收货地址'
  open.value = true
}

function handleUpdate(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    receiver: row.receiver || row.receiverName || '',
    mobile: row.mobile || '',
    province: row.province || '',
    city: row.city || '',
    district: row.district || '',
    detail: row.detail || row.detailAddress || '',
    isDefault: row.isDefault || row.defaultFlag || '0'
  })
  title.value = '修改收货地址'
  open.value = true
}

async function submitForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateMallAddress(form)
    } else {
      await addMallAddress(form)
    }
    ElMessage.success('保存成功')
    open.value = false
    getList()
  } finally {
    saving.value = false
  }
}

async function handleRemove(row) {
  await ElMessageBox.confirm('确认删除该收货地址吗？', '提示', { type: 'warning' })
  await removeMallAddress(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function handleDefault(row) {
  await setDefaultMallAddress(row)
  ElMessage.success('已设为默认地址')
  getList()
}

onMounted(getList)
</script>

<template>
  <div class="address-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <strong>收货地址</strong>
          <div>
            <RouterLink to="/mall/checkout" class="back-link">返回结算</RouterLink>
            <el-button type="primary" @click="handleAdd">新增地址</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="!loading && addressList.length === 0" description="暂无收货地址" />
        <div v-else class="address-grid">
          <div v-for="item in addressList" :key="item.id" class="address-card">
            <div class="address-title">
              <strong>{{ item.receiver || item.receiverName }} {{ item.mobile }}</strong>
              <el-tag v-if="isDefaultAddress(item)" type="success" size="small">默认</el-tag>
            </div>
            <p>{{ addressText(item) }}</p>
            <div class="actions">
              <el-button v-if="!isDefaultAddress(item)" link type="primary" @click="handleDefault(item)">设为默认</el-button>
              <el-button link type="primary" @click="handleUpdate(item)">编辑</el-button>
              <el-button link type="danger" @click="handleRemove(item)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="open" :title="title" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="form.receiver" placeholder="请输入收货人" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="省" prop="province">
              <el-input v-model="form.province" placeholder="省" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市" prop="city" label-width="40px">
              <el-input v-model="form.city" placeholder="市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县" prop="district" label-width="52px">
              <el-input v-model="form.district" placeholder="区县" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" placeholder="街道、门牌号等" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header,
.address-title,
.actions {
  display: flex;
  align-items: center;
}

.header,
.address-title {
  justify-content: space-between;
}

.back-link {
  margin-right: 14px;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.address-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
}

.address-card p {
  min-height: 44px;
  color: #6b7280;
}

.actions {
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 800px) {
  .address-grid {
    grid-template-columns: 1fr;
  }
}
</style>
