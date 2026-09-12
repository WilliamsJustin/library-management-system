<template>
  <el-dialog v-model="visible" title="选择副本借阅" width="520px">
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
              @click="borrow(c)"
            >
              借阅
            </el-button>
          </div>
        </li>
      </ul>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'
import type { Book, BookCopy } from '@/types'

/**
 * 「选择副本借阅」对话框（检索结果页 / 图书详情页 / 读者后台借阅页共用）。
 *
 * 点「借阅」后在本页弹出，不再跳转到后台页面；由父组件通过 ref 调 `open(book)` 打开。
 * 借阅成功后 `emit('borrowed')`，父组件据此刷新可借数量。
 */
const emit = defineEmits<{ (e: 'borrowed', book: Book): void }>()

const visible = ref(false)
const book = ref<Book | null>(null)
const copies = ref<BookCopy[]>([])
const copyLoading = ref(false)
const borrowingId = ref<number | null>(null)

/** 打开弹窗并加载该书副本 */
async function open(target: Book): Promise<void> {
  book.value = target
  visible.value = true
  copies.value = []
  copyLoading.value = true
  try {
    copies.value = await http.get<BookCopy[]>(`/books/${target.id}/copies`)
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载副本失败'))
  } finally {
    copyLoading.value = false
  }
}

/** 借出所选副本（借阅人为当前登录读者本人） */
async function borrow(copy: BookCopy): Promise<void> {
  borrowingId.value = copy.id
  try {
    await http.post('/loans/self', { copyId: copy.id })
    ElMessage.success('借阅成功')
    visible.value = false
    if (book.value) emit('borrowed', book.value)
  } catch (err) {
    ElMessage.error(errorMessage(err, '借阅失败'))
  } finally {
    borrowingId.value = null
  }
}

defineExpose({ open })
</script>

<style scoped>
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
