<template>
  <div class="admin-layout">
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
            <!-- 实时咨询：有待回复消息时图标右上角显示角标，点击进入帮助与反馈 -->
            <el-tooltip content="实时咨询" placement="bottom">
              <el-badge
                :value="pendingCount"
                :hidden="pendingCount === 0"
                :max="99"
                class="chat-badge"
              >
                <el-button
                  class="chat-entry-btn"
                  circle
                  size="small"
                  :icon="ChatDotRound"
                  @click="goHelpCenter"
                />
              </el-badge>
            </el-tooltip>
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
        <!-- 桌面端侧栏常驻；手机端由头部汉堡呼出抽屉（管理端不做底部 Tab） -->
        <el-aside v-if="!isMobile" width="200px">
          <LayoutSideMenu :items="menuItems" />
        </el-aside>
        <el-main>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useHelpSessions } from '@/composables/useHelpSessions'
import { useBreakpoint } from '@/composables/useBreakpoint'
import LayoutSideMenu, { type SideMenuItem } from '@/components/LayoutSideMenu.vue'
import { HomeFilled, Notebook, User, Document, Warning, Bell, Calendar, ArrowDown, Reading, QuestionFilled, ChatDotRound, Menu } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const { isMobile } = useBreakpoint()

// 跨回桌面宽度时收起手机端抽屉，避免遮罩残留（spec「跨越断点缩放窗口」）
watch(isMobile, (mobile) => {
  if (!mobile) menuOpen.value = false
})

// 顶部实时咨询角标：每 10 秒轮询会话与留言，有待处理消息时显示数量角标
const { pendingCount, targetHelpTab } = useHelpSessions()

/** 点击图标：优先去有未回复消息的页签（实时对话优先于留言管理） */
function goHelpCenter() {
  router.push(`/admin/help?tab=${targetHelpTab.value}`)
}

// 菜单单一数据源：桌面侧栏与手机抽屉共用（design.md D3）
const menuItems = computed<SideMenuItem[]>(() => [
  { index: 'home', icon: HomeFilled, label: '首页', to: '/admin' },
  { index: 'books', icon: Notebook, label: '图书编目', to: '/admin/books', matchPrefixes: ['/books'] },
  { index: 'readers', icon: User, label: '读者管理', to: '/admin/readers', matchPrefixes: ['/readers'] },
  { index: 'loans', icon: Document, label: '借阅流通', to: '/admin/loans', matchPrefixes: ['/loans'] },
  { index: 'penalties', icon: Warning, label: '逾期罚款', to: '/admin/penalties', matchPrefixes: ['/penalties'] },
  { index: 'announcements', icon: Bell, label: '公告管理', to: '/admin/announcements', matchPrefixes: ['/announcements'] },
  { index: 'activities', icon: Calendar, label: '活动管理', to: '/admin/activities', matchPrefixes: ['/activities'] },
  { index: 'help', icon: QuestionFilled, label: '帮助与反馈', to: '/admin/help', matchPrefixes: ['/help'] }
])

/* 手机端菜单抽屉 */
const menuOpen = ref(false)

const changePassword = () => {
  router.push('/change-password')
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  /* 外层不滚动：页面高度锁定为视口，滚动交给 el-main */
  overflow: hidden;
}

.el-container {
  height: 100%;
}

/* 外层容器：纵向 flex（header + 内容区），高度占满 */
.admin-layout > .el-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 内容区容器：横向 flex（aside + main），撑满剩余高度。
   min-height: 0 是关键 —— 否则 flex 子项不会收缩，内容溢出后会出现浏览器外层滚动条 */
.admin-layout > .el-container > .el-container {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: row;
}

/* 顶部导航固定不收缩 */
.admin-layout .el-header {
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

/* 实时咨询入口：圆形图标按钮，白色描边融入深色头部；角标用红色突出 */
.chat-badge {
  display: inline-flex;
}
.chat-entry-btn {
  --el-button-bg-color: rgba(255, 255, 255, 0.15);
  --el-button-border-color: rgba(255, 255, 255, 0.6);
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.28);
  --el-button-hover-border-color: #fff;
  --el-button-hover-text-color: #fff;
  width: 32px;
  height: 32px;
}
.chat-badge :deep(.el-badge__content) {
  z-index: 1;
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

/* ===== 手机端（<=768px）：汉堡入口 + 头部收紧（无底部 Tab） ===== */

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
  /* 「进入前台」按钮在小屏只保留图标，缓解头部拥挤（功能保留，spec「两端后台导航壳」） */
  .site-entry-btn :deep(span) {
    display: none;
  }
  .site-entry-btn {
    padding: 8px;
  }
}
</style>