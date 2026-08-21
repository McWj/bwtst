import { createRouter, createWebHistory } from 'vue-router'
import { currentUser } from '../stores/auth'

// add by codebuddy 20260819: 移动端路由
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('../layout/TabLayout.vue'),
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'home',
          component: () => import('../views/Home.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'bills/:type',
          name: 'billList',
          component: () => import('../views/BillList.vue'),
          meta: { title: '单据列表' }
        },
        {
          path: 'bill/:type/:id',
          name: 'billDetail',
          component: () => import('../views/BillDetail.vue'),
          meta: { title: '单据详情' }
        },
        {
          path: 'requisition/edit/:id?',
          name: 'requisitionEdit',
          component: () => import('../views/RequisitionEdit.vue'),
          meta: { title: '要货申请' }
        },
        {
          path: 'mine',
          name: 'mine',
          component: () => import('../views/Mine.vue'),
          meta: { title: '我的' }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !currentUser.value) {
    return '/login'
  }
  if (to.path === '/login' && currentUser.value) {
    return '/'
  }
  return true
})

export default router
