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
          <el-button type="success" :disabled="!currentShopId" @click="openDialog()">新增配送退货</el-button>
        </span>
      </el-tooltip>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="distributionReturnNo" label="退货单号" min-width="160" />
      <el-table-column prop="siteName" label="门店" min-width="130" />
      <!-- add by codebuddy 20260817: 配送中心列 -->
      <el-table-column prop="deliveryCenterNo" label="配送中心" min-width="100">
        <template #default="{ row }">{{ row.deliveryCenterName || row.deliveryCenterNo }}</template>
      </el-table-column>
      <el-table-column prop="distributionReturnDate" label="退货日期" width="110" />
      <el-table-column prop="erpNo" label="ERP单号" min-width="120" />
      <!-- add by codebuddy 20260817: 来源配送收货单号 -->
      <el-table-column prop="sourceDistributionNo" label="来源单号" min-width="140" />
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑配送退货' : '新增配送退货'" width="920px" top="3vh" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="退货单号">
            <el-input v-model="form.distributionReturnNo" :disabled="!!form.id" placeholder="留空自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="退货日期" prop="distributionReturnDate">
            <el-date-picker v-model="form.distributionReturnDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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
        <!-- add by codebuddy 20260817: 配送中心按门店商品配置表自动带出，只读展示 -->
        <el-col :span="8">
          <el-form-item label="配送中心">
            <el-input :model-value="form.deliveryCenterName || form.deliveryCenterNo" disabled />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">商品明细</el-divider>
    <!-- add by codebuddy 20260817: 新增时选择来源配送收货单，自动带出明细 -->
    <div style="margin-bottom: 10px">
      <el-button v-if="!form.id" type="primary" plain size="small" @click="openSourceDialog">选择来源配送收货单</el-button>
      <el-tag v-if="form.sourceDistributionNo" type="info" style="margin-left: 8px">来源单：{{ form.sourceDistributionNo }}</el-tag>
    </div>
    <GoodsDetailTable
      :rows="details"
      :goods-options="goodsOptions"
      qty-label="退数量"
      show-receipt-qty
      @add="details.push({ receiptQty: 0, qty: 0, unit: '', price: 0 })"
      @remove="(i: number) => details.splice(i, 1)"
    />

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="viewVisible" title="配送退货明细" width="860px">
    <el-descriptions :column="3" border size="small">
      <el-descriptions-item label="退货单号">{{ viewData.distributionReturnNo }}</el-descriptions-item>
      <el-descriptions-item label="门店">{{ viewData.siteName }}</el-descriptions-item>
      <!-- add by codebuddy 20260817: 配送中心展示 -->
      <el-descriptions-item label="配送中心">{{ viewData.deliveryCenterName || viewData.deliveryCenterNo }}</el-descriptions-item>
      <el-descriptions-item label="退货日期">{{ viewData.distributionReturnDate }}</el-descriptions-item>
      <el-descriptions-item label="ERP单号">{{ viewData.erpNo }}</el-descriptions-item>
      <!-- add by codebuddy 20260817: 来源配送收货单号 -->
      <el-descriptions-item label="来源单号">{{ viewData.sourceDistributionNo }}</el-descriptions-item>
      <el-descriptions-item label="创建人">{{ viewData.creator }}</el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">商品明细</el-divider>
    <el-table :data="viewDetails" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="itemNo" label="商品编号" min-width="110" />
      <el-table-column prop="itemName" label="商品名称" min-width="160" />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column prop="receiptQty" label="收数量" width="90" align="right" />
      <el-table-column prop="qty" label="退数量" width="90" align="right" />
      <el-table-column prop="price" label="单价" width="100" align="right" />
      <el-table-column prop="amount" label="金额" width="100" align="right" />
    </el-table>
  </el-dialog>

  <!-- add by codebuddy 20260817: 来源配送收货单选择对话框 -->
  <el-dialog v-model="sourceDialogVisible" title="选择来源配送收货单" width="760px">
    <el-table :data="sourceRows" v-loading="sourceLoading" border stripe highlight-current-row @current-change="(row: Distribution | null) => (selectedSource = row)">
      <el-table-column prop="distributionNo" label="配送收货单号" min-width="150" />
      <el-table-column prop="distributionDate" label="收货日期" width="110" />
      <el-table-column label="配送中心" min-width="100">
        <template #default="{ row }">{{ row.deliveryCenterName || row.deliveryCenterNo }}</template>
      </el-table-column>
      <el-table-column prop="erpNo" label="ERP单号" min-width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'TJ' ? 'success' : row.status === 'Y' ? 'warning' : 'info'">
            {{ row.status === 'TJ' ? '已提交' : row.status === 'Y' ? '已确认' : '已上传' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="sourceDialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedSource" @click="applySource">选用此单</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { distributionReturnApi, goodsApi } from '../api'
import type { Distribution, DistributionReturn, DistributionReturnDetail, Goods } from '../types'
import { currentShopId, currentShopNo } from '../stores/globalShop'
import GoodsDetailTable, { type GoodsRow } from '../components/GoodsDetailTable.vue'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<DistributionReturn[]>([])
const total = ref(0)
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; siteId?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const viewVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<DistributionReturn>({})
const details = ref<GoodsRow[]>([])
const goodsOptions = ref<Goods[]>([])
const viewData = reactive<DistributionReturn>({})
const viewDetails = ref<DistributionReturnDetail[]>([])
// add by codebuddy 20260817: 来源配送收货单选择
const sourceDialogVisible = ref(false)
const sourceLoading = ref(false)
const sourceRows = ref<Distribution[]>([])
const selectedSource = ref<Distribution | null>(null)
const rules: FormRules = {
  distributionReturnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }],
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
    const res: any = await distributionReturnApi.page({ ...query })
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

async function openDialog(row?: DistributionReturn) {
  await initOptions()
  if (!row && !currentShopId.value) {
    ElMessage.warning('请先在右上角选择门店')
    return
  }
  dialogVisible.value = true
  details.value = []
  if (row) {
    const res: any = await distributionReturnApi.get(row.id!)
    Object.assign(form, res.data.header)
    details.value = (res.data.details || []).map((d: DistributionReturnDetail) => ({ ...d }))
  } else {
    // add by codebuddy 20260817: 新增时自动带入全局门店
    Object.assign(form, {
      id: undefined, distributionReturnNo: '', distributionReturnDate: '', erpNo: '',
      siteId: currentShopId.value, creator: currentShopNo(),
      // add by codebuddy 20260817: 新增时清空来源配送收货单
      sourceDistributionId: undefined, sourceDistributionNo: ''
    })
  }
}

async function view(row: DistributionReturn) {
  const res: any = await distributionReturnApi.get(row.id!)
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
      await distributionReturnApi.update(dto as any)
      ElMessage.success('修改成功')
    } else {
      await distributionReturnApi.save(dto as any)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: DistributionReturn) {
  await ElMessageBox.confirm(`确认删除退货单「${row.distributionReturnNo}」？`, '提示', { type: 'warning' })
  await distributionReturnApi.remove(row.id!)
  ElMessage.success('删除成功')
  load()
}

// add by codebuddy 20260818: 确认配送退货单（状态置为Y）
async function confirmDoc(row: DistributionReturn) {
  try {
    await ElMessageBox.confirm(`确认配送退货单「${row.distributionReturnNo}」？`, '提示', { type: 'warning' })
    await distributionReturnApi.confirm(row.id!)
    ElMessage.success('确认成功')
    load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '确认失败，请重试')
  }
}

