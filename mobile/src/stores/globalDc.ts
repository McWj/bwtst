import { ref } from 'vue'
import type { DistributionCenter } from '../types'

// add by codebuddy 20260819: 移动端视角与配送中心维度
export const currentView = ref<'hq' | 'shop' | 'dc'>('hq')
export const currentDeliveryCenterId = ref<number | undefined>(undefined)
export const deliveryCenterOptions = ref<DistributionCenter[]>([])

export function currentDeliveryCenterNo() {
  return deliveryCenterOptions.value.find((d) => d.id === currentDeliveryCenterId.value)?.dcNo ?? ''
}
