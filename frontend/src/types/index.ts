// add by codebuddy 20260817
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

// add by codebuddy 20260817: 首页大屏数据总览
// add by codebuddy 20260818: 支持 总部(hq)/门店(shop)/配送中心(dc) 视角，区分门店数据(storeData)与配送中心数据(dcData)
export type ViewType = 'hq' | 'shop' | 'dc'

// add by codebuddy 20260818: 单据统计（状态：空-未确认 / Y-已确认 / TJ-已提交 / SC-已上传；未上传 = status=TJ 且 erp_no 为空）
export interface DocStats {
  total: number
  unConfirmed: number
  confirmed: number
  submitted: number
  uploaded: number
  unUploaded: number
}

export interface DashboardOverview {
  view: ViewType
  storeData?: {
    requisition: DocStats
    distribution: DocStats
    purchase: DocStats
    distributionReturn: DocStats
    purchaseReturn: DocStats
  }
  dcData?: {
    deliveryShipment: DocStats
    purchaseInbound: DocStats
    warehouseReturn: DocStats
  }
  taskConfig: {
    intervalMinutes: number
    lastRunAt?: string | null
    nextRunAt?: string | null
    // add by codebuddy 20260818: 定时任务开关（true-启用/false-停用）
    enabled?: boolean
  }
}

// add by codebuddy 20260817: 定时任务配置
// add by codebuddy 20260818: 增加 enabled 开关状态
export interface TaskConfig {
  intervalMinutes: number
  lastRunAt?: string | null
  nextRunAt?: string | null
  enabled?: boolean
}

// add by codebuddy 20260818: 未上传 ERP 单据明细
export interface UnuploadedBill {
  id: number
  billNo: string
  orgId: number
  orgNo: string
  orgName: string
  createDatetime: string
  // add by codebuddy 20260818: 最近一次调用 ERP 接口的时间（传输时间）
  lastTransmitAt?: string
  // add by codebuddy 20260818: 是否终止自动上传：Y-终止
  abortUpload?: string
  errorMessage?: string
}

// add by codebuddy 20260818: 未上传明细支持的单据类型
export type UnuploadedBillType =
  | 'requisition'
  | 'distribution'
  | 'purchase'
  | 'distributionReturn'
  | 'purchaseReturn'
  | 'deliveryShipment'
  | 'purchaseInbound'
  | 'warehouseReturn'

export interface BillDto<H, D> {
  header: H
  details: D[]
}

export interface Shop {
  id?: number
  shopNo: string
  shopName: string
}

export interface Goods {
  id?: number
  itemNo: string
  itemName: string
}

export interface GoodsShop {
  id?: number
  shopId?: number
  itemId?: number
  deliveryType?: number
  price?: number
  deliveryCenterId?: number
  shopName?: string
  itemNo?: string
  itemName?: string
  deliveryCenterNo?: string
  deliveryCenterName?: string
}

// add by codebuddy 20260817: 配送中心
export interface DistributionCenter {
  id?: number
  dcNo: string
  dcName: string
}

// add by codebuddy 20260817: 配送发货单主表
export interface DeliveryShipment {
  id?: number
  shipmentNo?: string
  shipmentDate?: string
  volume?: number
  weight?: number
  erpNo?: string
  // add by codebuddy 20260817: ERP配送单号（发货前必填）
  erpShipmentNo?: string
  // add by codebuddy 20260817: 发货状态：空=未发货，Y=已发货待上传，SC=已上传
  status?: string
  deliveryCenterId?: number
  creator?: string
  createDatetime?: string
  deliveryCenterNo?: string
  deliveryCenterName?: string
  // add by codebuddy 20260818: 收货门店id/名称（上传 ERP 成功后据此生成对应门店的配送收货单）
  siteId?: number
  siteName?: string
}

// add by codebuddy 20260817: 配送发货单明细
export interface DeliveryShipmentDetail {
  id?: number
  shipmentId?: string
  seq?: number
  orderSeq?: number
  itemId?: number
  qty?: number
  lotNo?: string
  itemNo?: string
  itemName?: string
}

export interface Ptemplate {
  id?: number
  ptemplateNo?: string
  ptemplateName?: string
}

export interface PtemplateGoods {
  id?: number
  ptemplateId?: number
  seq?: number
  itemId?: number
  unit?: string
  qty?: number
  itemNo?: string
  itemName?: string
}

export interface PtemplateShop {
  id?: number
  ptemplateId?: number
  seq?: number
  shopId?: number
  shopName?: string
}

export interface PtemplateDto {
  header: Ptemplate
  goodsList: PtemplateGoods[]
  shopList: PtemplateShop[]
}

export interface Requisition {
  id?: number
  requisitionNo?: string
  requisitionDate?: string
  siteId?: number
  templateNo?: number
  demandDate?: string
  creator?: string
  createDatetime?: string
  // add by codebuddy 20260817: 状态：空-未确认 Y-已确认 TJ-已提交 SC-已上传
  status?: string
  isAddPo?: string
  isUrgentOrder?: string
  distributionFee?: number
  weight?: number
  weightUnit?: string
  volume?: number
  volumeUnit?: string
  // add by codebuddy 20260817: ERP单号（多张用|分隔）
  erpNo?: string
  siteName?: string
  // add by codebuddy 20260817: 配送中心id（要货时按门店商品配置表取出）
  deliveryCenterId?: number
  deliveryCenterNo?: string
  deliveryCenterName?: string
}