// add by codebuddy 20260818: 提交配送退货单（状态置为TJ）
async function submitDoc(row: DistributionReturn) {
  try {
    await ElMessageBox.confirm(`提交配送退货单「${row.distributionReturnNo}」？`, '提示', { type: 'warning' })
    await distributionReturnApi.submit(row.id!)
    ElMessage.success('提交成功')
    load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '提交失败，请重试')
  }
}

// add by codebuddy 20260817: 打开来源配送收货单选择对话框
async function openSourceDialog() {
  selectedSource.value = null
  sourceDialogVisible.value = true
  sourceLoading.value = true
  try {
    const res: any = await distributionReturnApi.listSourceDistributions(query.siteId)
    sourceRows.value = res.data || []
    if (!sourceRows.value.length) {
      ElMessage.info('当前门店没有可退货的配送收货单')
    }
  } finally {
    sourceLoading.value = false
  }
}

// add by codebuddy 20260817: 选用来源配送收货单并带出明细
async function applySource() {
  const src = selectedSource.value
  if (!src) return
  const res: any = await distributionReturnApi.getSourceDistribution(src.id!)
  const h = res.data.header
  details.value = (res.data.details || []).map((d: any) => ({
    itemId: d.itemId,
    itemNo: d.itemNo,
    itemName: d.itemName,
    unit: d.unit,
    price: d.price,
    receiptQty: d.qty,
    qty: 0,
    amount: 0
  }))
  form.sourceDistributionId = h.id
  form.sourceDistributionNo = h.distributionNo
  // add by codebuddy 20260817: 来源单的配送中心优先带出
  if (!form.deliveryCenterId && h.deliveryCenterId) {
    form.deliveryCenterId = h.deliveryCenterId
  }
  sourceDialogVisible.value = false
  ElMessage.success('已带出来源配送收货单明细，请填写退数量')
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
