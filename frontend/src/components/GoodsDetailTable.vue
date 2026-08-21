<template>
  <div>
    <div class="detail-toolbar">
      <el-button type="primary" size="small" @click="emit('add')">添加明细</el-button>
    </div>
    <el-table :data="rows" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="itemNo" label="商品编号" min-width="110" />
      <!-- add by codebuddy 20260817: 改为虚拟滚动下拉，商品数量大时不卡 -->
      <el-table-column label="商品名称" min-width="180">
        <template #default="{ row }">
          <el-select-v2
            v-model="row.itemId"
            :options="goodsSelectOptions"
            placeholder="选择商品"
            filterable
            size="small"
            style="width: 100%"
            @change="(v: number) => onGoodsChange(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column v-if="showRequisitionQty" prop="requisitionQty" label="要货数量" width="110" align="right">
        <template #default="{ row }"><el-input-number v-model="row.requisitionQty" :precision="3" :min="0" size="small" style="width: 100%" /></template>
      </el-table-column>
      <el-table-column v-if="showReceiptQty" prop="receiptQty" label="收数量" width="110" align="right">
        <template #default="{ row }"><el-input-number v-model="row.receiptQty" :precision="3" :min="0" size="small" style="width: 100%" /></template>
      </el-table-column>
      <el-table-column :label="qtyLabel" width="120" align="right">
        <template #default="{ row }">
          <el-input-number v-model="row.qty" :precision="3" :min="0" size="small" style="width: 100%" @change="calcAmount(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="90">
        <template #default="{ row }"><el-input v-model="row.unit" size="small" /></template>
      </el-table-column>
      <el-table-column label="单价" width="120" align="right">
        <template #default="{ row }">
          <el-input-number v-model="row.price" :precision="4" :min="0" size="small" style="width: 100%" @change="calcAmount(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="110" align="right" />
      <el-table-column label="操作" width="70">
        <template #default="{ $index }">
          <el-button link type="danger" @click="emit('remove', $index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
// add by codebuddy 20260817
import { computed } from 'vue'

export interface GoodsRow {
  itemId?: number
  itemNo?: string
  itemName?: string
  qty?: number
  requisitionQty?: number
  receiptQty?: number
  unit?: string
  price?: number
  amount?: number
}

const props = defineProps<{
  rows: GoodsRow[]
  goodsOptions: { id?: number; itemNo?: string; itemName?: string }[]
  qtyLabel?: string
  showRequisitionQty?: boolean
  showReceiptQty?: boolean
}>()

const emit = defineEmits<{
  (e: 'add'): void
  (e: 'remove', index: number): void
}>()

// add by codebuddy 20260817: 虚拟滚动下拉选项（value 为商品ID，label 为 编号 - 名称）
const goodsSelectOptions = computed(() =>
  props.goodsOptions.map((g) => ({ value: g.id, label: `${g.itemNo} - ${g.itemName}` }))
)

function onGoodsChange(row: GoodsRow, v: number) {
  const g = props.goodsOptions.find((x) => x.id === v)
  if (g) {
    row.itemNo = g.itemNo
    row.itemName = g.itemName
  }
  calcAmount(row)
}

function calcAmount(row: GoodsRow) {
  row.amount = (Number(row.qty || 0) * Number(row.price || 0))
}
</script>

<style scoped>
.detail-toolbar {
  margin-bottom: 10px;
}
</style>
