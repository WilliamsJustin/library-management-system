<template>
  <div class="borrow-view">
    <div class="page-head">
      <h1>图书借阅</h1>
      <BookSearchBar
        class="search"
        v-model:keyword="keyword"
        v-model:field="searchField"
        :loading="loading"
        @search="openSearchTab"
      />
    </div>

    <div v-loading="loading" class="book-list">
      <el-empty v-if="!books.length && !loading" description="没有找到图书" />
      <el-card v-for="b in books" :key="b.id" class="book-card" shadow="hover">
        <div class="book-main" title="查看图书详情" @click="goDetail(b)">
          <div class="book-title">{{ b.title }}</div>
          <div class="book-meta isbn">ISBN {{ b.isbn || '—' }}</div>
          <div class="book-meta">{{ b.author }} · {{ b.publisher }} · {{ b.category }}</div>
          <div class="book-status">
            <el-tag :type="b.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ b.status === 'ACTIVE' ? '馆藏可借' : '暂不可借' }}
            </el-tag>
            <span class="copies">可借副本：{{ b.availableCopies }}/{{ b.totalCopies }}</span>
          </div>
          <div class="view-detail">查看详情 ›</div>
        </div>
        <div class="ops">
          <el-button type="primary" :disabled="b.availableCopies === 0" @click="openBorrow(b)">
            借阅
          </el-button>
          <el-button
            plain
            :type="isFavorited(b) ? 'warning' : 'primary'"
            :icon="isFavorited(b) ? StarFilled : Star"
            @click="toggleFavorite(b)"
          >{{ isFavorited(b) ? '已收藏' : '收藏' }}</el-button>
        </div>
      </el-card>
    </div>

    <!-- 借阅：在本页弹出副本选择框办理，借完刷新本页列表 -->
    <BorrowDialog ref="borrowDialog" @borrowed="search" />
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import { openInNewTab } from '@/utils/navigation'
import BookSearchBar from '@/components/BookSearchBar.vue'
import BorrowDialog from '@/components/BorrowDialog.vue'
import { useBookActions } from '@/composables/useBookActions'
import type { Book, PageResult } from '@/types'

const route = useRoute()

// 收藏：与检索结果页 / 图书详情页共用同一套动作，按钮随之显示「收藏 / 已收藏」
const { toggleFavorite, isFavorited, favoriteStore } = useBookActions()

// 检索框与前台首页完全一致（共用 BookSearchBar：检索字段下拉 + 关键词 + 检索按钮）
const keyword = ref('')
const searchField = ref('any')
const books = ref<Book[]>([])
const loading = ref(false)

/** 副本选择弹窗（与检索结果页 / 图书详情页共用同一个组件） */
const borrowDialog = ref<InstanceType<typeof BorrowDialog> | null>(null)

/** 按检索条件加载本页书目列表（也用于从详情页「借阅」按钮带 query 进来时精确定位） */
async function search() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Book>>('/books', {
      keyword: keyword.value.trim() || undefined,
      field: searchField.value || 'any',
      size: 20,
      page: 0
    })
    books.value = data.content || []
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载图书失败'))
  } finally {
    loading.value = false
  }
}

/** 检索框：与前台首页一致——在新标签页打开「图书检索结果」页，本页保持原样、不被覆盖 */
function openSearchTab() {
  openInNewTab('/search', {
    keyword: keyword.value.trim(),
    field: searchField.value !== 'any' ? searchField.value : ''
  })
}

/** 点击书目主体 → 新标签页打开图书详情；带上来源标记，详情页据此显示返回入口 */
function goDetail(book: Book) {
  openInNewTab(`/books/${book.id}`, { from: 'borrow' })
}

/** 借阅：在本页弹出副本选择框（本页已在读者后台内，无需再校验身份） */
function openBorrow(book: Book) {
  borrowDialog.value?.open(book)
}

onMounted(async () => {
  // 支持从检索结果页 / 详情页「借阅」按钮跳进来时带上检索条件（按 ISBN 精确定位到那本书）
  const q = route.query
  if (typeof q.keyword === 'string') keyword.value = q.keyword
  if (typeof q.field === 'string' && q.field) searchField.value = q.field
  // 先拉一次收藏 ID，卡片上才能正确显示「已收藏」状态
  await favoriteStore.refresh()
  search()
})
</script>

<style scoped>
.borrow-view {
  padding: 20px;
}
.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.page-head h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  white-space: nowrap;
}
.search {
  max-width: 640px;
  flex: 1;
}
.book-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  min-height: 120px;
}
.book-card {
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
/* 书目主体整体可点，点击进入详情页 */
.book-main {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.book-main:hover .book-title {
  color: #409eff;
}
.view-detail {
  margin-top: 8px;
  font-size: 13px;
  color: #409eff;
  opacity: 0;
  transition: opacity 0.2s;
}
.book-main:hover .view-detail {
  opacity: 1;
}
.book-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
  transition: color 0.2s;
}
.book-meta {
  font-size: 13px;
  color: #86909c;
  margin: 4px 0 8px;
}
/* ISBN 用等宽字体、字色略深，和著者/出版信息区分开 */
.book-meta.isbn {
  color: #4e5969;
  font-family: Consolas, 'Courier New', monospace;
  letter-spacing: 0.3px;
  margin: 2px 0 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.book-status {
  display: flex;
  align-items: center;
  gap: 10px;
}
.copies {
  font-size: 13px;
  color: #4e5969;
}
/* 右侧操作按钮纵向排列（借阅 / 收藏），清掉 el-button 相邻的默认左外边距 */
.ops {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ops :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
