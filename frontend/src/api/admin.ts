import adminRequest from './adminRequest'
import type { LoginResult, PageResult, SysTenant } from '../types'

// add by codebuddy 20260923: 平台管理端 API（走独立登录态，与业务系统隔离）

export const adminAuthApi = {
  /** 平台管理端登录，仅平台超级管理员可登录 */
  login: (data: { username: string; password: string; tenantCode?: string }) =>
    adminRequest.post<LoginResult>('/auth/platform-login', data)
}

export const adminTenantApi = {
  page: (params: { page: number; size: number; keyword?: string }) =>
    adminRequest.get<PageResult<SysTenant>>('/tenant/page', { params }),
  save: (data: SysTenant) => adminRequest.post('/tenant/save', data),
  setStatus: (id: number, status: string) => adminRequest.post('/tenant/status', { id, status }),
  resetAdminPassword: (tenantId: number, username: string, newPassword: string) =>
    adminRequest.post('/tenant/reset-admin-password', { tenantId, username, newPassword })
}
