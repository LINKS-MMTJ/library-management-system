<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">图书管理系统</div>
      <div class="login-subtitle">欢迎使用，请登录或注册账号</div>

      <div class="tabs">
        <button class="tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button class="tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <!-- 登录表单 -->
      <form v-if="mode === 'login'" @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input class="form-input" v-model="loginForm.username" placeholder="请输入用户名" />
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input class="form-input" type="password" v-model="loginForm.password" placeholder="请输入密码" @keyup.enter="handleLogin" />
        </div>
        <button type="submit" class="btn btn-primary btn-block" style="margin-top:8px" :disabled="loading">
          {{ loading ? '登录中...' : '登  录' }}
        </button>
      </form>

      <!-- 注册表单 -->
      <form v-else @submit.prevent="handleRegister">
        <div class="form-group">
          <label class="form-label">用户名 *</label>
          <input class="form-input" v-model="regForm.username" placeholder="请输入用户名" />
        </div>
        <div class="form-group">
          <label class="form-label">密码（至少6位）*</label>
          <input class="form-input" type="password" v-model="regForm.password" placeholder="请输入密码" />
        </div>
        <div class="form-group">
          <label class="form-label">真实姓名 *</label>
          <input class="form-input" v-model="regForm.name" placeholder="请输入姓名" />
        </div>
        <div class="form-group">
          <label class="form-label">邮箱</label>
          <input class="form-input" v-model="regForm.email" placeholder="选填" />
        </div>
        <div class="form-group">
          <label class="form-label">手机号</label>
          <input class="form-input" v-model="regForm.phone" placeholder="选填" />
        </div>
        <button type="submit" class="btn btn-success btn-block" :disabled="loading">
          {{ loading ? '注册中...' : '注  册' }}
        </button>
      </form>

      <div v-if="error" class="alert alert-danger" style="margin-top:16px">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const mode = ref('login')
const loading = ref(false)
const error = ref('')

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', name: '', email: '', phone: '' })

async function handleLogin() {
  error.value = ''
  if (!loginForm.username || !loginForm.password) { error.value = '用户名和密码不能为空'; return }
  loading.value = true
  try {
    await auth.login(loginForm.username, loginForm.password)
    router.push('/dashboard')
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function handleRegister() {
  error.value = ''
  if (!regForm.username || !regForm.password || !regForm.name) { error.value = '用户名、密码、真实姓名不能为空'; return }
  if (regForm.password.length < 6) { error.value = '密码至少需要6位'; return }
  loading.value = true
  try {
    await auth.register(regForm)
    Object.assign(regForm, { username: '', password: '', name: '', email: '', phone: '' })
    mode.value = 'login'
    loginForm.username = regForm.username || ''
    error.value = ''
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
</script>
