<template>
  <div class="mine">
    <div class="mine-head">
      <van-icon name="user-circle-o" size="56" color="#fff" />
      <div class="mine-name">{{ user?.realName || user?.username || '未登录' }}</div>
      <div class="mine-role">{{ roleText }}</div>
    </div>

    <van-cell-group inset class="mine-info">
      <van-cell title="账户" :value="user?.username || '-'" />
      <van-cell title="当前视角" :value="viewText" />
    </van-cell-group>

    <div class="mine-btn">
      <van-button round block type="danger" @click="doLogout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { currentUser, logoutUser } from '../stores/auth'
import { currentView } from '../stores/globalDc'

const router = useRouter()
const user = computed(() => currentUser.value)

const roleText = computed(() => {
  const roles = user.value?.roles ?? []
  if (roles.includes('hq')) return '总部管理员'
  if (roles.includes('dc')) return '配送中心'
  if (roles.includes('shop')) return '门店'
  return '普通用户'
})

const viewText = computed(() => {
  const v = currentView.value
  if (v === 'hq') return '总部视角'
  if (v === 'dc') return '配送中心视角'
  return '门店视角'
})

async function doLogout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确认退出登录？' })
  } catch {
    return
  }
  logoutUser()
  showToast('已退出')
  router.replace('/login')
}
</script>

<style scoped>
.mine-head {
  padding: 32px 20px 28px;
  background: linear-gradient(160deg, #1989fa, #39b6ff);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.mine-name {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 600;
}
.mine-role {
  margin-top: 4px;
  font-size: 13px;
  opacity: 0.9;
}
.mine-info {
  margin-top: 16px;
}
.mine-btn {
  margin: 28px 16px;
}
</style>
