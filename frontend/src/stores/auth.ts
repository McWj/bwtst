import { ref } from 'vue'
import type { SysUser } from '../types'

const STORAGE_KEY = 'bwtst_user'
// add by codebuddy 20260923: 访问令牌存储键
const TOKEN_KEY = 'bwtst_token'

function loadUser(): SysUser | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

// add by codebuddy 20260819: 登录态（与项目其他全局 store 一致，使用普通 ref）
export const currentUser = ref<SysUser | null>(loadUser())

// add by codebuddy 20260923: 访问令牌，请求时通过 Authorization: Bearer 携带
export const currentToken = ref<string | null>(localStorage.getItem(TOKEN_KEY))

export function setToken(t: string | null) {
  currentToken.value = t
  if (t) localStorage.setItem(TOKEN_KEY, t)
  else localStorage.removeItem(TOKEN_KEY)
}

export function getToken(): string | null {
  return currentToken.value
}

/**
 * add by codebuddy 20261014: 预判令牌是否已过期（仅解析 exp，不做签名校验，真正的校验在后端）。
 * 用于路由守卫提前清理过期登录态，避免带着过期令牌进入业务页面后集中爆 401。
 */
export function isTokenExpired(token: string | null): boolean {
  if (!token) {
    return true
  }
  const parts = token.split('.')
  if (parts.length !== 3) {
    return true
  }
  try {
    const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
    const matched = json.match(/"exp"\s*:\s*(\d+)/)
    if (!matched) {
      return false
    }
    return Number(matched[1]) * 1000 <= Date.now()
  } catch {
    return false
  }
}

export function setUser(u: SysUser | null) {
  currentUser.value = u
  if (u) localStorage.setItem(STORAGE_KEY, JSON.stringify(u))
  else localStorage.removeItem(STORAGE_KEY)
}

export function logoutUser() {
  setUser(null)
  // add by codebuddy 20260923: 同时清理令牌与组织上下文，避免切换账号后残留上一个账号的数据范围
  setToken(null)
  localStorage.removeItem('currentView')
  localStorage.removeItem('currentShopId')
  localStorage.removeItem('currentDeliveryCenterId')
}

// add by codebuddy 20260819: 是否拥有某角色
export function hasRole(role: string): boolean {
  return !!currentUser.value?.roles?.includes(role)
}

// add by codebuddy 20260819: 推断默认视角：hq > dc > shop
export function defaultView(): 'hq' | 'dc' | 'shop' {
  const roles = currentUser.value?.roles ?? []
  if (roles.includes('hq')) return 'hq'
  if (roles.includes('dc')) return 'dc'
  if (roles.includes('shop')) return 'shop'
  return 'hq'
}
