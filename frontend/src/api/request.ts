import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, logoutUser } from '../stores/auth'
import router from '../router'

// add by codebuddy 20260817
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// add by codebuddy 20260923: 所有后端请求自动携带访问令牌
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// add by codebuddy 20260923: 令牌失效时只跳转一次登录页，避免并发请求重复弹窗
// add by codebuddy 20261014: 改用时间窗口去重，并发 401 只提示一次（原实现跳转后立即复位，会被后续 401 再次触发）
let lastAuthWarnAt = 0

request.interceptors.response.use(
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
      if (Date.now() - lastAuthWarnAt > 3000) {
        lastAuthWarnAt = Date.now()
        ElMessage.warning('登录已过期，请重新登录')
      }
      logoutUser()
      if (router.currentRoute.value.name !== 'Login') {
        router.replace({ name: 'Login' })
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
