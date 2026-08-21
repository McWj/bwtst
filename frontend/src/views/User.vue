<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <div class="page-actions">
        <el-input v-model="keyword" placeholder="账户/用户名" clearable style="width: 200px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openAdd">新增</el-button>
        <el-button type="warning" :loading="genLoading" @click="generateShopUsers">批量生成门店用户</el-button>
      </div>
    </div>

    <el-table :data="list" border stripe v-loading="loading" class="page-table">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="username" label="登录账户" width="140" />
      <el-table-column prop="realName" label="用户名" width="140" />
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles || []" :key="r" class="role-tag" :type="roleTag(r)">{{ roleLabel(r) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'info' : 'success'">{{ row.status === '0' ? '停用' : '启用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openPwd(row)">改密</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="page-pagination"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="onPageChange"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="1000px" top="5vh" class="user-dialog">
      <div class="user-edit-layout">
        <!-- add by codebuddy 20260819: 左侧基础信息 -->
        <div class="user-edit-left">
          <el-form :model="form" label-width="90px">
            <el-form-item label="登录账户" required>
              <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入登录账户" />
            </el-form-item>
            <el-form-item label="用户名" required>
              <el-input v-model="form.realName" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item v-if="!form.id" label="密码" required>
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" value="1" />
                <el-option label="停用" value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="角色" required>
              <el-checkbox-group v-model="form.roles">
                <el-checkbox label="hq">总部角色</el-checkbox>
                <el-checkbox label="dc">配送中心角色</el-checkbox>
                <el-checkbox label="shop">门店角色</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="默认门店" v-if="(form.roles || []).includes('shop')">
              <el-select v-model="form.defaultShopId" placeholder="请先勾选门店" clearable style="width: 100%"
                         :disabled="!(form.shopIds && form.shopIds.length)">
                <el-option v-for="s in defaultShopOptions" :key="s.id" :label="`${s.shopNo} ${s.shopName}`" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="默认配送中心" v-if="(form.roles || []).includes('dc')">
              <el-select v-model="form.defaultDcId" placeholder="请先勾选配送中心" clearable style="width: 100%"
                         :disabled="!(form.dcIds && form.dcIds.length)">
                <el-option v-for="d in defaultDcOptions" :key="d.id" :label="`${d.dcNo} ${d.dcName}`" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
        <!-- add by codebuddy 20260819: 右侧组织勾选列表 -->
        <div class="user-edit-right">
          <div class="scope-panel" v-if="(form.roles || []).includes('shop')">
            <div class="scope-panel-title">
              可访问门店
              <el-button size="small" link type="primary" @click="selectAllShops">{{ shopAllSelected ? '取消全选' : '全选' }}</el-button>
            </div>
            <el-input v-model="shopKeyword" placeholder="搜索门店编号/名称" clearable size="small">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-checkbox-group v-model="form.shopIds" class="scope-list">
              <el-checkbox v-for="s in displayShopOptions" :key="s.id" :value="s.id" class="scope-item">
                {{ s.shopNo || '-' }}（{{ s.shopName || '-' }}）
              </el-checkbox>
            </el-checkbox-group>
            <div v-if="displayShopOptions.length === 0" class="scope-empty">暂无门店</div>
            <div v-else-if="!shopKeyword && shopOptions.length > displayShopOptions.length" class="scope-hint">
              共 {{ shopOptions.length }} 家门店，输入关键字可搜索全部；本次显示 {{ displayShopOptions.length }} 条
            </div>
          </div>
          <div class="scope-panel" v-if="(form.roles || []).includes('dc')">
            <div class="scope-panel-title">
              可访问配送中心
              <el-button size="small" link type="primary" @click="selectAllDcs">{{ dcAllSelected ? '取消全选' : '全选' }}</el-button>
            </div>
            <el-input v-model="dcKeyword" placeholder="搜索配送中心编号/名称" clearable size="small">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-checkbox-group v-model="form.dcIds" class="scope-list">
              <el-checkbox v-for="d in displayDcOptions" :key="d.id" :value="d.id" class="scope-item">
                {{ d.dcNo || '-' }}（{{ d.dcName || '-' }}）
              </el-checkbox>
            </el-checkbox-group>
            <div v-if="displayDcOptions.length === 0" class="scope-empty">暂无配送中心</div>
            <div v-else-if="!dcKeyword && dcOptions.length > displayDcOptions.length" class="scope-hint">
              共 {{ dcOptions.length }} 个配送中心，输入关键字可搜索全部
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- add by codebuddy 20260819: 独立修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ pwdUser.realName }}（{{ pwdUser.username }}）</span>
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { userApi, shopApi, distributionCenterApi } from '../api'
import type { SysUser, Shop, DistributionCenter } from '../types'

