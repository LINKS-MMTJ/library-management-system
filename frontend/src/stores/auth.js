import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, getMe } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isLibrarian = computed(() => user.value?.role === 'LIBRARIAN')
  const isBorrower = computed(() => user.value?.role === 'BORROWER')
  const canManage = computed(() => isAdmin.value || isLibrarian.value)

  async function login(username, password) {
    const res = await apiLogin(username, password)
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(user.value))
    return res.data
  }

  async function register(data) {
    const res = await apiRegister(data)
    return res.data
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function refreshUser() {
    if (!token.value) return
    try {
      const res = await getMe()
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(user.value))
    } catch { logout() }
  }

  return { token, user, isLoggedIn, isAdmin, isLibrarian, isBorrower, canManage, login, register, logout, refreshUser }
})
