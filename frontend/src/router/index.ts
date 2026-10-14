import { createRouter, createWebHistory } from 'vue-router'
import { currentUser, currentToken, logoutUser, isTokenExpired } from '../stores/auth'
import { adminToken, adminUser, adminLogout } from '../stores/adminAuth'

// 角色约束：hq-总部 dc-配送中心 shop-门店，不设置表示所有已登录用户可见
// add by codebuddy 20260817
const router = createRouter({
  history: createWebHistory(),
  routes: [
    // add by codebuddy 20260819: 登录页（独立于布局）
    { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('../layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        // add by codebuddy 20260817: 首页大屏
        { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '数据总览', roles: ['hq', 'dc', 'shop'] } },
        // 基础资料（仅总部）
        { path: '/shop', name: 'Shop', component: () => import('../views/Shop.vue'), meta: { title: '门店资料', roles: ['hq'] } },
        { path: '/goods', name: 'Goods', component: () => import('../views/Goods.vue'), meta: { title: '商品资料', roles: ['hq'] } },
        { path: '/goods-shop', name: 'GoodsShop', component: () => import('../views/GoodsShop.vue'), meta: { title: '门店商品配置', roles: ['hq'] } },
        { path: '/ptemplate', name: 'Ptemplate', component: () => import('../views/Ptemplate.vue'), meta: { title: '要货模板', roles: ['hq'] } },
        // add by codebuddy 20260817: 配送中心资料（总部+配送中心）
        { path: '/distribution-center', name: 'DistributionCenter', component: () => import('../views/DistributionCenter.vue'), meta: { title: '配送中心资料', roles: ['hq', 'dc'] } },
        // add by codebuddy 20260819: 供应商资料与供应商价格表（总部）
        { path: '/supplier', name: 'Supplier', component: () => import('../views/Supplier.vue'), meta: { title: '供应商资料', roles: ['hq'] } },
        { path: '/supplier-price', name: 'SupplierPrice', component: () => import('../views/SupplierPrice.vue'), meta: { title: '供应商价格表', roles: ['hq'] } },
        // 业务单据
        { path: '/requisition', name: 'Requisition', component: () => import('../views/Requisition.vue'), meta: { title: '要货申请', roles: ['hq', 'shop'] } },
        { path: '/distribution', name: 'Distribution', component: () => import('../views/Distribution.vue'), meta: { title: '配送收货', roles: ['hq', 'shop'] } },
        { path: '/purchase', name: 'Purchase', component: () => import('../views/Purchase.vue'), meta: { title: '采购收货', roles: ['hq', 'shop'] } },
        { path: '/distribution-return', name: 'DistributionReturn', component: () => import('../views/DistributionReturn.vue'), meta: { title: '配送退货', roles: ['hq', 'shop'] } },
        { path: '/purchase-return', name: 'PurchaseReturn', component: () => import('../views/PurchaseReturn.vue'), meta: { title: '采购退货', roles: ['hq', 'shop'] } },
        // add by codebuddy 20260817: 配送发货单（配送中心视角）
        { path: '/delivery-shipment', name: 'DeliveryShipment', component: () => import('../views/DeliveryShipment.vue'), meta: { title: '配送发货', roles: ['hq', 'dc'] } },
        // add by codebuddy 20260818: 采购入库单/采购仓退单（配送中心视角）
        { path: '/purchase-inbound', name: 'PurchaseInbound', component: () => import('../views/PurchaseInbound.vue'), meta: { title: '采购入库', roles: ['hq', 'dc'] } },
        { path: '/warehouse-return', name: 'WarehouseReturn', component: () => import('../views/WarehouseReturn.vue'), meta: { title: '采购仓退', roles: ['hq', 'dc'] } },
        // add by codebuddy 20260819: 用户管理（总部）
        { path: '/user', name: 'User', component: () => import('../views/User.vue'), meta: { title: '用户管理', roles: ['hq'] } }
      ]
    },
    // add by codebuddy 20260923: 平台管理端——完全独立入口，业务系统内不出现任何入口
    { path: '/admin/login', name: 'AdminLogin', component: () => import('../views/admin/AdminLogin.vue'), meta: { public: true } },
    {
      path: '/admin',
      component: () => import('../layout/AdminLayout.vue'),
      redirect: '/admin/tenant',
      children: [
        { path: '/admin/tenant', name: 'AdminTenant', component: () => import('../views/admin/Tenant.vue'), meta: { title: '租户管理' } }
      ]
    }
  ]
})

// add by codebuddy 20260819: 路由守卫——未登录跳登录页；按角色限制菜单访问
router.beforeEach((to, from, next) => {
  // add by codebuddy 20261014: 进入任何页面前先清理已过期的令牌，
  // 避免带着过期登录态进入业务页面后，页面并发拉数据集中触发 401 报错
  if (currentToken.value && isTokenExpired(currentToken.value)) {
    logoutUser()
  }
  if (adminToken.value && isTokenExpired(adminToken.value)) {
    adminLogout()
  }
  // add by codebuddy 20260923: 平台管理端独立守卫——仅平台超级管理员可进入，否则回平台登录页
  if (to.path.startsWith('/admin')) {
    if (to.meta?.public) {
      next()
      return
    }
    if (!adminToken.value || !adminUser.value || adminUser.value.isPlatformAdmin !== '1') {
      next({ name: 'AdminLogin' })
      return
    }
    next()
    return
  }
  const roles = to.meta?.roles as string[] | undefined
  if (to.meta?.public) {
    next()
    return
  }
  // add by codebuddy 20260923: 未登录或无令牌一律回登录页
  if (!currentUser.value || !currentToken.value) {
    next({ name: 'Login' })
    return
  }
  if (roles && roles.length && !roles.some((r) => currentUser.value!.roles?.includes(r))) {
    next({ name: 'Dashboard' })
    return
  }
  next()
})

export default router