const list = ref<SysUser[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)

const shopOptions = ref<Shop[]>([])
const dcOptions = ref<DistributionCenter[]>([])
// add by codebuddy 20260819: 列表勾选模式的搜索关键字
const shopKeyword = ref('')
const dcKeyword = ref('')

// add by codebuddy 20260819: 按关键字过滤门店/配送中心列表
const filteredShopOptions = computed(() => {
  const key = shopKeyword.value.trim().toLowerCase()
  const base = shopOptions.value
  if (!key) return base
  return base.filter((s) => s.shopNo.toLowerCase().includes(key) || s.shopName.toLowerCase().includes(key))
})
const filteredDcOptions = computed(() => {
  const key = dcKeyword.value.trim().toLowerCase()
  const base = dcOptions.value
  if (!key) return base
  return base.filter((d) => d.dcNo.toLowerCase().includes(key) || d.dcName.toLowerCase().includes(key))
})

// add by codebuddy 20260819: 默认最多显示前 100 条，避免一次性渲染过多导致空白
const displayShopOptions = computed(() => {
  const list = filteredShopOptions.value
  if (shopKeyword.value.trim()) return list.slice(0, 300)
  return list.slice(0, 100)
})
const displayDcOptions = computed(() => {
  const list = filteredDcOptions.value
  if (dcKeyword.value.trim()) return list.slice(0, 300)
  return list.slice(0, 100)
})

const defaultShopOptions = computed(() =>
  shopOptions.value.filter((s) => s.id != null && (form.shopIds || []).includes(s.id)))
const defaultDcOptions = computed(() =>
  dcOptions.value.filter((d) => d.id != null && (form.dcIds || []).includes(d.id)))

// add by codebuddy 20260819: 全选状态与切换
const shopAllSelected = computed(() =>
  shopOptions.value.length > 0 && shopOptions.value.every((s) => s.id != null && (form.shopIds || []).includes(s.id)))
const dcAllSelected = computed(() =>
  dcOptions.value.length > 0 && dcOptions.value.every((d) => d.id != null && (form.dcIds || []).includes(d.id)))
function selectAllShops() {
  if (shopAllSelected.value) {
    form.shopIds = []
  } else {
    form.shopIds = shopOptions.value.map((s) => s.id).filter((id): id is number => id != null)
  }
}
function selectAllDcs() {
  if (dcAllSelected.value) {
    form.dcIds = []
  } else {
    form.dcIds = dcOptions.value.map((d) => d.id).filter((id): id is number => id != null)
  }
}

const dialogVisible = ref(false)
const form = reactive<SysUser>({
  username: '',
  realName: '',
  password: '',
  status: '1',
  roles: [],
  shopIds: [],
  dcIds: [],
  defaultShopId: undefined,
  defaultDcId: undefined
})

// add by codebuddy 20260819: 取消门店/配送中心角色时清空对应组织选择，避免脏数据
watch(() => form.roles, (roles = []) => {
  if (!roles.includes('shop')) {
    form.shopIds = []
    form.defaultShopId = undefined
  }
  if (!roles.includes('dc')) {
    form.dcIds = []
    form.defaultDcId = undefined
  }
}, { deep: true })

function roleLabel(r: string) {
  return r === 'hq' ? '总部角色' : r === 'dc' ? '配送中心角色' : '门店角色'
}
function roleTag(r: string): any {
  return r === 'hq' ? 'danger' : r === 'dc' ? 'warning' : 'success'
}

