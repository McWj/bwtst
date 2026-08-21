import request from './request'
import type {
  BillDto, DashboardOverview, DeliveryShipment, DeliveryShipmentDetail, Distribution, DistributionCenter,
  DistributionDetail, DistributionReturn, DistributionReturnDetail, Goods, PageResult, Ptemplate,
  PtemplateDto, Purchase, PurchaseDetail, PurchaseInbound, PurchaseInboundDetail, PurchaseReturn,
  PurchaseReturnDetail, Requisition, RequisitionDetail, Shop, SysUser, ViewType, WarehouseReturn,
  WarehouseReturnDetail
} from '../types'

// add by codebuddy 20260819: 移动端 API（复用后端接口）
export const shopApi = {
  list: () => request.get<Shop[]>('/shop/list'),
  page: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<PageResult<Shop>>('/shop/page', { params })
}

export const goodsApi = {
  list: () => request.get<Goods[]>('/goods/list')
}

export const ptemplateApi = {
  list: () => request.get<Ptemplate[]>('/ptemplate/list'),
  listByShop: (shopId: number) => request.get<Ptemplate[]>('/ptemplate/listByShop', { params: { shopId } }),
  get: (id: number) => request.get<PtemplateDto>(`/ptemplate/${id}`)
}

export const requisitionApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; siteId?: number }) =>
    request.get<PageResult<Requisition>>('/requisition/page', { params }),
  get: (id: number) => request.get<BillDto<Requisition, RequisitionDetail>>(`/requisition/${id}`),
  save: (data: BillDto<Requisition, RequisitionDetail>) => request.post('/requisition', data),
  update: (data: BillDto<Requisition, RequisitionDetail>) => request.put('/requisition', data),
  remove: (id: number) => request.delete(`/requisition/${id}`),
  confirm: (id: number, data?: Partial<Requisition>) => request.post(`/requisition/confirm/${id}`, data),
  submit: (id: number) => request.post(`/requisition/submit/${id}`)
}

export const distributionApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; siteId?: number }) =>
    request.get<PageResult<Distribution>>('/distribution/page', { params }),
  get: (id: number) => request.get<BillDto<Distribution, DistributionDetail>>(`/distribution/${id}`),
  save: (data: BillDto<Distribution, DistributionDetail>) => request.post('/distribution', data),
  update: (data: BillDto<Distribution, DistributionDetail>) => request.put('/distribution', data),
  remove: (id: number) => request.delete(`/distribution/${id}`),
  confirm: (id: number) => request.post(`/distribution/confirm/${id}`),
  submit: (id: number) => request.post(`/distribution/submit/${id}`)
}

export const purchaseApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; siteId?: number }) =>
    request.get<PageResult<Purchase>>('/purchase/page', { params }),
  get: (id: number) => request.get<BillDto<Purchase, PurchaseDetail>>(`/purchase/${id}`),
  save: (data: BillDto<Purchase, PurchaseDetail>) => request.post('/purchase', data),
  update: (data: BillDto<Purchase, PurchaseDetail>) => request.put('/purchase', data),
  remove: (id: number) => request.delete(`/purchase/${id}`),
  confirm: (id: number) => request.post(`/purchase/confirm/${id}`),
  submit: (id: number) => request.post(`/purchase/submit/${id}`)
}

export const distributionReturnApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; siteId?: number }) =>
    request.get<PageResult<DistributionReturn>>('/distribution-return/page', { params }),
  get: (id: number) => request.get<BillDto<DistributionReturn, DistributionReturnDetail>>(`/distribution-return/${id}`),
  save: (data: BillDto<DistributionReturn, DistributionReturnDetail>) => request.post('/distribution-return', data),
  update: (data: BillDto<DistributionReturn, DistributionReturnDetail>) => request.put('/distribution-return', data),
  remove: (id: number) => request.delete(`/distribution-return/${id}`),
  confirm: (id: number) => request.post(`/distribution-return/confirm/${id}`),
  submit: (id: number) => request.post(`/distribution-return/submit/${id}`)
}

export const purchaseReturnApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; siteId?: number }) =>
    request.get<PageResult<PurchaseReturn>>('/purchase-return/page', { params }),
  get: (id: number) => request.get<BillDto<PurchaseReturn, PurchaseReturnDetail>>(`/purchase-return/${id}`),
  save: (data: BillDto<PurchaseReturn, PurchaseReturnDetail>) => request.post('/purchase-return', data),
  update: (data: BillDto<PurchaseReturn, PurchaseReturnDetail>) => request.put('/purchase-return', data),
  remove: (id: number) => request.delete(`/purchase-return/${id}`),
  confirm: (id: number) => request.post(`/purchase-return/confirm/${id}`),
  submit: (id: number) => request.post(`/purchase-return/submit/${id}`)
}

export const distributionCenterApi = {
  list: () => request.get<DistributionCenter[]>('/distribution-center/list')
}

export const deliveryShipmentApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; deliveryCenterId?: number }) =>
    request.get<PageResult<DeliveryShipment>>('/delivery-shipment/page', { params }),
  get: (id: number) => request.get<BillDto<DeliveryShipment, DeliveryShipmentDetail>>(`/delivery-shipment/${id}`),
  save: (data: BillDto<DeliveryShipment, DeliveryShipmentDetail>) => request.post('/delivery-shipment', data),
  update: (data: BillDto<DeliveryShipment, DeliveryShipmentDetail>) => request.put('/delivery-shipment', data),
  remove: (id: number) => request.delete(`/delivery-shipment/${id}`),
  ship: (id: number, data?: Partial<DeliveryShipment>) => request.post(`/delivery-shipment/${id}/ship`, data || {})
}

export const purchaseInboundApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; deliveryCenterId?: number }) =>
    request.get<PageResult<PurchaseInbound>>('/purchase-inbound/page', { params }),
  get: (id: number) => request.get<BillDto<PurchaseInbound, PurchaseInboundDetail>>(`/purchase-inbound/${id}`),
  save: (data: BillDto<PurchaseInbound, PurchaseInboundDetail>) => request.post('/purchase-inbound', data),
  update: (data: BillDto<PurchaseInbound, PurchaseInboundDetail>) => request.put('/purchase-inbound', data),
  remove: (id: number) => request.delete(`/purchase-inbound/${id}`),
  confirm: (id: number) => request.post(`/purchase-inbound/${id}/confirm`),
  submit: (id: number) => request.post(`/purchase-inbound/${id}/submit`)
}

export const warehouseReturnApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; deliveryCenterId?: number }) =>
    request.get<PageResult<WarehouseReturn>>('/warehouse-return/page', { params }),
  get: (id: number) => request.get<BillDto<WarehouseReturn, WarehouseReturnDetail>>(`/warehouse-return/${id}`),
  save: (data: BillDto<WarehouseReturn, WarehouseReturnDetail>) => request.post('/warehouse-return', data),
  update: (data: BillDto<WarehouseReturn, WarehouseReturnDetail>) => request.put('/warehouse-return', data),
  remove: (id: number) => request.delete(`/warehouse-return/${id}`),
  confirm: (id: number) => request.post(`/warehouse-return/${id}/confirm`),
  submit: (id: number) => request.post(`/warehouse-return/${id}/submit`)
}

export const authApi = {
  login: (data: { username: string; password: string }) => request.post<SysUser>('/auth/login', data)
}

export const dashboardApi = {
  overview: (params?: { view?: ViewType; shopId?: number; dcId?: number }) =>
    request.get<DashboardOverview>('/dashboard/overview', { params })
}
