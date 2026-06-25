import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  {
    path: '/',
    component: () => import('../components/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'books', name: 'Books', component: () => import('../views/Books.vue') },
      { path: 'users', name: 'Users', component: () => import('../views/Users.vue'), meta: { adminOnly: true } },
      { path: 'my-borrows', name: 'MyBorrows', component: () => import('../views/MyBorrows.vue') },
      { path: 'borrow-manage', name: 'BorrowManage', component: () => import('../views/BorrowManage.vue'), meta: { admin: true } },
      { path: 'reservations', name: 'Reservations', component: () => import('../views/Reservations.vue') },
      { path: 'notifications', name: 'Notifications', component: () => import('../views/Notifications.vue') },
      { path: 'system', name: 'System', component: () => import('../views/System.vue'), meta: { adminOnly: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) return next('/login')
  if (to.meta.adminOnly && !auth.isAdmin) return next('/dashboard')
  if (to.meta.admin && !auth.isAdmin && !auth.isLibrarian) return next('/my-borrows')
  if (to.meta.guest && auth.token) return next('/dashboard')
  next()
})

export default router
