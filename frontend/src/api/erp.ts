// add by codebuddy 20260817: ERP 接口封装
// add by codebuddy 20260923: SaaS 改造——前端不再直连 ERP、也不再自行构建报文。
// 浏览器端无法感知当前租户应使用哪个 ERP 账套（代理目标是服务器层静态配置），
// 因此统一改走后端代理 /api/erp/*，由后端按当前租户的账套配置转发。
import request from './request'

// add by codebuddy 20260820: ERP 错误信息也可能放在 description 字段
export interface ErpExecution {
  code: string
  message?: string
  description?: string
}

export interface ErpStdData<T = Record<string, unknown>> {
  execution?: ErpExecution
  parameter?: T
  [key: string]: unknown
}

// add by codebuddy 20260817: ERP 要货单报文明细行
export interface RequisitionErpDetail {
  seq: number
  item_no: string
  base_unit: string
  base_qty: number
  price: number
  amount: number
  packing_unit: string
  packing_qty: number
  distri_price: number
  distri_amount: number
}

// add by codebuddy 20260817: ERP 要货单报文 requisition 节点
export interface RequisitionErpParams {
  version: string
  creator: string
  create_datetime: string
  modify_no: string
  modify_datetime: string
  approve_no: string
  approve_datetime: string
  status: string
  site_no: string
  requisition_date: string
  create_date: string
  po_template_no: string
  is_add_po: string
  is_urgent_order: string
  remark: string
  front_no: string
  callType: string
  oType: string
  requisition_detail: RequisitionErpDetail[]
}

// add by codebuddy 20260817: ERP 要货单确认返回的预估运费/重量/体积等
export interface RequisitionDeductResult {
  doc_no?: string
  org_no?: string
  distributionFee?: number
  remaining_amount?: number | null
  esShipFee?: number
  esWeight?: number
  weightUnit?: string
  esVolume?: number
  VolumeUnit?: string
}

/** 门店可用额度查询（后端代理 ERP credit.query） */
export async function creditQuery(shopNo: string): Promise<ErpStdData<{ remaining_amount?: number }>> {
  const res: any = await request.post('/erp/credit-query', { shopNo })
  return res.data
}

/**
 * 要货单确认(callType=2,status=0)/提交(callType=0,status=2)
 * add by codebuddy 20260923: 由后端按当前租户的账套配置转发
 */
export async function requisitionCreditDeduct(options: {
  shopNo: string
  requisition: RequisitionErpParams
}): Promise<ErpStdData<RequisitionDeductResult>> {
  const res: any = await request.post('/erp/requisition-credit-deduct', {
    shopNo: options.shopNo,
    requisition: options.requisition
  })
  return res.data
}
