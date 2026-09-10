<template>
  <div class="home-view">
    <div class="home-header">
      <h1>欢迎使用图书借阅系统</h1>
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的借阅概览</span>
              <el-button text type="primary" @click="$router.push('/reader/my-loans')">查看全部</el-button>
            </div>
          </template>
          <div v-loading="loadingLoans">
            <el-statistic title="在借数量" :value="activeCount" />
            <el-statistic title="逾期未还" :value="overdueCount" class="stat-warn" />
            <div class="btn-row">
              <el-button type="primary" :icon="Collection" @click="$router.push('/reader/borrow')">
                去借阅
              </el-button>
              <el-button :icon="Refresh" @click="$router.push('/reader/renew')">
                去续借
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>通知 ({{ unreadCount }} 条未读)</span>
              <el-button text type="primary" :disabled="unreadCount === 0" @click="markAllRead">
                全部已读
              </el-button>
            </div>
          </template>
          <div v-loading="loadingNotices">
            <el-timeline v-if="notifications.length">
              <el-timeline-item
                v-for="n in notifications"
                :key="n.id"
                :timestamp="formatTime(n.createdAt)"
                :type="n.read ? 'info' : 'warning'"
              >
                <span :style="{ fontWeight: n.read ? 'normal' : '600' }">{{ n.content }}</span>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无通知" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Collection, Refresh } from '@element-plus/icons-vue'
import { http } from '@/api/http'

const notifications = ref([])
const unreadCount = ref(0)
const loadingNotices = ref(false)
const activeCount = ref(0)
const overdueCount = ref(0)
const loadingLoans = ref(false)

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function loadNotifications() {
  loadingNotices.value = true
  try {
    const [list, count] = await Promise.all([
      http.get('/notifications/my'),
      http.get('/notifications/my/unread-count')
    ])
    notifications.value = list
    unreadCount.value = count.count || 0
  } catch (err) {
    ElMessage.error(err.message || '加载通知失败')
  } finally {
    loadingNotices.value = false
  }
}

async function loadLoans() {
  loadingLoans.value = true
  try {
    const data = await http.get('/loans/my', { page: 0, size: 50 })
    const items = data.content || []
    activeCount.value = items.filter((l) => l.status === 'ACTIVE').length
    overdueCount.value = items.filter((l) => l.status === 'OVERDUE').length
  } catch (err) {
    ElMessage.error(err.message || '加载借阅失败')
  } finally {
    loadingLoans.value = false
  }
}

async function markAllRead() {
  try {
    await http.post('/notifications/my/read-all')
    notifications.value.forEach((n) => (n.read = true))
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

onMounted(() => {
  loadNotifications()
  loadLoans()
})
</script>

<style scoped>
.home-view {
  padding: 20px;
}
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.home-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
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
</style>
