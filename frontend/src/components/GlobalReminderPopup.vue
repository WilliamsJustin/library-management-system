<template>
  <el-dialog
    v-model="visible"
    title="逾期提醒"
    width="460px"
    align-center
    append-to-body
    class="reminder-popup"
  >
    <div class="reminder-body">
      <ul v-if="reminders.length" class="reminder-list">
        <li v-for="r in reminders" :key="r.id" class="reminder-item">
          <el-icon class="bell" :size="20"><Bell /></el-icon>
          <div class="texts">
            <div class="content">{{ r.content }}</div>
            <div class="time">{{ formatTime(r.createdAt) }}</div>
          </div>
        </li>
      </ul>
    </div>
    <template #footer>
      <el-button type="primary" @click="visible = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { AppNotification } from '@/types'

/**
 * 全局「逾期提醒」弹窗（挂在 App.vue，覆盖站内任一网页）。
 *
 * 逾期/到期提醒是**读者私有消息**：A 读者的提醒只有 A 读者可见。
 * 本组件只对登录的读者生效——拉取自己的站内消息（/notifications/my，
 * 后端按登录身份隔离），把其中 type = REMINDER 且未读、且本浏览器
 * 还没弹过的提醒以弹窗展示，每条只弹一次（已弹的 id 记在 localStorage）。
 * 游客与管理员不弹；拉取失败静默忽略（弹窗是附加能力，不影响正常浏览）。
 */
const route = useRoute()
const authStore = useAuthStore()

const visible = ref(false)
const reminders = ref<AppNotification[]>([])

/** 已弹提醒的本地存储键：{ 消息id: 弹出时间戳 }，每条提醒每个浏览器只弹一次 */
const SHOWN_KEY = 'help-reminder-shown'

function loadShown(): Record<string, number> {
  try {
    return JSON.parse(localStorage.getItem(SHOWN_KEY) || '{}')
  } catch {
    return {}
  }
}

function markShown(ids: number[]) {
  const shown = loadShown()
  const now = Date.now()
  for (const id of ids) shown[id] = now
  // 只保留最近 200 条记录，防止无限膨胀
  const entries = Object.entries(shown).sort((a, b) => b[1] - a[1])
  localStorage.setItem(
    SHOWN_KEY,
    JSON.stringify(Object.fromEntries(entries.slice(0, 200)))
  )
}

async function refreshAndShow() {
  // 提醒是读者私有消息：游客没有消息可拉，管理员不需要提醒
  if (!authStore.isAuthenticated || authStore.userRole === 'ADMIN') return
  try {
    const data = await http.get<AppNotification[]>('/notifications/my')
    reminders.value = (data || []).filter(
      (n) => n.type === 'REMINDER' && !n.read
    )

    const shown = loadShown()
    const fresh = reminders.value.filter((r) => !shown[r.id])
    if (fresh.length) {
      markShown(fresh.map((r) => r.id))
      visible.value = true
    }
  } catch {
    // 静默：消息拉取失败不影响页面使用
  }
}

function formatTime(value: string): string {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

// 首次加载 + 每次切换页面都检查一次（有自己未读的新提醒才弹）
watch(
  () => route.fullPath,
  () => refreshAndShow()
)

onMounted(() => {
  refreshAndShow()
})
</script>

<style scoped>
.reminder-body {
  max-height: 320px;
  overflow-y: auto;
}
.reminder-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.reminder-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 12px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
}
.bell {
  color: #e6a23c;
  margin-top: 2px;
}
.texts {
  flex: 1;
  min-width: 0;
}
.content {
  font-size: 14px;
  color: #1f2329;
  line-height: 1.6;
}
.time {
  margin-top: 4px;
  font-size: 12px;
  color: #a9aeb8;
}
</style>
