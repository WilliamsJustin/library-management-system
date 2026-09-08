<template>
  <div class="public-layout">
    <header class="site-header">
      <div class="header-inner">
        <router-link to="/" class="brand">
          <el-icon class="brand-icon"><Reading /></el-icon>
          <span class="brand-name">学校图书借阅系统</span>
        </router-link>

        <nav class="nav">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/about" active-class="active">本馆概况</router-link>
          <router-link to="/services" active-class="active">读者服务</router-link>
          <router-link to="/activities" active-class="active">读者活动</router-link>
        </nav>

        <div class="header-actions">
          <template v-if="!authStore.isAuthenticated">
            <router-link to="/login" class="link-login">登录</router-link>
            <router-link to="/register" class="btn-register">注册</router-link>
          </template>
          <template v-else>
            <span class="welcome">你好，{{ authStore.user?.name }}</span>
            <router-link :to="authStore.userRole === 'admin' ? '/admin' : '/reader'" class="link-login">
              进入后台
            </router-link>
            <el-button text type="info" @click="logout">退出</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="site-main">
      <router-view />
    </main>

    <footer class="site-footer">
      <div class="footer-inner">
        <span>© 2026 学校图书借阅系统</span>
        <span class="muted">本系统仅供校内师生使用 · 服务热线 400-000-0000</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Reading } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const logout = () => {
  authStore.logout()
  router.push('/')
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
  border-bottom: 1px solid #e4e7ed;
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
  color: #1f2329;
  flex-shrink: 0;
}

.brand-icon {
  font-size: 26px;
  color: #409eff;
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
  color: #4e5969;
  font-size: 15px;
  padding: 6px 2px;
  position: relative;
  transition: color 0.2s;
}

.nav a:hover {
  color: #409eff;
}

.nav a.active {
  color: #409eff;
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
  background: #409eff;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.link-login {
  text-decoration: none;
  color: #4e5969;
  font-size: 15px;
}

.link-login:hover {
  color: #409eff;
}

.btn-register {
  text-decoration: none;
  background: #409eff;
  color: #fff;
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
  color: #4e5969;
}

.site-main {
  flex: 1;
}

.site-footer {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  margin-top: 40px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 24px;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #1f2329;
}

.footer-inner .muted {
  color: #86909c;
}
</style>
