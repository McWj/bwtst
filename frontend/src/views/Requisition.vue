<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="单号/创建人" clearable style="width: 200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <!-- add by codebuddy 20260817: 右上角统一门店维度，未选择门店时禁止新增 -->
      <el-tooltip :disabled="!!currentShopId" content="请先在右上角选择门店" placement="top">
        <span>
          <el-button type="success" :disabled="!currentShopId" @click="openDialog()">新增要货申请</el-button>
        </span>
      </el-tooltip>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="requisitionNo" label="要货单号" min-width="160" />
      <el-table-column prop="siteName" label="门店" min-width="130" />
      <!-- add by codebuddy 20260817: 配送中心列（按门店商品配置表带出） -->
      <el-table-column prop="deliveryCenterNo" label="配送中心" min-width="100">
        <template #default="{ row }">{{ row.deliveryCenterName || '-' }}({{ row.deliveryCenterNo || '-' }})</template>
      </el-table-column>
      <el-table-column prop="requisitionDate" label="要货日期" width="110" />
      <el-table-column prop="demandDate" label="需求日期" width="110" />
      <el-table-column prop="creator" label="创建人" width="90" />
      <el-table-column label="紧急" width="80">
        <template #default="{ row }">{{ row.isUrgentOrder === 'Y' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="distributionFee" label="预估运费" width="100" align="right" />
      <!-- add by codebuddy 20260817: 状态列：空-未确认 Y-已确认 TJ-已提交 SC-已上传 -->
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SC' ? 'primary' : row.status === 'TJ' ? 'success' : row.status === 'Y' ? 'warning' : 'info'">
            {{ row.status === 'SC' ? '已上传' : row.status === 'TJ' ? '已提交' : row.status === 'Y' ? '已确认' : '未确认' }}
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑要货申请' : '新增要货申请'" width="920px" top="3vh" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <!-- add by codebuddy 20260817: 要货单号不录入，新增时留空，保存后由后端生成；编辑时只读展示 -->
        <el-col v-if="form.id" :span="8">
          <el-form-item label="要货单号">
            <el-input v-model="form.requisitionNo" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="要货日期" prop="requisitionDate">
            <el-date-picker v-model="form.requisitionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="需求日期" prop="demandDate">
            <el-date-picker v-model="form.demandDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="要货模板">
            <!-- add by codebuddy 20260817: 只能选择适用该门店的模板 -->
            <el-select
              v-model="form.templateNo"
              placeholder="选择模板"
              clearable
              filterable
              :disabled="!currentShopId"
              style="width: 100%"
              @change="onTemplateChange"
            >
              <el-option v-for="t in templateOptions" :key="t.id" :label="t.ptemplateName" :value="t.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="创建人">
            <el-input v-model="form.creator" placeholder="创建人" />
          </el-form-item>
        </el-col>
        <!-- add by codebuddy 20260817: 配送中心由门店商品配置表自动带出，只读展示 -->
        <el-col :span="8">
          <el-form-item label="配送中心">
            <el-input :model-value="form.deliveryCenterName || form.deliveryCenterNo" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="追加要货">
            <el-radio-group v-model="form.isAddPo">
              <el-radio value="Y">是</el-radio>
              <el-radio value="N">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="紧急要货">
            <el-radio-group v-model="form.isUrgentOrder">
              <el-radio value="Y">是</el-radio>
              <el-radio value="N">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <!-- add by codebuddy 20260817: 运费/重量/体积无需录入，确认后由 ERP 接口返回 -->
      </el-row>
    </el-form>

    <!-- add by codebuddy 20260817: 将保存/取消按钮放到明细列表上方 -->
    <div class="detail-toolbar">
      <el-divider content-position="left">商品明细</el-divider>
      <div class="detail-actions">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </div>
    </div>
    <GoodsDetailTable
      :rows="details"
      :goods-options="goodsOptions"
      qty-label="要货数量"
      @add="details.push({ qty: 0, unit: '', price: 0 })"
      @remove="(i: number) => details.splice(i, 1)"
    />
  </el-dialog>

  <el-dialog v-model="viewVisible" title="要货申请明细" width="860px">
    <el-descriptions :column="3" border size="small">
      <el-descriptions-item label="要货单号">{{ viewData.requisitionNo }}</el-descriptions-item>
      <el-descriptions-item label="门店">{{ viewData.siteName }}</el-descriptions-item>
      <!-- add by codebuddy 20260817: 配送中心展示 -->
      <el-descriptions-item label="配送中心">{{ viewData.deliveryCenterName || viewData.deliveryCenterNo }}</el-descriptions-item>
      <el-descriptions-item label="要货日期">{{ viewData.requisitionDate }}</el-descriptions-item>
      <el-descriptions-item label="需求日期">{{ viewData.demandDate }}</el-descriptions-item>
      <el-descriptions-item label="创建人">{{ viewData.creator }}</el-descriptions-item>
      <el-descriptions-item label="紧急要货">{{ viewData.isUrgentOrder === 'Y' ? '是' : '否' }}</el-descriptions-item>
      <!-- add by codebuddy 20260817: 展示确认后 ERP 返回的预估运费/重量/体积 -->
      <el-descriptions-item label="预估运费">{{ viewData.distributionFee }}</el-descriptions-item>
      <el-descriptions-item label="预估重量">{{ viewData.weight }} {{ viewData.weightUnit }}</el-descriptions-item>
      <el-descriptions-item label="预估体积">{{ viewData.volume }} {{ viewData.volumeUnit }}</el-descriptions-item>
      <!-- add by codebuddy 20260817: 展示 ERP 单号（多张用 | 分隔） -->
      <el-descriptions-item v-if="viewData.erpNo" label="ERP单号">{{ viewData.erpNo }}</el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">商品明细</el-divider>
    <el-table :data="viewDetails" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="itemNo" label="商品编号" min-width="110" />
      <el-table-column prop="itemName" label="商品名称" min-width="160" />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column prop="qty" label="数量" width="90" align="right" />
      <el-table-column prop="price" label="单价" width="100" align="right" />
      <el-table-column prop="amount" label="金额" width="100" align="right" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { goodsApi, ptemplateApi, requisitionApi } from '../api'
