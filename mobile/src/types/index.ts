// add by codebuddy 20260819: 移动端类型定义（复用后端数据结构）
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export type ViewType = 'hq' | 'shop' | 'dc'

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
    enabled?: boolean
  }
}

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

export interface DistributionCenter {
  id?: number
  dcNo: string
  dcName: string
}

export interface DeliveryShipment {
  id?: number
  shipmentNo?: string
  shipmentDate?: string
  volume?: number
  weight?: number
  erpNo?: string
  erpShipmentNo?: string
  status?: string
  deliveryCenterId?: number
  creator?: string
  createDatetime?: string
  deliveryCenterNo?: string
  deliveryCenterName?: string
  siteId?: number
  siteName?: string
}

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

export interface PtemplateDto {
  header: Ptemplate
  goodsList: PtemplateGoods[]
  shopList: { id?: number; ptemplateId?: number; seq?: number; shopId?: number; shopName?: string }[]
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
  status?: string
  isAddPo?: string
  isUrgentOrder?: string
  distributionFee?: number
  weight?: number
  weightUnit?: string
  volume?: number
  volumeUnit?: string
  erpNo?: string
  siteName?: string
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
  status?: string
  siteName?: string
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
  status?: string
  siteName?: string
  deliveryCenterId?: number
  deliveryCenterNo?: string
  deliveryCenterName?: string
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

export interface SysUser {
  id?: number
  username?: string
  password?: string
  realName?: string
  status?: string
  roles?: string[]
  shopIds?: number[]
  dcIds?: number[]
  defaultShopId?: number
  defaultDcId?: number
}

export interface PurchaseInbound {
  id?: number
  purchaseNo?: string
  purchaseDate?: string
  supplierCode?: string
  deliveryCenterId?: number
  erpNo?: string
  status?: string
  creator?: string
  createDatetime?: string
  supplierName?: string
  deliveryCenterNo?: string
  deliveryCenterName?: string
}

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

export interface WarehouseReturn {
  id?: number
  returnNo?: string
  returnDate?: string
  supplierCode?: string
  deliveryCenterId?: number
  erpNo?: string
  status?: string
  creator?: string
  createDatetime?: string
  supplierName?: string
  deliveryCenterNo?: string
  deliveryCenterName?: string
}

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
