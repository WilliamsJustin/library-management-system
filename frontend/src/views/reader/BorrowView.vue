<template>
  <div class="borrow-view">
    <div class="page-head">
      <h1>图书借阅</h1>
      <el-input
        v-model="keyword"
        class="search"
        placeholder="输入书名 / 作者 / ISBN 搜索"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #append>
          <el-button :icon="Search" @click="search">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div v-loading="loading" class="book-list">
      <el-empty v-if="!books.length && !loading" description="没有找到图书" />
      <el-card v-for="b in books" :key="b.id" class="book-card" shadow="hover">
        <div class="book-main">
          <div class="book-title">{{ b.title }}</div>
          <div class="book-meta">{{ b.author }} · {{ b.publisher }} · {{ b.category }}</div>
          <div class="book-status">
            <el-tag :type="b.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ b.status === 'ACTIVE' ? '馆藏可借' : '暂不可借' }}
            </el-tag>
            <span class="copies">可借副本：{{ b.availableCopies }}/{{ b.totalCopies }}</span>
          </div>
        </div>
        <el-button type="primary" :disabled="b.availableCopies === 0" @click="openCopies(b)">
          借阅
        </el-button>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible" title="选择副本借阅" width="520px">
      <div v-loading="copyLoading" class="copy-body">
        <el-empty v-if="!copyLoading && !copies.length" description="该书暂无副本" />
        <ul v-else class="copy-list">
          <li v-for="c in copies" :key="c.id" class="copy-item">
            <div class="copy-info">
              <div>条码：{{ c.barcode }}</div>
              <div class="copy-loc">位置：{{ c.location || '—' }}</div>
            </div>
            <div class="copy-right">
              <el-tag :type="c.status === 'IN_STOCK' ? 'success' : 'warning'" size="small">
                {{ c.status === 'IN_STOCK' ? '在库' : c.status === 'BORROWED' ? '已借出' : '已下架' }}
              </el-tag>
              <el-button
                size="small"
                type="primary"
                :disabled="c.status !== 'IN_STOCK'"
                :loading="borrowingId === c.id"
                @click="borrowCopy(c)"
              >
                借阅
              </el-button>
            </div>
          </li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Collection } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import type { Book, BookCopy, PageResult } from '@/types'

const keyword = ref('')
const books = ref<Book[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const copies = ref<BookCopy[]>([])
const copyLoading = ref(false)
const borrowingId = ref<number | null>(null)

async function search() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Book>>('/books', { keyword: keyword.value, size: 20, page: 0 })
    books.value = data.content || []
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载图书失败'))
  } finally {
    loading.value = false
  }
}

async function openCopies(book: Book) {
  dialogVisible.value = true
  copies.value = []
  copyLoading.value = true
  try {
    copies.value = await http.get<BookCopy[]>(`/books/${book.id}/copies`)
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载副本失败'))
  } finally {
    copyLoading.value = false
  }
}

async function borrowCopy(copy: BookCopy) {
  borrowingId.value = copy.id
  try {
    await http.post('/loans/self', { copyId: copy.id })
    ElMessage.success('借阅成功')
    dialogVisible.value = false
    await search()
  } catch (err) {
    ElMessage.error(errorMessage(err, '借阅失败'))
  } finally {
    borrowingId.value = null
  }
}

onMounted(search)
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
  max-width: 420px;
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
.book-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}
.book-meta {
  font-size: 13px;
  color: #86909c;
  margin: 4px 0 8px;
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
.copy-body {
  min-height: 120px;
}
.copy-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.copy-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 4px;
  border-bottom: 1px dashed #eef0f3;
}
.copy-item:last-child {
  border-bottom: none;
}
.copy-loc {
  font-size: 12px;
  color: #86909c;
  margin-top: 2px;
}
.copy-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
