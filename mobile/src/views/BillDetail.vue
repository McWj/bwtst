<template>
  <div class="bill-detail">
    <van-nav-bar :title="meta?.title || '单据详情'" left-arrow fixed placeholder @click-left="router.back()" />

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="header">
      <van-cell-group inset title="基本信息">
        <van-cell title="单号" :value="billNo" />
        <van-cell title="状态">
          <template #value>
            <van-tag :type="statusType(header.status)" plain>{{ statusText(header.status) }}</van-tag>
          </template>
        </van-cell>
        <van-cell v-for="f in headFields" :key="f.key" :title="f.label" :value="header[f.key] || '-'" />
      </van-cell-group>

      <van-cell-group inset title="商品明细" class="goods-group">
        <div v-if="details.length === 0" class="empty">暂无明细</div>
        <div v-for="(d, i) in details" :key="i" class="goods-item">
          <div class="goods-head">
            <span class="goods-name">{{ d.itemNo }} · {{ d.itemName }}</span>
          </div>
          <div class="goods-fields">
            <div class="goods-field"><span class="goods-label">单位</span><span>{{ d.unit || '-' }}</span></div>
            <div class="goods-field"><span class="goods-label">数量</span><span>{{ d.qty }}</span></div>
            <div class="goods-field"><span class="goods-label">单价</span><span>{{ d.price }}</span></div>
            <div class="goods-field"><span class="goods-label">金额</span><span>{{ d.amount }}</span></div>
          </div>
        </div>
      </van-cell-group>

      <div class="action-wrap">
        <van-button round block type="primary" :loading="confirming" @click="doConfirm">确认</van-button>
        <van-button round block type="success" :loading="submitting" @click="doSubmit">提交 / 上传</van-button>
      </div>
    </template>

    <van-empty v-else description="未找到单据" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import {
  requisitionApi, distributionApi, purchaseApi, distributionReturnApi, purchaseReturnApi,
  deliveryShipmentApi, purchaseInboundApi, warehouseReturnApi
} from '../api'
import { requisitionCreditDeduct, type RequisitionErpParams } from '../api/erp'
import { currentShopNo } from '../stores/globalShop'
import { BILL_META, statusText as statusTextFn, statusType as statusTypeFn, type BillType } from '../config/bills'

// add by codebuddy 20260821: 重建单据详情页；确认不调用 ERP，提交(要货)时调用 ERP 扣减
const ERP_URL = (import.meta.env.VITE_SERVER || '') + '/wtoptst/ws/r/awsp920'

const route = useRoute()
const router = useRouter()
const type = route.params.type as BillType
const id = Number(route.params.id)
const meta = computed(() => BILL_META[type])

const header = ref<any>(null)
const details = ref<any[]>([])
const loading = ref(false)
const confirming = ref(false)
const submitting = ref(false)

function statusText(status: string) {
  return statusTextFn(type, status)
}
function statusType(status: string) {
  return statusTypeFn(status)
}

const billNo = computed(() => {
  const h = header.value
  if (!h) return ''
  return h.requisitionNo || h.distributionNo || h.purchaseNo || h.distributionReturnNo ||
    h.purchaseReturnNo || h.shipmentNo || h.erpShipmentNo || h.returnNo || ''
})

const headFields = computed(() => {
  const h = header.value
  if (!h) return []
  const map: Record<string, string[]> = {
    requisition: ['requisitionDate', 'demandDate', 'siteName', 'creator'],
    distribution: ['distributionDate', 'siteName', 'creator'],
    purchase: ['purchaseDate', 'siteName', 'supplierName'],
    distributionReturn: ['distributionReturnDate', 'siteName'],
    purchaseReturn: ['purchaseReturnDate', 'siteName', 'supplierName'],
    deliveryShipment: ['shipmentDate', 'deliveryCenterName', 'erpShipmentNo'],
    purchaseInbound: ['purchaseDate', 'supplierName', 'erpNo'],
    warehouseReturn: ['returnDate', 'supplierName']
  }
  const keys = map[type] || []
  const labelMap: Record<string, string> = {
    requisitionDate: '要货日期', demandDate: '需求日期', siteName: '门店', creator: '创建人',
    distributionDate: '配送日期', purchaseDate: '采购日期', supplierName: '供应商',
    distributionReturnDate: '退货日期', purchaseReturnDate: '退货日期',
    deliveryCenterName: '配送中心', erpShipmentNo: 'ERP 出货单号', erpNo: 'ERP 单号', returnDate: '退仓日期'
  }
  return keys
    .filter((k) => h[k] !== undefined && h[k] !== null && h[k] !== '')
    .map((k) => ({ key: k, label: labelMap[k] || k }))
})

