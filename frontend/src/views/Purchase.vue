<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="单号/ERP单号/创建人" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <!-- add by codebuddy 20260817: 右上角统一门店维度，未选择门店时禁止新增 -->
      <el-tooltip :disabled="!!currentShopId" content="请先在右上角选择门店" placement="top">
        <span>
          <el-button type="success" :disabled="!currentShopId" @click="openDialog()">新增采购收货</el-button>
        </span>
      </el-tooltip>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="purchaseNo" label="采购单号" min-width="160" />
      <el-table-column prop="siteName" label="门店" min-width="130" />
      <el-table-column prop="purchaseDate" label="收货日期" width="110" />
      <el-table-column prop="erpNo" label="ERP单号" min-width="120" />
      <el-table-column prop="creator" label="创建人" width="90" />
      <!-- add by codebuddy 20260817: 状态列：空-未确认 Y-已确认 TJ-已提交 -->
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'TJ' ? 'success' : row.status === 'Y' ? 'warning' : 'info'">
            {{ row.status === 'TJ' ? '已提交' : row.status === 'Y' ? '已确认' : '未确认' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="view(row)">明细</el-button>
          <el-button v-if="!row.status" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="!row.status" link type="danger" @click="remove(row)">删除</el-button>
          <!-- add by codebuddy 20260817: 先确认后提交 -->
          <el-button v-if="!row.status" link type="warning" @click="confirmDoc(row)">确认</el-button>
          <el-button v-if="row.status === 'Y'" link type="success" @click="submitDoc(row)">提交</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]"
      @current-change="load"
      @size-change="load"
    />
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑采购收货' : '新增采购收货'" width="920px" top="3vh" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="采购单号">
            <el-input v-model="form.purchaseNo" :disabled="!!form.id" placeholder="留空自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="收货日期" prop="purchaseDate">
            <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="ERP单号">
            <el-input v-model="form.erpNo" placeholder="ERP单号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="创建人">
            <el-input v-model="form.creator" placeholder="创建人" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">商品明细</el-divider>
    <GoodsDetailTable
      :rows="details"
      :goods-options="goodsOptions"
      qty-label="收货数量"
      show-requisition-qty
      @add="details.push({ requisitionQty: 0, qty: 0, unit: '', price: 0 })"
      @remove="(i: number) => details.splice(i, 1)"
    />

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="viewVisible" title="采购收货明细" width="860px">
    <el-descriptions :column="3" border size="small">
      <el-descriptions-item label="采购单号">{{ viewData.purchaseNo }}</el-descriptions-item>
      <el-descriptions-item label="门店">{{ viewData.siteName }}</el-descriptions-item>
      <el-descriptions-item label="收货日期">{{ viewData.purchaseDate }}</el-descriptions-item>
      <el-descriptions-item label="ERP单号">{{ viewData.erpNo }}</el-descriptions-item>
      <el-descriptions-item label="创建人">{{ viewData.creator }}</el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">商品明细</el-divider>
    <el-table :data="viewDetails" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="itemNo" label="商品编号" min-width="110" />
      <el-table-column prop="itemName" label="商品名称" min-width="160" />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column prop="requisitionQty" label="要货数量" width="90" align="right" />
      <el-table-column prop="qty" label="收货数量" width="90" align="right" />
      <el-table-column prop="price" label="单价" width="100" align="right" />
      <el-table-column prop="amount" label="金额" width="100" align="right" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { goodsApi, purchaseApi } from '../api'
import type { Goods, Purchase, PurchaseDetail } from '../types'
import { currentShopId, currentShopNo } from '../stores/globalShop'
import GoodsDetailTable, { type GoodsRow } from '../components/GoodsDetailTable.vue'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<Purchase[]>([])
const total = ref(0)
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; siteId?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const viewVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Purchase>({})
const details = ref<GoodsRow[]>([])
const goodsOptions = ref<Goods[]>([])
const viewData = reactive<Purchase>({})
const viewDetails = ref<PurchaseDetail[]>([])
const rules: FormRules = {
  purchaseDate: [{ required: true, message: '请选择收货日期', trigger: 'change' }],
  siteId: [{ required: true, message: '请选择门店', trigger: 'change' }]
}

// add by codebuddy 20260817: 右上角切换门店后自动刷新当前单据列表
watch(currentShopId, (val) => {
  query.siteId = val
  query.pageNum = 1
  load()
})

async function load() {
  // add by codebuddy 20260817: 未选择门店时不查询任何数据
  if (!query.siteId) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res: any = await purchaseApi.page({ ...query })
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.pageNum = 1
  load()
}

async function openDialog(row?: Purchase) {
  await initOptions()
  if (!row && !currentShopId.value) {
    ElMessage.warning('请先在右上角选择门店')
    return
  }
  dialogVisible.value = true
  details.value = []
  if (row) {
    const res: any = await purchaseApi.get(row.id!)
    Object.assign(form, res.data.header)
    details.value = (res.data.details || []).map((d: PurchaseDetail) => ({ ...d }))
  } else {
    // add by codebuddy 20260817: 新增时自动带入全局门店
    Object.assign(form, {
      id: undefined, purchaseNo: '', purchaseDate: '', erpNo: '',
      siteId: currentShopId.value, creator: currentShopNo()
    })
  }
}

async function view(row: Purchase) {
  const res: any = await purchaseApi.get(row.id!)
  Object.assign(viewData, res.data.header)
  viewDetails.value = res.data.details || []
  viewVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  if (!details.value.length) {
    ElMessage.warning('请至少添加一条商品明细')
    return
  }
  saving.value = true
  try {
    const dto = { header: { ...form }, details: details.value.map((d) => ({ ...d })) }
    if (form.id) {
      await purchaseApi.update(dto as any)
      ElMessage.success('修改成功')
    } else {
      await purchaseApi.save(dto as any)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Purchase) {
  await ElMessageBox.confirm(`确认删除采购单「${row.purchaseNo}」？`, '提示', { type: 'warning' })
  await purchaseApi.remove(row.id!)
  ElMessage.success('删除成功')
  load()
}

// add by codebuddy 20260818: 确认采购收货单（状态置为Y）
async function confirmDoc(row: Purchase) {
  try {
    await ElMessageBox.confirm(`确认采购收货单「${row.purchaseNo}」？`, '提示', { type: 'warning' })
    await purchaseApi.confirm(row.id!)
    ElMessage.success('确认成功')
    load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '确认失败，请重试')
  }
}

// add by codebuddy 20260818: 提交采购收货单（状态置为TJ）
async function submitDoc(row: Purchase) {
  try {
    await ElMessageBox.confirm(`提交采购收货单「${row.purchaseNo}」？`, '提示', { type: 'warning' })
    await purchaseApi.submit(row.id!)
    ElMessage.success('提交成功')
    load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '提交失败，请重试')
  }
}

async function initOptions() {
  const res: any = await goodsApi.list()
  goodsOptions.value = res.data
}

onMounted(() => {
  query.siteId = currentShopId.value
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.spacer {
  flex: 1;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
