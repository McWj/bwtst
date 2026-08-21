<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-select v-model="query.shopId" placeholder="选择门店" clearable style="width: 220px" @change="load">
        <el-option v-for="s in shops" :key="s.id" :label="s.shopName" :value="s.id" />
      </el-select>
      <el-select v-model="query.itemId" placeholder="选择商品" clearable style="width: 220px" @change="load">
        <el-option v-for="g in goods" :key="g.id" :label="g.itemName" :value="g.id" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="success" @click="openDialog()">新增配置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <!-- mark by codebuddy 20260817: 不显示 id 列 <el-table-column prop="id" label="ID" width="70" /> -->
      <el-table-column prop="shopName" label="门店" min-width="140" />
      <el-table-column prop="itemNo" label="商品编号" min-width="120" />
      <el-table-column prop="itemName" label="商品名称" min-width="180" />
      <el-table-column prop="deliveryCenterNo" label="配送中心" min-width="90" />
      <el-table-column prop="deliveryType" label="配送方式" width="110">
        <template #default="{ row }">{{ deliveryTypeText(row.deliveryType) }}</template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="120" align="right" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]"
      @current-change="load"
      @size-change="load"
    />
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑配置' : '新增配置'" width="520px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="门店" prop="shopId">
        <el-select v-model="form.shopId" placeholder="选择门店" style="width: 100%">
          <el-option v-for="s in shops" :key="s.id" :label="s.shopName" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品" prop="itemId">
        <el-select v-model="form.itemId" placeholder="选择商品" style="width: 100%">
          <el-option v-for="g in goods" :key="g.id" :label="`${g.itemNo} - ${g.itemName}`" :value="g.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="配送方式" prop="deliveryType">
        <el-select v-model="form.deliveryType" placeholder="选择配送方式" style="width: 100%">
          <el-option label="统采直送" :value="1" />
          <el-option label="统采配送" :value="2" />
        </el-select>
      </el-form-item>
      <!-- add by codebuddy 20260817: 配送中心（可选） -->
      <el-form-item label="配送中心">
        <el-select v-model="form.deliveryCenterId" placeholder="选择配送中心" clearable style="width: 100%">
          <el-option v-for="d in deliveryCenters" :key="d.id" :label="`${d.dcNo} - ${d.dcName}`" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="价格" prop="price">
        <el-input-number v-model="form.price" :precision="4" :min="0" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { distributionCenterApi, goodsApi, goodsShopApi, shopApi } from '../api'
import type { DistributionCenter, Goods, GoodsShop, Shop } from '../types'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<GoodsShop[]>([])
const total = ref(0)
const shops = ref<Shop[]>([])
const goods = ref<Goods[]>([])
const deliveryCenters = ref<DistributionCenter[]>([])
const query = reactive<{ pageNum: number; pageSize: number; shopId?: number; itemId?: number }>({ pageNum: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<GoodsShop>({})
const rules: FormRules = {
  shopId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  itemId: [{ required: true, message: '请选择商品', trigger: 'change' }]
}

function deliveryTypeText(v?: number) {
  return v === 1 ? '统采直送' : v === 2 ? '统采配送' : '-'
}

async function load() {
  loading.value = true
  try {
    const res: any = await goodsShopApi.page({ ...query })
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.shopId = undefined
  query.itemId = undefined
  query.pageNum = 1
  load()
}

function openDialog(row?: GoodsShop) {
  dialogVisible.value = true
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: undefined, shopId: undefined, itemId: undefined, deliveryType: 1, price: undefined, deliveryCenterId: undefined })
  }
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await goodsShopApi.update({ ...form })
      ElMessage.success('修改成功')
    } else {
      await goodsShopApi.save({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: GoodsShop) {
  await ElMessageBox.confirm(`确认删除「${row.shopName} - ${row.itemName}」配置？`, '提示', { type: 'warning' })
  await goodsShopApi.remove(row.id!)
  ElMessage.success('删除成功')
  load()
}

async function initOptions() {
  const [shopRes, goodsRes, dcRes]: any = await Promise.all([shopApi.list(), goodsApi.list(), distributionCenterApi.list()])
  shops.value = shopRes.data
  goods.value = goodsRes.data
  deliveryCenters.value = dcRes.data
}

onMounted(() => {
  initOptions()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.spacer {
  flex: 1;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
