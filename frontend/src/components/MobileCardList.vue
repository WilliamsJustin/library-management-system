<template>
  <div v-loading="loading" class="mobile-card-list">
    <el-empty v-if="!loading && rows.length === 0" :description="emptyText" />
    <div v-for="(row, index) in rows" :key="rowKeyOf(row)" class="m-card" @click="emit('row-click', row)">
      <div v-if="$slots.head" class="m-card-head">
        <slot name="head" :row="row" :index="index" />
      </div>
      <div v-if="$slots.sub" class="m-card-sub">
        <slot name="sub" :row="row" :index="index" />
      </div>
      <div v-if="$slots.body" class="m-card-body">
        <slot name="body" :row="row" :expanded="isExpanded(row)" />
      </div>
      <div v-if="$slots.detail && isExpanded(row)" class="m-card-detail">
        <slot name="detail" :row="row" />
      </div>
      <div
        v-if="$slots.detail || ($slots.foot && (!footVisible || footVisible(row)))"
        class="m-card-foot"
      >
        <button v-if="$slots.detail" class="m-expand" type="button" @click="toggleExpand(row)">
          {{ isExpanded(row) ? '收起' : '展开详情' }}
        </button>
        <slot name="foot" :row="row" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T">
/**
 * 移动卡片形态（CONTEXT.md）：数据表格在 ≤768px 的摘要卡片列表。
 *
 * 骨架（卡片结构 / expanded 状态 / 展开按钮 / 空态 / 加载）藏在这里，
 * 字段编排由页面经插槽注入——页面只写个性，不复制骨架。
 * expanded 按 rowKey 记忆：翻页与筛选后保留，仅收起时移除。
 * 桌面端不使用本组件（桌面仍渲染 el-table）。
 */
import { computed, ref } from 'vue'

const props = defineProps<{
  rows: T[]
  /** 展开态的记忆键：推荐 (row) => row.id */
  rowKey: (row: T) => number | string
  loading?: boolean
  emptyText?: string
  /** 按行决定是否渲染操作区（如已归还的借阅没有还书/续借）；缺省恒渲染 */
  footVisible?: (row: T) => boolean
}>()

const slots = defineSlots<{
  head?: (scope: { row: T; index: number }) => unknown
  sub?: (scope: { row: T; index: number }) => unknown
  body?: (scope: { row: T; expanded: boolean }) => unknown
  /** 有此插槽才渲染「展开详情」按钮与详情区 */
  detail?: (scope: { row: T }) => unknown
  foot?: (scope: { row: T }) => unknown
  empty?: () => unknown
}>()

const expandedIds = ref<Set<number | string>>(new Set())
const hasDetail = computed(() => !!slots.detail)

/**
 * 整卡点击（可选）：如帮助后台的留言卡，点卡片即打开回复。
 * 未监听时无副作用；卡片内按钮的点击会冒泡到卡，语义等价的页面无影响。
 */
const emit = defineEmits<{ (e: 'row-click', row: T): void }>()

function rowKeyOf(row: T): number | string {
  return props.rowKey(row)
}

function isExpanded(row: T): boolean {
  return expandedIds.value.has(props.rowKey(row))
}

function toggleExpand(row: T): void {
  const key = props.rowKey(row)
  const next = new Set(expandedIds.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedIds.value = next
}
</script>
