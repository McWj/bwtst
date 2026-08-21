<template>
  <div class="bill-list">
    <van-nav-bar :title="meta?.title || '单据'" left-arrow fixed placeholder @click-left="router.back()" />

    <van-search v-model="keyword" placeholder="输入单号搜索" @search="onSearch" @clear="onSearch" />

    <van-tabs v-model:active="activeStatus" @change="onSearch" shrink>
      <van-tab v-for="s in statusTabs" :key="s.value" :title="s.label" :name="s.value" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-for="row in rows" :key="row.id" class="bill-row" @click="goDetail(row.id)">
          <div class="bill-row-top">
            <span class="bill-no">{{ row.no }}</span>
            <van-tag :type="statusType(row.status)" plain>{{ row.statusText }}</van-tag>
          </div>
          <div class="bill-row-sub">
            <span>{{ row.sub }}</span>
            <span>{{ row.date }}</span>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>

    <!-- 门店视角：要货申请右下角新增按钮 -->
    <van-button
      v-if="isRequisition && currentView === 'shop'"
      class="fab"
      type="primary"
      round
      icon="plus"
      @click="router.push('/requisition/edit')"
    >
      新增要货
    </van-button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  requisitionApi, distributionApi, purchaseApi, distributionReturnApi, purchaseReturnApi,
  deliveryShipmentApi, purchaseInboundApi, warehouseReturnApi
} from '../api'
import { currentView, currentDeliveryCenterId } from '../stores/globalDc'
import { currentShopId } from '../stores/globalShop'
import { BILL_META, statusText, statusType, type BillType } from '../config/bills'

const route = useRoute()
const router = useRouter()
const type = route.params.type as BillType
const meta = computed(() => BILL_META[type])

const keyword = ref('')
const activeStatus = ref('all')
const rows = ref<any[]>([])
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const isRequisition = computed(() => type === 'requisition')

const statusTabs = computed(() => {
  const m = meta.value?.statusMap ?? {}
  const tabs: { value: string; label: string }[] = [{ value: 'all', label: '全部' }]
  Object.entries(m).forEach(([k, v]) => tabs.push({ value: k, label: v }))
  return tabs
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

function normalizeRow(h: any) {
  let no = ''
  let sub = ''
  let date = ''
  let status = h.status
  if (type === 'requisition') {
    no = h.requisitionNo ?? ''
    sub = h.siteName ?? ''
    date = h.requisitionDate ?? ''
  } else if (type === 'distribution') {
    no = h.distributionNo ?? ''
    sub = h.siteName ?? ''
    date = h.distributionDate ?? ''
  } else if (type === 'purchase') {
    no = h.purchaseNo ?? ''
    sub = h.siteName ?? ''
    date = h.purchaseDate ?? ''
  } else if (type === 'distributionReturn') {
    no = h.distributionReturnNo ?? ''
    sub = h.siteName ?? ''
    date = h.distributionReturnDate ?? ''
  } else if (type === 'purchaseReturn') {
    no = h.purchaseReturnNo ?? ''
    sub = h.siteName ?? ''
    date = h.purchaseReturnDate ?? ''
  } else if (type === 'deliveryShipment') {
    no = h.shipmentNo ?? h.erpShipmentNo ?? ''
    sub = h.deliveryCenterName ?? ''
    date = h.shipmentDate ?? ''
  } else if (type === 'purchaseInbound') {
    no = h.purchaseNo ?? ''
    sub = h.supplierName ?? ''
    date = h.purchaseDate ?? ''
  } else if (type === 'warehouseReturn') {
    no = h.returnNo ?? ''
    sub = h.supplierName ?? ''
    date = h.returnDate ?? ''
  }
  return { id: h.id, no, sub, date, status, statusText: statusText(type, status) }
}

async function load(reset = false) {
  if (reset) {
    pageNum.value = 1
    finished.value = false
  }
  const api: any = pickApi()
  if (!api) return
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize }
    if (keyword.value) params.keyword = keyword.value
    if (currentView.value === 'shop' && currentShopId.value) params.siteId = currentShopId.value
    if (currentView.value === 'dc' && currentDeliveryCenterId.value) params.deliveryCenterId = currentDeliveryCenterId.value
    const res: any = await api.page(params)
    const list = res.data?.records ?? []
    let mapped = list.map(normalizeRow)
    if (activeStatus.value !== 'all') {
      mapped = mapped.filter((r: any) => r.status === activeStatus.value)
    }
    if (reset) rows.value = mapped
    else rows.value = rows.value.concat(mapped)
    const total = res.data?.total ?? 0
    if (rows.value.length >= total || list.length === 0) finished.value = true
    else pageNum.value += 1
  } catch {
    finished.value = true
    showToast('加载失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onLoad() {
  load(false)
}

function onRefresh() {
  load(true)
}

function onSearch() {
  load(true)
}

function goDetail(id: number) {
  router.push(`/bill/${type}/${id}`)
}
</script>

<style scoped>
.bill-row {
  background: #fff;
  margin: 10px 12px;
  padding: 12px 14px;
  border-radius: 10px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
}
.bill-row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.bill-no {
  font-size: 15px;
  font-weight: 600;
  color: #323233;
}
.bill-row-sub {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: #969799;
}
.fab {
  position: fixed;
  right: 20px;
  bottom: 80px;
  box-shadow: 0 4px 12px rgba(25, 137, 250, 0.4);
}
</style>
