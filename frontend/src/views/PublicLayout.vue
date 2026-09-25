<template>
  <div class="public-layout">
    <header class="site-header">
      <div class="header-inner">
        <!-- 手机端汉堡入口（<=768px 显示，桌面隐藏） -->
        <button v-if="isMobile" class="hamburger" aria-label="打开导航菜单" @click="mobileMenuOpen = true">
          <el-icon :size="22"><Menu /></el-icon>
        </button>
        <router-link to="/" class="brand">
          <el-icon class="brand-icon"><Reading /></el-icon>
          <span class="brand-name">学校图书借阅系统</span>
        </router-link>

        <nav class="nav">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <div class="nav-item has-dropdown">
            <router-link to="/about" class="nav-link" :class="{ active: isAboutActive }">本馆概况</router-link>
            <div class="dropdown">
              <router-link class="dropdown-item" :class="{ current: route.path === '/about' }" to="/about">本馆简介</router-link>
              <router-link class="dropdown-item" :class="{ current: route.path === '/about/rules' }" to="/about/rules">规章制度</router-link>
              <router-link class="dropdown-item" :class="{ current: route.path === '/about/floors' }" to="/about/floors">开放时间</router-link>
            </div>
          </div>
          <router-link to="/services" active-class="active">读者服务</router-link>
          <router-link to="/activities" active-class="active">读者活动</router-link>
        </nav>

        <div class="header-actions">
          <template v-if="!authStore.isAuthenticated">
            <router-link :to="{ path: '/login', query: { redirect: route.fullPath } }" class="link-login">登录</router-link>
            <router-link to="/register" class="btn-register">注册</router-link>
          </template>
          <template v-else>
            <router-link :to="isAdmin ? '/admin' : '/reader'" class="link-login">
              进入后台
            </router-link>
            <el-dropdown>
              <span class="welcome user-trigger">
                {{ authStore.user?.name }}
                <el-icon class="trigger-caret"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="changePassword">修改密码</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>

    <main class="site-main">
      <router-view />
    </main>

    <footer class="site-footer">
      <div class="footer-inner">
        <div class="footer-info">
          <nav class="footer-nav">
            <button
              v-for="item in footerNav"
              :key="item.key"
              class="footer-nav-item"
              :class="{ active: activeFooter === item.key }"
              @click="onFooterNavClick(item)"
            >
              <el-icon class="footer-nav-icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </button>
          </nav>
          <div class="footer-content">
            <div v-if="activeFooter === 'traffic'" class="footer-content-block">
              <div class="footer-content-title">详细地址</div>
              <p>地铁 1 号线 · 图书馆站 B 口；公交 12 / 34 / 56 路至校南门。</p>
              <p>测试地址：示范市示范区示范路 1 号 学校图书馆</p>
            </div>
            <div v-else class="footer-content-block">
              <div class="footer-content-title">开馆时间</div>
              <p>周一至周五 8:00–22:00；周末及节假日 9:00–21:00。</p>
              <p>国家法定节假日另行通知，寒暑假开放时间以馆内公告为准。</p>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <span>© 2026 学校图书借阅系统</span>
          <span class="muted">本系统仅供校内师生使用 · 服务热线 400-000-0000</span>
        </div>
      </div>
    </footer>

    <!-- 前台右下角「帮助与反馈」悬浮窗（FAQ 检索 / 转人工 / 去留言） -->
    <FloatingHelp />

    <!-- 手机端导航抽屉（<=768px）：一级导航平铺 + 本馆概况子项 + 登录/注册或后台入口 -->
    <el-drawer
      v-model="mobileMenuOpen"
      direction="ltr"
      size="300px"
      :with-header="false"
      append-to-body
      class="mobile-nav-drawer"
    >
      <nav class="drawer-nav">
        <router-link class="drawer-link" :class="{ active: route.path === '/' }" to="/" @click="closeDrawer">首页</router-link>
        <div class="drawer-group">本馆概况</div>
        <router-link class="drawer-link sub" :class="{ active: route.path === '/about' }" to="/about" @click="closeDrawer">本馆简介</router-link>
        <router-link class="drawer-link sub" :class="{ active: route.path === '/about/rules' }" to="/about/rules" @click="closeDrawer">规章制度</router-link>
        <router-link class="drawer-link sub" :class="{ active: route.path === '/about/floors' }" to="/about/floors" @click="closeDrawer">开放时间</router-link>
        <router-link class="drawer-link" :class="{ active: route.path.startsWith('/services') }" to="/services" @click="closeDrawer">读者服务</router-link>
        <router-link class="drawer-link" :class="{ active: route.path.startsWith('/activities') }" to="/activities" @click="closeDrawer">读者活动</router-link>
        <div class="drawer-divider"></div>
        <template v-if="!authStore.isAuthenticated">
          <router-link class="drawer-link" :to="{ path: '/login', query: { redirect: route.fullPath } }" @click="closeDrawer">登录</router-link>
          <router-link class="drawer-link" to="/register" @click="closeDrawer">注册</router-link>
        </template>
        <template v-else>
          <router-link class="drawer-link" :to="isAdmin ? '/admin' : '/reader'" @click="closeDrawer">进入后台</router-link>
          <button class="drawer-link" @click="changePassword">修改密码</button>
          <button class="drawer-link" @click="logout">退出登录</button>
        </template>
      </nav>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useBreakpoint } from '@/composables/useBreakpoint'
