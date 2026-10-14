import request from './request'
import type {
  BillDto, DashboardOverview, DeliveryShipment, DeliveryShipmentDetail, Distribution, DistributionCenter,
  DistributionDetail, DistributionReturn, DistributionReturnDetail,
  Goods, GoodsShop, LoginResult, PageResult, Ptemplate, PtemplateDto, Purchase, PurchaseDetail,
  PurchaseInbound, PurchaseInboundDetail, PurchaseReturn, PurchaseReturnDetail,
  Requisition, RequisitionDetail, Shop, Supplier, SupplierPrice, SysUser, TaskConfig, UnuploadedBill, UnuploadedBillType, ViewType,
  WarehouseReturn, WarehouseReturnDetail
} from '../types'

// add by codebuddy 20260817
export const shopApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<PageResult<Shop>>('/shop/page', { params }),
  list: () => request.get<Shop[]>('/shop/list'),
  get: (id: number) => request.get<Shop>(`/shop/${id}`),
  save: (data: Shop) => request.post('/shop', data),
  update: (data: Shop) => request.put('/shop', data),
  remove: (id: number) => request.delete(`/shop/${id}`)
}

export const goodsApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<PageResult<Goods>>('/goods/page', { params }),
  list: () => request.get<Goods[]>('/goods/list'),
  get: (id: number) => request.get<Goods>(`/goods/${id}`),
  save: (data: Goods) => request.post('/goods', data),
  update: (data: Goods) => request.put('/goods', data),
  remove: (id: number) => request.delete(`/goods/${id}`)
}

export const goodsShopApi = {
  page: (params: { pageNum: number; pageSize: number; shopId?: number; itemId?: number }) =>
    request.get<PageResult<GoodsShop>>('/goods-shop/page', { params }),
  list: (shopId?: number) => request.get<GoodsShop[]>('/goods-shop/list', { params: { shopId } }),
  get: (id: number) => request.get<GoodsShop>(`/goods-shop/${id}`),
  save: (data: GoodsShop) => request.post('/goods-shop', data),
  update: (data: GoodsShop) => request.put('/goods-shop', data),
  remove: (id: number) => request.delete(`/goods-shop/${id}`)
}

export const ptemplateApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<PageResult<Ptemplate>>('/ptemplate/page', { params }),
  list: () => request.get<Ptemplate[]>('/ptemplate/list'),
  listByShop: (shopId: number) => request.get<Ptemplate[]>('/ptemplate/listByShop', { params: { shopId } }),
  get: (id: number) => request.get<PtemplateDto>(`/ptemplate/${id}`),
  save: (data: PtemplateDto) => request.post('/ptemplate', data),
  update: (data: PtemplateDto) => request.put('/ptemplate', data),
  remove: (id: number) => request.delete(`/ptemplate/${id}`)
}

export const requisitionApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; siteId?: number }) =>
    request.get<PageResult<Requisition>>('/requisition/page', { params }),
  get: (id: number) => request.get<BillDto<Requisition, RequisitionDetail>>(`/requisition/${id}`),
  save: (data: BillDto<Requisition, RequisitionDetail>) => request.post('/requisition', data),
  update: (data: BillDto<Requisition, RequisitionDetail>) => request.put('/requisition', data),
  remove: (id: number) => request.delete(`/requisition/${id}`),
  // add by codebuddy 20260817: 确认/提交要货单（confirm 可携带 ERP 返回的预估运费/重量/体积）
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
  // add by codebuddy 20260817: 确认/提交配送收货单
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
  // add by codebuddy 20260817: 确认/提交采购收货单
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
  // add by codebuddy 20260817: 确认/提交配送退货单
  confirm: (id: number) => request.post(`/distribution-return/confirm/${id}`),
  submit: (id: number) => request.post(`/distribution-return/submit/${id}`),
  // add by codebuddy 20260817: 可选来源配送收货单列表及明细（新增退货单时选择带出）
  listSourceDistributions: (siteId?: number) =>
    request.get<Distribution[]>('/distribution-return/source-distributions', { params: { siteId } }),
  getSourceDistribution: (id: number) =>
    request.get<BillDto<Distribution, DistributionDetail>>(`/distribution-return/source/${id}`)
}

