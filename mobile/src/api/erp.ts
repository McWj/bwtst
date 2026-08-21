// add by codebuddy 20260819: ERP 报文接口封装（与桌面端一致）
import axios from 'axios'

export interface ErpRequestOptions {
  url: string
  service: string
  shopNo: string
  stdData?: Record<string, unknown>
  timeout?: number
}

export interface ErpExecution {
  code: string
  message?: string
  // add by codebuddy 20260820: ERP 错误描述字段
  description?: string
}

export interface ErpStdData<T = Record<string, unknown>> {
  execution?: ErpExecution
  parameter?: T
  [key: string]: unknown
}

function formatTimestamp(d: Date) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}${String(d.getMilliseconds()).padStart(3, '0')}`
}

export function buildErpPayload(options: ErpRequestOptions) {
  const { service, shopNo, stdData = {} } = options
  return {
    'digi-header': {
      'digi-type': 'sync',
      'digi-host': {
        prod: 'POSSERVER1', ver: '1.0', ip: '192.168.0.1', id: 'postst', lang: 'zh_CN',
        timestamp: formatTimestamp(new Date()), acct: 'tiptop'
      },
      'digi-service': {
        prod: 'T100', name: service, srvver: '1.0', ip: '192.0.0.1', id: 'topprd'
      },
      'digi-key': '85d966d2f533fca68f83da455edabcbe',
      'digi-datakey': { EntId: '10', CompanyId: shopNo }
    },
    'digi-body': {
      std_data: stdData
    }
  }
}

// add by codebuddy 20260820: ERP 错误信息也可能放在 description 字段
export async function requestErp<T = Record<string, unknown>>(options: ErpRequestOptions): Promise<ErpStdData<T>> {
  const resp = await axios.post(options.url, buildErpPayload(options), { timeout: options.timeout ?? 15000 })
  const stdData = resp.data?.['digi-body']?.std_data as ErpStdData<T> | undefined
  const execution = stdData?.execution
  if (execution?.code === '0') {
    if (!stdData) {
      throw new Error(`ERP 接口响应为空(${options.service})`)
    }
    return stdData
  }
  const errMsg = execution?.description || execution?.message || `ERP 接口调用失败(${options.service})`
  throw new Error(errMsg)
}

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

export async function requisitionCreditDeduct(options: {
  url: string
  shopNo: string
  requisition: RequisitionErpParams
  timeout?: number
}): Promise<ErpStdData<RequisitionDeductResult>> {
  return requestErp<RequisitionDeductResult>({
    url: options.url,
    service: 'requisition.credit.deduct',
    shopNo: options.shopNo,
    stdData: { parameter: { requisition: options.requisition } },
    timeout: options.timeout
  })
}
