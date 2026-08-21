<template>
  <div class="dashboard-screen">
    <!-- 顶部标题栏 -->
    <div class="dash-header">
      <!-- add by codebuddy 20260819: 大标题仅在总部视角显示 -->
      <div class="header-title" v-show="currentView === 'hq'">
        <span class="title-glow">数据总览</span>
      </div>
      <div class="header-sub" v-show="currentView === 'hq'">{{ viewLabel }} · 要货 / 发货 / 收货 实时监控</div>
      <div class="header-time">{{ now }}</div>
    </div>

    <!-- 门店数据（总部/门店视角） -->
    <!-- add by codebuddy 20260818: 移除核心指标区（基础资料类指标），数据总览只展示单据情况 -->
    <div v-if="overview.storeData" class="dash-section" :class="{ 'is-loading': dataLoading }">
      <!-- add by codebuddy 20260819: 非总部视角隐藏分组标题，保留面板数据 -->
      <div class="group-title" v-show="currentView === 'hq'">
        <span class="group-icon">STORE</span>门店数据<span class="group-sub">STORE DATA</span>
      </div>
      <div class="panel-grid">
        <!-- 要货单 -->
        <div class="panel-card panel-requisition" :class="{ clickable: currentView !== 'hq' }" @click="navigateToPanel('requisition')">
          <div class="panel-title">要货单 REQUISITION</div>
          <div class="panel-body">
            <div class="panel-big">
              <span class="big-num">{{ overview.storeData?.requisition?.total ?? '--' }}</span>
              <span class="big-label">总单量</span>
            </div>
            <div class="status-list">
              <div class="status-item">
                <span class="status-dot dot-empty"></span>
                <span class="status-name">未确认</span>
                <span class="status-num">{{ overview.storeData?.requisition?.unConfirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-yellow"></span>
                <span class="status-name">已确认</span>
                <span class="status-num">{{ overview.storeData?.requisition?.confirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-blue"></span>
                <span class="status-name">已提交</span>
                <span class="status-num">{{ overview.storeData?.requisition?.submitted ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-green"></span>
                <span class="status-name">已上传</span>
                <span class="status-num">{{ overview.storeData?.requisition?.uploaded ?? '--' }}</span>
              </div>
            </div>
          </div>
          <!-- add by codebuddy 20260818: 点击可查看未上传明细 -->
          <div v-if="(overview.storeData?.requisition?.unUploaded || 0) > 0" class="panel-warn clickable" @click.stop="openDetail('requisition', '要货单')">
            <span class="warn-num">{{ overview.storeData?.requisition?.unUploaded }}</span>
            <span class="warn-label">张已提交未上传 ERP</span>
          </div>
        </div>

        <!-- 配送收货单 -->
        <div class="panel-card panel-delivery" :class="{ clickable: currentView !== 'hq' }" @click="navigateToPanel('distribution')">
          <div class="panel-title">配送收货单 DISTRIBUTION</div>
          <div class="panel-body">
            <div class="panel-big">
              <span class="big-num">{{ overview.storeData?.distribution?.total ?? '--' }}</span>
              <span class="big-label">总单量</span>
            </div>
            <div class="status-list">
              <div class="status-item">
                <span class="status-dot dot-empty"></span><span class="status-name">未确认</span>
                <span class="status-num">{{ overview.storeData?.distribution?.unConfirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-yellow"></span><span class="status-name">已确认</span>
                <span class="status-num">{{ overview.storeData?.distribution?.confirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-blue"></span><span class="status-name">已提交</span>
                <span class="status-num">{{ overview.storeData?.distribution?.submitted ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-green"></span><span class="status-name">已上传</span>
                <span class="status-num">{{ overview.storeData?.distribution?.uploaded ?? '--' }}</span>
              </div>
            </div>
          </div>
          <!-- add by codebuddy 20260818: 点击可查看未上传明细 -->
          <div v-if="(overview.storeData?.distribution?.unUploaded || 0) > 0" class="panel-warn clickable" @click.stop="openDetail('distribution', '配送收货单')">
            <span class="warn-num">{{ overview.storeData?.distribution?.unUploaded }}</span>
            <span class="warn-label">张已提交未上传 ERP</span>
          </div>
        </div>

        <!-- 采购收货单 -->
        <div class="panel-card panel-delivery" :class="{ clickable: currentView !== 'hq' }" @click="navigateToPanel('purchase')">
          <div class="panel-title">采购收货单 PURCHASE</div>
          <div class="panel-body">
            <div class="panel-big">
              <span class="big-num">{{ overview.storeData?.purchase?.total ?? '--' }}</span>
              <span class="big-label">总单量</span>
            </div>
            <div class="status-list">
              <div class="status-item">
                <span class="status-dot dot-empty"></span><span class="status-name">未确认</span>
                <span class="status-num">{{ overview.storeData?.purchase?.unConfirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-yellow"></span><span class="status-name">已确认</span>
                <span class="status-num">{{ overview.storeData?.purchase?.confirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-blue"></span><span class="status-name">已提交</span>
                <span class="status-num">{{ overview.storeData?.purchase?.submitted ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-green"></span><span class="status-name">已上传</span>
                <span class="status-num">{{ overview.storeData?.purchase?.uploaded ?? '--' }}</span>
              </div>
            </div>
          </div>
          <!-- add by codebuddy 20260818: 点击可查看未上传明细 -->
          <div v-if="(overview.storeData?.purchase?.unUploaded || 0) > 0" class="panel-warn clickable" @click.stop="openDetail('purchase', '采购收货单')">
            <span class="warn-num">{{ overview.storeData?.purchase?.unUploaded }}</span>
            <span class="warn-label">张已提交未上传 ERP</span>
          </div>
        </div>

        <!-- 配送退货单 -->
        <div class="panel-card panel-delivery" :class="{ clickable: currentView !== 'hq' }" @click="navigateToPanel('distributionReturn')">
          <div class="panel-title">配送退货单 DISTRIBUTION RETURN</div>
          <div class="panel-body">
            <div class="panel-big">
              <span class="big-num">{{ overview.storeData?.distributionReturn?.total ?? '--' }}</span>
              <span class="big-label">总单量</span>
            </div>
            <div class="status-list">
              <div class="status-item">
                <span class="status-dot dot-empty"></span><span class="status-name">未确认</span>
                <span class="status-num">{{ overview.storeData?.distributionReturn?.unConfirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-yellow"></span><span class="status-name">已确认</span>
                <span class="status-num">{{ overview.storeData?.distributionReturn?.confirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-blue"></span><span class="status-name">已提交</span>
                <span class="status-num">{{ overview.storeData?.distributionReturn?.submitted ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-green"></span><span class="status-name">已上传</span>
                <span class="status-num">{{ overview.storeData?.distributionReturn?.uploaded ?? '--' }}</span>
              </div>
            </div>
          </div>
          <!-- add by codebuddy 20260818: 点击可查看未上传明细 -->
          <div v-if="(overview.storeData?.distributionReturn?.unUploaded || 0) > 0" class="panel-warn clickable" @click.stop="openDetail('distributionReturn', '配送退货单')">
            <span class="warn-num">{{ overview.storeData?.distributionReturn?.unUploaded }}</span>
            <span class="warn-label">张已提交未上传 ERP</span>
          </div>
        </div>

        <!-- 采购退货单 -->
        <div class="panel-card panel-delivery" :class="{ clickable: currentView !== 'hq' }" @click="navigateToPanel('purchaseReturn')">
          <div class="panel-title">采购退货单 PURCHASE RETURN</div>
          <div class="panel-body">
            <div class="panel-big">
              <span class="big-num">{{ overview.storeData?.purchaseReturn?.total ?? '--' }}</span>
              <span class="big-label">总单量</span>
            </div>
            <div class="status-list">
              <div class="status-item">
                <span class="status-dot dot-empty"></span><span class="status-name">未确认</span>
                <span class="status-num">{{ overview.storeData?.purchaseReturn?.unConfirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-yellow"></span><span class="status-name">已确认</span>
                <span class="status-num">{{ overview.storeData?.purchaseReturn?.confirmed ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-blue"></span><span class="status-name">已提交</span>
                <span class="status-num">{{ overview.storeData?.purchaseReturn?.submitted ?? '--' }}</span>
              </div>
              <div class="status-item">
                <span class="status-dot dot-green"></span><span class="status-name">已上传</span>
                <span class="status-num">{{ overview.storeData?.purchaseReturn?.uploaded ?? '--' }}</span>
              </div>
            </div>
          </div>
          <!-- add by codebuddy 20260818: 点击可查看未上传明细 -->
          <div v-if="(overview.storeData?.purchaseReturn?.unUploaded || 0) > 0" class="panel-warn clickable" @click.stop="openDetail('purchaseReturn', '采购退货单')">
            <span class="warn-num">{{ overview.storeData?.purchaseReturn?.unUploaded }}</span>
            <span class="warn-label">张已提交未上传 ERP</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 配送中心数据（总部/配送中心视角） -->
    <div v-if="overview.dcData" class="dash-section" :class="{ 'is-loading': dataLoading }">
      <!-- add by codebuddy 20260819: 非总部视角隐藏分组标题，保留面板数据 -->
      <div class="group-title" v-show="currentView === 'hq'">
        <span class="group-icon dc">DC</span>配送中心数据<span class="group-sub">DC DATA</span>
      </div>
      <div class="panel-grid">
        <div v-for="p in dcPanels" :key="p.en" class="panel-card panel-delivery" :class="{ clickable: currentView !== 'hq' }" @click="navigateToPanel(p.type)">
          <div class="panel-title">{{ p.title }} {{ p.en }}</div>
          <div class="panel-body">
            <div class="panel-big">
              <span class="big-num">{{ p.data?.total ?? '--' }}</span>
              <span class="big-label">总单量</span>
            </div>
            <div class="progress-wrap">
              <div class="progress-info">
                <span>已上传</span>
                <span class="progress-val">{{ calcUploaded(p.data?.total, p.data?.unUploaded) }}</span>
              </div>
              <el-progress :percentage="calcPercent(p.data?.total, p.data?.unUploaded)" :show-text="false" color="#19d3ff" />
            </div>
          </div>
          <!-- add by codebuddy 20260818: 点击可查看未上传明细 -->
          <div v-if="(p.data?.unUploaded || 0) > 0" class="panel-warn clickable" @click.stop="openDetail(p.type, p.title)">
            <span class="warn-num">{{ p.data?.unUploaded }}</span>
            <span class="warn-label">张未上传 ERP</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 未上传预警 + 定时任务配置 -->
    <div class="dash-section" :class="{ 'is-loading': dataLoading }">
      <div class="bottom-grid" :class="{ 'single-col': overview.view !== 'hq' }">
        <!-- 未上传预警 -->
        <div class="bottom-card warn-card">
          <div class="panel-title">未上传 ERP 预警</div>
          <div class="warn-grid">
            <div v-if="overview.storeData" class="warn-block">
              <div class="wb-num">{{ overview.storeData?.requisition?.unUploaded ?? '--' }}</div>
              <div class="wb-name">要货</div>
            </div>
            <div v-if="overview.storeData" class="warn-block">
              <div class="wb-num">{{ receiveUnUploaded }}</div>
              <div class="wb-name">收货</div>
            </div>
            <div v-if="overview.dcData" class="warn-block">
              <div class="wb-num">{{ overview.dcData?.deliveryShipment?.unUploaded ?? '--' }}</div>
              <div class="wb-name">发货</div>
            </div>
            <div v-if="overview.dcData" class="warn-block">
              <div class="wb-num">{{ dcReceiveUnUploaded }}</div>
              <div class="wb-name">入库/仓退</div>
            </div>
            <div class="warn-block warn-total">
              <div class="wb-num">{{ totalUnUploaded }}</div>
              <div class="wb-name">合计</div>
            </div>
          </div>
        </div>

        <!-- 定时任务配置（仅总部视角可改） -->
        <div v-if="overview.view === 'hq'" class="bottom-card config-card">
          <div class="panel-title">ERP 上传定时任务配置</div>
          <div class="config-body">
            <!-- add by codebuddy 20260818: 定时任务开关 -->
            <div class="config-item">
              <span class="c-label">任务开关</span>
              <el-switch
                v-model="enabled"
                active-text="启用"
                inactive-text="停用"
                :loading="saving"
                @change="saveConfig"
              />
              <span class="c-status" :class="enabled ? 'c-status-on' : 'c-status-off'">
                {{ enabled ? '运行中' : '已停用' }}
              </span>
            </div>
            <!-- add by codebuddy 20260818: 停用状态下隐藏执行间隔、上次执行、下次执行 -->
            <div v-if="enabled" class="config-detail">
              <div class="config-item">
                <span class="c-label">执行间隔</span>
                <el-input-number
                  v-model="intervalMinutes"
                  :min="1"
                  :max="1440"
                  :step="1"
                  size="large"
                  controls-position="right"
                  style="width: 160px"
                />
                <span class="c-unit">分钟</span>
                <el-button type="primary" size="large" :loading="saving" @click="saveConfig">保存</el-button>
              </div>
              <div class="config-item">
                <span class="c-label">上次执行</span>
                <span class="c-value">{{ formatTime(overview.taskConfig?.lastRunAt) }}</span>
              </div>
              <div class="config-item">
                <span class="c-label">下次执行</span>
                <span class="c-value">{{ formatTime(overview.taskConfig?.nextRunAt) }}</span>
              </div>
            </div>
            <div class="config-tip">说明：开启后定时任务按固定间隔自动抓取已提交未上传的单据上传 ERP；关闭后任务停止执行，修改后点击保存立即生效</div>
          </div>
        </div>
      </div>
    </div>

    <!-- add by codebuddy 20260818: 未上传 ERP 单据详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="90%"
      :max-width="1280"
      align-center
      destroy-on-close
      class="unupload-dialog"
    >
      <el-table v-loading="detailLoading" :data="detailList" stripe max-height="70vh" empty-text="暂无数据">
        <el-table-column prop="billNo" label="单据编号" min-width="220" show-overflow-tooltip />
        <el-table-column label="所属门店/配送中心" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.orgNo ? `${row.orgNo} - ${row.orgName}` : row.orgName }}</span>
          </template>
        </el-table-column>
        <!-- add by codebuddy 20260818: 改为传输时间（上一次调用 ERP 接口的时间） -->
        <el-table-column label="传输时间" min-width="170">
          <template #default="{ row }">
            <span>{{ row.lastTransmitAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="ERP 报错信息" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="error-text">{{ row.errorMessage || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="toggleAbort(row)">终止上传</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// add by codebuddy 20260817: 首页大屏数据总览
// add by codebuddy 20260818: 按 总部/门店/配送中心 视角分组展示（门店数据 storeData / 配送中心数据 dcData）
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { dashboardApi } from '../api'
import { currentView, currentDeliveryCenterId } from '../stores/globalDc'
import { currentShopId } from '../stores/globalShop'
import type { DashboardOverview, DocStats, UnuploadedBill, UnuploadedBillType } from '../types'

// add by codebuddy 20260819: 门店/配送中心视角点击面板跳转到对应单据页面
const router = useRouter()
const shopPanelRoutes: Record<string, string> = {
  requisition: '/requisition',
  distribution: '/distribution',
  purchase: '/purchase',
  distributionReturn: '/distribution-return',
  purchaseReturn: '/purchase-return'
}
const dcPanelRoutes: Record<string, string> = {
  deliveryShipment: '/delivery-shipment',
  purchaseInbound: '/purchase-inbound',
  warehouseReturn: '/warehouse-return'
}
function navigateToPanel(type: string) {
  if (currentView.value === 'shop' && shopPanelRoutes[type]) {
    router.push(shopPanelRoutes[type])
  } else if (currentView.value === 'dc' && dcPanelRoutes[type]) {
    router.push(dcPanelRoutes[type])
  }
}

const overview = ref<Partial<DashboardOverview>>({})
const intervalMinutes = ref(3)
// add by codebuddy 20260818: 定时任务开关状态，后端默认停用，需用户手动开启
const enabled = ref(false)
const saving = ref(false)
// add by codebuddy 20260818: 数据加载中状态，切换视角时显示遮罩避免页面跳动
const dataLoading = ref(false)
const now = ref('')
let timer: ReturnType<typeof setInterval> | null = null

// add by codebuddy 20260818: 未上传 ERP 单据详情弹窗状态
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailList = ref<UnuploadedBill[]>([])
const detailTitle = ref('')
// add by codebuddy 20260818: 当前弹窗对应的单据类型（终止上传时需要）
const detailType = ref<UnuploadedBillType>('requisition')

const viewLabel = computed(() => {
  const v = overview.value.view || currentView.value
  if (v === 'shop') return '门店视角'
  if (v === 'dc') return '配送中心视角'
  return '总部视角'
})

// add by codebuddy 20260818: 配送中心面板增加 type，用于未上传明细弹窗
const dcPanels = computed<{ title: string; en: string; type: UnuploadedBillType; data?: DocStats }[]>(() => {
  const d = overview.value.dcData
  if (!d) return []
  return [
    { title: '配送发货单', en: 'DELIVERY', type: 'deliveryShipment', data: d.deliveryShipment },
    { title: '采购入库单', en: 'INBOUND', type: 'purchaseInbound', data: d.purchaseInbound },
    { title: '采购仓退单', en: 'WAREHOUSE RETURN', type: 'warehouseReturn', data: d.warehouseReturn }
  ]
})

// 门店数据中收货/退货类单据未上传合计
const receiveUnUploaded = computed(() => {
  const d = overview.value.storeData
  if (!d) return 0
  return (d.distribution?.unUploaded ?? 0) + (d.purchase?.unUploaded ?? 0)
    + (d.distributionReturn?.unUploaded ?? 0) + (d.purchaseReturn?.unUploaded ?? 0)
})

// 配送中心数据中采购入库/采购仓退未上传合计
const dcReceiveUnUploaded = computed(() => {
  const d = overview.value.dcData
  if (!d) return 0
  return (d.purchaseInbound?.unUploaded ?? 0) + (d.warehouseReturn?.unUploaded ?? 0)
})

const totalUnUploaded = computed(() => {
  const requisition = overview.value.storeData?.requisition?.unUploaded ?? 0
  return requisition + receiveUnUploaded.value
    + (overview.value.dcData?.deliveryShipment?.unUploaded ?? 0) + dcReceiveUnUploaded.value
})

function calcUploaded(total?: number, unUploaded?: number) {
  if (total === undefined || unUploaded === undefined) return '--'
  return Math.max(0, total - unUploaded)
}

function calcPercent(total?: number, unUploaded?: number) {
  if (!total) return 0
  const uploaded = Math.max(0, total - (unUploaded ?? 0))
  return Math.round((uploaded / total) * 100)
}

function formatTime(t?: string | null) {
  if (!t) return '--'
  return t.replace('T', ' ').substring(0, 19)
}

function updateClock() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  now.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// add by codebuddy 20260818: 加载总览数据；showLoading=true 时显示遮罩，避免切换视角时页面跳动
async function loadOverview(showLoading = false) {
  if (showLoading) dataLoading.value = true
  try {
    const res: any = await dashboardApi.overview({
      view: currentView.value,
      shopId: currentShopId.value,
      dcId: currentDeliveryCenterId.value
    })
    overview.value = res.data || {}
    intervalMinutes.value = res.data?.taskConfig?.intervalMinutes ?? intervalMinutes.value
    // add by codebuddy 20260818: 同步定时任务开关状态；保存中(用户刚切换)不覆盖，避免轮询把开关打回原状
    if (!saving.value) {
      enabled.value = res.data?.taskConfig?.enabled ?? enabled.value
    }
  } catch (e) {
    // add by codebuddy 20260818: 手动切换视角失败时清空旧数据，避免总部/门店/配送中心数据错位显示
    if (showLoading) {
      overview.value = { view: currentView.value }
    }
  } finally {
    if (showLoading) dataLoading.value = false
  }
}

// add by codebuddy 20260818: 视角/门店/配送中心变化时刷新数据并显示 loading 遮罩
watch([currentView, currentShopId, currentDeliveryCenterId], () => loadOverview(true))

async function saveConfig() {
  saving.value = true
  try {
    await dashboardApi.updateTaskConfig(intervalMinutes.value, enabled.value)
    ElMessage.success(enabled.value ? '定时任务已开启并保存配置' : '定时任务已停用')
    await loadOverview()
  } finally {
    saving.value = false
  }
}

// add by codebuddy 20260818: 点击未上传红框，弹出明细弹窗
async function openDetail(type: UnuploadedBillType, title: string) {
  detailType.value = type
  detailVisible.value = true
  detailTitle.value = title + ' - 未上传 ERP 明细'
  detailLoading.value = true
  try {
    const res: any = await dashboardApi.unuploadedDetail({
      type,
      view: currentView.value,
      shopId: currentShopId.value,
      dcId: currentDeliveryCenterId.value
    })
    detailList.value = res.data || []
  } finally {
    detailLoading.value = false
  }
}

// add by codebuddy 20260818: 终止单据自动上传（终止后从未上传明细移除，并刷新统计）
async function toggleAbort(row: UnuploadedBill) {
  try {
    await dashboardApi.abortUpload(detailType.value, row.id)
    ElMessage.success('已终止该单据自动上传')
    const idx = detailList.value.findIndex((it) => it.id === row.id)
    if (idx >= 0) detailList.value.splice(idx, 1)
    await loadOverview()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

// add by codebuddy 20260818: 时钟实时每秒跳动
let clockTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  updateClock()
  loadOverview(false)
  clockTimer = setInterval(updateClock, 1000)
  timer = setInterval(() => {
    loadOverview(false)
  }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.dashboard-screen {
  min-height: 100%;
  padding: 16px 20px 24px;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(25, 130, 255, 0.18) 0%, transparent 55%),
    radial-gradient(ellipse at 85% 10%, rgba(0, 230, 200, 0.12) 0%, transparent 50%),
    linear-gradient(160deg, #071227 0%, #0a1a33 45%, #0b2345 100%);
  color: #d6e6ff;
  overflow: auto;
}

/* 顶部标题 */
.dash-header {
  position: relative;
  text-align: center;
  padding: 6px 0 18px;
}
.header-title {
  display: inline-block;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 6px;
  background: linear-gradient(90deg, #7ee8ff, #37b6ff 40%, #7ee8ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 22px rgba(55, 182, 255, 0.45);
}
.header-sub {
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 4px;
  color: rgba(140, 190, 255, 0.75);
}
.header-time {
  position: absolute;
  right: 4px;
  top: 10px;
  font-family: 'Consolas', monospace;
  font-size: 15px;
  color: rgba(120, 200, 255, 0.85);
  letter-spacing: 1px;
}

.dash-section {
  margin-bottom: 18px;
  position: relative;
  transition: opacity 0.2s ease;
}
/* add by codebuddy 20260818: 切换视角时保持容器占位并显示 loading 遮罩，避免页面跳动 */
.dash-section.is-loading {
  opacity: 0.65;
}
.dash-section.is-loading::after {
  content: '数据加载中...';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(7, 18, 39, 0.45);
  border-radius: 12px;
  color: #4ee1ff;
  font-size: 14px;
  letter-spacing: 2px;
  text-shadow: 0 0 8px rgba(78, 225, 255, 0.6);
  z-index: 10;
}

/* 分组标题 */
.group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 3px;
  color: #bfe3ff;
}
.group-icon {
  padding: 2px 10px;
  border-radius: 4px;
  background: linear-gradient(90deg, #1989ff, #19d3ff);
  color: #fff;
  font-size: 12px;
  letter-spacing: 1px;
}
.group-icon.dc {
  background: linear-gradient(90deg, #9a5cff, #19d3ff);
}
.group-sub {
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(120, 180, 255, 0.6);
}

/* 面板 */
.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 1400px) {
  .panel-grid { grid-template-columns: repeat(2, 1fr); }
}
.panel-card {
  border-radius: 10px;
  padding: 14px 18px 16px;
  background: linear-gradient(180deg, rgba(18, 44, 82, 0.6), rgba(10, 26, 52, 0.5));
  border: 1px solid rgba(60, 140, 240, 0.3);
  box-shadow: 0 0 16px rgba(20, 80, 180, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}
/* add by codebuddy 20260819: 面板悬浮效果 */
.panel-card.clickable {
  cursor: pointer;
}
.panel-card.clickable:hover {
  transform: translateY(-4px);
  border-color: rgba(78, 225, 255, 0.6);
  box-shadow: 0 6px 24px rgba(30, 120, 230, 0.35);
  background: linear-gradient(180deg, rgba(24, 56, 102, 0.75), rgba(14, 34, 66, 0.6));
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #a8dcff;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(80, 160, 255, 0.2);
  margin-bottom: 12px;
}
.panel-body {
  display: flex;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
}
.panel-big {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 88px;
}
.big-num {
  font-size: 40px;
  font-weight: 700;
  font-family: 'DIN', 'Consolas', monospace;
  color: #eaf6ff;
  text-shadow: 0 0 16px rgba(120, 200, 255, 0.4);
}
.big-label {
  margin-top: 2px;
  font-size: 12px;
  color: rgba(140, 190, 255, 0.7);
}
.status-list {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  min-width: 200px;
}
.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-empty { background: #8b9bb8; }
.dot-yellow { background: #ffc53d; box-shadow: 0 0 8px rgba(255, 197, 61, 0.6); }
.dot-blue { background: #3d9bff; box-shadow: 0 0 8px rgba(61, 155, 255, 0.6); }
.dot-green { background: #2fe08a; box-shadow: 0 0 8px rgba(47, 224, 138, 0.6); }
.status-name { color: rgba(170, 210, 255, 0.9); }
.status-num {
  margin-left: auto;
  font-weight: 700;
  font-size: 16px;
  font-family: 'Consolas', monospace;
  color: #eaf6ff;
}

.panel-warn {
  margin-top: 12px;
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(255, 90, 60, 0.12);
  border: 1px solid rgba(255, 110, 80, 0.35);
}
.panel-warn.clickable {
  cursor: pointer;
  transition: all 0.2s ease;
}
.panel-warn.clickable:hover {
  background: rgba(255, 90, 60, 0.2);
  border-color: rgba(255, 110, 80, 0.55);
}
.warn-num {
  font-size: 26px;
  font-weight: 700;
  font-family: 'DIN', 'Consolas', monospace;
  color: #ff7a5c;
  text-shadow: 0 0 12px rgba(255, 122, 92, 0.5);
}
.warn-label {
  font-size: 13px;
  color: rgba(255, 160, 130, 0.9);
}

/* 进度条 */
.progress-wrap {
  flex: 1;
  min-width: 160px;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
  color: rgba(160, 205, 255, 0.85);
}
.progress-val {
  font-weight: 700;
  color: #4ee1ff;
}
:deep(.el-progress-bar__outer) {
  background: rgba(60, 100, 160, 0.35);
}

/* 底部 */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 16px;
}
.bottom-grid.single-col {
  grid-template-columns: 1fr;
}
.bottom-card {
  border-radius: 10px;
  padding: 14px 18px 16px;
  background: linear-gradient(180deg, rgba(18, 44, 82, 0.6), rgba(10, 26, 52, 0.5));
  border: 1px solid rgba(60, 140, 240, 0.3);
  box-shadow: 0 0 16px rgba(20, 80, 180, 0.12);
}
.warn-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 14px;
  text-align: center;
}
.warn-block {
  padding: 10px 6px;
  border-radius: 8px;
  background: rgba(30, 70, 130, 0.25);
}
.wb-num {
  font-size: 30px;
  font-weight: 700;
  font-family: 'DIN', 'Consolas', monospace;
  color: #ffd54d;
  text-shadow: 0 0 14px rgba(255, 213, 77, 0.45);
}
.warn-total .wb-num {
  color: #ff7a5c;
  text-shadow: 0 0 14px rgba(255, 122, 92, 0.55);
}
.wb-name {
  margin-top: 4px;
  font-size: 13px;
  letter-spacing: 2px;
  color: rgba(170, 210, 255, 0.9);
}

.config-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.config-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}
.c-label {
  width: 80px;
  color: rgba(160, 205, 255, 0.9);
  letter-spacing: 1px;
}
.c-unit {
  color: rgba(140, 190, 255, 0.7);
}
.c-value {
  font-family: 'Consolas', monospace;
  color: #4ee1ff;
}
/* add by codebuddy 20260818: 定时任务开关状态样式 */
.c-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  letter-spacing: 1px;
}
.c-status-on {
  color: #3ddc97;
  background: rgba(61, 220, 151, 0.12);
  border: 1px solid rgba(61, 220, 151, 0.4);
}
.c-status-off {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.12);
  border: 1px solid rgba(255, 107, 107, 0.4);
}
.config-tip {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(140, 190, 255, 0.6);
}
:deep(.el-input-number .el-input__inner) {
  background: rgba(12, 30, 60, 0.6);
  border-color: rgba(64, 150, 255, 0.4);
  color: #d6e6ff;
}
:deep(.el-button--primary) {
  background: linear-gradient(90deg, #1989ff, #19d3ff);
  border: none;
  font-weight: 600;
}

@media (max-width: 900px) {
  .panel-grid { grid-template-columns: 1fr; }
}
@media (max-width: 1200px) {
  .bottom-grid { grid-template-columns: 1fr; }
}

/* add by codebuddy 20260818: 未上传 ERP 单据详情弹窗样式 */
.unupload-dialog ::deep(.el-dialog__header) {
  margin-right: 0;
  padding: 16px 20px;
  background: linear-gradient(90deg, #122c52, #0a1a33);
  border-bottom: 1px solid rgba(60, 140, 240, 0.25);
}
.unupload-dialog ::deep(.el-dialog__title) {
  color: #a8dcff;
  letter-spacing: 2px;
  font-size: 16px;
}
.unupload-dialog ::deep(.el-dialog__body) {
  padding: 0;
  background: #0a1a33;
}
.unupload-dialog ::deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #a8dcff;
}
.unupload-dialog ::deep(.el-table) {
  background: transparent;
}
.unupload-dialog ::deep(.el-table th.el-table__cell) {
  background: rgba(18, 44, 82, 0.9);
  color: #a8dcff;
  font-weight: 600;
}
.unupload-dialog ::deep(.el-table td.el-table__cell) {
  background: transparent;
  color: #d6e6ff;
  border-bottom: 1px solid rgba(60, 100, 160, 0.25);
}
.unupload-dialog ::deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(18, 44, 82, 0.35);
}
.unupload-dialog ::deep(.el-table__empty-text) {
  color: rgba(140, 190, 255, 0.6);
}
.unupload-dialog .error-text {
  color: #ff9a7a;
}
</style>
