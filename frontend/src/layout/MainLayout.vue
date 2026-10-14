<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon class="logo-icon"><Box /></el-icon>
        <span>测试中台</span>
      </div>
      <!-- add by codebuddy 20260817: 菜单按视角切换 -->
      <!-- add by codebuddy 20260818: 基础资料仅在总部视角(hq)可见；门店/配送中心视角仅显示各自业务单据 -->
      <el-menu
        :default-active="activeTab"
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ffffff"
      >
        <template v-if="currentView === 'hq'">
          <el-sub-menu index="basic-hq">
            <template #title><el-icon><Folder /></el-icon>基础资料</template>
            <el-menu-item index="/shop" @click="openTab('/shop', '门店资料')">门店资料</el-menu-item>
            <el-menu-item index="/goods" @click="openTab('/goods', '商品资料')">商品资料</el-menu-item>
            <el-menu-item index="/goods-shop" @click="openTab('/goods-shop', '门店商品配置')">门店商品配置</el-menu-item>
            <el-menu-item index="/ptemplate" @click="openTab('/ptemplate', '要货模板')">要货模板</el-menu-item>
            <el-menu-item index="/distribution-center" @click="openTab('/distribution-center', '配送中心资料')">配送中心资料</el-menu-item>
            <el-menu-item index="/supplier" @click="openTab('/supplier', '供应商资料')">供应商资料</el-menu-item>
            <el-menu-item index="/supplier-price" @click="openTab('/supplier-price', '供应商价格表')">供应商价格表</el-menu-item>
            <el-menu-item index="/user" @click="openTab('/user', '用户管理')">用户管理</el-menu-item>
          </el-sub-menu>
        </template>
        <template v-else-if="currentView === 'shop'">
          <el-sub-menu index="bill-shop">
            <template #title><el-icon><Document /></el-icon>业务单据</template>
            <el-menu-item index="/requisition" @click="openTab('/requisition', '要货申请')">要货申请</el-menu-item>
            <el-menu-item index="/distribution" @click="openTab('/distribution', '配送收货')">配送收货</el-menu-item>
            <el-menu-item index="/purchase" @click="openTab('/purchase', '采购收货')">采购收货</el-menu-item>
            <el-menu-item index="/distribution-return" @click="openTab('/distribution-return', '配送退货')">配送退货</el-menu-item>
            <el-menu-item index="/purchase-return" @click="openTab('/purchase-return', '采购退货')">采购退货</el-menu-item>
          </el-sub-menu>
        </template>
        <template v-else>
          <el-sub-menu index="bill-dc">
            <template #title><el-icon><Document /></el-icon>业务单据</template>
            <el-menu-item index="/delivery-shipment" @click="openTab('/delivery-shipment', '配送发货')">配送发货</el-menu-item>
            <el-menu-item index="/purchase-inbound" @click="openTab('/purchase-inbound', '采购入库')">采购入库</el-menu-item>
            <el-menu-item index="/warehouse-return" @click="openTab('/warehouse-return', '采购仓退')">采购仓退</el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <!-- add by codebuddy 20260819: 左上角固定为首页按钮 -->
        <div class="page-title" @click="goHome" style="cursor:pointer" title="回到首页">
          <el-icon class="page-title-icon"><HomeFilled /></el-icon>
          <span>首页</span>
        </div>
        <div class="header-right">
          <!-- add by codebuddy 20260818: 门店视角下显示可用额度 -->
          <div v-if="currentView === 'shop'" class="credit-text" :class="{ 'credit-error': creditError }">
            {{ creditText }}
          </div>
          <!-- add by codebuddy 20260818: 右上角组织视角触发器，点击打开切换弹窗 -->
          <div class="org-trigger" @click="openOrgSwitcher">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="org-name" :title="currentOrgText">{{ currentOrgText }}</span>
            <el-icon class="org-arrow"><ArrowDown /></el-icon>
          </div>
          <!-- add by codebuddy 20260819: 用户信息 + 退出 -->
          <el-dropdown trigger="click" @command="onUserCommand">
            <div class="user-trigger">
              <el-icon><User /></el-icon>
              <span class="user-name">{{ currentUser?.realName || currentUser?.username }}</span>
              <el-icon class="org-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <!-- add by codebuddy 20260819: 页签导航（数据总览不单独占用页签，仅在有打开页面时显示） -->
        <el-tabs
          v-if="tabList.length > 0"
          v-model="activeTab"
          type="card"
          closable
          class="main-tabs"
          @tab-change="onTabChange"
          @tab-remove="onTabRemove"
        >
          <el-tab-pane
            v-for="tab in tabList"
            :key="tab.path"
            :label="tab.title"
            :name="tab.path"
          />
        </el-tabs>
        <div class="tab-content">
          <Dashboard v-if="activeTab === '/dashboard' || tabList.length === 0" />
          <router-view v-else />
        </div>
      </el-main>
    </el-container>
  </el-container>

  <!-- add by codebuddy 20260818: 组织视角切换弹窗（左分类 + 右列表 + 搜索） -->
  <el-dialog
    v-model="orgDialogVisible"
    title="组织视角切换"
    width="720px"
    align-center
    destroy-on-close
    class="org-switch-dialog"
  >
    <div class="org-switch-body">
      <!-- 左侧视角分类 -->
      <div class="org-switch-menu">
        <div
          v-for="item in viewMenuList"
          :key="item.value"
          class="org-menu-item"
          :class="{ active: dialogView === item.value }"
          @click="dialogView = item.value"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>

      <!-- 右侧组织列表 -->
      <div class="org-switch-panel">
        <el-input
          v-if="dialogView !== 'hq'"
          v-model="searchKey"
          :placeholder="searchPlaceholder"
          clearable
          class="org-search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <div class="org-list">
          <!-- 总部视角：只有一项 -->
          <template v-if="dialogView === 'hq'">
            <div
              class="org-list-item"
              :class="{ active: currentView === 'hq' }"
              @click="applyOrg('hq')"
            >
              <div class="org-item-title">总部全局</div>
            </div>
          </template>

          <!-- 配送中心视角：列表 -->
          <template v-else-if="dialogView === 'dc'">
            <div
              v-for="d in filteredDcOptions"
              :key="d.id"
              class="org-list-item"
              :class="{ active: currentView === 'dc' && currentDeliveryCenterId === d.id }"
              @click="applyOrg('dc', undefined, d.id)"
            >
              <div class="org-item-title">{{ d.dcNo }}（{{ d.dcName }}）</div>
            </div>
            <el-empty v-if="filteredDcOptions.length === 0" description="暂无配送中心" />
          </template>

          <!-- 门店视角：列表 -->
          <template v-else>
            <div
              v-for="s in filteredShopOptions"
              :key="s.id"
              class="org-list-item"
              :class="{ active: currentView === 'shop' && currentShopId === s.id }"
              @click="applyOrg('shop', s.id)"
            >
              <div class="org-item-title">{{ s.shopNo }}（{{ s.shopName }}）</div>
            </div>
            <el-empty v-if="filteredShopOptions.length === 0" description="暂无门店" />
          </template>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">

