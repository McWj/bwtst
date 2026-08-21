<template>
  <div class="home">
    <van-nav-bar :title="navTitle" fixed placeholder>
      <template #right>
        <span class="view-switch" @click="switchView">{{ viewLabel }}<van-icon name="arrow-down" /></span>
      </template>
    </van-nav-bar>

    <div class="home-body">
      <!-- 视角切换面板 -->
      <van-popup v-model:show="viewPopupShow" position="bottom" round>
        <div class="view-popup">
          <div
            v-for="v in availableViews"
            :key="v.value"
            class="view-item"
            :class="{ active: currentView === v.value }"
            @click="pickView(v.value)"
          >
            {{ v.label }}
            <van-icon v-if="currentView === v.value" name="success" color="#1989fa" />
          </div>
        </div>
      </van-popup>

      <!-- 数据总览卡片 -->
      <div class="section-title">
        <span>{{ currentView === 'dc' ? '配送中心数据' : '门店数据' }}</span>
      </div>

      <div class="bill-grid">
        <div
          v-for="meta in currentBillMetas"
          :key="meta.type"
          class="bill-card"
          @click="goList(meta.type)"
        >
          <div class="bill-card-title">{{ meta.title }}</div>
          <div class="bill-card-en">{{ meta.en }}</div>
          <div class="bill-card-stats">
            <div class="stat">
              <span class="stat-num">{{ stats[meta.type]?.total ?? 0 }}</span>
              <span class="stat-label">总数</span>
            </div>
            <div class="stat warn">
              <span class="stat-num">{{ stats[meta.type]?.unConfirmed ?? 0 }}</span>
              <span class="stat-label">待处理</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { dashboardApi, shopApi, distributionCenterApi } from '../api'
import { currentView, currentDeliveryCenterId, deliveryCenterOptions } from '../stores/globalDc'
import { currentShopId, shopOptions } from '../stores/globalShop'
import { currentUser } from '../stores/auth'
import { BILL_META, SHOP_BILL_TYPES, DC_BILL_TYPES, type BillType, type BillMeta } from '../config/bills'

const router = useRouter()
const viewPopupShow = ref(false)
const stats = ref<Record<string, { total: number; unConfirmed: number }>>({})

const navTitle = computed(() => (currentView.value === 'dc' ? '配送中心工作台' : currentView.value === 'shop' ? '门店工作台' : '总部工作台'))
const viewLabel = computed(() => (currentView.value === 'hq' ? '总部' : currentView.value === 'dc' ? '配送中心' : '门店'))

const roles = computed(() => currentUser.value?.roles ?? [])
const availableViews = computed(() => {
  const arr: { value: 'hq' | 'shop' | 'dc'; label: string }[] = []
  if (roles.value.includes('hq')) arr.push({ value: 'hq', label: '总部视角' })
  if (roles.value.includes('shop')) arr.push({ value: 'shop', label: '门店视角' })
  if (roles.value.includes('dc')) arr.push({ value: 'dc', label: '配送中心视角' })
  return arr
})

const currentBillMetas = computed<BillMeta[]>(() => {
  const types: BillType[] = currentView.value === 'dc' ? DC_BILL_TYPES : SHOP_BILL_TYPES
  return types.map((t) => BILL_META[t])
})

function switchView() {
  if (availableViews.value.length <= 1) return
  viewPopupShow.value = true
}

async function pickView(v: 'hq' | 'shop' | 'dc') {
  viewPopupShow.value = false
  if (currentView.value === v) return
  currentView.value = v
  if (v === 'shop') {
    const shopId = currentUser.value?.defaultShopId ?? currentUser.value?.shopIds?.[0]
    if (shopId) currentShopId.value = shopId
  } else if (v === 'dc') {
    const dcId = currentUser.value?.defaultDcId ?? currentUser.value?.dcIds?.[0]
    if (dcId) currentDeliveryCenterId.value = dcId
  }
  await loadOverview()
}

function goList(type: BillType) {
  router.push(`/bills/${type}`)
}

async function loadOverview() {
  try {
    const params: any = { view: currentView.value }
    if (currentView.value === 'shop' && currentShopId.value) params.shopId = currentShopId.value
    if (currentView.value === 'dc' && currentDeliveryCenterId.value) params.dcId = currentDeliveryCenterId.value
    const res: any = await dashboardApi.overview(params)
    const data = res.data
    if (currentView.value === 'dc' && data.dcData) {
      stats.value = {
        deliveryShipment: { total: data.dcData.deliveryShipment.total, unConfirmed: data.dcData.deliveryShipment.unConfirmed },
        purchaseInbound: { total: data.dcData.purchaseInbound.total, unConfirmed: data.dcData.purchaseInbound.unConfirmed },
        warehouseReturn: { total: data.dcData.warehouseReturn.total, unConfirmed: data.dcData.warehouseReturn.unConfirmed }
      }
    } else if (data.storeData) {
      stats.value = {
        requisition: { total: data.storeData.requisition.total, unConfirmed: data.storeData.requisition.unConfirmed },
        distribution: { total: data.storeData.distribution.total, unConfirmed: data.storeData.distribution.unConfirmed },
        purchase: { total: data.storeData.purchase.total, unConfirmed: data.storeData.purchase.unConfirmed },
        distributionReturn: { total: data.storeData.distributionReturn.total, unConfirmed: data.storeData.distributionReturn.unConfirmed },
        purchaseReturn: { total: data.storeData.purchaseReturn.total, unConfirmed: data.storeData.purchaseReturn.unConfirmed }
      }
    }
  } catch {
    showToast('加载数据失败')
  }
}

onMounted(async () => {
  try {
    if (shopOptions.value.length === 0) {
      const r: any = await shopApi.list()
      shopOptions.value = r.data ?? []
    }
    if (deliveryCenterOptions.value.length === 0) {
      const r2: any = await distributionCenterApi.list()
      deliveryCenterOptions.value = r2.data ?? []
    }
  } catch {
    // 忽略下拉数据加载失败
  }
  await loadOverview()
})
</script>

<style scoped>
.home-body {
  padding: 0 12px 20px;
}
.view-switch {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 14px;
  color: #1989fa;
}
.view-popup {
  padding: 12px 0;
}
.view-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  font-size: 16px;
}
.view-item.active {
  color: #1989fa;
  font-weight: 600;
}
.section-title {
  margin: 14px 4px 10px;
  font-size: 14px;
  color: #969799;
}
.bill-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.bill-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.15s ease;
}
.bill-card:active {
  transform: scale(0.97);
}
.bill-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #323233;
}
.bill-card-en {
  margin-top: 2px;
  font-size: 10px;
  color: #c8c9cc;
  letter-spacing: 1px;
}
.bill-card-stats {
  display: flex;
  gap: 24px;
  margin-top: 14px;
}
.stat {
  display: flex;
  flex-direction: column;
}
.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #1989fa;
}
.stat.warn .stat-num {
  color: #ee0a24;
}
.stat-label {
  margin-top: 2px;
  font-size: 12px;
  color: #969799;
}
</style>
