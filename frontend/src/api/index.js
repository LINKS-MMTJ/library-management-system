import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const api = axios.create({ baseURL: '/api' })

// 请求拦截器：附加 token
api.interceptors.request.use(config => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 响应拦截器：检查业务 code + 401 时清除登录状态
api.interceptors.response.use(
  res => {
    const data = res.data
    // 服务端始终返回 HTTP 200，业务错误通过 JSON code 字段区分
    if (data && data.code && data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  err => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout()
    }
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

// ── Auth ──
export const login = (username, password) => api.post('/auth/login', { username, password })
export const register = (data) => api.post('/auth/register', data)
export const getMe = () => api.get('/auth/me')

// ── Books ──
export const getBooks = (keyword) => api.get('/books', { params: { keyword } })
export const getBook = (id) => api.get(`/books/${id}`)
export const addBook = (data) => api.post('/books', data)
export const updateBook = (id, data) => api.put(`/books/${id}`, data)
export const removeBook = (id, data) => api.delete(`/books/${id}`, { data })
export const borrowBook = (id) => api.post(`/books/${id}/borrow`)
export const reserveBook = (id) => api.post(`/books/${id}/reserve`)

// ── Users ──
export const getUsers = () => api.get('/users')
export const getUser = (id) => api.get(`/users/${id}`)
export const createUser = (data) => api.post('/users', data)
export const updateUser = (id, data) => api.put(`/users/${id}`, data)
export const deleteUser = (id) => api.delete(`/users/${id}`)
export const disableUser = (id) => api.put(`/users/${id}/disable`)
export const enableUser = (id) => api.put(`/users/${id}/enable`)
export const payFine = (id, amount) => api.put(`/users/${id}/pay-fine`, { amount })

// ── Borrows ──
export const getMyBorrows = () => api.get('/borrows/my')
export const getAllBorrows = () => api.get('/borrows')
export const returnBookById = (id) => api.post(`/borrows/${id}/return`)
export const renewBookById = (id) => api.post(`/borrows/${id}/renew`)

// ── Reservations ──
export const getReservations = () => api.get('/reservations')
export const cancelReservation = (id) => api.post(`/reservations/${id}/cancel`)

// ── Notifications ──
export const getNotifications = () => api.get('/notifications')
export const getUnreadCount = () => api.get('/notifications/unread-count')
export const markAsRead = (id) => api.put(`/notifications/${id}/read`)

// ── Dashboard ──
export const getDashboard = () => api.get('/dashboard')

// ── System ──
export const sendOverdueReminders = () => api.post('/system/overdue-reminders')
export const sendOutOfStock = () => api.post('/system/out-of-stock')

export default api