import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import { currentShopId, shopOptions } from '../stores/globalShop'
import { currentView, currentDeliveryCenterId, deliveryCenterOptions } from '../stores/globalDc'
import { distributionCenterApi, shopApi } from '../api'
import { creditQuery } from '../api/erp'
import { currentUser, hasRole, defaultView, logoutUser } from '../stores/auth'
import { Folder, Document, Box, DataAnalysis, OfficeBuilding, ArrowDown, Search, HomeFilled, SwitchButton, User } from '@element-plus/icons-vue'

// add by codebuddy 20260818: 组织视角切换弹窗
const orgDialogVisible = ref(false)
const dialogView = ref<'hq' | 'shop' | 'dc'>('hq')
const searchKey = ref('')

const viewMenuList = computed(() => {
  const roles = currentUser.value?.roles ?? []
  const list: { value: 'hq' | 'dc' | 'shop'; label: string; icon: any }[] = []
  if (roles.includes('hq')) list.push({ value: 'hq', label: '总部视角', icon: OfficeBuilding })
  if (roles.includes('dc')) list.push({ value: 'dc', label: '配送视角', icon: Box })
  if (roles.includes('shop')) list.push({ value: 'shop', label: '门店视角', icon: HomeFilled })
  return list
})

// add by codebuddy 20260818: 当前右上角显示的组织名称
const currentOrgText = computed(() => {
  if (currentView.value === 'hq') return '总部全局'
  if (currentView.value === 'dc') {
    const d = deliveryCenterOptions.value.find((x) => x.id === currentDeliveryCenterId.value)
    return d ? `${d.dcNo}（${d.dcName}）` : '请选择配送中心'
  }
  const s = shopOptions.value.find((x) => x.id === currentShopId.value)
  return s ? `${s.shopNo}（${s.shopName}）` : '请选择门店'
})

