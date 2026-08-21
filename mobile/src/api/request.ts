import axios from 'axios'
import { showToast } from 'vant'

// add by codebuddy 20260819: 移动端请求封装，复用后端 /api 前缀
// add by codebuddy 20260819: 打包后可配置后端地址，开发环境走 vite 代理
const SERVER = import.meta.env.VITE_SERVER || ''
const request = axios.create({
  baseURL: SERVER + '/api',
  timeout: 15000
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 200) {
      return res
    }
    showToast(res?.message || '请求失败')
    return Promise.reject(new Error(res?.message || '请求失败'))
  },
  (error) => {
    showToast(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
