<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="login-title">门店配送管理系统</h2>
      <el-form :model="form" label-position="top" @submit.prevent="doLogin">
        <el-form-item label="登录账户" required>
          <el-input v-model="form.username" placeholder="请输入登录账户" autofocus />
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
import { authApi } from '../api'
import { currentUser, setUser, defaultView } from '../stores/auth'
import { currentShopId } from '../stores/globalShop'
import { currentView, currentDeliveryCenterId } from '../stores/globalDc'
import type { SysUser } from '../types'

const router = useRouter()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账户和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await authApi.login(form)
    const u: SysUser = res.data
    setUser(u)
    // 进入默认视角
    const v = defaultView()
    currentView.value = v
    // add by codebuddy 20260819: 拥有总部角色权限的用户登录默认进入总部视角，覆盖上次保存的非总部视角
    if (v === 'hq') {
      localStorage.setItem('currentView', 'hq')
    }
    if (v === 'shop') {
      const shopId = u.defaultShopId ?? u.shopIds?.[0]
      if (shopId) currentShopId.value = shopId
    } else if (v === 'dc') {
      const dcId = u.defaultDcId ?? u.dcIds?.[0]
      if (dcId) currentDeliveryCenterId.value = dcId
    }
    ElMessage.success('登录成功')
    router.replace('/')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '登录失败')
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
  background: linear-gradient(135deg, #1f2d3d 0%, #3a4a5a 100%);
}
.login-card {
  width: 360px;
  padding: 8px 16px 16px;
}
.login-title {
  text-align: center;
  font-size: 20px;
  margin: 8px 0 20px;
}
.login-btn {
  width: 100%;
}
</style>
