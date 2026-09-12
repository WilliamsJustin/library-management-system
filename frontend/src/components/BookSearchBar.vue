<template>
  <div class="book-search-bar">
    <el-select
      v-model="field"
      size="large"
      class="field-select"
      placeholder="检索字段"
    >
      <el-option label="任意词" value="any" />
      <el-option label="题名" value="title" />
      <el-option label="著者" value="author" />
      <el-option label="ISBN" value="isbn" />
      <el-option label="出版社" value="publisher" />
      <el-option label="主题" value="subject" />
    </el-select>
    <el-input
      v-model="keyword"
      size="large"
      class="keyword-input"
      placeholder="输入检索词进行检索"
      :prefix-icon="Search"
      clearable
      @keyup.enter="emit('search')"
    />
    <el-button type="primary" size="large" :loading="loading" @click="emit('search')">
      检索
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'

// 前台首页与「图书检索结果」页共用的检索框，保证两处交互与样式完全一致。
const keyword = defineModel<string>('keyword', { default: '' })
const field = defineModel<string>('field', { default: 'any' })

withDefaults(defineProps<{ loading?: boolean }>(), { loading: false })

const emit = defineEmits<{ (e: 'search'): void }>()
</script>

<style scoped>
.book-search-bar {
  display: flex;
  gap: 12px;
  width: 100%;
}

.field-select {
  width: 130px;
  flex: 0 0 130px;
}

.keyword-input {
  flex: 1;
  min-width: 0;
}
</style>