const searchPlaceholder = computed(() =>
  dialogView.value === 'dc' ? '请输入配送中心编号或名称' : '请输入门店编号或名称'
)

// add by codebuddy 20260819: 仅展示当前用户被授权访问的门店/配送中心
const authorizedShopOptions = computed(() => {
  const ids = currentUser.value?.shopIds ?? []
  if (ids.length === 0) return shopOptions.value
  return shopOptions.value.filter((s) => s.id != null && ids.includes(s.id))
})

const authorizedDcOptions = computed(() => {
  const ids = currentUser.value?.dcIds ?? []
  if (ids.length === 0) return deliveryCenterOptions.value
  return deliveryCenterOptions.value.filter((d) => d.id != null && ids.includes(d.id))
})

const filteredShopOptions = computed(() => {
  const key = searchKey.value.trim().toLowerCase()
  const base = authorizedShopOptions.value
  if (!key) return base
  return base.filter(
    (s) => s.shopNo.toLowerCase().includes(key) || s.shopName.toLowerCase().includes(key)
  )
})

const filteredDcOptions = computed(() => {
  const key = searchKey.value.trim().toLowerCase()
  const base = authorizedDcOptions.value
  if (!key) return base
  return base.filter(
    (d) => d.dcNo.toLowerCase().includes(key) || d.dcName.toLowerCase().includes(key)
  )
})

function openOrgSwitcher() {
  dialogView.value = currentView.value
  searchKey.value = ''
  orgDialogVisible.value = true
}

// add by codebuddy 20260818: 应用选中的组织视角并关闭弹窗
// add by codebuddy 20260819: 仅允许切换到用户拥有角色对应的视角
function applyOrg(view: 'hq' | 'shop' | 'dc', shopId?: number, dcId?: number) {
  const roles = currentUser.value?.roles ?? []
  if (view === 'hq' && !roles.includes('hq')) return
  if (view === 'dc' && !roles.includes('dc')) return
  if (view === 'shop' && !roles.includes('shop')) return
  if (view === 'hq') {
    currentView.value = 'hq'
  } else if (view === 'shop' && shopId) {
    currentView.value = 'shop'
    currentShopId.value = shopId
  } else if (view === 'dc' && dcId) {
    currentView.value = 'dc'
    currentDeliveryCenterId.value = dcId
  }
  orgDialogVisible.value = false
}

// add by codebuddy 20260819: 退出登录
function onUserCommand(command: string) {
  if (command === 'logout') {
    logoutUser()
    // add by codebuddy 20260923: 重置组织视角，避免切换账号后残留上一个账号的数据范围
    currentView.value = 'hq'
    currentDeliveryCenterId.value = undefined
    currentShopId.value = undefined
    router.replace({ name: 'Login' })
  }
}

const router = useRouter()
const route = useRoute()

// add by codebuddy 20260819: 页签管理（数据总览不占用页签，仅作为首页背景）
interface Tab { path: string; title: string }
const tabList = ref<Tab[]>([])
const activeTab = ref('/dashboard')

function loadTabs() {
  const allowed = currentView.value === 'hq' ? hqViewPaths : currentView.value === 'shop' ? shopViewPaths : dcViewPaths
  try {
    const saved = localStorage.getItem('mainTabs')
    if (saved) {
      tabList.value = (JSON.parse(saved) as Tab[]).filter((t) => allowed.includes(t.path))
    }
  } catch {
    tabList.value = []
  }
  activeTab.value = route.path
  if (route.path === '/dashboard') {
    saveTabs()
    return
  }
  if (!tabList.value.some((t) => t.path === route.path)) {
    ensureTab(route.path, route.meta.title as string || '未命名')
  }
  saveTabs()
}

function saveTabs() {
  localStorage.setItem('mainTabs', JSON.stringify(tabList.value))
}

function ensureTab(path: string, title: string) {
  if (!tabList.value.some((t) => t.path === path)) {
    tabList.value.push({ path, title })
    saveTabs()
  }
}

function openTab(path: string, title: string) {
  ensureTab(path, title)
  activeTab.value = path
  router.push(path)
}

function goHome() {
  activeTab.value = '/dashboard'
  router.push('/dashboard')
}

function onTabChange(name: string) {
  activeTab.value = name
  router.push(name)
}

function onTabRemove(name: string) {
  const idx = tabList.value.findIndex((t) => t.path === name)
  if (idx === -1) return
  tabList.value.splice(idx, 1)
  saveTabs()
  // 关闭所有页签后回到首页
  if (tabList.value.length === 0) {
    activeTab.value = '/dashboard'
    router.push('/dashboard')
    return
  }
  // 若关闭的是当前页，跳到左侧相邻页签
  if (activeTab.value === name) {
    const next = tabList.value[Math.max(0, idx - 1)]
    activeTab.value = next.path
    router.push(next.path)
  }
}

