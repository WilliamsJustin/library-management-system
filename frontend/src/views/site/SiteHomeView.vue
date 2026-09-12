<template>
  <div class="home">
    <!-- 顶部 Hero + 书目检索 -->
    <section class="hero">
      <h1 class="hero-title">学校图书借阅系统</h1>
      <p class="hero-sub">检索馆藏书目，在线预约、续借，畅享校园阅读服务</p>

      <div class="hero-search">
        <BookSearchBar
          v-model:keyword="keyword"
          v-model:field="searchField"
          @search="doSearch"
        />
      </div>
    </section>

    <!-- 馆藏精选 -->
    <section class="container">
      <div class="section-title">馆藏精选</div>

      <div v-loading="loading">
        <el-empty v-if="!loading && books.length === 0" description="未找到相关图书" />
        <div v-else class="book-grid">
          <el-card v-for="b in books" :key="b.id" class="book-card" shadow="hover" @click="goDetail(b)">
            <div class="book-title" :title="b.title">{{ b.title }}</div>
            <div class="book-meta">{{ b.author }} · {{ b.publisher }}</div>
            <div class="book-tags">
              <el-tag size="small" effect="plain">{{ b.category }}</el-tag>
              <el-tag size="small" :type="b.status === 'ACTIVE' ? 'success' : 'info'">
                {{ b.status === 'ACTIVE' ? '在架' : '已下架' }}
              </el-tag>
            </div>
            <div class="book-copies">可借 {{ b.availableCopies }} / 共 {{ b.totalCopies }}</div>
          </el-card>
        </div>
      </div>
    </section>

    <!-- 公告 -->
    <section class="container">
      <div class="section-title">
        <el-icon><Bell /></el-icon>
        <span>公告</span>
      </div>
      <el-card class="ann-card" v-loading="loadingNotices">
        <el-empty v-if="!loadingNotices && notices.length === 0" description="暂无公告" :image-size="50" />
        <ul v-else class="notice-list">
          <li v-for="n in notices" :key="n.id" class="notice-item" @click="openNotice(n)">
            <el-tag v-if="n.pinned" size="small" type="danger" effect="dark" class="pin">置顶</el-tag>
            <span class="notice-title">{{ n.title }}</span>
            <span class="notice-date">{{ formatDateTime(n.publishedAt) }}</span>
            <div class="notice-preview">{{ n.content }}</div>
          </li>
        </ul>
      </el-card>

      <PageBar
        v-if="noticeTotal > noticeSize"
        center
        background
        :total="noticeTotal"
        :page-size="noticeSize"
        :current-page="noticePage"
        @change="handleNoticePageChange"
      />

      <el-dialog
        v-model="dialogVisible"
        :title="activeNotice?.title"
        width="560px"
        class="notice-dialog"
        append-to-body
      >
        <div v-if="activeNotice" class="notice-detail">
          <div class="notice-detail-meta">
            <el-tag v-if="activeNotice.pinned" size="small" type="danger" effect="dark">置顶</el-tag>
            <span class="notice-detail-date">{{ formatDateTime(activeNotice.publishedAt) }}</span>
          </div>
          <div class="notice-detail-content">{{ activeNotice.content }}</div>
        </div>
      </el-dialog>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import { openInNewTab } from '@/utils/navigation'
import PageBar from '@/components/PageBar.vue'
import BookSearchBar from '@/components/BookSearchBar.vue'
import type { Announcement, Book, PageResult } from '@/types'

const keyword = ref('')
const searchField = ref('any')
const books = ref<Book[]>([])
const loading = ref(false)

const notices = ref<Announcement[]>([])
const loadingNotices = ref(false)
const noticePage = ref(1)
const noticeSize = 5
const noticeTotal = ref(0)

const dialogVisible = ref(false)
const activeNotice = ref<Announcement | null>(null)
function openNotice(n: Announcement) {
  activeNotice.value = n
  dialogVisible.value = true
}

/** 发布时间，精确到分钟，如 2026-09-10 15:47 */
function formatDateTime(v?: string | null) {
  if (!v) return ''
  const d = new Date(v)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 首页检索：在新标签页打开「图书检索结果」页，首页保持原样、不被覆盖 */
function doSearch() {
  openInNewTab('/search', {
    keyword: keyword.value.trim(),
    field: searchField.value !== 'any' ? searchField.value : ''
  })
}

/** 点击馆藏卡片：在新标签页打开图书详情，首页保持原样、不被覆盖 */
function goDetail(b: Book) {
  openInNewTab(`/books/${b.id}`)
}

async function loadFeatured() {
  try {
    const data = await http.get<PageResult<Book>>('/books', { size: 8, page: 0 })
    books.value = data.content || []
  } catch (e) {
    books.value = []
  }
}

async function loadNotices() {
  loadingNotices.value = true
  try {
    const data = await http.get<PageResult<Announcement>>('/announcements', { page: noticePage.value - 1, size: noticeSize })
    notices.value = data.content || []
    noticeTotal.value = data.totalElements || 0
  } catch (e) {
    notices.value = []
    noticeTotal.value = 0
  } finally {
    loadingNotices.value = false
  }
}

function handleNoticePageChange(p: number) {
  noticePage.value = p
  loadNotices()
}

onMounted(() => {
  loadFeatured()
  loadNotices()
})
</script>

<style scoped>
.home {
  padding-bottom: 20px;
}

.hero {
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
  color: #fff;
  text-align: center;
  padding: 56px 24px 48px;
}

.hero-title {
  font-size: 34px;
  font-weight: 700;
  margin: 0 0 12px;
}

.hero-sub {
  font-size: 15px;
  opacity: 0.92;
  margin: 0 0 28px;
}

.hero-search {
  max-width: 640px;
  margin: 0 auto;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #1f2329;
  margin: 32px 0 16px;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.book-card {
  border-radius: 10px;
  cursor: pointer;
}

.book-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-meta {
  font-size: 13px;
  color: #86909c;
  margin: 6px 0 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.book-copies {
  font-size: 13px;
  color: #4e5969;
}

.ann-card {
  border-radius: 10px;
}

.notice-pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.notice-item {
  padding: 14px 4px;
  border-bottom: 1px solid #f2f3f5;
  cursor: pointer;
  transition: background 0.15s ease;
  border-radius: 6px;
}

.notice-item:hover {
  background: #f5f8ff;
}

.notice-item:last-child {
  border-bottom: none;
}

.pin {
  margin-right: 8px;
  vertical-align: middle;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.notice-date {
  float: right;
  font-size: 13px;
  color: #c0c4cc;
}

.notice-preview {
  font-size: 14px;
  color: #4e5969;
  margin-top: 6px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.notice-detail-date {
  font-size: 13px;
  color: #c0c4cc;
}

.notice-detail-content {
  font-size: 15px;
  color: #1f2329;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
