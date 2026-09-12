<template>
  <div class="home-view">
    <el-row :gutter="16">
      <!-- 左盒：欢迎、借阅概览、罚款概览 -->
      <el-col :span="12">
        <!-- 欢迎小盒子：问候 + 实时时间与年月日 -->
        <el-card class="welcome-card">
          <div class="welcome-body">
            <div class="welcome-left">
              <div class="welcome-greet">{{ greeting }}，{{ authStore.user?.name || '读者' }}</div>
              <div class="welcome-date">{{ currentDate }}</div>
            </div>
            <div class="welcome-clock">{{ currentTime }}</div>
          </div>
        </el-card>

        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的借阅概览</span>
              <el-button text type="primary" @click="$router.push('/reader/my-loans')">查看全部</el-button>
            </div>
          </template>
          <!-- 与罚款概览同款横向布局：统计并排 + 操作按钮 -->
          <div v-loading="loadingLoans" class="overview-body">
            <el-statistic title="在借数量" :value="activeCount" />
            <el-statistic title="逾期未还" :value="overdueCount" :class="{ 'stat-warn': overdueCount > 0 }" />
            <div class="btn-row">
              <el-button type="primary" :icon="Collection" @click="$router.push('/reader/borrow')">
                去借阅
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 我的罚款概览：与借阅概览同风格，未缴情况一目了然 -->
        <el-card class="penalty-card">
          <template #header>
            <div class="card-header">
              <span>我的罚款概览</span>
              <el-button text type="primary" @click="$router.push('/reader/penalties')">查看全部</el-button>
            </div>
          </template>
          <div v-loading="loadingPenalties" class="overview-body">
            <el-statistic title="未缴总额（元）" :value="unpaidTotal" :precision="2" :class="{ 'stat-warn': unpaidTotal > 0 }" />
            <el-statistic title="未缴笔数" :value="unpaidCount" :class="{ 'stat-warn': unpaidCount > 0 }" />
            <div class="btn-row">
              <el-button type="primary" :icon="Warning" :disabled="unpaidCount === 0" @click="$router.push('/reader/penalties')">
                去缴费
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右盒：通知 -->
      <el-col :span="12">
        <el-card class="notice-card">
          <template #header>
            <div class="card-header">
              <span>通知 ({{ unreadCount }} 条未读)</span>
              <el-button text type="primary" :disabled="unreadCount === 0" @click="markAllRead">
                全部已读
              </el-button>
            </div>
          </template>
          <div v-loading="loadingNotices">
            <el-timeline v-if="pagedNotifications.length">
              <el-timeline-item
                v-for="n in pagedNotifications"
                :key="n.id"
                :timestamp="formatTime(n.createdAt)"
                :type="n.read ? 'info' : 'warning'"
              >
                <span :style="{ fontWeight: n.read ? 'normal' : '600' }">{{ n.content }}</span>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无通知" :image-size="60" />

            <!-- 一页最多 6 条，超出分页展示 -->
            <el-pagination
              v-if="notifications.length > noticePageSize"
              class="notice-pager"
              layout="prev, pager, next"
              small
              :total="notifications.length"
              :page-size="noticePageSize"
              :current-page="noticePage"
              @current-change="handleNoticePage"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Collection, Warning } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { AppNotification, Loan, PageResult, Penalty, UnreadCount } from '@/types'

const authStore = useAuthStore()

/* -------- 欢迎小盒子：问候语 + 实时时钟（每秒刷新） -------- */
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
  clockTimer = window.setInterval(() => (now.value = new Date()), 1000)
})

onBeforeUnmount(() => {
  if (clockTimer !== undefined) window.clearInterval(clockTimer)
})

const notifications = ref<AppNotification[]>([])
const unreadCount = ref(0)
const loadingNotices = ref(false)
const activeCount = ref(0)
const overdueCount = ref(0)
const loadingLoans = ref(false)

/** 通知卡片一页最多展示 5 条，超出用分页器翻页（客户端分页） */
const noticePageSize = 5
const noticePage = ref(1)
const pagedNotifications = computed(() => {
  const start = (noticePage.value - 1) * noticePageSize
  return notifications.value.slice(start, start + noticePageSize)
})

function handleNoticePage(page: number) {
  noticePage.value = page
}

function formatTime(value?: string | null) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function loadNotifications() {
  loadingNotices.value = true
  try {
    const [list, count] = await Promise.all([
      http.get<AppNotification[]>('/notifications/my'),
      http.get<UnreadCount>('/notifications/my/unread-count')
    ])
    notifications.value = list
    unreadCount.value = count.count || 0
    // 数据刷新后若当前页超出范围，回到第一页
    const maxPage = Math.max(1, Math.ceil(notifications.value.length / noticePageSize))
    if (noticePage.value > maxPage) noticePage.value = 1
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载通知失败'))
  } finally {
    loadingNotices.value = false
  }
}

async function loadLoans() {
  loadingLoans.value = true
  try {
    const data = await http.get<PageResult<Loan>>('/loans/my', { page: 0, size: 50 })
    const items = data.content || []
    activeCount.value = items.filter((l) => l.status === 'ACTIVE').length
    overdueCount.value = items.filter((l) => l.status === 'OVERDUE').length
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载借阅失败'))
  } finally {
    loadingLoans.value = false
  }
}

/** 罚款概览：与「我的罚款」页同口径，统计未缴总额与笔数 */
const loadingPenalties = ref(false)
const unpaidTotal = ref(0)
const unpaidCount = ref(0)

async function loadPenalties() {
  loadingPenalties.value = true
  try {
    const data = await http.get<PageResult<Penalty>>('/penalties/my', { page: 0, size: 50 })
    const unpaid = (data.content || []).filter((p) => p.status === 'UNPAID')
    unpaidCount.value = unpaid.length
    unpaidTotal.value = unpaid.reduce((sum, p) => sum + Number(p.amount || 0), 0)
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载罚款失败'))
  } finally {
    loadingPenalties.value = false
  }
}

async function markAllRead() {
  try {
    await http.post('/notifications/my/read-all')
    notifications.value.forEach((n) => (n.read = true))
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (err) {
    ElMessage.error(errorMessage(err, '操作失败'))
  }
}

onMounted(() => {
  loadNotifications()
  loadLoans()
  loadPenalties()
})
</script>

<style scoped>
.home-view {
  padding: 20px;
}
/* 左右盒子等高：列内纵向 flex，右侧通知卡片撑满与左列对齐 */
.home-view :deep(.el-col) {
  display: flex;
  flex-direction: column;
}
.notice-card {
  flex: 1;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-warn :deep(.el-statistic__content) {
  color: #f56c6c;
}
.mt {
  margin-top: 12px;
}
.btn-row {
  margin-top: 16px;
  display: flex;
  gap: 0;
}
.notice-pager {
  margin-top: 12px;
  justify-content: flex-end;
}
/* 欢迎小盒子：问候 + 实时时钟；flex:1 占据左列剩余高度，使左右两盒等高 */
.welcome-card {
  margin-bottom: 16px;
  flex: 1;
  min-height: 96px;
}
.welcome-card :deep(.el-card__body) {
  height: 100%;
  display: flex;
  align-items: center;
}
.welcome-body {
  flex: 1;
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
/* 左盒内两张卡片上下堆叠的间距 */
.penalty-card {
  margin-top: 16px;
}
/* 概览数据横向排布：两项统计 + 操作按钮（借阅/罚款概览共用） */
.overview-body {
  display: flex;
  align-items: center;
  gap: 48px;
}
</style>
