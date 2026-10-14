import { ref } from 'vue'
import type { SysUser } from '../types'

// add by codebuddy 20260923: 平台管理端独立登录态。
// 与业务系统的 stores/auth 使用不同的存储键，两者互不影响，可在同一浏览器同时登录不同账号。
const ADMIN_TOKEN_KEY = 'bwtst_admin_token'
const ADMIN_USER_KEY = 'bwtst_admin_user'

function loadAdminUser(): SysUser | null {
  const raw = localStorage.getItem(ADMIN_USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export const adminToken = ref<string | null>(localStorage.getItem(ADMIN_TOKEN_KEY))
export const adminUser = ref<SysUser | null>(loadAdminUser())

export function setAdminToken(t: string | null) {
  adminToken.value = t
  if (t) localStorage.setItem(ADMIN_TOKEN_KEY, t)
  else localStorage.removeItem(ADMIN_TOKEN_KEY)
}

export function setAdminUser(u: SysUser | null) {
  adminUser.value = u
  if (u) localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(u))
  else localStorage.removeItem(ADMIN_USER_KEY)
}

/** 仅清理平台管理端会话，不影响业务系统登录态 */
export function adminLogout() {
  setAdminToken(null)
  setAdminUser(null)
}

/** 建立平台管理端会话（平台登录页、以及超管从业务登录页登录时复用） */
export function establishAdminSession(token: string, user: SysUser) {
  setAdminToken(token)
  setAdminUser(user)
}