import { Reading, ArrowDown, Location, Clock, Menu } from '@element-plus/icons-vue'
import FloatingHelp from '@/components/FloatingHelp.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isMobile } = useBreakpoint()

// 后端返回的角色是大写（ADMIN/READER），比较前统一小写。
// 原先直接与 'admin' 比较恒为 false，会导致管理员点「进入后台」也被送到读者端。
const isAdmin = computed(() => authStore.userRole?.toLowerCase() === 'admin')

// 「本馆概况」导航激活标识：本馆简介/规章制度/开放时间 任一子页均保持高亮。
// 不能依赖 router-link 的 active-class —— /about/rules 等是独立路由记录，不会激活 /about 链接。
const isAboutActive = computed(() => route.path === '/about' || route.path.startsWith('/about/'))

// 手机端导航抽屉
const mobileMenuOpen = ref(false)
// 跨回桌面宽度时收起抽屉，避免遮罩残留（spec「跨越断点缩放窗口」）
watch(isMobile, (mobile) => {
  if (!mobile) mobileMenuOpen.value = false
})
const closeDrawer = () => {
  mobileMenuOpen.value = false
}

// 前台退出：清除会话后停留在当前路由，不跳转首页（页面均为公开内容，无需重定向）
const logout = () => {
  closeDrawer()
  authStore.logout()
}

const changePassword = () => {
  closeDrawer()
  router.push('/change-password')
}

// 页脚左侧导航（图标 + 文字按钮），与右侧内容区以竖线分隔
interface FooterNavItem {
  key: string
  label: string
  icon: Component
}
const footerNav: FooterNavItem[] = [
  { key: 'traffic', label: '交通信息', icon: Location },
  { key: 'opening', label: '开馆时间', icon: Clock }
]
const activeFooter = ref('traffic')

// 页脚导航点击：开馆时间跳转到「本馆概况 - 开放时间」页面，其余切换右侧内容区
function onFooterNavClick(item: FooterNavItem) {
  if (item.key === 'opening') {
    router.push('/about/floors')
    return
  }
  activeFooter.value = item.key
}
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--sl-text);
  flex-shrink: 0;
}

.brand-icon {
  font-size: 26px;
  color: var(--sl-primary-strong);
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 28px;
  flex: 1;
}

.nav a {
  text-decoration: none;
  color: var(--sl-text-secondary);
  font-size: 15px;
  padding: 6px 2px;
  position: relative;
  transition: color 0.2s;
}

.nav a:hover {
  color: var(--sl-primary-strong);
}

.nav a.active {
  color: var(--sl-primary-strong);
  font-weight: 600;
}

.nav a.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -4px;
  height: 3px;
  border-radius: 2px;
  background: var(--sl-primary);
}

/* 带下拉的导航项（鼠标悬停展开） */
.nav-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  height: 100%;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 150px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.10);
  padding: 6px 0;
  display: none;
  flex-direction: column;
  z-index: 200;
}

.nav-item.has-dropdown:hover .dropdown,
.nav-item.has-dropdown:focus-within .dropdown {
  display: flex;
}

.nav .dropdown-item {
  padding: 9px 18px;
  color: var(--sl-text-secondary);
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.15s, background 0.15s;
}

