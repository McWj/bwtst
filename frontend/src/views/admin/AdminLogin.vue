<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="login-title">测试中台</h2>
      <p class="login-sub">平台管理 · 仅平台超级管理员可登录</p>
      <el-form :model="form" label-position="top" @submit.prevent="doLogin">
        <el-form-item label="企业编码">
          <el-input v-model="form.tenantCode" placeholder="留空则使用默认企业" />
        </el-form-item>
        <el-form-item label="管理员账号" required>
          <el-input v-model="form.username" placeholder="请输入平台管理员账号" autofocus />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" @click="doLogin">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminAuthApi } from '../../api/admin'
import { establishAdminSession } from '../../stores/adminAuth'

const router = useRouter()

const form = reactive({ username: '', password: '', tenantCode: '' })
const loading = ref(false)

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await adminAuthApi.login(form)
    establishAdminSession(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.replace('/admin/tenant')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #14243a 0%, #274060 100%);
}
.login-card {
  width: 380px;
  padding: 8px 16px 16px;
}
.login-title {
  text-align: center;
  font-size: 20px;
  margin: 8px 0 4px;
}
.login-sub {
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin: 0 0 18px;
}
.login-btn {
  width: 100%;
}
</style>
