<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="发货单号/ERP单号/创建人" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <!-- add by codebuddy 20260817: 右上角统一配送中心维度，未选择配送中心时禁止新增 -->
      <el-tooltip :disabled="!!currentDeliveryCenterId" content="请先在右上角选择配送中心" placement="top">
        <span>
          <el-button type="success" :disabled="!currentDeliveryCenterId" @click="openDialog()">新增配送发货</el-button>
        </span>
      </el-tooltip>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="shipmentNo" label="发货单号" min-width="180" />
      <el-table-column prop="deliveryCenterNo" label="配送中心" min-width="100" />
      <!-- add by codebuddy 20260818: 收货门店列 -->
      <el-table-column prop="siteName" label="收货门店" min-width="110" />
      <el-table-column prop="shipmentDate" label="发货日期" width="110" />
      <el-table-column prop="volume" label="发货体积" width="110" align="right" />
      <el-table-column prop="weight" label="发货重量" width="110" align="right" />
      <el-table-column prop="erpShipmentNo" label="ERP配送单号" min-width="130" />
      <el-table-column prop="erpNo" label="ERP发货单号" min-width="120" />
      <!-- add by codebuddy 20260817: 发货状态列 -->
      <el-table-column prop="status" label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="creator" label="创建人" width="90" />
      <el-table-column prop="createDatetime" label="创建时间" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="view(row)">明细</el-button>
          <el-button link type="primary" :disabled="!!row.status" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" :disabled="!!row.status" @click="remove(row)">删除</el-button>
          <!-- add by codebuddy 20260817: 未发货时可点击发货 -->
          <el-button link type="success" :disabled="!!row.status" @click="openShip(row)">发货</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑配送发货单' : '新增配送发货单'" width="1000px" top="3vh" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="发货单号">
            <el-input v-model="form.shipmentNo" :disabled="!!form.id" placeholder="留空自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="发货日期" prop="shipmentDate">
            <el-date-picker v-model="form.shipmentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <!-- add by codebuddy 20260818: 收货门店必选，上传 ERP 成功后据此生成对应门店的配送收货单 -->
          <el-form-item label="收货门店" prop="siteId">
            <el-select v-model="form.siteId" filterable placeholder="选择收货门店" style="width: 100%">
              <el-option v-for="s in shopOptions" :key="s.id" :label="s.shopName" :value="s.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <!-- add by codebuddy 20260817: ERP配送单号，发货前必填 -->
          <el-form-item label="ERP配送单号" prop="erpShipmentNo">
            <el-input v-model="form.erpShipmentNo" placeholder="发货前必须录入" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="ERP单号">
            <el-input v-model="form.erpNo" placeholder="上传后自动回填" :disabled="true" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="发货体积">
            <el-input-number v-model="form.volume" :min="0" :precision="4" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="发货重量">
            <el-input-number v-model="form.weight" :min="0" :precision="4" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="创建人">
            <el-input v-model="form.creator" placeholder="创建人" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">发货明细（同一项次可多批次，项序依次为1、2…）</el-divider>
    <el-table :data="details" border size="small">
      <!-- add by codebuddy 20260909: 编辑已保存单据时项次/项序/商品直接展示文本，不显示控件外观 -->
      <el-table-column label="项次" width="80">
        <template #default="{ row }">
          <span v-if="!form.id">
            <el-input-number v-model="row.seq" :min="1" size="small" controls-position="right" style="width: 100%" />
          </span>
          <span v-else>{{ row.seq }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项序" width="80">
        <template #default="{ row }">
          <span v-if="!form.id">
            <el-input-number v-model="row.orderSeq" :min="1" size="small" controls-position="right" style="width: 100%" />
          </span>
          <span v-else>{{ row.orderSeq }}</span>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="240">
        <template #default="{ row }">
          <!-- add by codebuddy 20260909: 新增时可选择商品；编辑已保存单据直接展示商品文本 -->
          <span v-if="!form.id">
            <el-select v-model="row.itemId" filterable placeholder="选择商品" style="width: 100%" @change="onItemChange(row)">
              <el-option v-for="g in goodsOptions" :key="g.id" :label="`${g.itemNo}(${g.itemName})`" :value="g.id" />
            </el-select>
          </span>
          <span v-else>{{ row.itemNo }}({{ row.itemName }})</span>
        </template>
      </el-table-column>
      <el-table-column label="发货数量" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.qty" :min="0" :precision="4" size="small" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="批号" width="140">
        <template #default="{ row }">
          <el-input v-model="row.lotNo" size="small" placeholder="批号" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row, $index }">
          <!-- add by codebuddy 20260909: 加批次=同一项次新增一行批次；删除保留该商品至少一条批次 -->
          <el-button link type="primary" size="small" @click="addBatch(row)">加批次</el-button>
          <el-button link type="danger" size="small" @click="removeDetail(row, $index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 8px; display: flex; align-items: center; gap: 10px">
      <!-- add by codebuddy 20260909: 已保存单据禁止新增商品，仅可对各商品加批次 -->
      <el-button v-if="!form.id" size="small" type="primary" plain @click="addDetail">添加明细</el-button>
      <span v-else class="lock-tip">已保存单据的商品清单已锁定，仅可在各商品行【加批次】增加发货批次</span>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="viewVisible" title="配送发货单明细" width="920px">
    <el-descriptions :column="3" border size="small">
      <el-descriptions-item label="发货单号">{{ viewData.shipmentNo }}</el-descriptions-item>
      <el-descriptions-item label="配送中心">{{ viewData.deliveryCenterName || viewData.deliveryCenterNo }}</el-descriptions-item>
      <!-- add by codebuddy 20260818: 收货门店展示 -->
      <el-descriptions-item label="收货门店">{{ viewData.siteName || (viewData.siteId ? shopName(viewData.siteId) : '') }}</el-descriptions-item>
      <el-descriptions-item label="发货日期">{{ viewData.shipmentDate }}</el-descriptions-item>
      <el-descriptions-item label="发货体积">{{ viewData.volume }}</el-descriptions-item>
      <el-descriptions-item label="发货重量">{{ viewData.weight }}</el-descriptions-item>
      <el-descriptions-item label="ERP配送单号">{{ viewData.erpShipmentNo }}</el-descriptions-item>
      <el-descriptions-item label="ERP单号">{{ viewData.erpNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusType(viewData.status)" size="small">{{ statusText(viewData.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="创建人">{{ viewData.creator }}</el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">发货明细</el-divider>
    <el-table :data="viewDetails" border size="small">
      <el-table-column prop="seq" label="项次" width="60" align="center" />
      <el-table-column prop="orderSeq" label="项序" width="70" align="center" />
      <el-table-column prop="itemNo" label="商品编号" min-width="110" />
      <el-table-column prop="itemName" label="商品名称" min-width="160" />
      <el-table-column prop="qty" label="发货数量" width="100" align="right" />
      <el-table-column prop="lotNo" label="批号" min-width="110" />
    </el-table>
  </el-dialog>

  <!-- add by codebuddy 20260817: 发货确认弹窗，校验 ERP 配送单号 -->
  <el-dialog v-model="shipVisible" title="配送发货" width="520px" destroy-on-close>
    <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="110px">
      <el-form-item label="发货单号">
        <el-input :model-value="shipForm.shipmentNo" disabled />
      </el-form-item>
      <el-form-item label="ERP配送单号" prop="erpShipmentNo">
        <el-input v-model="shipForm.erpShipmentNo" placeholder="发货前必须录入 ERP 配送单号" />
      </el-form-item>
      <el-form-item label="发货体积">
        <el-input-number v-model="shipForm.volume" :min="0" :precision="4" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-form-item label="发货重量">
        <el-input-number v-model="shipForm.weight" :min="0" :precision="4" controls-position="right" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="shipVisible = false">取消</el-button>
      <el-button type="primary" :loading="shiping" @click="doShip">确认发货</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { deliveryShipmentApi, goodsApi, shopApi } from '../api'
import type { DeliveryShipment, DeliveryShipmentDetail, Goods } from '../types'
import { currentDeliveryCenterId, currentDeliveryCenterNo } from '../stores/globalDc'
// add by codebuddy 20260818: 收货门店下拉数据源
import { shopOptions } from '../stores/globalShop'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<DeliveryShipment[]>([])
const total = ref(0)
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; deliveryCenterId?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const viewVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<DeliveryShipment>({})
const details = ref<DeliveryShipmentDetail[]>([])
const goodsOptions = ref<Goods[]>([])
const viewData = reactive<DeliveryShipment>({})
const viewDetails = ref<DeliveryShipmentDetail[]>([])
const rules: FormRules = {
  shipmentDate: [{ required: true, message: '请选择发货日期', trigger: 'change' }],
  // add by codebuddy 20260818: 收货门店必填
  siteId: [{ required: true, message: '请选择收货门店', trigger: 'change' }],
  // add by codebuddy 20260817: ERP配送单号，发货前必填（保存时允许为空，发货时必须已录入）
  erpShipmentNo: []
}
// add by codebuddy 20260817: 发货确认弹窗
const shipVisible = ref(false)
const shiping = ref(false)
const shipFormRef = ref<FormInstance>()
const shipForm = reactive<DeliveryShipment>({})
const shipRules: FormRules = {
  erpShipmentNo: [{ required: true, message: '请录入 ERP 配送单号', trigger: 'blur' }]
}

// add by codebuddy 20260817: 发货状态展示：空=未发货，Y=已发货待上传，SC=已上传
function statusText(status?: string) {
  if (!status) return '未发货'
  if (status === 'Y') return '已发货待上传'
  if (status === 'SC') return '已上传'
  return status
}
function statusType(status?: string) {
  if (!status) return 'info'
  if (status === 'Y') return 'warning'
  if (status === 'SC') return 'success'
  return 'info'
}

// add by codebuddy 20260817: 右上角切换配送中心后自动刷新当前单据列表
watch(currentDeliveryCenterId, (val) => {
  query.deliveryCenterId = val
  query.pageNum = 1
  load()
})

async function load() {
  // add by codebuddy 20260817: 未选择配送中心时不查询任何数据
  if (!query.deliveryCenterId) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res: any = await deliveryShipmentApi.page({ ...query })
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

// add by codebuddy 20260909: 新增商品行使用新的项次(seq=最大项次+1)，避免与已有商品共用项次
function addDetail() {
  const maxSeq = details.value.reduce((m, d) => Math.max(m, d.seq ?? 0), 0)
  details.value.push({ seq: maxSeq + 1, orderSeq: 1, itemId: undefined, qty: 0, lotNo: '' })
}

// add by codebuddy 20260909: 在当前商品项次行之后插入同一项次、项序+1 的新批次
function addBatch(row: DeliveryShipmentDetail) {
  const sameSeq = details.value.filter((d) => d.seq === row.seq)
  const maxOrderSeq = sameSeq.reduce((m, d) => Math.max(m, d.orderSeq ?? 0), 0)
  const idx = details.value.indexOf(row)
  details.value.splice(idx + 1, 0, {
    seq: row.seq,
    orderSeq: maxOrderSeq + 1,
    itemId: row.itemId,
    itemNo: row.itemNo,
    itemName: row.itemName,
    qty: 0,
    lotNo: ''
  })
}

// add by codebuddy 20260909: 删除单条批次；若该商品只剩一条批次则禁止删除，保证商品不丢失
function removeDetail(row: DeliveryShipmentDetail, index: number) {
  const sameSeq = details.value.filter((d) => d.seq === row.seq)
  if (sameSeq.length <= 1) {
    ElMessage.warning('该商品至少保留一个批次，不能删除')
    return
  }
  details.value.splice(index, 1)
}

function onItemChange(row: DeliveryShipmentDetail) {
  const g = goodsOptions.value.find((x) => x.id === row.itemId)
  if (g) {
    row.itemNo = g.itemNo
    row.itemName = g.itemName
  }
}

async function openDialog(row?: DeliveryShipment) {
  await initOptions()
  if (!row && !currentDeliveryCenterId.value) {
    ElMessage.warning('请先在右上角选择配送中心')
    return
  }
  dialogVisible.value = true
  details.value = []
  if (row) {
    const res: any = await deliveryShipmentApi.get(row.id!)
    Object.assign(form, res.data.header)
    details.value = (res.data.details || []).map((d: DeliveryShipmentDetail) => ({ ...d }))
  } else {
    // add by codebuddy 20260817: 新增时自动带入全局配送中心
    // add by codebuddy 20260818: 新增时清空收货门店
    Object.assign(form, {
      id: undefined, shipmentNo: '', shipmentDate: '', volume: undefined, weight: undefined,
      erpNo: '', erpShipmentNo: '', status: undefined, siteId: undefined,
      deliveryCenterId: currentDeliveryCenterId.value, creator: currentDeliveryCenterNo()
    })
  }
}

async function view(row: DeliveryShipment) {
  const res: any = await deliveryShipmentApi.get(row.id!)
  Object.assign(viewData, res.data.header)
  viewDetails.value = res.data.details || []
  viewVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  if (!details.value.length) {
    ElMessage.warning('请至少添加一条发货明细')
    return
  }
  if (details.value.some((d) => !d.itemId)) {
    ElMessage.warning('请选择明细中的商品')
    return
  }
  // add by codebuddy 20260909: 每个批次的发货数量必须大于 0
  if (details.value.some((d) => !((d.qty ?? 0) > 0))) {
    ElMessage.warning('每条批次的发货数量必须大于 0')
    return
  }
  saving.value = true
  try {
    // add by codebuddy 20260817: 项次(seq)已可编辑，缺省时按行序兜底
    const dto = {
      header: { ...form },
      details: details.value.map((d, i) => ({ ...d, seq: d.seq ?? i + 1, orderSeq: d.orderSeq ?? 1 }))
    }
    if (form.id) {
      await deliveryShipmentApi.update(dto as any)
      ElMessage.success('修改成功')
    } else {
      await deliveryShipmentApi.save(dto as any)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: DeliveryShipment) {
  await ElMessageBox.confirm(`确认删除发货单「${row.shipmentNo}」？`, '提示', { type: 'warning' })
  await deliveryShipmentApi.remove(row.id!)
  ElMessage.success('删除成功')
  load()
}

// add by codebuddy 20260817: 打开发货确认弹窗，带回已录入的 ERP 配送单号
function openShip(row: DeliveryShipment) {
  Object.assign(shipForm, {
    id: row.id,
    shipmentNo: row.shipmentNo,
    erpShipmentNo: row.erpShipmentNo || '',
    volume: row.volume,
    weight: row.weight
  })
  shipVisible.value = true
}

// add by codebuddy 20260817: 确认发货，调用发货接口（后端校验 ERP 配送单号非空）
async function doShip() {
  await shipFormRef.value?.validate()
  shiping.value = true
  try {
    await deliveryShipmentApi.ship(shipForm.id!, {
      erpShipmentNo: shipForm.erpShipmentNo,
      volume: shipForm.volume,
      weight: shipForm.weight
    })
    ElMessage.success('发货成功')
    shipVisible.value = false
    load()
  } finally {
    shiping.value = false
  }
}

async function initOptions() {
  const res: any = await goodsApi.list()
  goodsOptions.value = res.data
  // add by codebuddy 20260818: 门店下拉数据源（右上角门店列表为空时主动加载）
  if (!shopOptions.value.length) {
    const shopRes: any = await shopApi.list()
    shopOptions.value = shopRes.data
  }
}

// add by codebuddy 20260818: 根据门店id取门店名称
function shopName(siteId?: number) {
  return shopOptions.value.find((s) => s.id === siteId)?.shopName ?? ''
}

onMounted(() => {
  query.deliveryCenterId = currentDeliveryCenterId.value
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
.lock-tip {
  font-size: 13px;
  color: #909399;
}
</style>