export interface RequisitionDetail {
  id?: number
  requisitionId?: string
  seq?: number
  itemId?: number
  qty?: number
  unit?: string
  price?: number
  amount?: number
  itemNo?: string
  itemName?: string
}

export interface Distribution {
  id?: number
  distributionNo?: string
  distributionDate?: string
  erpNo?: string
  siteId?: number
  creator?: string
  createDatetime?: string
  // add by codebuddy 20260817: 状态：空-未确认 Y-已确认 TJ-已提交
  status?: string
  siteName?: string
  // add by codebuddy 20260817: 配送中心id
  deliveryCenterId?: number
  deliveryCenterNo?: string
  deliveryCenterName?: string
}

export interface DistributionDetail {
  id?: number
  distributionId?: string
  seq?: number
  itemId?: number
  requisitionQty?: number
  qty?: number
  unit?: string
  price?: number
  amount?: number
  itemNo?: string
  itemName?: string
}

export interface Purchase {
  id?: number
  purchaseNo?: string
  purchaseDate?: string
  erpNo?: string
  siteId?: number
  creator?: string
  createDatetime?: string
  // add by codebuddy 20260817: 状态：空-未确认 Y-已确认 TJ-已提交
  status?: string
  siteName?: string
}

export interface PurchaseDetail {
  id?: number
  purchaseId?: string
  seq?: number
  itemId?: number
  requisitionQty?: number
  qty?: number
  unit?: string
  price?: number
  amount?: number
  itemNo?: string
  itemName?: string
}

export interface DistributionReturn {
  id?: number
  distributionReturnNo?: string
  distributionReturnDate?: string
  erpNo?: string
  siteId?: number
  creator?: string
  createDatetime?: string
  // add by codebuddy 20260817: 状态：空-未确认 Y-已确认 TJ-已提交
  status?: string
  siteName?: string
  // add by codebuddy 20260817: 配送中心id
  deliveryCenterId?: number
  deliveryCenterNo?: string
  deliveryCenterName?: string
  // add by codebuddy 20260817: 来源配送收货单id/单号
  sourceDistributionId?: number
  sourceDistributionNo?: string
}

export interface DistributionReturnDetail {
  id?: number
  distributionReturnId?: string
  seq?: number
  itemId?: number
  receiptQty?: number
  qty?: number
  unit?: string
  price?: number
  amount?: number
  itemNo?: string
  itemName?: string
}

export interface PurchaseReturn {
  id?: number
  purchaseReturnNo?: string
  purchaseReturnDate?: string
  erpNo?: string
  siteId?: number
  creator?: string
  createDatetime?: string
  // add by codebuddy 20260817: 状态：空-未确认 Y-已确认 TJ-已提交
  status?: string
  siteName?: string
}

export interface PurchaseReturnDetail {
  id?: number
  purchaseReturnId?: string
  seq?: number
  itemId?: number
  receiptQty?: number
  qty?: number
  unit?: string
  price?: number
  amount?: number
  itemNo?: string
  itemName?: string
}

// add by codebuddy 20260818: 供应商
export interface Supplier {
  id?: number
  supplierCode: string
  supplierName: string
  // add by codebuddy 20260819: 供应商资料扩展字段
  contact?: string
  phone?: string
  address?: string
  status?: string
  remark?: string
}

// add by codebuddy 20260819: 供应商价格表
export interface SupplierPrice {
  id?: number
  supplierId?: number
  itemId?: number
  price?: number
  status?: string
  remark?: string
  // 列表展示用
  supplierName?: string
  supplierCode?: string
  itemNo?: string
  itemName?: string
}

// add by codebuddy 20260819: 系统用户
export interface SysUser {
  id?: number
  username?: string
  password?: string
  realName?: string
  status?: string
  roles?: string[]            // hq | dc | shop
  shopIds?: number[]         // 可访问门店
  dcIds?: number[]           // 可访问配送中心
  defaultShopId?: number
  defaultDcId?: number
}

// add by codebuddy 20260818: 配送中心采购入库单主表
export interface PurchaseInbound {
  id?: number
  purchaseNo?: string
  purchaseDate?: string
  supplierCode?: string
  deliveryCenterId?: number
  erpNo?: string
  // add by codebuddy 20260818: 状态：空-未确认 Y-已确认 TJ-已提交
  status?: string
  creator?: string
  createDatetime?: string
  supplierName?: string
  deliveryCenterNo?: string
  deliveryCenterName?: string
}

// add by codebuddy 20260818: 配送中心采购入库单明细
export interface PurchaseInboundDetail {
  id?: number
  purchaseId?: string
  seq?: number
  itemId?: number
  unit?: string
  qty?: number
  price?: number
  amount?: number
  itemNo?: string
  itemName?: string
}

// add by codebuddy 20260818: 配送中心采购仓退单主表
export interface WarehouseReturn {
  id?: number
  returnNo?: string
  returnDate?: string
  supplierCode?: string
  deliveryCenterId?: number
  erpNo?: string
  // add by codebuddy 20260818: 状态：空-未确认 Y-已确认 TJ-已提交
  status?: string
  creator?: string
  createDatetime?: string
  supplierName?: string
  deliveryCenterNo?: string
  deliveryCenterName?: string
}

// add by codebuddy 20260818: 配送中心采购仓退单明细
export interface WarehouseReturnDetail {
  id?: number
  returnId?: string
  seq?: number
  itemId?: number
  unit?: string
  qty?: number
  price?: number
  amount?: number
  itemNo?: string
  itemName?: string
}
