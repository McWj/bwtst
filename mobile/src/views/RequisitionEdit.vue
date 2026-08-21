<template>
  <div class="req-edit">
    <van-nav-bar :title="form.id ? '编辑要货申请' : '新增要货申请'" left-arrow fixed placeholder @click-left="router.back()" />

    <van-form @submit="submit">
      <van-cell-group inset title="基本信息">
        <van-field
          v-model="form.requisitionDate"
          name="requisitionDate"
          label="要货日期"
          placeholder="请选择"
          is-link
          readonly
          :rules="[{ required: true, message: '请选择要货日期' }]"
          @click="showDatePicker('requisitionDate')"
        />
        <van-field
          v-model="form.demandDate"
          name="demandDate"
          label="需求日期"
          placeholder="请选择"
          is-link
          readonly
          :rules="[{ required: true, message: '请选择需求日期' }]"
          @click="showDatePicker('demandDate')"
        />
        <van-field v-model="form.creator" label="创建人" placeholder="创建人" />
        <van-field
          v-model="templateName"
          label="要货模板"
          placeholder="选择模板"
          is-link
          readonly
          @click="templatePopupShow = true"
        />
        <van-field label="追加要货">
          <template #input>
            <van-radio-group v-model="form.isAddPo" direction="horizontal">
              <van-radio name="Y">是</van-radio>
              <van-radio name="N">否</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field label="紧急要货">
          <template #input>
            <van-radio-group v-model="form.isUrgentOrder" direction="horizontal">
              <van-radio name="Y">是</van-radio>
              <van-radio name="N">否</van-radio>
            </van-radio-group>
          </template>
        </van-field>
      </van-cell-group>

      <van-cell-group inset title="商品明细" class="goods-group">
        <div v-if="details.length === 0" class="empty">请选择要货模板带出商品</div>
        <div v-for="(d, i) in details" :key="i" class="goods-item">
          <div class="goods-head">
            <span class="goods-name">{{ d.itemNo }} · {{ d.itemName }}</span>
            <van-icon name="cross" color="#c8c9cc" @click="removeDetail(i)" />
          </div>
          <div class="goods-fields">
            <div class="goods-field">
              <span class="goods-label">单位</span>
              <span>{{ d.unit || '-' }}</span>
            </div>
            <div class="goods-field">
              <span class="goods-label">要货数量</span>
              <van-stepper v-model="d.qty" :min="0" integer />
            </div>
            <div class="goods-field">
              <span class="goods-label">单价</span>
              <van-field v-model="d.price" type="number" placeholder="0" class="price-input" />
            </div>
          </div>
        </div>
      </van-cell-group>

      <div class="submit-wrap">
        <van-button round block type="primary" native-type="submit" :loading="saving">保存</van-button>
      </div>
    </van-form>

    <!-- 日期选择 -->
    <van-popup v-model:show="datePickerShow" position="bottom" round>
      <van-date-picker
        v-model="pickerDate"
        title="选择日期"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="onDateConfirm"
        @cancel="datePickerShow = false"
      />
    </van-popup>

    <!-- 模板选择 -->
    <van-popup v-model:show="templatePopupShow" position="bottom" round>
      <van-picker
        :columns="templateColumns"
        @confirm="onTemplateConfirm"
        @cancel="templatePopupShow = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ptemplateApi, requisitionApi } from '../api'
import { requisitionCreditDeduct, type RequisitionErpParams } from '../api/erp'
import { currentShopId, currentShopNo } from '../stores/globalShop'
import type { Ptemplate, PtemplateGoods } from '../types'

// add by codebuddy 20260820: 保存时调用 ERP 算预估运费/重量/体积的地址与辅助方法
const ERP_URL = (import.meta.env.VITE_SERVER || '') + '/wtoptst/ws/r/awsp920'

function toYyyyMmDd(date?: string) {
  return date ? date.replace(/-/g, '') : ''
}

async function callRequisitionDeduct(h: any, details: any[], callType: string, status: string) {
  const shopNo = currentShopNo() || h.creator || ''
  if (!shopNo) throw new Error('无法获取门店编码')
  let ptemplateNo = ''
  if (h.templateNo) {
    try {
      const tRes: any = await ptemplateApi.get(h.templateNo)
      ptemplateNo = tRes.data?.header?.ptemplateNo ?? ''
    } catch {
      ptemplateNo = ''
    }
  }
  const now = new Date()
  const today = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const creator = h.creator || shopNo
  const requisition: RequisitionErpParams = {
    version: '3.0', creator, create_datetime: today, modify_no: creator, modify_datetime: today,
    approve_no: creator, approve_datetime: today, status, site_no: shopNo,
    requisition_date: toYyyyMmDd(h.requisitionDate) || today, create_date: today,
    po_template_no: ptemplateNo, is_add_po: h.isAddPo === 'Y' ? 'Y' : 'N',
    is_urgent_order: h.isUrgentOrder === 'Y' ? 'Y' : 'N', remark: '', front_no: h.requisitionNo || '',
    callType, oType: '0',
    requisition_detail: details.filter((d) => Number(d.qty) > 0).map((d, idx) => {
      const price = Number(d.price) || 0
      const qty = Number(d.qty) || 0
      const amount = price * qty
      return {
        seq: idx + 1, item_no: d.itemNo || '', base_unit: d.unit || '', base_qty: qty, price, amount,
        packing_unit: d.unit || '', packing_qty: qty, distri_price: price, distri_amount: amount
      }
    })
  }
  return requisitionCreditDeduct({ url: ERP_URL, shopNo, requisition })
}

