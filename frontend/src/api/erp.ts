// add by codebuddy 20260817: ERP 报文接口封装，统一构建 digi-header / digi-service / digi-key / digi-datakey
import axios from 'axios'

export interface ErpRequestOptions {
  /** 请求地址，如 /wtoptst/ws/r/awsp920 */
  url: string
  /** 服务名称，如 credit.query */
  service: string
  /** 门店编码，作为 digi-datakey.CompanyId 与报文 customer */
  shopNo: string
  /** digi-body.std_data 内容，如 { parameter: { transfer: { customer } } } */
  stdData?: Record<string, unknown>
  /** 超时时间(ms)，默认 15000 */
  timeout?: number
}

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

// 动态生成 17 位时间戳 yyyyMMddHHmmssSSS
function formatTimestamp(d: Date) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}${String(d.getMilliseconds()).padStart(3, '0')}`
}

// 构建 ERP 完整报文
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

// 发送 ERP 请求，成功(execution.code === '0')时返回 digi-body.std_data，失败抛异常
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
  // add by codebuddy 20260820: ERP 失败时优先展示 description 中的业务错误说明
  const errMsg = execution?.description || execution?.message || `ERP 接口调用失败(${options.service})`
  throw new Error(errMsg)
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

// add by codebuddy 20260817: 要货单确认(callType=2,status=0)/提交(callType=0,status=2)，与查询额度共用 url
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
