<template>
  <div class="help-admin-view">
    <h1>帮助与反馈</h1>

    <el-tabs v-model="activeTab">
      <!-- ============ FAQ 管理 ============ -->
      <el-tab-pane label="FAQ 管理" name="faq">
        <el-card>
          <div class="toolbar">
            <el-button type="primary" :icon="Plus" @click="openFaqDialog()">新增 FAQ</el-button>
            <el-button :icon="RefreshLeft" @click="loadFaqs">刷新</el-button>
          </div>

          <el-table :data="faqs" v-loading="faqLoading" stripe>
            <el-table-column type="index" label="序号" width="70" />
            <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="answer" label="答案" min-width="260" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button size="small" @click="openFaqDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="removeFaq(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 实时对话 ============ -->
      <el-tab-pane label="实时对话" name="chat">
        <el-card class="chat-card">
          <div class="chat-layout">
            <div class="session-list" v-loading="sessionsLoading">
              <div class="session-head">会话列表</div>
              <el-empty v-if="!sessionsLoading && !sessions.length" description="暂无读者咨询" :image-size="60" />
              <div
                v-for="s in sessions"
                :key="s.readerId"
                class="session-item"
                :class="{ active: s.readerId === activeReaderId }"
                @click="selectSession(s)"
              >
                <div class="session-name-row">
                  <span class="session-name">{{ s.readerName }}</span>
                  <el-tag v-if="s.unrepliedCount > 0" size="small" type="danger" effect="plain">
                    未回复 {{ s.unrepliedCount }}
                  </el-tag>
                </div>
                <div class="session-last">{{ s.lastMessage }}</div>
                <div class="session-meta">{{ formatDateTime(s.lastTime) }} · 共 {{ s.messageCount }} 条</div>
              </div>
            </div>

            <div class="chat-main">
              <template v-if="activeReaderId">
                <div ref="adminMsgList" class="admin-msg-list">
                  <div
                    v-for="m in conversation"
                    :key="m.id"
                    class="msg-row"
                    :class="{ mine: m.senderRole === 'ADMIN' }"
                  >
                    <div class="bubble">
                      <div class="meta">{{ m.senderRole === 'ADMIN' ? '我' : m.senderName }} · {{ formatDateTime(m.createdAt) }}</div>
                      <div class="text">{{ m.content }}</div>
                    </div>
                  </div>
                </div>
                <div class="admin-input">
                  <el-input
                    v-model="chatDraft"
                    type="textarea"
                    :rows="2"
                    resize="none"
                    placeholder="回复读者…（Enter 发送）"
                    @keydown.enter.exact.prevent="sendReply"
                  />
                  <el-button type="primary" :loading="sending" @click="sendReply">发送</el-button>
                </div>
              </template>
              <el-empty v-else description="选择左侧会话开始回复" />
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ============ 留言管理 ============ -->
      <el-tab-pane label="留言管理" name="feedback">
        <el-card>
          <div class="toolbar">
            <el-input
              v-model="feedbackFilters.keyword"
              placeholder="账号 / 学号 / 留言人"
              clearable
              style="width: 200px"
              @keyup.enter="searchFeedback"
              @clear="searchFeedback"
            />
            <el-date-picker
              v-model="feedbackFilters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="留言开始日期"
              end-placeholder="留言结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
              @change="searchFeedback"
            />
            <el-select v-model="feedbackFilters.status" placeholder="全部状态" clearable style="width: 140px" @change="searchFeedback">
              <el-option label="未回复" value="UNREPLIED" />
              <el-option label="已回复" value="REPLIED" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="searchFeedback">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetFeedbackFilters">重置</el-button>
          </div>

          <!-- 双击行同样可以打开留言回复窗口 -->
          <el-table
            :data="feedbacks"
            v-loading="feedbackLoading"
            stripe
            @row-dblclick="openReplyDialog"
          >
            <el-table-column type="index" label="序号" width="70" :index="feedbackRowIndex" />
            <el-table-column label="账号" width="120">
              <template #default="{ row }">{{ row.readerAccount || '—' }}</template>
            </el-table-column>
            <el-table-column label="学号" width="120">
              <template #default="{ row }">{{ row.readerNo || '—' }}</template>
            </el-table-column>
            <el-table-column prop="readerName" label="留言人" width="110" show-overflow-tooltip />
            <el-table-column prop="content" label="留言内容" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'REPLIED' ? 'success' : 'danger'">
                  {{ row.status === 'REPLIED' ? '已回复' : '未回复' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="留言时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="replyContent" label="回复内容" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="primary"
                  @click="openReplyDialog(row)"
                >{{ row.status === 'REPLIED' ? '查看' : '回复' }}</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="feedbackTotal > feedbackPageSize"
            class="pagination"
            layout="total, prev, pager, next"
            :total="feedbackTotal"
            :page-size="feedbackPageSize"
            :current-page="feedbackPage"
            @current-change="handleFeedbackPage"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- FAQ 新增/编辑弹窗 -->
    <el-dialog v-model="faqDialogVisible" :title="faqForm.id ? '编辑 FAQ' : '新增 FAQ'" width="560px">
      <el-form ref="faqFormRef" :model="faqForm" :rules="faqRules" label-width="70px">
        <el-form-item label="问题" prop="question">
          <el-input v-model="faqForm.question" maxlength="200" show-word-limit placeholder="读者会怎么问？" />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input v-model="faqForm.answer" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="faqForm.enabled" />
          <span class="form-tip">停用后前台与读者端检索不到该条目</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faqDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="faqSaving" @click="saveFaq">保存</el-button>
      </template>
    </el-dialog>

    <!-- 留言回复弹窗 -->
    <el-dialog v-model="replyDialogVisible" title="留言回复" width="520px">
      <div class="reply-original">
        <div class="reply-original-meta">
          {{ replyTarget?.readerName }} · {{ formatDateTime(replyTarget?.createdAt) }}
        </div>
        <div class="reply-original-content">{{ replyTarget?.content }}</div>
      </div>
      <el-input
        v-model="replyDraft"
        type="textarea"
        resize="none"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="填写回复内容（已回复的留言可补充修改回复）"
      />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, reactive, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import type { ChatMessage, ChatSession, Faq, FeedbackMessage, PageResult } from '@/types'

/**
 * 管理后台「帮助与反馈」：FAQ 知识库维护、读者在线咨询回复、留言板管理。
 * 实时对话用轮询实现：会话列表 10 秒一轮，选中会话的消息 5 秒一轮（仅该页签激活时）。
 * 支持 ?tab= 直达某个页签（首页实时咨询卡片 / 顶部角标会跳到 chat）。
 */
const route = useRoute()

const validTabs = ['faq', 'chat', 'feedback']
const qTab = typeof route.query.tab === 'string' && validTabs.includes(route.query.tab)
  ? route.query.tab
  : 'faq'
const activeTab = ref(qTab)

// 已在本页时再点顶部图标（仅 query 变化）也要切换到目标页签
watch(
  () => route.query.tab,
  (t) => {
    if (typeof t === 'string' && validTabs.includes(t)) {
      activeTab.value = t
    }
  }
)

/* ---------------- FAQ 管理 ---------------- */
const faqs = ref<Faq[]>([])
const faqLoading = ref(false)
const faqDialogVisible = ref(false)
const faqSaving = ref(false)
const faqFormRef = ref<FormInstance>()
const faqForm = ref<{ id: number | null; question: string; answer: string; enabled: boolean }>({
  id: null, question: '', answer: '', enabled: true
})
const faqRules: FormRules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
}

async function loadFaqs() {
  faqLoading.value = true
  try {
    faqs.value = await http.get<Faq[]>('/help/faq/all')
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载 FAQ 失败'))
  } finally {
    faqLoading.value = false
  }
}

function openFaqDialog(faq?: Faq) {
  faqForm.value = faq
    ? { id: faq.id, question: faq.question, answer: faq.answer, enabled: faq.enabled }
    : { id: null, question: '', answer: '', enabled: true }
  faqDialogVisible.value = true
}

async function saveFaq() {
  if (!faqFormRef.value) return
  await faqFormRef.value.validate()
  faqSaving.value = true
  try {
    const payload = {
      question: faqForm.value.question.trim(),
      answer: faqForm.value.answer.trim(),
      enabled: faqForm.value.enabled
    }
    if (faqForm.value.id) {
      await http.put(`/help/faq/${faqForm.value.id}`, payload)
      ElMessage.success('已保存')
    } else {
      await http.post('/help/faq', payload)
      ElMessage.success('已新增')
    }
    faqDialogVisible.value = false
    loadFaqs()
  } catch (err) {
    ElMessage.error(errorMessage(err, '保存失败'))
  } finally {
    faqSaving.value = false
  }
}

async function removeFaq(faq: Faq) {
  try {
    await ElMessageBox.confirm(`确定删除 FAQ「${faq.question}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await http.delete(`/help/faq/${faq.id}`)
    ElMessage.success('已删除')
    loadFaqs()
  } catch (err) {
    ElMessage.error(errorMessage(err, '删除失败'))
  }
}

/* ---------------- 实时对话 ---------------- */
const sessions = ref<ChatSession[]>([])
const sessionsLoading = ref(false)
const activeReaderId = ref<number | null>(null)
const conversation = ref<ChatMessage[]>([])
const chatDraft = ref('')
const sending = ref(false)
const adminMsgList = ref<HTMLElement | null>(null)
let sessionTimer: number | undefined
let chatTimer: number | undefined

async function loadSessions() {
  sessionsLoading.value = sessions.value.length === 0
  try {
    sessions.value = await http.get<ChatSession[]>('/help/chat/sessions')
  } catch {
    // 轮询静默
  } finally {
    sessionsLoading.value = false
  }
}

async function selectSession(s: ChatSession) {
  activeReaderId.value = s.readerId
  await loadConversation(true)
}

async function loadConversation(scroll: boolean) {
  if (!activeReaderId.value) return
  try {
    const data = await http.get<ChatMessage[]>('/help/chat', { readerId: activeReaderId.value })
    conversation.value = data
    if (scroll) {
      await nextTick()
      if (adminMsgList.value) {
        adminMsgList.value.scrollTop = adminMsgList.value.scrollHeight
      }
    }
  } catch {
    // 轮询静默
  }
}

async function sendReply() {
  const content = chatDraft.value.trim()
  if (!content || !activeReaderId.value) return
  sending.value = true
  try {
    await http.post('/help/chat', { readerId: activeReaderId.value, content })
    chatDraft.value = ''
    await loadConversation(true)
    loadSessions()
  } catch (err) {
    ElMessage.error(errorMessage(err, '发送失败'))
  } finally {
    sending.value = false
  }
}

/* ---------------- 留言管理 ---------------- */
const feedbacks = ref<FeedbackMessage[]>([])
const feedbackLoading = ref(false)
const feedbackPage = ref(1)
const feedbackPageSize = ref(10)
const feedbackTotal = ref(0)
const feedbackFilters = reactive<{
  keyword: string
  status: string
  dateRange: [string, string] | null
}>({ keyword: '', status: '', dateRange: null })
const replyDialogVisible = ref(false)
const replying = ref(false)
const replyTarget = ref<FeedbackMessage | null>(null)
const replyDraft = ref('')

async function loadFeedback() {
  feedbackLoading.value = true
  try {
    const [startDate, endDate] = feedbackFilters.dateRange || []
    const data = await http.get<PageResult<FeedbackMessage>>('/help/feedback', {
      page: feedbackPage.value - 1,
      size: feedbackPageSize.value,
      status: feedbackFilters.status || undefined,
      keyword: feedbackFilters.keyword.trim() || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    feedbacks.value = data.content
    feedbackTotal.value = data.totalElements
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载留言失败'))
  } finally {
    feedbackLoading.value = false
  }
}

function searchFeedback() {
  feedbackPage.value = 1
  loadFeedback()
}

function resetFeedbackFilters() {
  feedbackFilters.keyword = ''
  feedbackFilters.status = ''
  feedbackFilters.dateRange = null
  searchFeedback()
}

/** 留言序号跨页连续：第 2 页从 pageSize+1 开始 */
function feedbackRowIndex(index: number): number {
  return (feedbackPage.value - 1) * feedbackPageSize.value + index + 1
}

function handleFeedbackPage(page: number) {
  feedbackPage.value = page
  loadFeedback()
}

function openReplyDialog(m: FeedbackMessage) {
  replyTarget.value = m
  replyDraft.value = m.replyContent || ''
  replyDialogVisible.value = true
}

async function submitReply() {
  const content = replyDraft.value.trim()
  if (!content || !replyTarget.value) {
    ElMessage.warning('请填写回复内容')
    return
  }
  replying.value = true
  try {
    await http.post(`/help/feedback/${replyTarget.value.id}/reply`, { content })
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    loadFeedback()
  } catch (err) {
    ElMessage.error(errorMessage(err, '回复失败'))
  } finally {
    replying.value = false
  }
}

function formatDateTime(value?: string | null) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(() => {
  loadFaqs()
  loadFeedback()
  loadSessions()
  sessionTimer = window.setInterval(loadSessions, 10_000)
})

// 实时对话页签激活时才轮询选中会话的消息
watch(activeTab, (tab) => {
  if (tab === 'chat') {
    loadSessions()
    loadConversation(true)
    chatTimer = window.setInterval(() => loadConversation(false), 5000)
  } else if (chatTimer !== undefined) {
    window.clearInterval(chatTimer)
    chatTimer = undefined
  }
})

onBeforeUnmount(() => {
  if (sessionTimer !== undefined) window.clearInterval(sessionTimer)
  if (chatTimer !== undefined) window.clearInterval(chatTimer)
})
</script>

<style scoped>
.help-admin-view {
  padding: 20px;
}
.help-admin-view h1 {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #a9aeb8;
}
/* 实时对话：左会话列表 + 右消息区 */
.chat-card :deep(.el-card__body) {
  height: 560px;
}
.chat-layout {
  display: flex;
  gap: 14px;
  height: 100%;
}
.session-list {
  flex: 0 0 240px;
  overflow-y: auto;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  padding: 8px;
}
.session-head {
  font-size: 13px;
  color: #86909c;
  padding: 4px 8px 8px;
  border-bottom: 1px solid #f2f3f5;
  margin-bottom: 8px;
}
.session-item {
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.session-item:hover {
  background: #f7f8fa;
}
.session-item.active {
  background: #ecf5ff;
}
.session-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.session-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-last {
  font-size: 13px;
  color: #4e5969;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-meta {
  font-size: 12px;
  color: #a9aeb8;
  margin-top: 4px;
}
.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.admin-msg-list {
  flex: 1;
  overflow-y: auto;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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
.admin-input {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
/* 留言回复弹窗 */
.reply-original {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
}
.reply-original-meta {
  font-size: 12px;
  color: #a9aeb8;
  margin-bottom: 4px;
}
.reply-original-content {
  font-size: 13px;
  color: #1f2329;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
