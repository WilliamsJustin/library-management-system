<template>
  <div class="help-view">
    <h1>帮助与反馈</h1>

    <el-tabs v-model="activeTab">
      <!-- 常见问题：关键词检索 + 点击展开答案 -->
      <el-tab-pane label="常见问题" name="faq">
        <el-card>
          <FaqSearch @to-chat="activeTab = 'chat'" @to-feedback="activeTab = 'feedback'" />
        </el-card>
      </el-tab-pane>

      <!-- 在线咨询：与管理员实时对话 -->
      <el-tab-pane label="在线咨询" name="chat">
        <el-card class="chat-card">
          <ChatPanel :active="activeTab === 'chat'" />
        </el-card>
      </el-tab-pane>

      <!-- 留言反馈：提交留言并查看管理员的回复 -->
      <el-tab-pane label="留言反馈" name="feedback">
        <el-card class="feedback-card">
          <el-form @submit.prevent>
            <el-form-item label="留言内容">
              <el-input
                v-model="draft"
                type="textarea"
                resize="none"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="请描述您的问题或建议（最多 500 字），管理员会尽快回复"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submit">提交留言</el-button>
              <el-button :icon="RefreshLeft" @click="loadMine">刷新</el-button>
            </el-form-item>
          </el-form>

          <div v-loading="loading" class="mine-list">
            <el-empty v-if="!loading && mine.length === 0" description="还没有留言记录" />
            <div v-for="m in mine" :key="m.id" class="mine-item">
              <div class="mine-head">
                <span class="mine-time">{{ formatDateTime(m.createdAt) }}</span>
                <el-tag size="small" :type="m.status === 'REPLIED' ? 'success' : 'info'">
                  {{ m.status === 'REPLIED' ? '已回复' : '待回复' }}
                </el-tag>
              </div>
              <div class="mine-content">{{ m.content }}</div>
              <div v-if="m.replyContent" class="mine-reply">
                <div class="reply-meta">管理员（{{ m.repliedBy }}）回复于 {{ formatDateTime(m.repliedAt) }}</div>
                <div class="reply-content">{{ m.replyContent }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshLeft } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import FaqSearch from '@/components/help/FaqSearch.vue'
import ChatPanel from '@/components/help/ChatPanel.vue'
import type { FeedbackMessage, PageResult } from '@/types'

/**
 * 读者后台「帮助与反馈」：常见问题检索、与管理员实时对话、留言提交与回复查看。
 * 支持通过 ?tab= 直达某个页签（前台悬浮窗的「去留言」会跳到 feedback）。
 */
const route = useRoute()

const validTabs = ['faq', 'chat', 'feedback']
const qTab = typeof route.query.tab === 'string' && validTabs.includes(route.query.tab)
  ? route.query.tab
  : 'faq'
const activeTab = ref(qTab)

/* -------- 留言 -------- */
const draft = ref('')
const mine = ref<FeedbackMessage[]>([])
const loading = ref(false)
const submitting = ref(false)

async function submit() {
  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('请填写留言内容')
    return
  }
  submitting.value = true
  try {
    await http.post('/help/feedback', { content })
    ElMessage.success('留言已提交，等待管理员回复')
    draft.value = ''
    loadMine()
  } catch (err) {
    ElMessage.error(errorMessage(err, '提交失败'))
  } finally {
    submitting.value = false
  }
}

async function loadMine() {
  loading.value = true
  try {
    const data = await http.get<PageResult<FeedbackMessage>>('/help/feedback/my', { page: 0, size: 50 })
    mine.value = data.content || []
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载留言失败'))
  } finally {
    loading.value = false
  }
}

function formatDateTime(value?: string | null) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(loadMine)
</script>

<style scoped>
.help-view {
  padding: 20px;
}
.help-view h1 {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
}
.chat-card :deep(.el-card__body) {
  height: 520px;
}
.feedback-card :deep(.el-form-item) {
  margin-bottom: 12px;
}
.mine-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 80px;
}
.mine-item {
  border: 1px solid #eef0f3;
  border-radius: 8px;
  padding: 12px 14px;
}
.mine-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.mine-time {
  font-size: 12px;
  color: #a9aeb8;
}
.mine-content {
  font-size: 14px;
  color: #1f2329;
  line-height: 1.6;
  white-space: pre-wrap;
}
.mine-reply {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 6px;
}
.reply-meta {
  font-size: 12px;
  color: #67c23a;
  margin-bottom: 4px;
}
.reply-content {
  font-size: 13px;
  color: #1f2329;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
