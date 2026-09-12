<template>
  <div class="session-list">
    <!-- 列表视图：读者咨询会话 -->
    <template v-if="!active">
      <div v-loading="loading" class="session-body">
        <el-empty v-if="!loading && sessions.length === 0" description="暂无读者咨询" :image-size="70" />
        <div
          v-for="s in sessions"
          :key="s.readerId"
          class="session-row"
          @click="open(s)"
        >
          <div class="avatar">{{ s.readerName.slice(0, 1) }}</div>
          <div class="session-main">
            <div class="row-1">
              <span class="name">{{ s.readerName }}</span>
              <el-tag v-if="s.lastSenderRole === 'READER'" size="small" type="danger">待回复</el-tag>
            </div>
            <div class="msg" :title="s.lastMessage">{{ s.lastMessage }}</div>
          </div>
          <div class="time">{{ formatChatTime(s.lastTime) }}</div>
        </div>
      </div>

      <div class="list-footer">
        <el-button size="small" type="primary" plain @click="goReply">进入后台实时对话</el-button>
      </div>
    </template>

    <!-- 对话视图：在面板内直接回复 -->
    <template v-else>
      <div class="conv-head">
        <el-button text :icon="ArrowLeft" size="small" @click="back">返回</el-button>
        <span class="conv-name">{{ active.readerName }}</span>
      </div>

      <div ref="convListRef" class="conv-list">
        <div
          v-for="m in conversation"
          :key="m.id"
          class="msg-row"
          :class="{ mine: m.senderRole === 'ADMIN' }"
        >
          <div class="bubble">
            <div class="meta">{{ m.senderRole === 'ADMIN' ? '我' : m.senderName }} · {{ hhmm(m.createdAt) }}</div>
            <div class="text">{{ m.content }}</div>
          </div>
        </div>
      </div>

      <div class="conv-input">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="回复读者…（Enter 发送）"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="sending" @click="send">发送</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import { formatChatTime } from '@/utils/timeFormat'
import { useHelpSessions } from '@/composables/useHelpSessions'
import type { ChatMessage, ChatSession } from '@/types'

/**
 * 管理端「读者咨询列表」（管理员在前台悬浮窗内查看）。
 *
 * 列表数据与后台「帮助与反馈 → 实时对话」共用 /help/chat/sessions（10 秒轮询）。
 * 点击会话在面板内进入对话视图：5 秒轮询消息记录，可直接回复（POST /help/chat），
 * 不必跳转后台；返回列表后自动刷新会话摘要。
 */
const router = useRouter()
const { sessions, loading, refresh } = useHelpSessions()

const active = ref<ChatSession | null>(null)
const conversation = ref<ChatMessage[]>([])
const draft = ref('')
const sending = ref(false)
const convListRef = ref<HTMLElement | null>(null)
let pollTimer: number | undefined

function goReply() {
  router.push('/admin/help?tab=chat')
}

function hhmm(value: string): string {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

async function open(s: ChatSession) {
  active.value = s
  draft.value = ''
  await loadConversation(true)
  // 对话视图打开期间轮询读者新消息
  stopPolling()
  pollTimer = window.setInterval(() => loadConversation(false), 5_000)
}

function back() {
  active.value = null
  stopPolling()
  refresh() // 会话摘要（最后一条消息/待回复状态）同步刷新
}

function stopPolling() {
  if (pollTimer !== undefined) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
}

async function loadConversation(scroll: boolean) {
  if (!active.value) return
  try {
    const data = await http.get<ChatMessage[]>('/help/chat', { readerId: active.value.readerId })
    conversation.value = data
    if (scroll) {
      await nextTick()
      if (convListRef.value) {
        convListRef.value.scrollTop = convListRef.value.scrollHeight
      }
    }
  } catch {
    // 轮询失败静默
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content || !active.value) return
  sending.value = true
  try {
    await http.post('/help/chat', { readerId: active.value.readerId, content })
    draft.value = ''
    await loadConversation(true)
    refresh()
  } catch (err) {
    ElMessage.error(errorMessage(err, '发送失败'))
  } finally {
    sending.value = false
  }
}

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.session-list {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.session-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.session-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.session-row:hover {
  border-color: #c6e2ff;
  background: #f7f9fc;
}
.avatar {
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.session-main {
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

/* 对话视图 */
.conv-head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f2f3f5;
}
.conv-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.conv-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 8px;
  margin: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.msg-row {
  display: flex;
  justify-content: flex-start;
}
.msg-row.mine {
  justify-content: flex-end;
}
.bubble {
  max-width: 82%;
  padding: 7px 9px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #eef0f3;
}
.msg-row.mine .bubble {
  background: #ecf5ff;
  border-color: #d9ecff;
}
.meta {
  font-size: 11px;
  color: #a9aeb8;
  margin-bottom: 3px;
}
.text {
  font-size: 13px;
  color: #1f2329;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.conv-input {
  flex: 0 0 auto;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
</style>