const route = useRoute()
const router = useRouter()
const id = route.params.id ? Number(route.params.id) : undefined

const form = reactive<any>({
  id: undefined, requisitionNo: '', requisitionDate: '', demandDate: '',
  siteId: undefined, templateNo: undefined, creator: '', isAddPo: 'N', isUrgentOrder: 'N'
})
const details = ref<any[]>([])
const saving = ref(false)

const templateOptions = ref<Ptemplate[]>([])
const templateName = computed(() => {
  if (!form.templateNo) return ''
  return templateOptions.value.find((t) => t.id === form.templateNo)?.ptemplateName ?? ''
})
const templatePopupShow = ref(false)
const templateColumns = computed(() =>
  templateOptions.value.map((t) => ({ text: t.ptemplateName, value: t.id }))
)

const datePickerShow = ref(false)
const pickerDate = ref<string[]>([])
const pickerField = ref<'requisitionDate' | 'demandDate'>('requisitionDate')
const minDate = new Date(2020, 0, 1)
const maxDate = new Date(2030, 11, 31)

function showDatePicker(field: 'requisitionDate' | 'demandDate') {
  pickerField.value = field
  const cur = form[field] || new Date().toISOString().split('T')[0]
  pickerDate.value = cur.split('-')
  datePickerShow.value = true
}

function onDateConfirm({ selectedValues }: any) {
  form[pickerField.value] = selectedValues.join('-')
  datePickerShow.value = false
}

function onTemplateConfirm({ selectedOptions }: any) {
  const t = selectedOptions?.[0]
  if (t) {
    form.templateNo = t.value
    loadTemplateGoods(t.value)
  }
  templatePopupShow.value = false
}

function removeDetail(i: number) {
  details.value.splice(i, 1)
}

async function loadTemplates() {
  if (!currentShopId.value) {
    showToast('请先选择门店')
    return
  }
  const res: any = await ptemplateApi.listByShop(currentShopId.value)
  templateOptions.value = res.data ?? []
}

async function loadTemplateGoods(templateId: number) {
  try {
    const res: any = await ptemplateApi.get(templateId)
    const goodsList: PtemplateGoods[] = res.data?.goodsList ?? []
    details.value = goodsList.map((g) => ({
      itemId: g.itemId,
      itemNo: g.itemNo,
      itemName: g.itemName,
      unit: g.unit,
      qty: Number(g.qty) || 0,
      price: 0
    }))
  } catch {
    showToast('加载模板商品失败')
  }
}

async function submit() {
  const validDetails = details.value
    .filter((d) => Number(d.qty) > 0)
    .map((d) => ({ ...d, price: Number(d.price) || 0 }))
  if (!validDetails.length) {
    showToast('请至少添加一条数量大于0的商品明细')
    return
  }
  saving.value = true
  try {
    // add by codebuddy 20260820: 保存时调用 ERP 算预估运费/重量/体积（确认不再调用），失败不允许保存
    const stdData = await callRequisitionDeduct(form, validDetails, '2', '0')
    const p: any = stdData.parameter
    form.distributionFee = Number(p?.distributionFee ?? 0)
    form.weight = Number(p?.esWeight ?? 0)
    form.weightUnit = p?.weightUnit ?? ''
    form.volume = Number(p?.esVolume ?? 0)
    form.volumeUnit = p?.VolumeUnit ?? ''
    const dto = { header: { ...form }, details: validDetails }
    if (form.id) {
      await requisitionApi.update(dto as any)
      showToast('修改成功')
    } else {
      await requisitionApi.save(dto as any)
      showToast('新增成功')
    }
    router.back()
  } catch (e: any) {
    showToast('保存失败：' + (e?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const today = new Date().toISOString().split('T')[0]
  form.requisitionDate = today
  form.demandDate = today
  form.siteId = currentShopId.value
  form.creator = currentShopNo()

  await loadTemplates()

  if (id) {
    try {
      const res: any = await requisitionApi.get(id)
      Object.assign(form, res.data?.header ?? {})
      details.value = (res.data?.details ?? []).map((d: any) => ({
        itemId: d.itemId, itemNo: d.itemNo, itemName: d.itemName,
        unit: d.unit, qty: Number(d.qty) || 0, price: d.price ?? 0
      }))
    } catch {
      showToast('加载单据失败')
    }
  }
})
</script>

<style scoped>
.req-edit {
  padding-bottom: 20px;
}
.goods-group {
  margin-bottom: 0;
}
.empty {
  padding: 24px;
  text-align: center;
  color: #c8c9cc;
}
.goods-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f2f3f5;
}
.goods-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.goods-name {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
}
.goods-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
.goods-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #646566;
}
.goods-label {
  color: #969799;
  margin-right: 12px;
}
.price-input {
  padding: 0;
  width: 120px;
}
.submit-wrap {
  margin: 24px 16px;
}
</style>
