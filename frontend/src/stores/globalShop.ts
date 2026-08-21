import { ref } from 'vue'
import type { Shop } from '../types'

// add by codebuddy 20260817: 全局门店维度，右上角选择后各业务单据统一使用
export const currentShopId = ref<number | undefined>(undefined)
export const shopOptions = ref<Shop[]>([])

// add by codebuddy 20260817: 获取当前选中门店的编码（如 SH0001），未选中返回空串
export function currentShopNo() {
  return shopOptions.value.find((s) => s.id === currentShopId.value)?.shopNo ?? ''
}
