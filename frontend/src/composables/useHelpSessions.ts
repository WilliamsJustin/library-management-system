import { errorMessage } from '@/utils/error'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'
import type { ChatSession } from '@/types'

/**
 * 管理端「实时咨询」会话轮询（管理员首页与顶部角标共用）。
 *
 * 每 10 秒拉一次 /help/chat/sessions 与未回复留言数；「待处理」= 实时对话
 * 未回复消息条数之和 + 未回复留言条数，顶部角标与首页卡片都按此口径统计，
 * 与后台「实时对话」列表的「未回复 N」数字保持一致。
 */
export function useHelpSessions(pollMs = 10_000) {
  const sessions = ref<ChatSession[]>([])
  const loading = ref(false)
  const pollTimer = ref<number | undefined>(undefined)

  /** 留言未回复数（独立于实时对话统计） */
  const feedbackPending = ref(0)

  /**
   * 待处理消息总数：实时对话未回复消息条数之和 + 未回复留言条数。
   * 顶部角标用这个总数；跳转目标仍按「实时对话优先」单独判断（targetHelpTab）。
   */
  const pendingCount = computed(
    () => sessions.value.reduce((sum, s) => sum + (s.unrepliedCount || 0), 0) + feedbackPending.value
  )

  /** 点击实时咨询图标应跳转的页签：有对话未回复优先实时对话，否则有未回复留言去留言管理，默认实时对话 */
  const targetHelpTab = computed<'chat' | 'feedback'>(() =>
    sessions.value.some((s) => (s.unrepliedCount || 0) > 0)
      ? 'chat'
      : feedbackPending.value > 0
        ? 'feedback'
        : 'chat'
  )

  async function refresh(silent = true) {
    if (!silent) loading.value = true
    try {
      const [list, feedback] = await Promise.all([
        http.get<ChatSession[]>('/help/chat/sessions'),
        http.get<{ totalElements: number }>('/help/feedback', {
          page: 0,
          size: 1,
          status: 'UNREPLIED'
        })
      ])
      sessions.value = list
      feedbackPending.value = feedback.totalElements || 0
    } catch (err) {
      if (!silent) ElMessage.error(errorMessage(err, '加载会话失败'))
    } finally {
      if (!silent) loading.value = false
    }
  }

  onMounted(() => {
    refresh()
    pollTimer.value = window.setInterval(() => refresh(), pollMs)
  })

  onBeforeUnmount(() => {
    if (pollTimer.value !== undefined) window.clearInterval(pollTimer.value)
  })

  return { sessions, pendingCount, feedbackPending, targetHelpTab, loading, refresh }
}
