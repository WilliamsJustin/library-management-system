<template>
  <div class="chat-panel">
    <div ref="listRef" v-loading="loading" class="msg-list" @scroll.passive="onUserScroll">
      <div v-if="!loading && !messages.length" class="empty-tip">
        暂无消息，发送第一条咨询吧～
      </div>
      <div
        v-for="m in messages"
        :key="m.id"
        class="msg-row"
        :class="{ mine: m.senderRole === 'READER' }"
      >
        <div class="bubble">
          <div class="meta">{{ m.senderRole === 'READER' ? '我' : '管理员' }} · {{ formatTime(m.createdAt) }}</div>
          <div class="text">{{ m.content }}</div>
        </div>
      </div>
    </div>

    <div class="input-bar">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="输入消息，Enter 发送（Shift+Enter 换行）"
        :disabled="sending"
        @keydown.enter.exact.prevent="send"
      />
      <el-button type="primary" :loading="sending" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'
import type { ChatMessage } from '@/types'

/**
 * 读者侧在线咨询面板（前台悬浮窗与读者后台「帮助与反馈」共用）。
 *
 * 实时性由 5 秒轮询实现（挂载且 active 时轮询，隐藏时自动停止），
 * 借助 unique id + 正序渲染；消息超过 200 条时后端只保留最近 200 条。
 */
const props = defineProps<{ active: boolean }>()

const messages = ref<ChatMessage[]>([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const listRef = ref<HTMLElement | null>(null)
let pollTimer: number | undefined

function formatTime(value: string): string {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 用户停留在消息底部附近时才自动滚动，避免轮询刷新打断回看历史 */
let stickToBottom = true

function onUserScroll() {
  if (!listRef.value) return
  const el = listRef.value
  stickToBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 40
}

async function load(scroll = false) {
  try {
    const data = await http.get<ChatMessage[]>('/help/chat/my')
    const changed = data.length !== messages.value.length ||
      (data.length && messages.value.length && data[data.length - 1].id !== messages.value[messages.value.length - 1].id)
    messages.value = data
    if (changed || scroll) {
      await nextTick()
      if (stickToBottom || scroll) scrollToBottom()
    }
  } catch {
    // 轮询失败静默（如掉线），不打断使用
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    await http.post<ChatMessage>('/help/chat', { content })
    draft.value = ''
    await load(true)
  } catch (err) {
    ElMessage.error(errorMessage(err, '发送失败'))
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

// 面板激活时加载历史并启动 5 秒轮询，隐藏时自动停止
watch(
  () => props.active,
  (active) => {
    if (active) {
      load(true)
      pollTimer = window.setInterval(() => load(), 5000)
    } else if (pollTimer !== undefined) {
      window.clearInterval(pollTimer)
      pollTimer = undefined
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (pollTimer !== undefined) window.clearInterval(pollTimer)
})
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}
.msg-list {
  flex: 1;
  min-height: 180px;
  overflow-y: auto;
  padding: 8px;
  background: #f7f8fa;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.empty-tip {
  text-align: center;
  color: #a9aeb8;
  font-size: 13px;
  padding: 24px 0;
}
.msg-row {
  display: flex;
  justify-content: flex-start;
}
.msg-row.mine {
  justify-content: flex-end;
}
.bubble {
  max-width: 80%;
  padding: 8px 10px;
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
  margin-bottom: 4px;
}
.text {
  font-size: 13px;
  color: #1f2329;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.input-bar {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
</style>
