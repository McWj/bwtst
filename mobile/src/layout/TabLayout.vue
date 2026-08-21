<template>
  <div class="tab-layout">
    <div class="tab-content">
      <router-view />
    </div>
    <van-tabbar v-model="active" route>
      <van-tabbar-item replace to="/home" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/mine" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// add by codebuddy 20260819: 底部导航布局
const active = ref('home')
</script>

<style scoped>
.tab-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.tab-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 12px;
}
</style>
