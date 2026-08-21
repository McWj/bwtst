<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="商品编号/名称" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="success" @click="openDialog()">新增商品</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <!-- mark by codebuddy 20260817: 不显示 id 列 <el-table-column prop="id" label="ID" width="70" /> -->
      <el-table-column prop="itemNo" label="商品编号" min-width="140" />
      <el-table-column prop="itemName" label="商品名称" min-width="200" />
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="480px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="商品编号" prop="itemNo">
        <el-input v-model="form.itemNo" placeholder="请输入商品编号" />
      </el-form-item>
      <el-form-item label="商品名称" prop="itemName">
        <el-input v-model="form.itemName" placeholder="请输入商品名称" />
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
import { goodsApi } from '../api'
import type { Goods } from '../types'

// add by codebuddy 20260817
const loading = ref(false)
const saving = ref(false)
const rows = ref<Goods[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Goods>({ itemNo: '', itemName: '' })
const rules: FormRules = {
  itemNo: [{ required: true, message: '请输入商品编号', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res: any = await goodsApi.page({ ...query })
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

function openDialog(row?: Goods) {
  dialogVisible.value = true
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: undefined, itemNo: '', itemName: '' })
  }
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await goodsApi.update({ ...form })
      ElMessage.success('修改成功')
    } else {
      await goodsApi.save({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Goods) {
  await ElMessageBox.confirm(`确认删除商品「${row.itemName}」？`, '提示', { type: 'warning' })
  await goodsApi.remove(row.id!)
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