export const purchaseReturnApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; siteId?: number }) =>
    request.get<PageResult<PurchaseReturn>>('/purchase-return/page', { params }),
  get: (id: number) => request.get<BillDto<PurchaseReturn, PurchaseReturnDetail>>(`/purchase-return/${id}`),
  save: (data: BillDto<PurchaseReturn, PurchaseReturnDetail>) => request.post('/purchase-return', data),
  update: (data: BillDto<PurchaseReturn, PurchaseReturnDetail>) => request.put('/purchase-return', data),
  remove: (id: number) => request.delete(`/purchase-return/${id}`),
  // add by codebuddy 20260817: 确认/提交采购退货单
  confirm: (id: number) => request.post(`/purchase-return/confirm/${id}`),
  submit: (id: number) => request.post(`/purchase-return/submit/${id}`)
}

// add by codebuddy 20260817: 配送中心
export const distributionCenterApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<PageResult<DistributionCenter>>('/distribution-center/page', { params }),
  list: () => request.get<DistributionCenter[]>('/distribution-center/list'),
  get: (id: number) => request.get<DistributionCenter>(`/distribution-center/${id}`),
  save: (data: DistributionCenter) => request.post('/distribution-center', data),
  update: (data: DistributionCenter) => request.put('/distribution-center', data),
  remove: (id: number) => request.delete(`/distribution-center/${id}`)
}

// add by codebuddy 20260817: 配送发货单
export const deliveryShipmentApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; deliveryCenterId?: number }) =>
    request.get<PageResult<DeliveryShipment>>('/delivery-shipment/page', { params }),
  get: (id: number) => request.get<BillDto<DeliveryShipment, DeliveryShipmentDetail>>(`/delivery-shipment/${id}`),
  save: (data: BillDto<DeliveryShipment, DeliveryShipmentDetail>) => request.post('/delivery-shipment', data),
  update: (data: BillDto<DeliveryShipment, DeliveryShipmentDetail>) => request.put('/delivery-shipment', data),
  remove: (id: number) => request.delete(`/delivery-shipment/${id}`),
  // add by codebuddy 20260817: 配送发货（需已录入 ERP 配送单号），body 可带 erpShipmentNo/volume/weight
  ship: (id: number, data?: Partial<DeliveryShipment>) => request.post(`/delivery-shipment/${id}/ship`, data || {})
}

// add by codebuddy 20260818: 供应商下拉
export const supplierApi = {
  // add by codebuddy 20260819: 改为下拉接口（兼容 WarehouseReturn/PurchaseInbound 的下拉加载）
  list: () => request.get<Supplier[]>('/supplier/options'),
  options: () => request.get<Supplier[]>('/supplier/options'),
  page: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<PageResult<Supplier>>('/supplier/page', { params }),
  get: (id: number) => request.get<Supplier>(`/supplier/${id}`),
  save: (data: Supplier) => request.post('/supplier/save', data),
  remove: (id: number) => request.delete(`/supplier/${id}`)
}

// add by codebuddy 20260819: 供应商价格表
export const supplierPriceApi = {
  page: (params: { pageNum: number; pageSize: number; supplierId?: number; itemId?: number }) =>
    request.get<PageResult<SupplierPrice>>('/supplier-price/page', { params }),
  get: (id: number) => request.get<SupplierPrice>(`/supplier-price/${id}`),
  save: (data: SupplierPrice) => request.post('/supplier-price/save', data),
  remove: (id: number) => request.delete(`/supplier-price/${id}`)
}

