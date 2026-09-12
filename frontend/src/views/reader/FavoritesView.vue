<template>
  <div class="favorites-view">
    <div class="page-head">
      <h1>我的收藏</h1>
      <span class="count" v-if="total > 0">共 {{ total }} 本</span>
    </div>

    <!-- 与「图书借阅」页一致的检索框：按字段过滤我的收藏 -->
    <BookSearchBar
      class="search"
      v-model:keyword="keyword"
      v-model:field="searchField"
      :loading="loading"
      @search="search"
    />

    <div v-loading="loading" class="fav-body">
      <!-- 未收藏过任何书：引导去检索 -->
      <el-empty
        v-if="!loading && books.length === 0 && !isSearching"
        description="还没有收藏任何图书"
      >
        <el-button type="primary" @click="router.push('/search')">去检索图书</el-button>
      </el-empty>
      <!-- 有收藏但检索无结果 -->
      <el-empty v-else-if="!loading && books.length === 0" description="没有找到匹配的收藏图书" />

      <div v-else class="book-grid">
        <el-card v-for="(b, idx) in books" :key="b.id" class="book-card" shadow="hover">
          <div class="book-main" title="查看图书详情" @click="goDetail(b)">
            <div class="cover">
              <img v-if="b.coverUrl" :src="b.coverUrl" :alt="b.title" />
              <div v-else class="cover-ph">{{ (b.title || '书').slice(0, 1) }}</div>
              <span class="row-index">{{ (page - 1) * pageSize + idx + 1 }}</span>
            </div>
            <div class="book-title" :title="b.title">{{ b.title }}</div>
            <div class="book-meta isbn">ISBN {{ b.isbn || '—' }}</div>
            <div class="book-meta">{{ b.author || '佚名' }} · {{ b.publisher || '出版社未知' }}</div>
            <div class="book-meta sub">
              {{ b.category || '未分类' }}
              <template v-if="b.publishDate"> · {{ b.publishDate }}</template>
            </div>
          </div>

          <div class="book-status">
            <el-tag :type="b.availableCopies > 0 ? 'success' : 'info'" size="small">
              {{ b.availableCopies > 0 ? '可借' : '暂无可借' }}
            </el-tag>
            <span class="copies">可借 {{ b.availableCopies }}/{{ b.totalCopies }}</span>
          </div>

          <div class="ops">
            <el-button
              type="primary"
              size="small"
              :disabled="b.availableCopies === 0"
              @click="borrow(b)"
            >借阅</el-button>
            <el-button
              size="small"
              :loading="removingId === b.id"
              @click="unfavorite(b)"
            >取消收藏</el-button>
          </div>
        </el-card>
      </div>

      <PageBar
        v-if="total > pageSize"
        background
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @change="handlePageChange"
      />
    </div>

    <!-- 借阅：在本页弹出副本选择框办理，不跳转页面（与检索结果页 / 图书详情页一致） -->
    <BorrowDialog ref="borrowDialog" @borrowed="load" />
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { openInNewTab } from '@/utils/navigation'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'
import BookSearchBar from '@/components/BookSearchBar.vue'
import BorrowDialog from '@/components/BorrowDialog.vue'
import { useBookActions } from '@/composables/useBookActions'
import { useFavoriteStore } from '@/stores/favorites'
import type { Book, PageResult } from '@/types'

const router = useRouter()
const favoriteStore = useFavoriteStore()
// 借阅入口与前台检索页保持一致：先校验读者身份，再弹副本选择框
const { ensureReader } = useBookActions()

/** 副本选择弹窗（借阅用） */
const borrowDialog = ref<InstanceType<typeof BorrowDialog> | null>(null)

const books = ref<Book[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const removingId = ref<number | null>(null)

// 检索框与「图书借阅」页一致（共用 BookSearchBar：检索字段下拉 + 关键词 + 检索按钮）
const keyword = ref('')
const searchField = ref('any')

/** 是否处于搜索状态（区分「没有收藏」和「搜索无结果」两种空态） */
const isSearching = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Book>>('/favorites/my', {
      page: page.value - 1,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      field: searchField.value || 'any'
    })
    books.value = data.content || []
    total.value = data.totalElements || 0
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载收藏失败'))
  } finally {
    loading.value = false
  }
}

/** 检索：回到第一页再加载 */
function search() {
  isSearching.value = keyword.value.trim() !== ''
  page.value = 1
  load()
}

/** 取消收藏：二次确认避免误点，成功后同步本地标记并刷新列表 */
async function unfavorite(book: Book) {
  try {
    await ElMessageBox.confirm(`确定要取消收藏《${book.title}》吗？`, '取消收藏', {
      confirmButtonText: '确定',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  removingId.value = book.id
  try {
    await favoriteStore.remove(book.id)
    ElMessage.success('已取消收藏')
    // 当前页删空后回退一页，避免停在空白页
    if (books.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await load()
  } catch (err) {
    ElMessage.error(errorMessage(err, '取消收藏失败'))
  } finally {
    removingId.value = null
  }
}

/** 借阅：校验读者身份后，在本页弹出副本选择框（不跳转页面） */
function borrow(book: Book) {
  if (!ensureReader()) return
  borrowDialog.value?.open(book)
}

/** 点击书目主体 → 新标签页打开图书详情；带上来源标记，详情页据此显示返回入口 */
function goDetail(book: Book) {
  openInNewTab(`/books/${book.id}`, { from: 'favorites' })
}

function handlePageChange(p: number) {
  page.value = p
  load()
}

onMounted(async () => {
  await favoriteStore.refresh()
  await load()
})
</script>

<style scoped>
.favorites-view {
  padding: 20px;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.page-head h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.count {
  font-size: 13px;
  color: #86909c;
}

.search {
  max-width: 640px;
  margin-bottom: 20px;
}

.fav-body {
  min-height: 200px;
}

/* 网格布局：与「图书借阅」页一致的自适应卡片栅格 */
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 16px;
}

.book-card {
  border-radius: 10px;
}

.book-main {
  cursor: pointer;
}

.book-main:hover .book-title {
  color: #409eff;
}

.cover {
  position: relative;
  width: 100%;
  height: 180px;
  border-radius: 8px;
  overflow: hidden;
  background: #f2f3f5;
  margin-bottom: 10px;
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
  font-size: 44px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
}

/* 序号：卡片左上角角标，跨页连续 */
.row-index {
  position: absolute;
  top: 8px;
  left: 8px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  background: rgba(31, 35, 41, 0.55);
  color: #fff;
  font-size: 12px;
  line-height: 22px;
  text-align: center;
}

.book-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.book-meta {
  font-size: 12px;
  color: #86909c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}

.book-meta.sub {
  color: #a9aeb8;
}

.book-meta.isbn {
  color: #4e5969;
  font-family: Consolas, 'Courier New', monospace;
  letter-spacing: 0.3px;
}

.book-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 10px;
}

.copies {
  font-size: 12px;
  color: #4e5969;
}

.ops {
  display: flex;
}

/* el-button 相邻默认有左外边距，纵向排列时要清掉 */
.ops :deep(.el-button + .el-button) {
  margin-left: 0;
}

.ops :deep(.el-button) {
  flex: 1;
}
</style>
