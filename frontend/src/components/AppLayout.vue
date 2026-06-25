<!-- Author: LINKS -->
<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-header">图书管理</div>
      <nav class="sidebar-nav">
        <div class="sidebar-section">主菜单</div>
        <router-link to="/dashboard" class="nav-item" active-class="active">仪表盘</router-link>

        <div class="sidebar-section">图书管理</div>
        <router-link to="/books" class="nav-item" active-class="active">图书列表</router-link>

        <template v-if="auth.isAdmin">
          <div class="sidebar-section">用户管理</div>
          <router-link to="/users" class="nav-item" active-class="active">用户列表</router-link>
        </template>

        <div class="sidebar-section">借阅服务</div>
        <router-link v-if="auth.canManage" to="/borrow-manage" class="nav-item" active-class="active">借阅管理</router-link>
        <router-link v-else to="/my-borrows" class="nav-item" active-class="active">我的借阅</router-link>
        <router-link to="/reservations" class="nav-item" active-class="active">预约管理</router-link>
        <router-link to="/notifications" class="nav-item" active-class="active">消息通知</router-link>

        <template v-if="auth.isAdmin">
          <div class="sidebar-section">系统管理</div>
          <router-link to="/system" class="nav-item" active-class="active">系统操作</router-link>
        </template>
      </nav>
      <div class="sidebar-footer">
        <div class="user-name">{{ auth.user?.name }}</div>
        <div class="user-role">{{ roleLabel }}</div>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <h1 class="topbar-title">{{ pageTitle }}</h1>
        <div class="topbar-right">
          <span>{{ auth.user?.name }}</span>
          <span style="font-size:11px;color:var(--text-gray)">{{ roleLabel }}</span>
          <button class="btn btn-outline btn-sm" @click="handleLogout">退出登录</button>
        </div>
      </header>
      <main class="content">
        <router-view @toast="showToast" />
      </main>
      <footer class="statusbar">
        <span>{{ toastMsg }}</span>
        <span>{{ today }}</span>
      </footer>
    </div>
    <div v-if="toastVisible" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getNotifications, getUnreadCount } from '../api'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const toastMsg = ref('就绪')
const toastVisible = ref(false)
const today = new Date().toISOString().slice(0, 10)

const titles = {
  dashboard: '仪表盘', books: '图书列表', users: '用户列表',
  myborrows: '我的借阅', borrowmanage: '借阅管理',
  reservations: '预约管理', notifications: '消息通知', system: '系统操作'
}
const pageTitle = computed(() => titles[route.name?.toLowerCase()] || route.name)

const roleLabel = computed(() => {
  if (auth.isAdmin) return '系统管理员'
  if (auth.isLibrarian) return '图书管理员'
  return '借阅者'
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}

let toastTimer
function showToast(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
    toastMsg.value = '就绪'
  }, 3000)
}

watch(() => route.name, () => { toastVisible.value = false })
</script>
