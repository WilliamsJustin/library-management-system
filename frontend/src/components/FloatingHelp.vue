<template>
  <div class="floating-help">
    <!-- 悬浮球：有待回复的实时对话或留言时，右上角显示小红点 -->
    <el-badge :is-dot="hasPending" class="fab-badge">
      <button class="help-fab" title="帮助与反馈" @click="toggle">
        <el-icon :size="24"><component :is="panelOpen ? Close : ChatDotRound" /></el-icon>
      </button>
    </el-badge>

    <!-- 弹出面板 -->
    <transition name="help-pop">
      <div v-if="panelOpen" class="help-panel">
        <div class="panel-head">
          <span class="panel-title">帮助与反馈</span>
          <el-button text :icon="Close" size="small" @click="panelOpen = false" />
        </div>

        <div class="mode-tabs">
          <button
            v-for="m in modes"
            :key="m.key"
            class="mode-tab"
            :class="{ active: mode === m.key }"
            @click="mode = m.key"
          >{{ m.label }}</button>
        </div>

        <div class="panel-body">
          <!-- 管理员：咨询列表 + 留言管理；读者/游客：常见问题 + 人工咨询 -->
          <template v-if="isAdmin">
            <SessionList v-show="mode === 'sessions'" />
            <FeedbackList v-show="mode === 'feedback'" />
          </template>
          <template v-else>
            <FaqSearch v-show="mode === 'faq'" @to-chat="mode = 'chat'" @to-feedback="goFeedback" />
            <ChatPanel v-if="panelOpen && mode === 'chat'" :active="mode === 'chat'" class="chat-area" />
          </template>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Close } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { http } from '@/api/http'
import FaqSearch from '@/components/help/FaqSearch.vue'
import ChatPanel from '@/components/help/ChatPanel.vue'
import SessionList from '@/components/help/SessionList.vue'
import FeedbackList from '@/components/help/FeedbackList.vue'
import type { ChatSession, FeedbackMessage, PageResult } from '@/types'

/**
 * 前台右下角「帮助与反馈」悬浮窗（按登录身份切换面板内容）。
 *
 * 读者/游客：常见问题（未收录时引导转人工 / 去留言）+ 人工咨询对话，
 * 「去留言」跳到读者后台留言页（游客先引导登录）。
 * 管理员：两个页签——「咨询列表」（读者及其最新消息）与「留言管理」
 * （读者留言及留言时间），点击条目在面板内直接回复；时间均按自然日
 * 规则显示（utils/timeFormat）。
 *
 * 悬浮球红点（仅管理员）：每 10 秒轮询一次——有待回复的实时对话
 * （会话最后一条是读者发的）或未回复留言时，图标右上角显示小红点；
 * 全部处理完毕后自动消失。
 */
const router = useRouter()
const authStore = useAuthStore()

const isAdmin = computed(() => authStore.userRole === 'ADMIN')

/* -------- 悬浮球红点：待处理消息数（仅管理员轮询） -------- */
const pendingChat = ref(0)
const pendingFeedback = ref(0)
const hasPending = computed(() => pendingChat.value + pendingFeedback.value > 0)
let pollTimer: number | undefined

async function refreshPending() {
  if (!isAdmin.value) return
  try {
    const [sessions, feedback] = await Promise.all([
      http.get<ChatSession[]>('/help/chat/sessions'),
      http.get<PageResult<FeedbackMessage>>('/help/feedback', {
        page: 0,
        size: 1,
        status: 'UNREPLIED'
      })
    ])
    pendingChat.value = (sessions || []).filter((s) => s.lastSenderRole === 'READER').length
    pendingFeedback.value = feedback.totalElements || 0
  } catch {
    // 轮询失败静默
  }
}

watch(
  isAdmin,
  (admin) => {
    if (admin) {
      refreshPending()
      pollTimer = window.setInterval(refreshPending, 10_000)
    } else {
      pendingChat.value = 0
      pendingFeedback.value = 0
      if (pollTimer !== undefined) {
        window.clearInterval(pollTimer)
        pollTimer = undefined
      }
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (pollTimer !== undefined) window.clearInterval(pollTimer)
})

/* -------- 面板页签 -------- */
type PanelMode = 'faq' | 'chat' | 'sessions' | 'feedback'
const panelOpen = ref(false)
const mode = ref<PanelMode>('faq')

const readerModes = [
  { key: 'faq' as const, label: '常见问题' },
  { key: 'chat' as const, label: '人工咨询' }
]
const adminModes = [
  { key: 'sessions' as const, label: '咨询列表' },
  { key: 'feedback' as const, label: '留言管理' }
]
const modes = computed(() => (isAdmin.value ? adminModes : readerModes))

function toggle() {
  panelOpen.value = !panelOpen.value
  // 每次打开恢复默认页签：管理员看咨询列表，读者看常见问题
  if (panelOpen.value) mode.value = isAdmin.value ? 'sessions' : 'faq'
}

/** 留言板在读者后台，需要读者身份；游客先去登录并回跳 */
function goFeedback() {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录读者账号后再留言')
    router.push({ path: '/login', query: { redirect: '/reader/help' } })
    return
  }
  if (isAdmin.value) {
    ElMessage.warning('当前是管理员账号，请使用读者账号留言')
    return
  }
  panelOpen.value = false
  router.push({ path: '/reader/help', query: { tab: 'feedback' } })
}
</script>

<style scoped>
.floating-help {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}
.fab-badge {
  display: inline-flex;
}
/* 红点稍微外扩，避免被悬浮球的圆形裁掉观感 */
.fab-badge :deep(.el-badge__content.is-dot) {
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
}
.help-fab {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
  box-shadow: 0 6px 16px rgba(47, 107, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, box-shadow 0.15s;
}
.help-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.45);
}
.help-panel {
  position: absolute;
  right: 0;
  bottom: 64px;
  width: 360px;
  max-width: calc(100vw - 32px);
  height: 460px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(31, 35, 41, 0.16);
  overflow: hidden;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f2f3f5;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}
.mode-tabs {
  display: flex;
  gap: 8px;
  padding: 10px 16px 0;
}
.mode-tab {
  flex: 1;
  padding: 7px 0;
  font-size: 13px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #fff;
  color: #4e5969;
  cursor: pointer;
  transition: all 0.15s;
}
.mode-tab.active {
  color: #409eff;
  border-color: #409eff;
  background: #ecf5ff;
}
.panel-body {
  flex: 1;
  min-height: 0;
  padding: 12px 16px 16px;
  display: flex;
  flex-direction: column;
}
.panel-body > * {
  flex: 1;
  min-height: 0;
}
.chat-area {
  height: 100%;
}
/* 弹出动画 */
.help-pop-enter-active,
.help-pop-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.help-pop-enter-from,
.help-pop-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}
</style>
