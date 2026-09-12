<template>
  <div class="search-page">
    <!-- 顶部检索框：与首页检索框完全一致 -->
    <div class="search-header">
      <div class="search-header-inner">
        <BookSearchBar
          v-model:keyword="keyword"
          v-model:field="field"
          :loading="loading"
          @search="onSearch"
        />
      </div>
    </div>

    <div class="container">
      <div class="layout">
        <!-- 左侧侧边导航：馆藏分类 / 出版社 -->
        <aside class="sidebar">
          <div class="side-block">
            <div class="side-title">馆藏分类</div>
            <ul class="side-list">
              <li class="side-item" :class="{ on: !category }" @click="selectCategory('')">
                <span>全部</span>
              </li>
              <li
                v-for="c in categories"
                :key="c"
                class="side-item"
                :class="{ on: category === c }"
                @click="selectCategory(c)"
              >
                <span>{{ c }}</span>
              </li>
            </ul>
          </div>

          <div class="side-block">
            <div class="side-title">出版社</div>
            <ul class="side-list">
              <li class="side-item" :class="{ on: !publisher }" @click="selectPublisher('')">
                <span>全部</span>
              </li>
              <li
                v-for="p in publishers"
                :key="p"
                class="side-item"
                :class="{ on: publisher === p }"
                @click="selectPublisher(p)"
              >
                <span :title="p">{{ p }}</span>
              </li>
            </ul>
          </div>
        </aside>

        <!-- 右侧内容：检索结果 -->
        <section class="results">
          <div class="result-head">
            共找到 <b>{{ total }}</b> 条
            <template v-if="keyword.trim()">与「{{ keyword.trim() }}」相关的</template>馆藏
            <span v-if="category" class="cond">分类：{{ category }}</span>
            <span v-if="publisher" class="cond">出版社：{{ publisher }}</span>
          </div>

          <div v-loading="loading" class="results-body">
            <el-empty v-if="!loading && books.length === 0" description="未找到相关图书" />
            <ul v-else class="book-list">
              <li v-for="b in books" :key="b.id" class="book-row" @click="goDetail(b)">
                <div class="cover">
                  <img v-if="b.coverUrl" :src="b.coverUrl" :alt="b.title" />
                  <div v-else class="cover-ph">{{ (b.title || '书').slice(0, 1) }}</div>
                </div>
                <div class="info">
                  <div class="title" :title="b.title">{{ b.title }}</div>
                  <div class="meta isbn">ISBN {{ b.isbn || '—' }}</div>
                  <div class="meta">{{ b.author || '佚名' }} · {{ b.publisher || '出版社未知' }}</div>
                  <div class="meta sub">
                    {{ b.publishDate || '出版日期未知' }}
                    <template v-if="b.price != null"> · 定价 ¥{{ Number(b.price).toFixed(2) }}</template>
                  </div>
                  <div class="tags">
                    <el-tag v-if="b.category" size="small" effect="plain">{{ b.category }}</el-tag>
                    <el-tag size="small" :type="b.status === 'ACTIVE' ? 'success' : 'info'">
                      {{ b.status === 'ACTIVE' ? '在架' : '已下架' }}
                    </el-tag>
                  </div>
                </div>
                <div class="avail">
                  <div class="avail-num" :class="{ zero: b.availableCopies === 0 }">{{ b.availableCopies }}</div>
                  <div class="avail-label">可借 / 共 {{ b.totalCopies }}</div>
                </div>

                <!-- 借阅 / 收藏：都在本页就地办理、不跳页（@click.stop 避免触发整行进详情） -->
                <div class="ops" @click.stop>
                  <el-button
                    type="primary"
                    size="small"
                    :icon="Collection"
                    :disabled="b.availableCopies === 0"
                    @click="openBorrow(b)"
                  >借阅</el-button>
                  <el-button
                    plain
                    size="small"
                    :type="isFavorited(b) ? 'warning' : 'primary'"
                    :icon="isFavorited(b) ? StarFilled : Star"
                    @click="toggleFavorite(b)"
                  >{{ isFavorited(b) ? '已收藏' : '收藏' }}</el-button>
                </div>
              </li>
            </ul>
          </div>

          <PageBar
            v-if="total > pageSize"
            background
            :total="total"
            :page-size="pageSize"
            :current-page="page"
            @change="handlePageChange"
          />
        </section>
      </div>
    </div>

    <!-- 借阅：在本页弹出副本选择框办理，不跳转页面 -->
    <BorrowDialog ref="borrowDialog" @borrowed="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Star, StarFilled } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'
import BookSearchBar from '@/components/BookSearchBar.vue'
import BorrowDialog from '@/components/BorrowDialog.vue'
import { useBookActions } from '@/composables/useBookActions'
import type { Book, PageResult } from '@/types'

const route = useRoute()
const router = useRouter()

// 借阅 / 收藏：都在本页就地把事办完，不跳转（未登录会提示并去登录页）
const { ensureReader, toggleFavorite, isFavorited, favoriteStore } = useBookActions()

/** 副本选择弹窗（借阅用） */
const borrowDialog = ref<InstanceType<typeof BorrowDialog> | null>(null)

