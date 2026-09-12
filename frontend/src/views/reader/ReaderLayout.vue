<template>
  <div class="reader-layout">
    <el-container>
      <el-header>
        <el-row align="middle" class="header-row">
          <el-col :span="6" class="brand">
            <el-icon class="brand-icon"><Reading /></el-icon>
            <span class="brand-title">图书借阅系统</span>
          </el-col>
          <el-col :span="18" class="header-right">
            <el-button
              class="site-entry-btn"
              type="primary"
              plain
              size="small"
              :icon="HomeFilled"
              @click="router.push('/')"
            >进入前台</el-button>
            <el-dropdown>
              <span class="el-dropdown-link">
                {{ authStore.user?.name }} <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="changePassword">修改密码</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-col>
        </el-row>
      </el-header>
      <el-container>
        <el-aside width="200px">
          <el-menu :default-active="activeMenu" class="el-menu-vertical">
            <el-menu-item index="home" @click="router.push('/reader')">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="borrow" @click="router.push('/reader/borrow')">
              <el-icon><Collection /></el-icon>
              <span>图书借阅</span>
            </el-menu-item>
            <el-menu-item index="favorites" @click="router.push('/reader/favorites')">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="my-loans" @click="router.push('/reader/my-loans')">
              <el-icon><Document /></el-icon>
              <span>借阅查询</span>
            </el-menu-item>
            <el-menu-item index="penalties" @click="router.push('/reader/penalties')">
              <el-icon><Warning /></el-icon>
              <span>我的罚款</span>
            </el-menu-item>
            <el-menu-item v-if="isTeacher" index="announcements" @click="router.push('/reader/announcements')">
              <el-icon><Bell /></el-icon>
              <span>发布公告</span>
            </el-menu-item>
            <el-menu-item v-if="isTeacher" index="activities" @click="router.push('/reader/activities')">
              <el-icon><Calendar /></el-icon>
              <span>活动管理</span>
            </el-menu-item>
            <el-menu-item index="help" @click="router.push('/reader/help')">
              <el-icon><QuestionFilled /></el-icon>
              <span>帮助与反馈</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { HomeFilled, Document, Bell, Calendar, ArrowDown, Reading, Warning, Collection, Star, QuestionFilled } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 教师专属：发布公告 / 活动管理入口（管理员在后台「公告管理」「活动管理」）
const isTeacher = computed(() => authStore.userReaderType === 'TEACHER')

const activeMenu = ref('home')

/** 路径前缀 → 菜单 index 映射（顺序敏感：/help 要放在 /renew 等之后无冲突，前缀唯一即可） */
const MENU_PATHS: Array<[string, string]> = [
  ['/borrow', 'borrow'],
  ['/favorites', 'favorites'],
  ['/my-loans', 'my-loans'],
  ['/penalties', 'penalties'],
  ['/announcements', 'announcements'],
  ['/activities', 'activities'],
  ['/help', 'help']
]

// 响应式监听路由：布局内跳转（如首页点「查看全部」进借阅查询）也能同步高亮
watch(
  () => router.currentRoute.value.path,
  (path) => {
    const hit = MENU_PATHS.find(([prefix]) => path.includes(prefix))
    activeMenu.value = hit ? hit[1] : 'home'
  },
  { immediate: true }
)

const changePassword = () => {
  router.push('/change-password')
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.reader-layout {
  height: 100vh;
  /* 外层不滚动：页面高度锁定为视口，滚动交给 el-main */
  overflow: hidden;
}

.el-container {
  height: 100%;
}

/* 外层容器：纵向 flex（header + 内容区），高度占满 */
.reader-layout > .el-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 内容区容器：横向 flex（aside + main），撑满剩余高度。
   min-height: 0 是关键 —— 否则 flex 子项不会收缩，内容溢出后会出现浏览器外层滚动条 */
.reader-layout > .el-container > .el-container {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: row;
}

/* 顶部导航固定不收缩 */
.reader-layout .el-header {
  flex: 0 0 auto;
}

.el-header {
  background: linear-gradient(90deg, #409eff 0%, #2f6bff 100%);
  color: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 21, 41, 0.18);
  position: relative;
  z-index: 10;
}

.header-row {
  width: 100%;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}

.brand-icon {
  font-size: 22px;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  white-space: nowrap;
}

.header-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 14px;
  height: 100%;
}

.site-entry-btn {
  --el-button-bg-color: rgba(255, 255, 255, 0.15);
  --el-button-border-color: rgba(255, 255, 255, 0.6);
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.28);
  --el-button-hover-border-color: #fff;
  --el-button-hover-text-color: #fff;
}

.el-dropdown-link {
  cursor: pointer;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 4px;
  outline: none;
}

.el-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  /* 侧栏固定不动，菜单过长时自身内部滚动 */
  flex: 0 0 200px;
}

.el-menu {
  border-right: none;
  padding: 8px 0;
}

.el-menu .el-menu-item.is-active {
  background: #ecf5ff;
  border-right: 3px solid #409eff;
  /* 激活项加一层浅蓝投影，突出当前页签 */
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.25);
}

.el-main {
  background: #f0f2f5;
  padding: 0;
  /* 仅内容区内部滚动 */
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}
</style>