// add by codebuddy 20260817: 所选门店的可用额度
const creditText = ref('')
const creditError = ref(false)

// add by codebuddy 20260817: 各视角可访问的路由路径
// add by codebuddy 20260818: 基础资料路由(hqViewPaths)仅总部视角可见；门店/配送中心视角仅保留各自业务单据路由
const hqViewPaths = ['/shop', '/goods', '/goods-shop', '/ptemplate', '/distribution-center', '/supplier', '/supplier-price', '/user']
const shopViewPaths = ['/requisition', '/distribution', '/purchase', '/distribution-return', '/purchase-return']
const dcViewPaths = ['/delivery-shipment', '/purchase-inbound', '/warehouse-return']

// add by codebuddy 20260817: 加载门店/配送中心列表并恢复上次选择与视角
// add by codebuddy 20260819: 未登录跳转到登录页；按用户角色决定默认视角与默认组织
onMounted(async () => {
  if (!currentUser.value) {
    router.replace({ name: 'Login' })
    return
  }
  await loadOptions()
  const roles = currentUser.value.roles ?? []
  const savedView = localStorage.getItem('currentView')
  if (savedView === 'hq' && roles.includes('hq')) {
    currentView.value = 'hq'
  } else if (savedView === 'dc' && roles.includes('dc')) {
    currentView.value = 'dc'
  } else if (savedView === 'shop' && roles.includes('shop')) {
    currentView.value = 'shop'
  } else {
    currentView.value = defaultView()
  }
  // 优先使用用户的默认授权组织
  if (currentView.value === 'shop') {
    const def = currentUser.value.defaultShopId ?? currentUser.value.shopIds?.[0]
    if (def) currentShopId.value = def
    const saved = localStorage.getItem('currentShopId')
    if (saved && (currentUser.value.shopIds ?? []).includes(Number(saved))) {
      currentShopId.value = Number(saved)
    }
  } else if (currentView.value === 'dc') {
    const def = currentUser.value.defaultDcId ?? currentUser.value.dcIds?.[0]
    if (def) currentDeliveryCenterId.value = def
    const saved = localStorage.getItem('currentDeliveryCenterId')
    if (saved && (currentUser.value.dcIds ?? []).includes(Number(saved))) {
      currentDeliveryCenterId.value = Number(saved)
    }
  }
  ensureDeliveryCenterSelected()
  await queryCredit()
  loadTabs()
  redirectByView()
})

// add by codebuddy 20260817: 分别加载门店/配送中心选项，避免一个失败导致另一个也无法加载
async function loadOptions() {
  try {
    const shopRes: any = await shopApi.list()
    shopOptions.value = shopRes.data || []
  } catch (e: any) {
    ElMessage.error('门店列表加载失败：' + (e.message || '未知错误'))
  }
  try {
    const dcRes: any = await distributionCenterApi.list()
    deliveryCenterOptions.value = dcRes.data || []
    ensureDeliveryCenterSelected()
  } catch (e: any) {
    ElMessage.error('配送中心列表加载失败：' + (e.message || '未知错误'))
  }
}

// add by codebuddy 20260817: 配送中心视角下自动默认第一笔，无需手动选择
function ensureDeliveryCenterSelected() {
  if (currentView.value !== 'dc') return
  const valid = deliveryCenterOptions.value.some((d) => d.id === currentDeliveryCenterId.value)
  if (!valid && deliveryCenterOptions.value.length > 0) {
    currentDeliveryCenterId.value = deliveryCenterOptions.value[0].id
  }
}

// add by codebuddy 20260819: 路由变化时同步页签状态（首页不加入页签）
watch(() => route.path, (path) => {
  activeTab.value = path
  if (path === '/dashboard') return
  ensureTab(path, route.meta.title as string || '未命名')
})

watch(currentShopId, (val) => {
  if (val) {
    localStorage.setItem('currentShopId', String(val))
  } else {
    localStorage.removeItem('currentShopId')
  }
  queryCredit()
})

watch(currentDeliveryCenterId, (val) => {
  if (val) {
    localStorage.setItem('currentDeliveryCenterId', String(val))
  } else {
    localStorage.removeItem('currentDeliveryCenterId')
    // add by codebuddy 20260817: 手动清空后自动回退到第一笔
    if (currentView.value === 'dc' && deliveryCenterOptions.value.length > 0) {
      currentDeliveryCenterId.value = deliveryCenterOptions.value[0].id
    }
  }
})

