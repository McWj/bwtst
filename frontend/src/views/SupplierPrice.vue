<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">供应商价格表</h2>
      <div class="page-actions">
        <el-select v-model="filterSupplier" placeholder="供应商" clearable style="width: 180px" @change="load">
          <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
        </el-select>
        <el-select v-model="filterItem" placeholder="商品" clearable filterable style="width: 200px" @change="load">
          <el-option v-for="g in goods" :key="g.id" :label="`${g.itemNo} ${g.itemName}`" :value="g.id" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openAdd">新增</el-button>
      </div>
    </div>

    <el-table :data="list" border stripe v-loading="loading" class="page-table">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="supplierCode" label="供应商编号" width="140" />
      <el-table-column prop="supplierName" label="供应商名称" min-width="160" />
      <el-table-column prop="itemNo" label="商品编号" width="140" />
      <el-table-column prop="itemName" label="商品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="price" label="单价" width="120" align="right">
        <template #default="{ row }">
          {{ formatPrice(row.price) }}
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑价格' : '新增价格'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="供应商" required>
          <el-select v-model="form.supplierId" placeholder="请选择供应商" :disabled="!!form.id" style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品" required>
          <el-select v-model="form.itemId" placeholder="请选择商品" filterable :disabled="!!form.id" style="width: 100%">
            <el-option v-for="g in goods" :key="g.id" :label="`${g.itemNo} ${g.itemName}`" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价" required>
          <el-input-number v-model="form.price" :min="0" :precision="4" :step="0.1" style="width: 100%" />
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
import { supplierPriceApi, supplierApi, goodsApi } from '../api'
import type { SupplierPrice, Supplier, Goods } from '../types'

const list = ref<SupplierPrice[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const suppliers = ref<Supplier[]>([])
const goods = ref<Goods[]>([])
const filterSupplier = ref<number | undefined>()
const filterItem = ref<number | undefined>()

const dialogVisible = ref(false)
const form = reactive<SupplierPrice>({
  supplierId: undefined,
  itemId: undefined,
  price: 0,
  status: '1',
  remark: ''
})

function formatPrice(v: any) {
  if (v === null || v === undefined || v === '') return '-'
  return Number(v).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const res: any = await supplierPriceApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      supplierId: filterSupplier.value,
      itemId: filterItem.value
    })
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
    supplierId: undefined,
    itemId: undefined,
    price: 0,
    status: '1',
    remark: ''
  })
}

function openAdd() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: SupplierPrice) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

async function submit() {
  if (form.supplierId == null || form.itemId == null) {
    ElMessage.warning('请选择供应商和商品')
    return
  }
  if (form.price == null) {
    ElMessage.warning('请填写单价')
    return
  }
  try {
    await supplierPriceApi.save(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

async function remove(row: SupplierPrice) {
  try {
    await ElMessageBox.confirm('确认删除该价格记录？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await supplierPriceApi.remove(row.id!)
    ElMessage.success('删除成功')
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(async () => {
  const [sRes, gRes]: any[] = await Promise.all([supplierApi.list(), goodsApi.list()])
  suppliers.value = sRes.data ?? []
  goods.value = gRes.data ?? []
  load()
})
</script>

<style scoped>
.page-container { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }
.page-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.page-table { margin-bottom: 16px; }
.page-pagination { justify-content: flex-end; display: flex; }
</style>
