<template>
  <div class="feedback-list">
    <!-- 列表视图：读者留言 -->
    <template v-if="!active">
      <div v-loading="loading" class="feedback-body">
        <el-empty v-if="!loading && items.length === 0" description="暂无读者留言" :image-size="70" />
        <div
          v-for="m in items"
          :key="m.id"
          class="feedback-row"
          @click="open(m)"
        >
          <div class="avatar">{{ m.readerName.slice(0, 1) }}</div>
          <div class="feedback-main">
            <div class="row-1">
              <span class="name">{{ m.readerName }}</span>
              <el-tag size="small" :type="m.status === 'REPLIED' ? 'success' : 'danger'">
                {{ m.status === 'REPLIED' ? '已回复' : '未回复' }}
              </el-tag>
            </div>
            <!-- 显示最新一条消息：读者留言或管理员回复（回复加「我：」前缀） -->
            <div class="msg" :title="lastMsg(m)">{{ lastMsg(m) }}</div>
          </div>
          <div class="time">{{ formatChatTime(lastTime(m)) }}</div>
        </div>
      </div>

      <div class="list-footer">
        <el-button size="small" type="primary" plain @click="goManage">进入后台留言管理</el-button>
      </div>
    </template>

    <!-- 详情视图：在面板内直接回复 -->
    <template v-else>
      <div class="detail-head">
        <el-button text :icon="ArrowLeft" size="small" @click="back">返回</el-button>
        <span class="detail-name">{{ active.readerName }}</span>
        <span class="detail-time">{{ formatChatTime(active.createdAt) }}</span>
      </div>

      <div class="detail-body">
        <div class="original">{{ active.content }}</div>

        <div v-if="active.replyContent" class="reply-block">
          <div class="reply-meta">已回复 · {{ formatChatTime(active.repliedAt) }}</div>
          <div class="reply-content">{{ active.replyContent }}</div>
        </div>

        <el-input
          v-model="replyDraft"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          resize="none"
          :placeholder="active.replyContent ? '补充 / 修改回复内容' : '填写回复内容…'"
        />
      </div>

      <div class="detail-footer">
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import { formatChatTime } from '@/utils/timeFormat'
import type { FeedbackMessage, PageResult } from '@/types'

/**
 * 管理端「读者留言列表」（管理员在前台悬浮窗内查看）。
 *
 * 列表来自 /help/feedback（管理员接口，最近 20 条，10 秒轮询）。
 * 点击留言在面板内进入详情视图：展示留言原文与已有回复，可直接
 * 提交回复（POST /help/feedback/{id}/reply），不必跳转后台。
 */
const router = useRouter()
const items = ref<FeedbackMessage[]>([])
const loading = ref(false)
let pollTimer: number | undefined

const active = ref<FeedbackMessage | null>(null)
const replyDraft = ref('')
const replying = ref(false)

function goManage() {
  router.push('/admin/help?tab=feedback')
}

async function load() {
  try {
    const data = await http.get<PageResult<FeedbackMessage>>('/help/feedback', {
      page: 0,
      size: 20
    })
    items.value = data.content || []
  } catch {
    // 轮询失败静默，不打断使用
  } finally {
    loading.value = false
  }
}

function open(m: FeedbackMessage) {
  active.value = m
  replyDraft.value = m.replyContent || ''
}

/** 列表行显示最新一条消息：已回复显示管理员的回复（「我：」前缀），否则显示读者留言 */
function lastMsg(m: FeedbackMessage): string {
  return m.status === 'REPLIED' && m.replyContent ? `我：${m.replyContent}` : m.content
}

/** 最新消息的时间：已回复取回复时间，否则取留言时间 */
function lastTime(m: FeedbackMessage): string | null {
  return m.status === 'REPLIED' && m.replyContent ? m.repliedAt : m.createdAt
}

function back() {
  active.value = null
  load() // 回列表刷新状态
}

async function submitReply() {
  const content = replyDraft.value.trim()
  if (!content || !active.value) {
    ElMessage.warning('请填写回复内容')
    return
  }
  replying.value = true
  try {
    await http.post(`/help/feedback/${active.value.id}/reply`, { content })
    ElMessage.success('回复成功')
    back()
  } catch (err) {
    ElMessage.error(errorMessage(err, '回复失败'))
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  load()
  pollTimer = window.setInterval(load, 10_000)
})

onBeforeUnmount(() => {
  if (pollTimer !== undefined) window.clearInterval(pollTimer)
})
</script>

<style scoped>
.feedback-list {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.feedback-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.feedback-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.feedback-row:hover {
  border-color: #c6e2ff;
  background: #f7f9fc;
}
.avatar {
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #67c23a 0%, #3a9e1f 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.feedback-main {
  flex: 1;
  min-width: 0;
}
.row-1 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg {
  font-size: 13px;
  color: #4e5969;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  flex: 0 0 auto;
  font-size: 12px;
  color: #a9aeb8;
  margin-top: 2px;
}
.list-footer {
  flex: 0 0 auto;
  padding-top: 10px;
  text-align: center;
}

/* 详情视图 */
.detail-head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f2f3f5;
}
.detail-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.detail-time {
  margin-left: auto;
  font-size: 12px;
  color: #a9aeb8;
}
.detail-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 10px 0;
}
.original {
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
  font-size: 13px;
  color: #1f2329;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.reply-block {
  padding: 10px 12px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
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
.detail-footer {
  flex: 0 0 auto;
  text-align: right;
}
</style>