// add by codebuddy 20260817: 切换视角后跳到新视角默认页面；若目标视角选项为空则重新加载
watch(currentView, async (val) => {
  localStorage.setItem('currentView', val)
  if (val === 'dc' && deliveryCenterOptions.value.length === 0) {
    try {
      const dcRes: any = await distributionCenterApi.list()
      deliveryCenterOptions.value = dcRes.data || []
    } catch (e: any) {
      ElMessage.error('配送中心列表加载失败：' + (e.message || '未知错误'))
    }
  }
  if (val === 'shop' && shopOptions.value.length === 0) {
    try {
      const shopRes: any = await shopApi.list()
      shopOptions.value = shopRes.data || []
    } catch (e: any) {
      ElMessage.error('门店列表加载失败：' + (e.message || '未知错误'))
    }
  }
  ensureDeliveryCenterSelected()
  // add by codebuddy 20260819: 切换视角后，关闭当前视角不可访问的页签，回到首页
  const allowed = val === 'hq' ? hqViewPaths : val === 'shop' ? shopViewPaths : dcViewPaths
  tabList.value = tabList.value.filter((t) => allowed.includes(t.path))
  saveTabs()
  if (activeTab.value !== '/dashboard' && !tabList.value.some((t) => t.path === activeTab.value)) {
    activeTab.value = '/dashboard'
    router.push('/dashboard')
  }
  redirectByView()
})

function redirectByView() {
  const path = router.currentRoute.value.path
  if (currentView.value === 'hq' && (shopViewPaths.includes(path) || dcViewPaths.includes(path))) {
    router.push('/dashboard')
  } else if (currentView.value === 'dc' && (shopViewPaths.includes(path) || hqViewPaths.includes(path))) {
    router.push('/delivery-shipment')
  } else if (currentView.value === 'shop' && (dcViewPaths.includes(path) || hqViewPaths.includes(path))) {
    router.push('/requisition')
  }
}

// add by codebuddy 20260817: 按ERP报文格式查询门店可用额度（报文构建已封装至 api/erp.ts）
async function queryCredit() {
  creditText.value = ''
  creditError.value = false
  const shop = shopOptions.value.find((s) => s.id === currentShopId.value)
  if (!shop) {
    return
  }
  try {
    // add by codebuddy 20260923: 改为走后端代理，账套地址由后端按当前租户决定
    const stdData = await creditQuery(shop.shopNo)
    const amt = stdData?.parameter?.remaining_amount
    creditText.value = amt !== undefined && amt !== null
      ? `可用额度：${Number(amt).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
      : '可用额度：—'
  } catch (e) {
    creditError.value = true
    creditText.value = '可用额度：获取失败'
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background-color: #001529;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-icon {
  font-size: 24px;
  color: #409eff;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.aside :deep(.el-menu-item.is-active) {
  background-color: #409eff;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.page-title-icon {
  font-size: 20px;
  color: #409eff;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
/* add by codebuddy 20260818: 右上角组织视角触发器 */
.org-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  cursor: pointer;
  transition: all 0.2s ease;
  max-width: 300px;
}
.org-trigger:hover {
  background: #ecf5ff;
  border-color: #c6e2ff;
  color: #409eff;
}
.org-name {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.org-arrow {
  font-size: 12px;
  color: #909399;
}
.credit-text {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}
.credit-error {
  color: #f56c6c;
}
.hq-text {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}
/* add by codebuddy 20260818: 组织视角切换弹窗样式 */
:deep(.org-switch-dialog .el-dialog__body) {
  padding: 0 20px 20px;
}
.org-switch-body {
  display: flex;
  height: 420px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}
.org-switch-menu {
  width: 160px;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  padding: 12px 0;
  flex-shrink: 0;
}
.org-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
}
.org-menu-item:hover {
  background: #ecf5ff;
  color: #409eff;
}
.org-menu-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
  border-right: 3px solid #409eff;
}
.org-switch-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
}
.org-search {
  margin-bottom: 12px;
}
.org-list {
  flex: 1;
  overflow-y: auto;
}
.org-list-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.org-list-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
}
.org-list-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.org-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.org-item-sub {
  font-size: 12px;
  color: #909399;
}
.main {
  background-color: #f0f2f5;
  padding: 12px 16px 16px;
  display: flex;
  flex-direction: column;
}
.main-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.tab-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
</style>
