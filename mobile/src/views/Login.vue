<template>
  <div class="login-wrap">
    <div class="login-head">
      <div class="login-logo">门店配送</div>
      <div class="login-slogan">移动业务工作台</div>
    </div>
    <van-form @submit="doLogin">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="账户"
          placeholder="请输入登录账户"
          :rules="[{ required: true, message: '请输入账户' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>
      <div class="login-btn-wrap">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { authApi } from '../api'
import { setUser, defaultView } from '../stores/auth'
import { currentShopId } from '../stores/globalShop'
import { currentView, currentDeliveryCenterId } from '../stores/globalDc'
import type { SysUser } from '../types'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function doLogin() {
  if (!form.username || !form.password) {
    showToast('请输入账户和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await authApi.login(form)
    const u: SysUser = res.data
    setUser(u)
    const v = defaultView()
    currentView.value = v
    if (v === 'shop') {
      const shopId = u.defaultShopId ?? u.shopIds?.[0]
      if (shopId) currentShopId.value = shopId
    } else if (v === 'dc') {
      const dcId = u.defaultDcId ?? u.dcIds?.[0]
      if (dcId) currentDeliveryCenterId.value = dcId
    }
    showToast('登录成功')
    router.replace('/')
  } catch (e: any) {
    showToast(e?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  padding: 0 16px;
  background: linear-gradient(160deg, #1989fa 0%, #39b6ff 45%, #e8f4ff 100%);
}
.login-head {
  padding: 96px 12px 48px;
  color: #fff;
}
.login-logo {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 4px;
}
.login-slogan {
  margin-top: 10px;
  font-size: 15px;
  opacity: 0.9;
}
.login-btn-wrap {
  margin: 28px 16px 0;
}
</style>