// add by codebuddy 20260819: 登录
export const authApi = {
  // add by codebuddy 20260923: 支持企业编码，返回令牌 + 用户信息
  login: (data: { username: string; password: string; tenantCode?: string }) =>
    request.post<LoginResult>('/auth/login', data)
}


// add by codebuddy 20260819: 用户管理
export const userApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<PageResult<SysUser>>('/user/page', { params }),
  get: (id: number) => request.get<SysUser>(`/user/${id}`),
  save: (data: SysUser) => request.post('/user/save', data),
  remove: (id: number) => request.delete(`/user/${id}`),
  generateShopUsers: () => request.post<string>('/user/generate-shop'),
  resetPassword: (id: number, password: string) => request.post(`/user/reset-password/${id}`, { password })
}

// add by codebuddy 20260818: 配送中心采购入库单
export const purchaseInboundApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; deliveryCenterId?: number }) =>
    request.get<PageResult<PurchaseInbound>>('/purchase-inbound/page', { params }),
  get: (id: number) => request.get<BillDto<PurchaseInbound, PurchaseInboundDetail>>(`/purchase-inbound/${id}`),
  save: (data: BillDto<PurchaseInbound, PurchaseInboundDetail>) => request.post('/purchase-inbound', data),
  update: (data: BillDto<PurchaseInbound, PurchaseInboundDetail>) => request.put('/purchase-inbound', data),
  remove: (id: number) => request.delete(`/purchase-inbound/${id}`),
  // add by codebuddy 20260818: 确认/提交采购入库单
  confirm: (id: number) => request.post(`/purchase-inbound/${id}/confirm`),
  submit: (id: number) => request.post(`/purchase-inbound/${id}/submit`)
}

// add by codebuddy 20260818: 配送中心采购仓退单
export const warehouseReturnApi = {
  page: (params: { pageNum: number; pageSize: number; keyword?: string; deliveryCenterId?: number }) =>
    request.get<PageResult<WarehouseReturn>>('/warehouse-return/page', { params }),
  get: (id: number) => request.get<BillDto<WarehouseReturn, WarehouseReturnDetail>>(`/warehouse-return/${id}`),
  save: (data: BillDto<WarehouseReturn, WarehouseReturnDetail>) => request.post('/warehouse-return', data),
  update: (data: BillDto<WarehouseReturn, WarehouseReturnDetail>) => request.put('/warehouse-return', data),
  remove: (id: number) => request.delete(`/warehouse-return/${id}`),
  // add by codebuddy 20260818: 确认/提交采购仓退单
  confirm: (id: number) => request.post(`/warehouse-return/${id}/confirm`),
  submit: (id: number) => request.post(`/warehouse-return/${id}/submit`)
}

// add by codebuddy 20260817: 首页大屏数据总览
// add by codebuddy 20260818: overview 支持 视角(view)+门店(shopId)+配送中心(dcId) 参数；task-config 支持 enabled 开关
export const dashboardApi = {
  overview: (params?: { view?: ViewType; shopId?: number; dcId?: number }) =>
    request.get<DashboardOverview>('/dashboard/overview', { params }),
  // add by codebuddy 20260818: 未上传 ERP 单据明细（含 ERP 报错）
  unuploadedDetail: (params: { type: UnuploadedBillType; view?: ViewType; shopId?: number; dcId?: number }) =>
    request.get<UnuploadedBill[]>('/dashboard/unuploaded-detail', { params }),
  // add by codebuddy 20260818: 终止单据自动上传（终止后不再出现在未上传列表）
  abortUpload: (type: UnuploadedBillType, id: number) =>
    request.post('/dashboard/abort-upload', null, { params: { type, id } }),
  getTaskConfig: () => request.get<TaskConfig>('/dashboard/task-config'),
  updateTaskConfig: (intervalMinutes: number, enabled?: boolean) =>
    request.put<TaskConfig>('/dashboard/task-config', { intervalMinutes, enabled })
}
