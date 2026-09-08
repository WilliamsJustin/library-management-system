import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    component: () => import('@/views/PublicLayout.vue'),
    children: [
      { path: '', component: () => import('@/views/site/SiteHomeView.vue') },
      { path: 'about', component: () => import('@/views/site/AboutView.vue') },
      { path: 'services', component: () => import('@/views/site/ServicesView.vue') },
      { path: 'activities', component: () => import('@/views/site/ActivitiesView.vue') }
    ]
  },
  {
    path: '/register',
    component: () => import('@/views/site/RegisterView.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/reader',
    component: () => import('@/views/reader/ReaderLayout.vue'),
    meta: { requiresAuth: true, role: 'reader' },
    children: [
      { path: '', component: () => import('@/views/reader/HomeView.vue') },
      { path: 'my-loans', component: () => import('@/views/reader/MyLoansView.vue') },
      { path: 'renew', component: () => import('@/views/reader/RenewView.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', component: () => import('@/views/admin/HomeView.vue') },
      { path: 'books', component: () => import('@/views/admin/BooksView.vue') },
      { path: 'readers', component: () => import('@/views/ReaderManagementView.vue') },
      { path: 'loans', component: () => import('@/views/admin/LoansView.vue') },
      { path: 'penalties', component: () => import('@/views/admin/PenaltiesView.vue') }
    ]
  },
  {
    path: '/change-password',
    name: 'change-password',
    component: () => import('@/views/ChangePasswordView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  // 后端返回的角色是大写（ADMIN/READER），而 meta.role 是小写，统一转小写后再比较，
  // 否则大小写不一致会让「角色不匹配」分支永远成立，导致登录后无限重定向到 /reader。
  const authed = authStore.isAuthenticated
  const role = (authStore.userRole || '').toLowerCase()

  // 1) 未登录访问受保护页面 → 去登录页
  if (to.meta.requiresAuth && !authed) {
    next('/login')
    return
  }

  // 2) 已登录却访问登录页 → 直接进各自首页（加 to.path 判断，杜绝死循环）
  if (to.path === '/login' && authed) {
    const dest = role === 'admin' ? '/admin' : '/reader'
    if (to.path !== dest) {
      next(dest)
      return
    }
  }

  // 3) 已登录但进了不属于自己角色的页面 → 拉回各自首页
  if (to.meta.role && authed && to.meta.role !== role) {
    const dest = role === 'admin' ? '/admin' : '/reader'
    if (to.path !== dest) {
      next(dest)
      return
    }
  }

  next()
})

export default router