const keyword = ref('')
const field = ref('any')
const category = ref('')
const publisher = ref('')
const page = ref(1)
const pageSize = 10

const books = ref<Book[]>([])
const total = ref(0)
const loading = ref(false)

const categories = ref<string[]>([])
const publishers = ref<string[]>([])

/** 从地址栏读取检索条件（支持刷新 / 分享链接后保持结果） */
function readQuery() {
  const q = route.query
  keyword.value = typeof q.keyword === 'string' ? q.keyword : ''
  field.value = typeof q.field === 'string' && q.field ? q.field : 'any'
  category.value = typeof q.category === 'string' ? q.category : ''
  publisher.value = typeof q.publisher === 'string' ? q.publisher : ''
  const p = Number(q.page)
  page.value = Number.isFinite(p) && p >= 1 ? p : 1
}

/** 把当前检索条件写回地址栏（不重新挂载组件，避免重复请求） */
function syncQuery() {
  router.replace({
    path: '/search',
    query: {
      keyword: keyword.value.trim() || undefined,
      field: field.value !== 'any' ? field.value : undefined,
      category: category.value || undefined,
      publisher: publisher.value || undefined,
      page: page.value > 1 ? String(page.value) : undefined
    }
  })
}

async function load() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Book>>('/books', {
      keyword: keyword.value.trim() || undefined,
      field: field.value || 'any',
      category: category.value || undefined,
      publisher: publisher.value || undefined,
      page: page.value - 1,
      size: pageSize
    })
    books.value = data.content || []
    total.value = data.totalElements || 0
  } catch {
    books.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadFacets() {
  try {
    categories.value = await http.get<string[]>('/books/categories')
  } catch {
    categories.value = []
  }
  try {
    publishers.value = await http.get<string[]>('/books/publishers')
  } catch {
    publishers.value = []
  }
}

function onSearch() {
  page.value = 1
  syncQuery()
  load()
}

function selectCategory(c: string) {
  category.value = c
  page.value = 1
  syncQuery()
  load()
}

function selectPublisher(p: string) {
  publisher.value = p
  page.value = 1
  syncQuery()
  load()
}

function handlePageChange(p: number) {
  page.value = p
  syncQuery()
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goDetail(b: Book) {
  router.push(`/books/${b.id}`)
}

/** 借阅：校验读者身份后，在本页弹出副本选择框（不跳转页面） */
function openBorrow(b: Book) {
  if (!ensureReader()) return
  borrowDialog.value?.open(b)
}

readQuery()
load()
loadFacets()
// 已登录读者：拉一次收藏 ID，用于把已收藏的条目显示成「已收藏」
favoriteStore.refresh()
</script>

<style scoped>
.search-page {
  padding-bottom: 40px;
}

.search-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 20px 0;
}

.search-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  margin-top: 20px;
}

.sidebar {
  width: 200px;
  flex: 0 0 200px;
  position: sticky;
  top: 84px;
}

.side-block {
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 10px;
  padding: 4px 0 8px;
  margin-bottom: 16px;
}

.side-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  padding: 12px 16px 8px;
}

.side-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 300px;
  overflow: auto;
}

.side-item {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 16px;
  font-size: 14px;
  color: #4e5969;
  cursor: pointer;
  border-right: 3px solid transparent;
  transition: background 0.15s, color 0.15s;
}

.side-item > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-item:hover {
  background: #f5f8ff;
  color: #409eff;
}

.side-item.on {
  color: #409eff;
  font-weight: 600;
  background: #f0f7ff;
  border-right-color: #409eff;
}

.results {
  flex: 1;
  min-width: 0;
}

.result-head {
  font-size: 15px;
  color: #4e5969;
  margin-bottom: 14px;
}

.result-head b {
  color: #409eff;
}

.cond {
  margin-left: 12px;
  font-size: 13px;
  color: #86909c;
}

.results-body {
  min-height: 200px;
}

.book-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.book-row {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow 0.15s, border-color 0.15s, transform 0.15s;
}

.book-row:hover {
  border-color: #c6e2ff;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.cover {
  width: 72px;
  height: 100px;
  flex: 0 0 72px;
  border-radius: 6px;
  overflow: hidden;
  background: #f2f3f5;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  font-size: 13px;
  color: #86909c;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ISBN 用等宽字体、字色略深，和著者/出版信息区分开 */
.meta.isbn {
  color: #4e5969;
  font-family: Consolas, 'Courier New', monospace;
  letter-spacing: 0.3px;
}

.tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.avail {
  flex: 0 0 100px;
  text-align: right;
}

.avail-num {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
  line-height: 1.2;
}

.avail-num.zero {
  color: #c0c4cc;
}

.avail-label {
  font-size: 12px;
  color: #86909c;
}

/* 借阅 / 收藏按钮列 */
.ops {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-self: center;
}

/* el-button 相邻默认有左外边距，纵向排列时要清掉 */
.ops :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex: none;
    position: static;
    display: flex;
    gap: 16px;
  }

  .side-block {
    flex: 1;
  }
}
</style>
