<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="模板编号/名称" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="success" @click="openDialog()">新增模板</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <!-- mark by codebuddy 20260817: 不显示 id 列 <el-table-column prop="id" label="ID" width="70" /> -->
      <el-table-column prop="ptemplateNo" label="模板编号" min-width="130" />
      <el-table-column prop="ptemplateName" label="模板名称" min-width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="view(row)">明细</el-button>
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

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" destroy-on-close top="4vh">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="模板编号">
            <el-input v-model="form.ptemplateNo" :disabled="!!form.id" placeholder="留空自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="模板名称" prop="ptemplateName">
            <el-input v-model="form.ptemplateName" placeholder="请输入模板名称" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">商品明细</el-divider>
    <div class="detail-toolbar">
      <el-button type="primary" size="small" @click="addGoodsRow">添加商品</el-button>
      <el-select v-model="addGoodsId" placeholder="从商品库选择" filterable clearable style="width: 240px">
        <el-option v-for="g in goodsOptions" :key="g.id" :label="`${g.itemNo} - ${g.itemName}`" :value="g.id" />
      </el-select>
      <el-button size="small" :disabled="!addGoodsId" @click="appendGoodsById">加入</el-button>
    </div>
    <el-table :data="goodsList" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="itemNo" label="商品编号" min-width="120" />
      <el-table-column prop="itemName" label="商品名称" min-width="160">
        <template #default="{ row }">
          <el-select v-model="row.itemId" placeholder="选择商品" filterable size="small" @change="(v: number) => onGoodsChange(row, v)">
            <el-option v-for="g in goodsOptions" :key="g.id" :label="`${g.itemNo} - ${g.itemName}`" :value="g.id" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="90">
        <template #default="{ row }"><el-input v-model="row.unit" size="small" /></template>
      </el-table-column>
      <el-table-column prop="qty" label="数量" width="120">
        <template #default="{ row }"><el-input-number v-model="row.qty" :precision="3" :min="0" size="small" style="width: 100%" /></template>
      </el-table-column>
      <el-table-column label="操作" width="70">
        <template #default="{ $index }">
          <el-button link type="danger" @click="goodsList.splice($index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">门店明细</el-divider>
    <div class="detail-toolbar">
      <el-button type="primary" size="small" @click="addShopRow">添加门店</el-button>
    </div>
    <el-table :data="shopList" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="shopName" label="门店名称" min-width="200">
        <template #default="{ row }">
          <el-select v-model="row.shopId" placeholder="选择门店" filterable size="small" @change="(v: number) => onShopChange(row, v)">
            <el-option v-for="s in shopOptions" :key="s.id" :label="s.shopName" :value="s.id" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70">
        <template #default="{ $index }">
          <el-button link type="danger" @click="shopList.splice($index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="viewVisible" title="模板明细" width="820px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="模板编号">{{ viewData.ptemplateNo }}</el-descriptions-item>
      <el-descriptions-item label="模板名称">{{ viewData.ptemplateName }}</el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">商品明细</el-divider>
    <el-table :data="viewGoods" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="itemNo" label="商品编号" min-width="110" />
      <el-table-column prop="itemName" label="商品名称" min-width="150" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="qty" label="数量" width="100" align="right" />
    </el-table>
    <el-divider content-position="left">门店明细</el-divider>
    <el-table :data="viewShops" border size="small">
      <el-table-column type="index" label="项次" width="60" />
      <el-table-column prop="shopName" label="门店名称" min-width="200" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { goodsApi, ptemplateApi, shopApi } from '../api'
import type { Goods, Ptemplate, PtemplateDto, PtemplateGoods, PtemplateShop, Shop } from '../types'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<Ptemplate[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const viewVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Ptemplate>({})
const goodsList = ref<PtemplateGoods[]>([])
const shopList = ref<PtemplateShop[]>([])
const goodsOptions = ref<Goods[]>([])
const shopOptions = ref<Shop[]>([])
const addGoodsId = ref<number>()
const viewData = reactive<Ptemplate>({})
const viewGoods = ref<PtemplateGoods[]>([])
const viewShops = ref<PtemplateShop[]>([])
const rules: FormRules = {
  ptemplateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}
const dialogTitle = computed(() => (form.id ? '编辑模板' : '新增模板'))

async function load() {
  loading.value = true
  try {
    const res: any = await ptemplateApi.page({ ...query })
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.pageNum = 1
  load()
}

async function openDialog(row?: Ptemplate) {
  await initOptions()
  dialogVisible.value = true
  goodsList.value = []
  shopList.value = []
  addGoodsId.value = undefined
  if (row) {
    const res: any = await ptemplateApi.get(row.id!)
    Object.assign(form, res.data.header)
    goodsList.value = res.data.goodsList || []
    shopList.value = res.data.shopList || []
  } else {
    Object.assign(form, { id: undefined, ptemplateNo: '', ptemplateName: '' })
  }
}

async function view(row: Ptemplate) {
  const res: any = await ptemplateApi.get(row.id!)
  Object.assign(viewData, res.data.header)
  viewGoods.value = res.data.goodsList || []
  viewShops.value = res.data.shopList || []
  viewVisible.value = true
}

function addGoodsRow() {
  goodsList.value.push({ itemId: undefined, unit: '', qty: 0 })
}

function appendGoodsById() {
  if (!addGoodsId.value) return
  const g = goodsOptions.value.find((x) => x.id === addGoodsId.value)
  if (g && !goodsList.value.some((x) => x.itemId === g.id)) {
    goodsList.value.push({ itemId: g.id, itemNo: g.itemNo, itemName: g.itemName, unit: '', qty: 0 })
  }
  addGoodsId.value = undefined
}

function onGoodsChange(row: PtemplateGoods, v: number) {
  const g = goodsOptions.value.find((x) => x.id === v)
  if (g) {
    row.itemNo = g.itemNo
    row.itemName = g.itemName
  }
}

function addShopRow() {
  shopList.value.push({ shopId: undefined })
}

function onShopChange(row: PtemplateShop, v: number) {
  const s = shopOptions.value.find((x) => x.id === v)
  if (s) {
    row.shopName = s.shopName
  }
}

async function submit() {
  await formRef.value?.validate()
  if (!goodsList.value.length && !shopList.value.length) {
    ElMessage.warning('请至少添加一条商品或门店明细')
    return
  }
  saving.value = true
  try {
    const dto: PtemplateDto = { header: { ...form }, goodsList: [...goodsList.value], shopList: [...shopList.value] }
    if (form.id) {
      await ptemplateApi.update(dto)
      ElMessage.success('修改成功')
    } else {
      await ptemplateApi.save(dto)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Ptemplate) {
  await ElMessageBox.confirm(`确认删除模板「${row.ptemplateName}」？`, '提示', { type: 'warning' })
  await ptemplateApi.remove(row.id!)
  ElMessage.success('删除成功')
  load()
}

async function initOptions() {
  const [goodsRes, shopRes]: any = await Promise.all([goodsApi.list(), shopApi.list()])
  goodsOptions.value = goodsRes.data
  shopOptions.value = shopRes.data
}

onMounted(load)
</script>

<style scoped>
.toolbar,
.detail-toolbar {
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
