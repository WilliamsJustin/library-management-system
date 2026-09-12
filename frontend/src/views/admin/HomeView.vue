<template>
  <div class="home-view">
    <!-- 欢迎组件：问候 + 实时时间与年月日（与读者后台首页同款） -->
    <el-card class="welcome-card">
      <div class="welcome-body">
        <div class="welcome-left">
          <div class="welcome-greet">{{ greeting }}，{{ authStore.user?.name || '管理员' }}</div>
          <div class="welcome-date">{{ currentDate }}</div>
        </div>
        <div class="welcome-clock">{{ currentTime }}</div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <!-- 借阅流通概况：当日 / 本周 / 历史 的借阅数与逾期数 -->
      <el-col :span="14">
        <el-card class="widget-card">
          <template #header>
            <div class="card-header">
              <span>借阅流通概况</span>
              <el-button text type="primary" @click="$router.push('/admin/loans')">进入借阅流通</el-button>
            </div>
          </template>
          <div v-loading="loading" class="flow-grid">
            <div class="flow-row head">
              <span class="cell label" />
              <span class="cell">当日</span>
              <span class="cell">本周</span>
              <span class="cell">历史</span>
            </div>
            <div class="flow-row">
              <span class="cell label">借阅数</span>
              <span class="cell num">{{ stats?.loans.today ?? '—' }}</span>
              <span class="cell num">{{ stats?.loans.week ?? '—' }}</span>
              <span class="cell num">{{ stats?.loans.total ?? '—' }}</span>
            </div>
            <div class="flow-row">
              <span class="cell label">逾期数</span>
              <span class="cell num" :class="{ warn: (stats?.loans.overdueToday ?? 0) > 0 }">{{ stats?.loans.overdueToday ?? '—' }}</span>
              <span class="cell num" :class="{ warn: (stats?.loans.overdueWeek ?? 0) > 0 }">{{ stats?.loans.overdueWeek ?? '—' }}</span>
              <span class="cell num" :class="{ warn: (stats?.loans.overdueTotal ?? 0) > 0 }">{{ stats?.loans.overdueTotal ?? '—' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 逾期罚款概况：当日 / 历史罚款金额 -->
      <el-col :span="10">
        <el-card class="widget-card">
          <template #header>
            <div class="card-header">
              <span>逾期罚款概况</span>
              <el-button text type="primary" @click="$router.push('/admin/penalties')">进入逾期罚款</el-button>
            </div>
          </template>
          <div v-loading="loading" class="penalty-overview">
            <el-statistic title="当日罚款金额（元）" :value="stats?.penalty.todayAmount ?? 0" :precision="2"
              :class="{ 'stat-warn': (stats?.penalty.todayAmount ?? 0) > 0 }" />
            <el-statistic title="历史罚款金额（元）" :value="stats?.penalty.totalAmount ?? 0" :precision="2" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时咨询：实时对话 / 留言管理 未回复消息数 -->
    <el-card class="widget-card chat-card">
      <template #header>
        <div class="card-header">
          <span>
            实时咨询
            <el-badge v-if="totalPending > 0" :value="totalPending" :max="99" class="pending-badge">
              待处理
            </el-badge>
          </span>
          <el-button size="small" type="primary" plain @click="$router.push('/admin/help?tab=chat')">
            进入帮助与反馈
          </el-button>
        </div>
      </template>
      <div v-loading="loading" class="chat-body">
        <div class="chat-row" @click="$router.push('/admin/help?tab=chat')">
          <span class="chat-label">实时对话</span>
          <span class="chat-desc">读者等待管理员回复的会话</span>
          <span class="chat-num" :class="{ warn: (stats?.chat.chatPending ?? 0) > 0 }">
            {{ stats?.chat.chatPending ?? 0 }} 条待回复
          </span>
        </div>
        <div class="chat-row" @click="$router.push('/admin/help?tab=feedback')">
          <span class="chat-label">留言管理</span>
          <span class="chat-desc">读者留言等待回复</span>
          <span class="chat-num" :class="{ warn: (stats?.chat.feedbackPending ?? 0) > 0 }">
            {{ stats?.chat.feedbackPending ?? 0 }} 条未回复
          </span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { DashboardStats } from '@/types'

/**
 * 管理员首页仪表盘：欢迎组件 + 借阅流通概况（当日/本周/历史借阅数与逾期数）
 * + 逾期罚款概况（当日/历史罚款金额）+ 实时咨询（对话/留言未回复数）。
 * 统计数据 30 秒轮询一次，各卡片可点击跳到对应功能页。
 */
const authStore = useAuthStore()

const stats = ref<DashboardStats | null>(null)
const loading = ref(false)
let pollTimer: number | undefined

const totalPending = computed(() =>
  (stats.value?.chat.chatPending ?? 0) + (stats.value?.chat.feedbackPending ?? 0)
)

async function loadStats() {
  loading.value = stats.value === null
  try {
    stats.value = await http.get<DashboardStats>('/stats/overview')
  } catch (err) {
    if (stats.value === null) ElMessage.error(errorMessage(err, '加载统计数据失败'))
  } finally {
    loading.value = false
  }
}

/* -------- 欢迎组件：问候 + 实时时钟（每秒刷新） -------- */
const now = ref(new Date())
let clockTimer: number | undefined

const greeting = computed(() => {
  const h = now.value.getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const currentDate = computed(() => {
  const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const d = now.value
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()} 年 ${p(d.getMonth() + 1)} 月 ${p(d.getDate())} 日 ${WEEKDAYS[d.getDay()]}`
})

const currentTime = computed(() => {
  const d = now.value
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
})

onMounted(() => {
  loadStats()
  pollTimer = window.setInterval(loadStats, 30_000)
  clockTimer = window.setInterval(() => (now.value = new Date()), 1000)
})

onBeforeUnmount(() => {
  if (pollTimer !== undefined) window.clearInterval(pollTimer)
  if (clockTimer !== undefined) window.clearInterval(clockTimer)
})
</script>

<style scoped>
.home-view {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.welcome-card {
  margin-bottom: 16px;
}
.welcome-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.welcome-greet {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}
.welcome-date {
  margin-top: 4px;
  font-size: 13px;
  color: #86909c;
}
.welcome-clock {
  font-size: 30px;
  font-weight: 700;
  color: #409eff;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}
/* 同一行的卡片等高：el-col 拉伸为行高，卡片撑满所在列 */
.home-view :deep(.el-col) {
  display: flex;
}
.widget-card {
  flex: 1;
  margin-bottom: 16px;
}
.stat-warn :deep(.el-statistic__content) {
  color: #f56c6c;
}
/* 借阅流通：3 列（当日/本周/历史）× 2 行（借阅数/逾期数）网格 */
.flow-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.flow-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr 1fr;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
}
.flow-row:nth-child(n + 2) {
  background: #f7f8fa;
}
.flow-row.head {
  font-size: 13px;
  color: #86909c;
  background: transparent;
}
.cell {
  text-align: center;
  font-size: 14px;
  color: #1f2329;
}
.cell.label {
  text-align: left;
  color: #4e5969;
}
.cell.head {
  color: #86909c;
}
.cell.num {
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.cell.warn {
  color: #f56c6c;
}
/* 罚款概况：两个统计横向排布 */
.penalty-overview {
  display: flex;
  align-items: center;
  gap: 48px;
  min-height: 96px;
}
/* 实时咨询 */
.chat-card :deep(.el-card__body) {
  padding-top: 6px;
}
.pending-badge {
  margin-left: 10px;
}
.chat-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 14px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.chat-row:hover {
  border-color: #c6e2ff;
  background: #f7f9fc;
}
.chat-label {
  flex: 0 0 80px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.chat-desc {
  flex: 1;
  font-size: 13px;
  color: #86909c;
}
.chat-num {
  font-size: 13px;
  color: #4e5969;
}
.chat-num.warn {
  color: #f56c6c;
  font-weight: 600;
}
</style>
