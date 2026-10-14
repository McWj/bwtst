<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="企业编码 / 企业名称"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button type="primary" plain @click="openCreate">新增租户</el-button>
        <span class="tip">租户开通后，使用「企业编码 + 管理员账号」在登录页登录</span>
      </div>

      <el-table v-loading="loading" :data="rows" border size="small">
        <el-table-column prop="tenantCode" label="企业编码" width="140" />
        <el-table-column prop="tenantName" label="企业名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'info'" size="small">
              {{ row.status === '1' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="erpEntId" label="ERP账套ID" width="110">
          <template #default="{ row }">{{ row.erpEntId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createDatetime" label="开通时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createDatetime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === '1' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === '1' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :current-page="page"
        :page-size="size"
        @current-change="onPageChange"
      />
    </el-card>

    <!-- 新增 / 编辑租户 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑租户' : '新增租户'" width="580px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="企业编码" required>
          <el-input v-model="form.tenantCode" :disabled="!!form.id" placeholder="登录时填写，如 acme（开通后不可修改）" />
        </el-form-item>
        <el-form-item label="企业名称" required>
          <el-input v-model="form.tenantName" placeholder="如 某某商贸有限公司" />
        </el-form-item>
        <template v-if="!form.id">
          <el-form-item label="管理员账号">
            <el-input v-model="form.adminUsername" placeholder="留空默认 admin" />
          </el-form-item>
          <el-form-item label="初始密码" required>
            <el-input v-model="form.adminPassword" type="password" show-password placeholder="登录后可自行修改" />
          </el-form-item>
        </template>
        <el-divider content-position="left">ERP 账套（可选，阶段 3 启用）</el-divider>
        <el-form-item label="接口地址">
          <el-input v-model="form.erpUrl" placeholder="如 http://xxx/ws/r/awsp920" />
        </el-form-item>
        <el-form-item label="签名密钥">
          <el-input v-model="form.erpDigiKey" placeholder="digi-key" />
        </el-form-item>
        <el-form-item label="产品码">
          <el-input v-model="form.erpProd" placeholder="如 BWZT" />
        </el-form-item>
        <el-form-item label="ERP 账号">
          <el-input v-model="form.erpAcct" placeholder="如 tiptop" />
        </el-form-item>
        <el-form-item label="账套 ID">
          <el-input v-model="form.erpEntId" placeholder="如 10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置管理员密码 -->
    <el-dialog v-model="resetVisible" title="重置管理员密码" width="440px">
      <el-form label-width="90px">
        <el-form-item label="租户">
          <span>{{ resetTarget?.tenantName }}（{{ resetTarget?.tenantCode }}）</span>
        </el-form-item>
        <el-form-item label="账号" required>
          <el-input v-model="resetForm.username" placeholder="默认 admin" />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="resetForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminTenantApi } from '../../api/admin'
import type { SysTenant } from '../../types'

const loading = ref(false)
const saving = ref(false)
const rows = ref<SysTenant[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const form = reactive<SysTenant>({})

const resetVisible = ref(false)
const resetTarget = ref<SysTenant | null>(null)
const resetForm = reactive({ username: 'admin', newPassword: '' })

function formatTime(v?: string) {
  if (!v) return '-'
  return v.replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const res: any = await adminTenantApi.page({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onPageChange(p: number) {
  page.value = p
  load()
}

function openCreate() {
  Object.keys(form).forEach((k) => delete (form as any)[k])
  form.adminUsername = ''
  form.adminPassword = ''
  dialogVisible.value = true
}

function openEdit(row: SysTenant) {
  Object.keys(form).forEach((k) => delete (form as any)[k])
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submit() {
  if (!form.tenantCode || !form.tenantName) {
    ElMessage.warning('请填写企业编码与企业名称')
    return
  }
  if (!form.id && !form.adminPassword) {
    ElMessage.warning('请填写管理员初始密码')
    return
  }
  saving.value = true
  try {
    await adminTenantApi.save({ ...form })
    ElMessage.success(form.id ? '已更新' : '租户开通成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: SysTenant) {
  const toEnable = row.status !== '1'
  try {
    await ElMessageBox.confirm(
      toEnable
        ? `确认启用租户「${row.tenantName}」？启用后该企业可正常登录。`
        : `确认停用租户「${row.tenantName}」？停用后该企业将无法登录。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await adminTenantApi.setStatus(row.id!, toEnable ? '1' : '0')
  ElMessage.success(toEnable ? '已启用' : '已停用')
  load()
}

function openReset(row: SysTenant) {
  resetTarget.value = row
  resetForm.username = 'admin'
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function submitReset() {
  if (!resetForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  saving.value = true
  try {
    await adminTenantApi.resetAdminPassword(resetTarget.value!.id!, resetForm.username, resetForm.newPassword)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.tip {
  font-size: 13px;
  color: #909399;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
