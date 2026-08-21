import { ref } from 'vue'
import type { DistributionCenter } from '../types'

// add by codebuddy 20260817: 右上角视角切换：shop-门店视角，dc-配送中心视角
// add by codebuddy 20260818: 新增 hq-总部视角（基础资料仅在总部视角可见，数据总览总部可见全部数据）
export const currentView = ref<'hq' | 'shop' | 'dc'>('hq')

// add by codebuddy 20260817: 配送中心维度
export const currentDeliveryCenterId = ref<number | undefined>(undefined)
export const deliveryCenterOptions = ref<DistributionCenter[]>([])

// add by codebuddy 20260817: 获取当前选中配送中心的编号（如 BWCS），未选中返回空串
export function currentDeliveryCenterNo() {
  return deliveryCenterOptions.value.find((d) => d.id === currentDeliveryCenterId.value)?.dcNo ?? ''
}
