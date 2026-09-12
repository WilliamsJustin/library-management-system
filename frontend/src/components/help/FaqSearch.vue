<template>
  <div class="faq-search">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="输入关键词，如：续借 / 罚款 / 开馆时间"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
    </div>

    <div v-loading="loading" class="faq-list">
      <el-empty v-if="!loading && !faqs.length" description="" :image-size="60">
        <div class="no-match">
          <p>该问题未收录，您可以转人工咨询，或到留言板留言等待管理员回复。</p>
          <div class="no-match-ops">
            <el-button type="primary" size="small" @click="emit('to-chat')">转人工</el-button>
            <el-button size="small" @click="emit('to-feedback')">去留言</el-button>
          </div>
        </div>
      </el-empty>

      <ul v-else class="faq-items">
        <li v-for="f in faqs" :key="f.id" class="faq-item" :class="{ open: activeId === f.id }">
          <div class="faq-question" @click="toggle(f.id)">
            <span class="q-text">{{ f.question }}</span>
            <el-icon class="caret"><CaretRight /></el-icon>
          </div>
          <div v-if="activeId === f.id" class="faq-answer">{{ f.answer }}</div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, CaretRight } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import type { Faq } from '@/types'

/**
 * FAQ 关键词检索面板（前台悬浮窗与读者后台「帮助与反馈」共用）。
 *
 * 点击问题展开答案；没有匹配项时提示未收录，并给出「转人工 / 去留言」两个出口，
 * 由父页面决定这两个动作的去向（emit('to-chat') / emit('to-feedback')）。
 */
const emit = defineEmits<{ (e: 'to-chat'): void; (e: 'to-feedback'): void }>()

const keyword = ref('')
const faqs = ref<Faq[]>([])
const loading = ref(false)
const activeId = ref<number | null>(null)

async function search() {
  loading.value = true
  activeId.value = null
  try {
    faqs.value = await http.get<Faq[]>('/help/faq', {
      keyword: keyword.value.trim() || undefined
    })
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载常见问题失败'))
  } finally {
    loading.value = false
  }
}

function toggle(id: number) {
  activeId.value = activeId.value === id ? null : id
}

onMounted(search)
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.faq-list {
  min-height: 120px;
}
.no-match {
  padding: 4px 8px;
}
.no-match p {
  margin: 0 0 10px;
  font-size: 13px;
  color: #86909c;
  line-height: 1.6;
}
.no-match-ops {
  display: flex;
  justify-content: center;
  gap: 8px;
}
.faq-items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.faq-item {
  border: 1px solid #eef0f3;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.15s;
}
.faq-item.open {
  border-color: #c6e2ff;
}
.faq-question {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  font-size: 14px;
  color: #1f2329;
}
.faq-question:hover {
  color: #409eff;
}
.caret {
  color: #a9aeb8;
  transition: transform 0.2s;
  flex: 0 0 auto;
}
.faq-item.open .caret {
  transform: rotate(90deg);
}
.faq-answer {
  padding: 0 12px 12px;
  font-size: 13px;
  color: #4e5969;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