import type { Goods, Ptemplate, PtemplateGoods, Requisition, RequisitionDetail } from '../types'
import { currentShopId, currentShopNo, shopOptions } from '../stores/globalShop'
import { requisitionCreditDeduct, type RequisitionErpParams } from '../api/erp'
import GoodsDetailTable, { type GoodsRow } from '../components/GoodsDetailTable.vue'

// add by codebuddy 20260817: 要货单确认/提交调用的 ERP 地址（与查询额度共用）
const ERP_URL = '/wtoptst/ws/r/awsp920'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<Requisition[]>([])
const total = ref(0)
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; siteId?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const viewVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Requisition>({})
const details = ref<GoodsRow[]>([])
const goodsOptions = ref<Goods[]>([])
const templateOptions = ref<Ptemplate[]>([])
const viewData = reactive<Requisition>({})
const viewDetails = ref<RequisitionDetail[]>([])
const rules: FormRules = {
  requisitionDate: [{ required: true, message: '请选择要货日期', trigger: 'change' }],
  demandDate: [{ required: true, message: '请选择需求日期', trigger: 'change' }],
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
    const res: any = await requisitionApi.page({ ...query })
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

async function openDialog(row?: Requisition) {
  await initOptions()
  if (!row && !currentShopId.value) {
    ElMessage.warning('请先在右上角选择门店')
    return
  }
  dialogVisible.value = true
  details.value = []
  if (row) {
    const res: any = await requisitionApi.get(row.id!)
    Object.assign(form, res.data.header)
    details.value = (res.data.details || []).map((d: RequisitionDetail) => ({ ...d }))
  } else {
    // add by codebuddy 20260817: 新增时自动带入全局门店，要货日期和需求日期默认当天
    const today = new Date().toISOString().split('T')[0]
    Object.assign(form, {
      id: undefined, requisitionNo: '', requisitionDate: today, demandDate: today,
      siteId: currentShopId.value, templateNo: undefined, creator: currentShopNo(), isAddPo: 'N', isUrgentOrder: 'N'
    })
  }
  // add by codebuddy 20260817: 只加载适用当前门店的模板
  await loadTemplates(currentShopId.value, !!row)
}

// add by codebuddy 20260817: 按门店加载适用模板
async function loadTemplates(shopId?: number, keepCurrent = false) {
  templateOptions.value = []
  if (!keepCurrent) {
    form.templateNo = undefined
  }
  if (!shopId) {
    return
  }
  const res: any = await ptemplateApi.listByShop(shopId)
  templateOptions.value = res.data
}

// add by codebuddy 20260817: 选择要货模板后自动带出模板商品到商品明细
function onTemplateChange() {
  loadTemplateGoods(form.templateNo)
}

async function loadTemplateGoods(templateId?: number) {
  if (!templateId) {
    return
  }
  const res: any = await ptemplateApi.get(templateId)
  const goodsList: PtemplateGoods[] = res.data.goodsList || []
  details.value = goodsList.map((g: PtemplateGoods) => ({
    itemId: g.itemId,
    itemNo: g.itemNo,
    itemName: g.itemName,
    unit: g.unit,
    qty: Number(g.qty) || 0
  }))
}

async function view(row: Requisition) {
  const res: any = await requisitionApi.get(row.id!)
  Object.assign(viewData, res.data.header)
  viewDetails.value = res.data.details || []
  viewVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  // add by codebuddy 20260817: 过滤数量为0的明细，单价为空默认0
  const validDetails = details.value
    .filter((d) => Number(d.qty) > 0)
    .map((d) => ({ ...d, price: Number(d.price) || 0 }))
  if (!validDetails.length) {
    ElMessage.warning('请至少添加一条数量大于0的商品明细')
    return
  }
  saving.value = true
  try {
    // add by codebuddy 20260820: 保存时调用 ERP 算预估运费/重量/体积（确认不再调用），失败不允许保存
    if (validDetails.length) {
      const stdData = await callRequisitionDeduct(form as unknown as Requisition, validDetails, '2', '0')
      const p = stdData.parameter
      form.distributionFee = Number(p?.distributionFee ?? 0)
      form.weight = Number(p?.esWeight ?? 0)
      form.weightUnit = p?.weightUnit ?? ''
      form.volume = Number(p?.esVolume ?? 0)
      form.volumeUnit = p?.VolumeUnit ?? ''
    }
    const dto = { header: { ...form }, details: validDetails }
    if (form.id) {
      await requisitionApi.update(dto as any)
      ElMessage.success('修改成功')
    } else {
      await requisitionApi.save(dto as any)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error('保存失败：' + (e?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function remove(row: Requisition) {
  await ElMessageBox.confirm(`确认删除要货单「${row.requisitionNo}」？`, '提示', { type: 'warning' })
  await requisitionApi.remove(row.id!)
  ElMessage.success('删除成功')
  load()
}

// add by codebuddy 20260817: 日期转 YYYYMMDD，如 2026-08-17 -> 20260817
function toYyyyMmDd(date?: string) {
  return date ? date.replace(/-/g, '') : ''
}

// add by codebuddy 20260817: 按门店id取门店编码
function getShopNo(siteId?: number) {
  return shopOptions.value.find((s) => s.id === siteId)?.shopNo ?? ''
}

// add by codebuddy 20260817: 构建要货单 ERP 报文并调用 requisition.credit.deduct（确认 callType=2/status=0，提交 callType=0/status=2）
async function callRequisitionDeduct(header: Requisition, detailList: RequisitionDetail[], callType: string, status: string) {
  const shopNo = getShopNo(header.siteId) || header.creator || ''
  if (!shopNo) {
    throw new Error('无法获取门店编码，请先在右上角选择门店')
  }
  let ptemplateNo = ''
  if (header.templateNo) {
    try {
      const tRes: any = await ptemplateApi.get(header.templateNo)
      ptemplateNo = tRes.data?.header?.ptemplateNo ?? ''
    } catch {
      ptemplateNo = ''
    }
  }
  const now = new Date()
  const today = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const creator = header.creator || shopNo
  const requisition: RequisitionErpParams = {
    version: '3.0',
    creator,
    create_datetime: today,
    modify_no: creator,
    modify_datetime: today,
    approve_no: creator,
    approve_datetime: today,
    status,
    site_no: shopNo,
    requisition_date: toYyyyMmDd(header.requisitionDate) || today,
    create_date: today,
    po_template_no: ptemplateNo,
    is_add_po: header.isAddPo === 'Y' ? 'Y' : 'N',
    is_urgent_order: header.isUrgentOrder === 'Y' ? 'Y' : 'N',
    remark: '',
    front_no: header.requisitionNo || '',
    callType,
    oType: '0',
    requisition_detail: detailList
      .filter((d) => Number(d.qty) > 0)
      .map((d, idx) => {
        const price = Number(d.price) || 0
        const qty = Number(d.qty) || 0
        const amount = price * qty
        return {
          seq: idx + 1,
          item_no: d.itemNo || '',
          base_unit: d.unit || '',
          base_qty: qty,
          price,
          amount,
          packing_unit: d.unit || '',
          packing_qty: qty,
          distri_price: price,
          distri_amount: amount
        }
      })
  }
  return requisitionCreditDeduct({ url: ERP_URL, shopNo, requisition })
}

// add by codebuddy 20260820: 确认要货单（体积/运费已在保存时由 ERP 计算并写入，此处不再调用 ERP）
async function confirmDoc(row: Requisition) {
  try {
    await ElMessageBox.confirm(`确认要货单「${row.requisitionNo}」？`, '提示', { type: 'warning' })
    await requisitionApi.confirm(row.id!)
    ElMessage.success('确认成功')
    load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '确认失败，请重试')
  }
}

// add by codebuddy 20260818: 提交要货单（先调 ERP callType=0/status=2 成功，再置状态为TJ；收货单/配送发货单待上传ERP成功后自动生成）
async function submitDoc(row: Requisition) {
  try {
    await ElMessageBox.confirm(`提交要货单「${row.requisitionNo}」？`, '提示', { type: 'warning' })
    const res: any = await requisitionApi.get(row.id!)
    const header: Requisition = res.data.header
    const detailList: RequisitionDetail[] = res.data.details || []
    await callRequisitionDeduct(header, detailList, '0', '2')
    await requisitionApi.submit(row.id!)
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
  // add by codebuddy 20260817: 模板改为按门店加载（loadTemplates），此处不再预加载全部模板
  templateOptions.value = []
}

// add by codebuddy 20260817: 页面加载时同步当前全局门店
onMounted(() => {
  initOptions()
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
/* add by codebuddy 20260817: 明细上方操作按钮布局 */
.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 10px 0;
}
.detail-toolbar .el-divider {
  flex: 1;
  margin: 0;
}
.detail-actions {
  display: flex;
  gap: 10px;
  margin-left: 20px;
}
</style>
