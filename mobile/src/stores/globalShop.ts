import { ref } from 'vue'
import type { Shop } from '../types'

// add by codebuddy 20260819: 移动端全局门店维度
export const currentShopId = ref<number | undefined>(undefined)
export const shopOptions = ref<Shop[]>([])

export function currentShopNo() {
  return shopOptions.value.find((s) => s.id === currentShopId.value)?.shopNo ?? ''
}
