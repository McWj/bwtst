// add by codebuddy 20260819: 单据元数据配置，供首页/列表/详情复用
export type BillType =
  | 'requisition'
  | 'distribution'
  | 'purchase'
  | 'distributionReturn'
  | 'purchaseReturn'
  | 'deliveryShipment'
  | 'purchaseInbound'
  | 'warehouseReturn'

export interface BillMeta {
  type: BillType
  title: string
  en: string
  // 归属视角：shop-门店视角可见，dc-配送中心视角可见
  view: 'shop' | 'dc'
  // 状态说明
  statusMap: Record<string, string>
}

export const BILL_META: Record<BillType, BillMeta> = {
  requisition: {
    type: 'requisition',
    title: '要货申请',
    en: 'REQUISITION',
    view: 'shop',
    statusMap: { '': '未确认', Y: '已确认', TJ: '已提交', SC: '已上传' }
  },
  distribution: {
    type: 'distribution',
    title: '配送收货',
    en: 'DISTRIBUTION',
    view: 'shop',
    statusMap: { '': '未确认', Y: '已确认', TJ: '已提交', SC: '已上传' }
  },
  purchase: {
    type: 'purchase',
    title: '采购收货',
    en: 'PURCHASE',
    view: 'shop',
    statusMap: { '': '未确认', Y: '已确认', TJ: '已提交', SC: '已上传' }
  },
  distributionReturn: {
    type: 'distributionReturn',
    title: '配送退货',
    en: 'DISTRIBUTION RETURN',
    view: 'shop',
    statusMap: { '': '未确认', Y: '已确认', TJ: '已提交', SC: '已上传' }
  },
  purchaseReturn: {
    type: 'purchaseReturn',
    title: '采购退货',
    en: 'PURCHASE RETURN',
    view: 'shop',
    statusMap: { '': '未确认', Y: '已确认', TJ: '已提交', SC: '已上传' }
  },
  deliveryShipment: {
    type: 'deliveryShipment',
    title: '配送发货',
    en: 'DELIVERY SHIPMENT',
    view: 'dc',
    statusMap: { '': '未发货', Y: '已发货', SC: '已上传' }
  },
  purchaseInbound: {
    type: 'purchaseInbound',
    title: '采购入库',
    en: 'PURCHASE INBOUND',
    view: 'dc',
    statusMap: { '': '未确认', Y: '已确认', TJ: '已提交', SC: '已上传' }
  },
  warehouseReturn: {
    type: 'warehouseReturn',
    title: '采购仓退',
    en: 'WAREHOUSE RETURN',
    view: 'dc',
    statusMap: { '': '未确认', Y: '已确认', TJ: '已提交', SC: '已上传' }
  }
}

export function statusText(type: BillType, status?: string): string {
  return BILL_META[type].statusMap[status ?? ''] ?? '未知'
}

export function statusType(status?: string): 'default' | 'primary' | 'success' | 'warning' | 'danger' {
  if (status === 'SC') return 'success'
  if (status === 'TJ') return 'primary'
  if (status === 'Y') return 'warning'
  if (status === '') return 'default'
  return 'default'
}

// 门店视角单据顺序
export const SHOP_BILL_TYPES: BillType[] = ['requisition', 'distribution', 'purchase', 'distributionReturn', 'purchaseReturn']
// 配送中心视角单据顺序
export const DC_BILL_TYPES: BillType[] = ['deliveryShipment', 'purchaseInbound', 'warehouseReturn']
