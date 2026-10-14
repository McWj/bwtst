import axios from 'axios'
import { ElMessage } from 'element-plus'
import { adminToken, adminLogout } from '../stores/adminAuth'
import router from '../router'

// add by codebuddy 20260923: 平台管理端专用请求实例。
// 使用独立令牌，与业务系统 request.ts 完全隔离，互不影响。
const adminRequest = axios.create({
  baseURL: '/api',
  timeout: 15000
})

adminRequest.interceptors.request.use((config) => {
  if (adminToken.value) {
    config.headers.Authorization = `Bearer ${adminToken.value}`
  }
  return config
})

// add by codebuddy 20260923: 平台端令牌失效时只清平台端会话并回平台登录页
// add by codebuddy 20261014: 用时间窗口对并发 401 去重，只提示一次
let lastAdminWarnAt = 0

adminRequest.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 200) {
      return res
    }
    ElMessage.error(res?.message || '请求失败')
    return Promise.reject(new Error(res?.message || '请求失败'))
  },
  (error) => {
    if (error?.response?.status === 401) {
      if (Date.now() - lastAdminWarnAt > 3000) {
        lastAdminWarnAt = Date.now()
        ElMessage.warning('登录已过期，请重新登录')
      }
      adminLogout()
      if (router.currentRoute.value.name !== 'AdminLogin') {
        router.replace({ name: 'AdminLogin' })
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default adminRequest
