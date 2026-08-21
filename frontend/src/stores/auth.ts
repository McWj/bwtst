import { ref } from 'vue'
import type { SysUser } from '../types'

const STORAGE_KEY = 'bwtst_user'

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

export function setUser(u: SysUser | null) {
  currentUser.value = u
  if (u) localStorage.setItem(STORAGE_KEY, JSON.stringify(u))
  else localStorage.removeItem(STORAGE_KEY)
}

export function logoutUser() {
  setUser(null)
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
