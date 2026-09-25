<template>
  <div class="reader-layout">
    <el-container>
      <el-header>
        <el-row align="middle" class="header-row">
          <el-col :span="6" class="brand">
            <!-- 手机端汉堡入口：打开菜单抽屉 -->
            <button v-if="isMobile" class="hamburger" aria-label="打开菜单" @click="menuOpen = true">
              <el-icon :size="22"><Menu /></el-icon>
            </button>
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
        <!-- 桌面端侧栏常驻；手机端隐藏（v-if），由头部汉堡呼出抽屉 -->
        <el-aside v-if="!isMobile" width="200px">
          <LayoutSideMenu :items="menuItems" />
        </el-aside>
        <el-main :class="{ 'with-tabbar': isMobile }">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <!-- 手机端：菜单抽屉（与桌面侧栏同一份 menuItems） -->
    <el-drawer
      v-model="menuOpen"
      direction="ltr"
      size="260px"
      :with-header="false"
      append-to-body
    >
      <LayoutSideMenu :items="menuItems" @select="menuOpen = false" />
    </el-drawer>

    <!-- 手机端：底部标签栏（首页/图书借阅/借阅查询/我的罚款 + 我的） -->
    <nav v-if="isMobile" class="tabbar">
      <button
        v-for="t in mainTabs"
        :key="t.index"
        class="tab-item"
        :class="{ active: activeTab === t.index }"
        @click="router.push(t.to)"
      >
        <el-icon :size="20"><component :is="t.icon" /></el-icon>
        <span>{{ t.label }}</span>
      </button>
      <button
        class="tab-item"
        :class="{ active: moreActive }"
        @click="moreOpen = true"
      >
        <el-icon :size="20"><Grid /></el-icon>
        <span>我的</span>
      </button>
    </nav>

    <!-- 手机端：「我的」底部弹层，收纳其余菜单项（我的收藏/帮助与反馈/教师专属） -->
    <el-drawer
      v-model="moreOpen"
      direction="btt"
      size="auto"
      :with-header="false"
      append-to-body
      class="more-drawer"
    >
      <div class="more-title">我的</div>
      <LayoutSideMenu :items="moreItems" :fallback-to-first="false" @select="moreOpen = false" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useBreakpoint } from '@/composables/useBreakpoint'
import LayoutSideMenu, { matchMenuIndex, type SideMenuItem } from '@/components/LayoutSideMenu.vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Document, Bell, Calendar, ArrowDown, Reading, Warning, Collection, Star, QuestionFilled, Menu, Grid } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isMobile } = useBreakpoint()

// 跨回桌面宽度时收起手机端抽屉，避免遮罩残留（spec「跨越断点缩放窗口」）
watch(isMobile, (mobile) => {
  if (!mobile) {
    menuOpen.value = false
    moreOpen.value = false
  }
})

// 教师专属：发布公告 / 活动管理入口（管理员在后台「公告管理」「活动管理」）
const isTeacher = computed(() => authStore.userReaderType === 'TEACHER')

// 菜单单一数据源：桌面侧栏与手机抽屉/底部 Tab 共用（design.md D3/D4）
const menuItems = computed<SideMenuItem[]>(() => [
  { index: 'home', icon: HomeFilled, label: '首页', to: '/reader' },
  { index: 'borrow', icon: Collection, label: '图书借阅', to: '/reader/borrow', matchPrefixes: ['/borrow'] },
  { index: 'favorites', icon: Star, label: '我的收藏', to: '/reader/favorites', matchPrefixes: ['/favorites'] },
  { index: 'my-loans', icon: Document, label: '借阅查询', to: '/reader/my-loans', matchPrefixes: ['/my-loans'] },
  { index: 'penalties', icon: Warning, label: '我的罚款', to: '/reader/penalties', matchPrefixes: ['/penalties'] },
  { index: 'announcements', icon: Bell, label: '发布公告', to: '/reader/announcements', matchPrefixes: ['/announcements'], visible: isTeacher.value },
  { index: 'activities', icon: Calendar, label: '活动管理', to: '/reader/activities', matchPrefixes: ['/activities'], visible: isTeacher.value },
  { index: 'help', icon: QuestionFilled, label: '帮助与反馈', to: '/reader/help', matchPrefixes: ['/help'] }
])

/* 手机端菜单抽屉 */
const menuOpen = ref(false)

/* 手机端底部 Tab：四个主入口 + 「我的」弹层 */
const mainTabs = [
  { index: 'home', label: '首页', icon: HomeFilled, to: '/reader' },
  { index: 'borrow', label: '图书借阅', icon: Collection, to: '/reader/borrow' },
  { index: 'my-loans', label: '借阅查询', icon: Document, to: '/reader/my-loans' },
  { index: 'penalties', label: '我的罚款', icon: Warning, to: '/reader/penalties' }
]
const MAIN_INDEXES = new Set(mainTabs.map((t) => t.index))

const activeTab = computed(() => matchMenuIndex(mainTabs, route.path))
const moreActive = computed(() => !MAIN_INDEXES.has(matchMenuIndex(menuItems.value, route.path)))
const moreItems = computed(() => menuItems.value.filter((it) => !MAIN_INDEXES.has(it.index)))
const moreOpen = ref(false)

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

/* ===== 手机端（<=768px）：汉堡、底部 Tab、间距收紧 ===== */

/* 汉堡按钮：桌面隐藏 */
.hamburger {
  display: none;
  border: none;
  background: transparent;
  color: #fff;
  cursor: pointer;
  padding: 8px;
  margin-right: 4px;
  min-width: 40px;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 底部标签栏：固定视口底部，白色底 + 顶部细线 */
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 500;
  display: flex;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -2px 8px rgba(0, 21, 41, 0.06);
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-height: 56px;
  border: none;
  background: transparent;
  color: #4e5969;
  font-size: 11px;
  cursor: pointer;
  padding: 4px 0;
}
.tab-item span {
  white-space: nowrap;
}
.tab-item.active {
  color: #409eff;
  font-weight: 600;
}

/* 「我的」底部弹层标题 */
.more-title {
  padding: 14px 16px 2px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

/* 有底部 Tab 时给内容区预留高度，避免最后一项被遮挡 */
.el-main.with-tabbar {
  padding-bottom: calc(58px + env(safe-area-inset-bottom)) !important;
}

@media (max-width: 768px) {
  .hamburger {
    display: inline-flex;
  }
  .el-header {
    padding: 0 12px;
  }
  /* 放开 el-col 固定 25%/75% 宽度，让品牌名与右侧操作区按内容自适应 */
  .header-row .el-col:first-child {
    flex: 1 1 auto;
    width: auto;
    max-width: none;
  }
  .header-row .el-col:last-child {
    flex: 0 0 auto;
    width: auto;
    max-width: none;
  }
  .brand {
    gap: 6px;
  }
  .brand-title {
    font-size: 15px;
    letter-spacing: 0;
  }
  .header-right {
    gap: 8px;
  }
  /* 「进入前台」小屏只保留图标，避免挤压品牌名（功能保留） */
  .site-entry-btn :deep(span) {
    display: none;
  }
  .site-entry-btn {
    padding: 8px;
  }
}
</style>