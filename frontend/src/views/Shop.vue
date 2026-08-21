<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="门店编号/名称" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="success" @click="openDialog()">新增门店</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <!-- mark by codebuddy 20260817: 不显示 id 列 <el-table-column prop="id" label="ID" width="70" /> -->
      <el-table-column prop="shopNo" label="门店编号" min-width="120" />
      <el-table-column prop="shopName" label="门店名称" min-width="160" />
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑门店' : '新增门店'" width="480px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="门店编号" prop="shopNo">
        <el-input v-model="form.shopNo" placeholder="请输入门店编号" />
      </el-form-item>
      <el-form-item label="门店名称" prop="shopName">
        <el-input v-model="form.shopName" placeholder="请输入门店名称" />
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
import { shopApi } from '../api'
import type { Shop } from '../types'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<Shop[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Shop>({ shopNo: '', shopName: '' })
const rules: FormRules = {
  shopNo: [{ required: true, message: '请输入门店编号', trigger: 'blur' }],
  shopName: [{ required: true, message: '请输入门店名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res: any = await shopApi.page({ ...query })
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

function openDialog(row?: Shop) {
  dialogVisible.value = true
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: undefined, shopNo: '', shopName: '' })
  }
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await shopApi.update({ ...form })
      ElMessage.success('修改成功')
    } else {
      await shopApi.save({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Shop) {
  await ElMessageBox.confirm(`确认删除门店「${row.shopName}」？`, '提示', { type: 'warning' })
  await shopApi.remove(row.id!)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
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
