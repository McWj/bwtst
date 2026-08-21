<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">供应商资料</h2>
      <div class="page-actions">
        <el-input v-model="keyword" placeholder="供应商编号/名称" clearable style="width: 200px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openAdd">新增</el-button>
      </div>
    </div>

    <el-table :data="list" border stripe v-loading="loading" class="page-table">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="supplierCode" label="供应商编号" width="140" />
      <el-table-column prop="supplierName" label="供应商名称" min-width="180" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column prop="address" label="联系地址" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'info' : 'success'">
            {{ row.status === '0' ? '停用' : '启用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="page-pagination"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="onPageChange"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑供应商' : '新增供应商'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="供应商编号" required>
          <el-input v-model="form.supplierCode" placeholder="请输入供应商编号" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="供应商名称" required>
          <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="form.address" placeholder="请输入联系地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { supplierApi } from '../api'
import type { Supplier } from '../types'

const list = ref<Supplier[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const form = reactive<Supplier>({
  supplierCode: '',
  supplierName: '',
  contact: '',
  phone: '',
  address: '',
  status: '1',
  remark: ''
})

async function load() {
  loading.value = true
  try {
    const res: any = await supplierApi.page({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    list.value = (res.data?.records ?? res.data?.list ?? res.data ?? [])
    total.value = res.data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) {
  pageNum.value = p
  load()
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    supplierCode: '',
    supplierName: '',
    contact: '',
    phone: '',
    address: '',
    status: '1',
    remark: ''
  })
}

function openAdd() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Supplier) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

async function submit() {
  if (!form.supplierCode || !form.supplierName) {
    ElMessage.warning('请填写供应商编号和名称')
    return
  }
  try {
    await supplierApi.save(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

async function remove(row: Supplier) {
  try {
    await ElMessageBox.confirm(`确认删除供应商「${row.supplierName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await supplierApi.remove(row.id!)
    ElMessage.success('删除成功')
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }
.page-actions { display: flex; gap: 8px; }
.page-table { margin-bottom: 16px; }
.page-pagination { justify-content: flex-end; display: flex; }
</style>
