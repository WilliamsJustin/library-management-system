<template>
  <div class="detail-page">
    <div class="container">
      <nav class="crumb">
        <router-link :to="backTo.path">{{ backTo.label }}</router-link>
        <span class="sep">/</span>
        <span class="cur">{{ book?.title || '图书详情' }}</span>
      </nav>

      <div v-loading="loading" class="detail-card">
        <template v-if="book">
          <div class="detail-body">
            <!-- 左：封面 -->
            <div class="cover-col">
              <div class="cover-box">
                <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" />
                <div v-else class="cover-ph">
                  <div class="ph-spine"></div>
                  <div class="ph-inner">
                    <div class="ph-title">{{ book.title }}</div>
                    <div class="ph-author">{{ book.author || '' }}</div>
                  </div>
                </div>
              </div>
              <div class="avail-line">
                可借 <b>{{ book.availableCopies }}</b> / 共 {{ book.totalCopies }}
              </div>
            </div>

            <!-- 右：书目信息 -->
            <div class="info-col">
              <h1 class="book-title">
                <span>{{ book.title }}</span>
                <el-tag size="small" :type="book.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ book.status === 'ACTIVE' ? '在架' : '已下架' }}
                </el-tag>
              </h1>

              <!-- 借阅 / 收藏：与检索结果页一致，都在本页就地办理、不跳页 -->
              <div class="detail-ops">
                <el-button
                  type="primary"
                  :icon="Collection"
                  :disabled="book.availableCopies === 0"
                  @click="openBorrow(book)"
                >借阅</el-button>
                <el-button
                  plain
                  :type="isFavorited(book) ? 'warning' : 'primary'"
                  :icon="isFavorited(book) ? StarFilled : Star"
                  @click="toggleFavorite(book)"
                >{{ isFavorited(book) ? '已收藏' : '收藏' }}</el-button>
              </div>

              <dl class="info-list">
                <div class="info-row">
                  <dt>ISBN</dt>
                  <dd>{{ book.isbn || '—' }}</dd>
                </div>
                <div class="info-row">
                  <dt>书名</dt>
                  <dd>{{ book.title || '—' }}</dd>
                </div>
                <div class="info-row">
                  <dt>作者 / 译者</dt>
                  <dd>{{ book.author || '—' }}</dd>
                </div>
                <div class="info-row">
                  <dt>出版社</dt>
                  <dd>{{ book.publisher || '—' }}</dd>
                </div>
                <div class="info-row">
                  <dt>出版日期</dt>
                  <dd>{{ book.publishDate || '—' }}</dd>
                </div>
                <div class="info-row">
                  <dt>分类</dt>
                  <dd>{{ book.category || '—' }}</dd>
                </div>
                <div class="info-row">
                  <dt>语言</dt>
                  <dd>{{ book.language || '—' }}</dd>
                </div>
                <div class="info-row">
                  <dt>定价</dt>
                  <dd class="price">{{ priceText }}</dd>
                </div>
                <div class="info-row">
                  <dt>封面</dt>
                  <dd>
                    <img v-if="book.coverUrl" :src="book.coverUrl" class="thumb" :alt="book.title" />
                    <span v-else class="muted">暂无封面</span>
                  </dd>
                </div>
                <div class="info-row">
                  <dt>简介</dt>
                  <dd class="desc">{{ book.description || '暂无简介' }}</dd>
                </div>
              </dl>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="未找到该图书" />
      </div>
    </div>

    <!-- 借阅：在本页弹出副本选择框办理，不跳转页面 -->
    <BorrowDialog ref="borrowDialog" @borrowed="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Collection, Star, StarFilled } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import BorrowDialog from '@/components/BorrowDialog.vue'
import { useBookActions } from '@/composables/useBookActions'
import type { Book } from '@/types'

const route = useRoute()
const book = ref<Book | null>(null)
const loading = ref(true)

// 借阅 / 收藏：与检索结果页共用同一套动作，都在本页就地把事办完、不跳转
const { ensureReader, toggleFavorite, isFavorited, favoriteStore } = useBookActions()

/** 副本选择弹窗（借阅用） */
const borrowDialog = ref<InstanceType<typeof BorrowDialog> | null>(null)

/** 借阅：校验读者身份后，在本页弹出副本选择框（不跳转页面） */
function openBorrow(b: Book) {
  if (!ensureReader()) return
  borrowDialog.value?.open(b)
}

/**
 * 来源页：所有来源（图书检索 / 图书借阅 / 我的收藏）统一显示
 * 「图书检索」并回图书检索结果页。
 */
const backTo = computed(() => ({ path: '/search', label: '图书检索' }))

const priceText = computed(() => {
  const p = book.value?.price
  return p != null ? `¥ ${Number(p).toFixed(2)}` : '—'
})

async function load() {
  loading.value = true
  try {
    book.value = await http.get<Book>(`/books/${String(route.params.id)}`)
  } catch {
    book.value = null
  } finally {
    loading.value = false
  }
}

load()
// 已登录读者：拉一次收藏 ID，把按钮显示成「已收藏 / 收藏」
favoriteStore.refresh()
</script>

<style scoped>
.detail-page {
  padding: 20px 0 48px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.crumb {
  font-size: 14px;
  color: #86909c;
  margin-bottom: 16px;
}

.crumb a {
  color: #4e5969;
  text-decoration: none;
}

.crumb a:hover {
  color: #409eff;
}

.crumb .sep {
  margin: 0 8px;
  color: #c9cdd4;
}

.crumb .cur {
  color: #1f2329;
}

.detail-card {
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 12px;
  padding: 32px;
  min-height: 320px;
}

.detail-body {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

/* 左侧封面 */
.cover-col {
  flex: 0 0 220px;
  width: 220px;
}

.cover-box {
  width: 220px;
  height: 306px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  background: #f2f3f5;
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-ph {
  position: relative;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
  color: #fff;
  padding: 24px 20px 24px 30px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
}

.ph-spine {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 10px;
  background: rgba(0, 0, 0, 0.16);
}

.ph-inner {
  width: 100%;
}

.ph-title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ph-author {
  font-size: 13px;
  opacity: 0.9;
}

.avail-line {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #4e5969;
}

.avail-line b {
  color: #409eff;
  font-size: 16px;
}

/* 右侧信息 */
.info-col {
  flex: 1;
  min-width: 0;
}

.book-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: 700;
  color: #1f2329;
  margin: 0 0 20px;
  line-height: 1.35;
}

.book-title span {
  word-break: break-word;
}

/* 借阅 / 收藏操作区 */
.detail-ops {
  display: flex;
  gap: 12px;
  margin: -4px 0 20px;
}

.info-list {
  margin: 0;
  border-top: 1px solid #f2f3f5;
}

.info-row {
  display: flex;
  gap: 16px;
  padding: 12px 4px;
  border-bottom: 1px dashed #f2f3f5;
}

.info-row dt {
  flex: 0 0 92px;
  color: #86909c;
  font-size: 14px;
  line-height: 1.7;
}

.info-row dd {
  flex: 1;
  margin: 0;
  color: #1f2329;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.info-row dd.price {
  color: #f56c6c;
  font-weight: 600;
}

.info-row dd.desc {
  white-space: pre-wrap;
}

.thumb {
  width: 48px;
  height: 66px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
}

.muted {
  color: #c0c4cc;
}

@media (max-width: 720px) {
  .detail-body {
    flex-direction: column;
    gap: 24px;
  }

  .cover-col {
    flex: none;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .detail-card {
    padding: 20px;
  }
}
</style>
