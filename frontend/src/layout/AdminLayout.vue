<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <el-icon class="logo-icon"><Setting /></el-icon>
        <span>平台管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#14243a"
        text-color="#a6adb4"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/admin/tenant">
          <el-icon><OfficeBuilding /></el-icon>
          <span>租户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="page-title">平台管理后台</div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="onUserCommand">
            <div class="user-trigger">
              <el-icon><User /></el-icon>
              <span class="user-name">{{ adminUser?.realName || adminUser?.username }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminUser, adminLogout } from '../stores/adminAuth'
import { Setting, OfficeBuilding, User, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

function onUserCommand(command: string) {
  if (command === 'logout') {
    // 仅清理平台端会话，业务系统登录态不受影响
    adminLogout()
    router.replace({ name: 'AdminLogin' })
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.aside {
  background-color: #14243a;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-icon {
  font-size: 18px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
}
.arrow {
  font-size: 12px;
  color: #909399;
}
.main {
  background: #f5f7fa;
}
</style>