async function load() {
  loading.value = true
  try {
    const res: any = await userApi.page({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    list.value = (res.data?.records ?? res.data?.list ?? res.data ?? [])
    total.value = res.data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) {
  pageNum.value = p
  load()
}

// add by codebuddy 20260819: 批量生成门店用户
const genLoading = ref(false)
async function generateShopUsers() {
  try {
    await ElMessageBox.confirm(
      '将按门店编码为每一家门店生成一个门店角色用户，密码默认为门店编码。已存在的账户会跳过。是否继续？',
      '批量生成门店用户',
      { type: 'warning' }
    )
  } catch {
    return
  }
  genLoading.value = true
  try {
    const res: any = await userApi.generateShopUsers()
    ElMessage.success(res.data ?? '生成完成')
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '生成失败')
  } finally {
    genLoading.value = false
  }
}

// add by codebuddy 20260819: 独立修改密码
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdUser = reactive<SysUser>({ username: '', realName: '' })
const pwdForm = reactive({ password: '', confirm: '' })
function openPwd(row: SysUser) {
  Object.assign(pwdUser, { id: row.id, username: row.username, realName: row.realName })
  pwdForm.password = ''
  pwdForm.confirm = ''
  pwdVisible.value = true
}
async function submitPwd() {
  if (!pwdForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (pwdForm.password !== pwdForm.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await userApi.resetPassword(pwdUser.id!, pwdForm.password)
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '修改失败')
  } finally {
    pwdLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    username: '',
    realName: '',
    password: '',
    status: '1',
    roles: [],
    shopIds: [],
    dcIds: [],
    defaultShopId: undefined,
    defaultDcId: undefined
  })
  shopKeyword.value = ''
  dcKeyword.value = ''
}

function openAdd() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: SysUser) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  // add by codebuddy 20260819: 编辑时不修改密码，清空密码字段避免误提交
  form.password = ''
  dialogVisible.value = true
}

async function submit() {
  if (!form.username || !form.realName) {
    ElMessage.warning('请填写登录账户和用户名')
    return
  }
  if (!form.id && !form.password) {
    ElMessage.warning('新增用户必须设置密码')
    return
  }
  if (!form.roles || form.roles.length === 0) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  // 默认组织必须包含在可访问范围内
  if (form.defaultShopId != null && !(form.shopIds || []).includes(form.defaultShopId)) {
    ElMessage.warning('默认门店必须是可访问门店之一')
    return
  }
  if (form.defaultDcId != null && !(form.dcIds || []).includes(form.defaultDcId)) {
    ElMessage.warning('默认配送中心必须是可访问配送中心之一')
    return
  }
  try {
    await userApi.save(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

async function remove(row: SysUser) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.realName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await userApi.remove(row.id!)
    ElMessage.success('删除成功')
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(async () => {
  const [sRes, dRes]: any[] = await Promise.all([shopApi.list(), distributionCenterApi.list()])
  shopOptions.value = sRes.data ?? []
  dcOptions.value = dRes.data ?? []
  load()
})
</script>

<style scoped>
.page-container { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }
.page-actions { display: flex; gap: 8px; }
.page-table { margin-bottom: 16px; }
.page-pagination { justify-content: flex-end; display: flex; }
.role-tag { margin-right: 4px; }
/* add by codebuddy 20260819: 弹窗左右布局 */
.user-edit-layout { display: flex; gap: 16px; }
.user-edit-left { width: 340px; flex: none; }
.user-edit-right { flex: 1; display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.scope-panel { border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px; }
.scope-panel-title { font-weight: 600; margin-bottom: 8px; color: #303133; }
.scope-list { display: flex; flex-direction: column; gap: 4px; height: 200px; overflow-y: auto; width: 100%; margin-top: 8px; }
.scope-item { width: 100%; margin-right: 0; margin-left: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: none; min-height: 24px; }
.scope-empty { color: #909399; font-size: 13px; text-align: center; padding: 12px 0; }
.scope-hint { color: #909399; font-size: 12px; text-align: center; padding: 6px 0 2px; }
</style>