.nav .dropdown-item:hover {
  background: var(--el-color-primary-light-9);
  color: var(--sl-primary-strong);
}

/* 当前子页在下拉菜单中的激活标识 */
.nav .dropdown-item.current {
  color: var(--sl-primary-strong);
  font-weight: 600;
  background: var(--el-color-primary-light-9);
}

.nav .dropdown-item.current::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--sl-primary);
  margin-right: 8px;
  vertical-align: 2px;
}

.nav .dropdown-item::after {
  display: none;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.link-login {
  text-decoration: none;
  color: var(--sl-text-secondary);
  font-size: 15px;
}

.link-login:hover {
  color: var(--sl-primary-strong);
}

.btn-register {
  text-decoration: none;
  background: var(--sl-primary);
  color: #000;
  font-size: 14px;
  padding: 8px 18px;
  border-radius: 6px;
  transition: background 0.2s;
}

.btn-register:hover {
  background: #337ecc;
}

.welcome {
  font-size: 14px;
  color: var(--sl-text-secondary);
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  outline: none;
  user-select: none;
}

.user-trigger:hover {
  color: var(--sl-primary-strong);
}

.trigger-caret {
  font-size: 12px;
}

.site-main {
  flex: 1;
}

.site-footer {
  background: #fff;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 40px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  font-size: 13px;
  color: var(--sl-text);
}

.footer-info {
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
}

.footer-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-right: 32px;
  border-right: 1px solid var(--el-border-color-lighter);
  min-width: 150px;
}

.footer-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  color: var(--sl-text-secondary);
  font-size: 14px;
  cursor: pointer;
  border-radius: 6px;
  text-align: left;
  transition: background 0.15s ease, color 0.15s ease;
}

.footer-nav-item:hover {
  background: var(--el-color-primary-light-9);
  color: var(--sl-primary-strong);
}

.footer-nav-icon {
  font-size: 16px;
}

.footer-content {
  flex: 1;
  min-width: 240px;
}

.footer-content-block p {
  margin: 6px 0;
  color: #6b7280;
  line-height: 1.7;
}

.footer-content-title {
  font-weight: 600;
  color: var(--sl-text);
  font-size: 14px;
  margin-bottom: 4px;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #eef0f3;
}

.footer-inner .muted {
  color: var(--sl-text-secondary);
}

/* ===== 手机端（<=768px）：桌面优先、只在此媒体查询内覆盖 ===== */

/* 汉堡按钮：桌面隐藏，手机端由媒体查询打开 */
.hamburger {
  display: none;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--sl-text);
  padding: 8px;
  min-width: 40px;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 抽屉导航（仅手机端渲染） */
.drawer-nav {
  display: flex;
  flex-direction: column;
  padding: 12px 10px;
}
.drawer-link {
  display: flex;
  align-items: center;
  min-height: 46px;
  padding: 0 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: #303133;
  font-size: 15px;
  text-decoration: none;
  cursor: pointer;
  text-align: left;
}
.drawer-link.sub {
  padding-left: 26px;
  color: var(--sl-text-secondary);
  font-size: 14px;
}
.drawer-link:hover,
.drawer-link.active {
  background: var(--el-color-primary-light-9);
  color: var(--sl-primary-strong);
  font-weight: 600;
}
.drawer-group {
  padding: 14px 12px 4px;
  font-size: 12px;
  color: var(--sl-text-secondary);
}
.drawer-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 10px 4px;
}

@media (max-width: 768px) {
  .header-inner {
    height: 56px;
    padding: 0 12px;
    gap: 8px;
  }
  .hamburger {
    display: inline-flex;
  }
  .brand-name {
    font-size: 16px;
  }
  .nav {
    display: none;
  }
  .header-actions {
    display: none;
  }
  /* 页脚：左导航与内容区纵向堆叠，去竖线分隔 */
  .footer-inner {
    padding: 16px 16px;
  }
  .footer-info {
    flex-direction: column;
    gap: 14px;
  }
  .footer-nav {
    flex-direction: row;
    min-width: 0;
    padding-right: 0;
    padding-bottom: 10px;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  .footer-nav-item {
    flex: 1;
    justify-content: center;
  }
  .footer-content {
    min-width: 0;
  }
  .footer-bottom {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}

</style>
