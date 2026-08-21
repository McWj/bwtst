import { ref } from 'vue'
import type { SysUser } from '../types'

const STORAGE_KEY = 'bwtst_user'

// add by codebuddy 20260819: 移动端登录态
function loadUser(): SysUser | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export const currentUser = ref<SysUser | null>(loadUser())

export function setUser(u: SysUser | null) {
  currentUser.value = u
  if (u) localStorage.setItem(STORAGE_KEY, JSON.stringify(u))
  else localStorage.removeItem(STORAGE_KEY)
}

export function logoutUser() {
  setUser(null)
}

export function hasRole(role: string): boolean {
  return !!currentUser.value?.roles?.includes(role)
}

export function defaultView(): 'hq' | 'dc' | 'shop' {
  const roles = currentUser.value?.roles ?? []
  if (roles.includes('hq')) return 'hq'
  if (roles.includes('dc')) return 'dc'
  if (roles.includes('shop')) return 'shop'
  return 'hq'
}