function pickApi() {
  switch (type) {
    case 'requisition': return requisitionApi
    case 'distribution': return distributionApi
    case 'purchase': return purchaseApi
    case 'distributionReturn': return distributionReturnApi
    case 'purchaseReturn': return purchaseReturnApi
    case 'deliveryShipment': return deliveryShipmentApi
    case 'purchaseInbound': return purchaseInboundApi
    case 'warehouseReturn': return warehouseReturnApi
    default: return null
  }
}

function toYyyyMmDd(date?: string) {
  return date ? date.replace(/-/g, '') : ''
}

// add by codebuddy 20260821: 提交要货单时调用 ERP 扣减（callType=0/status=2）
async function callRequisitionDeduct(h: any, ds: any[]) {
  const shopNo = currentShopNo() || h.creator || ''
  if (!shopNo) throw new Error('无法获取门店编码')
  let ptemplateNo = ''
  if (h.templateNo) {
    try {
      const tRes: any = await requisitionApi.get(h.id ?? id)
      ptemplateNo = tRes.data?.header?.ptemplateNo ?? ''
    } catch {
      ptemplateNo = ''
    }
  }
  const now = new Date()
  const today = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const creator = h.creator || shopNo
  const requisition: RequisitionErpParams = {
    version: '3.0', creator, create_datetime: today, modify_no: creator, modify_datetime: today,
    approve_no: creator, approve_datetime: today, status: '2', site_no: shopNo,
    requisition_date: toYyyyMmDd(h.requisitionDate) || today, create_date: today,
    po_template_no: ptemplateNo, is_add_po: h.isAddPo === 'Y' ? 'Y' : 'N',
    is_urgent_order: h.isUrgentOrder === 'Y' ? 'Y' : 'N', remark: '', front_no: h.requisitionNo || '',
    callType: '0', oType: '0',
    requisition_detail: ds.filter((d) => Number(d.qty) > 0).map((d, idx) => {
      const price = Number(d.price) || 0
      const qty = Number(d.qty) || 0
      const amount = price * qty
      return {
        seq: idx + 1, item_no: d.itemNo || '', base_unit: d.unit || '', base_qty: qty, price, amount,
        packing_unit: d.unit || '', packing_qty: qty, distri_price: price, distri_amount: amount
      }
    })
  }
  return requisitionCreditDeduct({ url: ERP_URL, shopNo, requisition })
}

async function load() {
  const api: any = pickApi()
  if (!api) {
    showToast('未知单据类型')
    return
  }
  loading.value = true
  try {
    const res: any = await api.get(id)
    header.value = res.data?.header ?? null
    details.value = res.data?.details ?? []
  } catch {
    showToast('加载单据失败')
  } finally {
    loading.value = false
  }
}

// add by codebuddy 20260821: 确认不再调用 ERP，仅调用后端 confirm
async function doConfirm() {
  const api: any = pickApi()
  if (!api) return
  confirming.value = true
  try {
    await api.confirm(id)
    showToast('确认成功')
    await load()
  } catch (e: any) {
    showToast('确认失败：' + (e?.message || '未知错误'))
  } finally {
    confirming.value = false
  }
}

async function doSubmit() {
  const api: any = pickApi()
  if (!api) return
  try {
    await showConfirmDialog({ title: '提示', message: '确定要提交/上传该单据吗？' })
  } catch {
    return
  }
  submitting.value = true
  try {
    // add by codebuddy 20260821: 要货单提交时调用 ERP 扣减（callType=0/status=2），失败不允许提交
    if (type === 'requisition') {
      await callRequisitionDeduct(header.value || {}, details.value)
    }
    await api.submit(id)
    showToast('提交成功')
    await load()
  } catch (e: any) {
    showToast('提交失败：' + (e?.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.bill-detail {
  padding-bottom: 20px;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #969799;
}
.goods-group {
  margin-bottom: 0;
}
.empty {
  padding: 24px;
  text-align: center;
  color: #c8c9cc;
}
.goods-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f2f3f5;
}
.goods-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.goods-name {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
}
.goods-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  margin-top: 10px;
}
.goods-field {
  font-size: 13px;
  color: #646566;
}
.goods-label {
  color: #969799;
  margin-right: 8px;
}
.action-wrap {
  margin: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
