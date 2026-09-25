<template>
  <el-menu :default-active="activeIndex" class="layout-side-menu">
    <el-menu-item
      v-for="item in visibleItems"
      :key="item.index"
      :index="item.index"
      @click="onSelect(item)"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script lang="ts">
import type { Component } from 'vue'

/**
 * 侧栏菜单项定义：桌面 el-aside 与手机端抽屉/底部 Tab 共用同一份数据，
 * 避免两处菜单漂移（design.md D3）。
 */
export interface SideMenuItem {
  /** el-menu 的 index，同时作为激活标识 */
  index: string
  icon: Component
  label: string
  /** 点击跳转的路由 */
  to: string
  /** 激活高亮的路径前缀匹配（route.path.includes），缺省用 to 本身 */
  matchPrefixes?: string[]
  /** 显隐条件（如教师专属菜单），缺省可见 */
  visible?: boolean
}

/**
 * 按当前路径求激活菜单 index：先精确匹配 to，再按 matchPrefixes 前缀匹配，
 * 都不命中回退首项（首页）。之所以导出：读者后台底部 Tab 需要同一判定。
 * 注意首页项的 to（如 /reader）是其他路由的前缀，必须走精确匹配而非前缀。
 */
export function matchMenuIndex(items: SideMenuItem[], path: string): string {
  const exact = items.find((it) => it.to === path)
  if (exact) return exact.index
  const hit = items.find((it) =>
    (it.matchPrefixes ?? []).some((prefix) => path.includes(prefix))
  )
  return hit ? hit.index : items[0]?.index ?? ''
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const props = withDefaults(
  defineProps<{
    items: SideMenuItem[]
    /** 无匹配时是否回退高亮首项；「我的」弹层等局部菜单应传 false，避免误高亮 */
    fallbackToFirst?: boolean
  }>(),
  { fallbackToFirst: true }
)
const emit = defineEmits<{ (e: 'select', item: SideMenuItem): void }>()

const route = useRoute()
const router = useRouter()

const visibleItems = computed(() => props.items.filter((it) => it.visible !== false))

const activeIndex = computed(() => {
  const exact = props.items.find((it) => it.to === route.path)
  if (exact) return exact.index
  const hit = props.items.find((it) =>
    (it.matchPrefixes ?? []).some((prefix) => route.path.includes(prefix))
  )
  if (hit) return hit.index
  return props.fallbackToFirst ? props.items[0]?.index ?? '' : ''
})

function onSelect(item: SideMenuItem) {
  router.push(item.to)
  emit('select', item)
}
</script>

<style scoped>
.layout-side-menu {
  border-right: none;
  padding: 8px 0;
}
</style>
