<template>
  <div class="page-bar" :class="{ 'is-center': center }">
    <span class="page-bar__total">共 {{ totalPages }} 页</span>
    <el-pagination
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      :background="background"
      @current-change="(p) => emit('change', p)"
    />
    <span class="page-bar__jump">
      跳至
      <el-input
        v-model="jumpValue"
        class="page-bar__input"
        size="small"
        @keyup.enter="doJump"
        @blur="jumpValue = ''"
      />
      页
    </span>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  /** 总条数 */
  total: { type: Number, default: 0 },
  /** 每页条数 */
  pageSize: { type: Number, default: 10 },
  /** 当前页（1 起） */
  currentPage: { type: Number, default: 1 },
  /** 页码按钮是否使用背景色 */
  background: { type: Boolean, default: false },
  /** 是否居中（默认右对齐） */
  center: { type: Boolean, default: false }
})

const emit = defineEmits(['change'])

/** 总页数：总数 0 时也至少显示 1 页，与 el-pagination 行为一致 */
const totalPages = computed(() => Math.max(1, Math.ceil((props.total || 0) / (props.pageSize || 1))))

/** 跳页输入框：默认为空，回车后跳转并清空 */
const jumpValue = ref('')

function doJump() {
  const n = parseInt(jumpValue.value, 10)
  jumpValue.value = ''
  if (!n || Number.isNaN(n)) return
  // 越界时收敛到有效范围
  const target = Math.min(Math.max(1, n), totalPages.value)
  if (target !== props.currentPage) emit('change', target)
}
</script>

<style scoped>
.page-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}
.page-bar.is-center {
  justify-content: center;
}
.page-bar__total {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}
.page-bar__jump {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}
.page-bar__input {
  width: 52px;
}
/* 覆盖全局 .el-pagination 的 margin-top，避免与容器重复 */
.page-bar :deep(.el-pagination) {
  margin-top: 0;
}
</style>
