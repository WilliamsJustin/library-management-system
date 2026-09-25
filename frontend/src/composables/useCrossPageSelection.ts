import { computed, onBeforeUnmount, onMounted, ref, watch, type ComputedRef, type Ref } from 'vue'
import { useBreakpoint } from '@/composables/useBreakpoint'

/**
 * 跨页选择集（CONTEXT.md）：批量操作的选择状态——翻页/筛选后保留已选。
 *
 * 一个 interface，两个 adapter（design.md D3）：
 * - 桌面：el-table 表级事件 + reserve-selection，由 tableOpts 绑定到表格；
 * - 手机：id Set（mobileSelectedIds），由卡片复选框经 toggleMobile 驱动。
 * 批量操作与导出只消费统一的 selected。
 *
 * ⚠ 约定（design.md D4）：页面的选择列必须写 `:reserve-selection="true"`
 * （列级属性，本 composable 的绑定盖不住它），否则跨页保留失效。
 */
export interface CrossPageTableRef<T extends { id: number }> {
  clearSelection: () => void
  toggleRowSelection: (row: T, selected?: boolean) => void
}

export interface CrossPageSelectionOptions<T extends { id: number }> {
  /** 当前页数据（Shift 区间锚定用） */
  rows: Ref<T[]>
  /** 表头全选时按当前筛选拉全量 id；失败请 throw——composable 会走「失败重置锚点」分支 */
  fetchAllIds: () => Promise<number[]>
  /** 全选拉取失败回调（默认无；页面在此弹错误提示） */
  onFetchError?: (err: unknown) => void
  /** 测试注入手机/桌面形态；缺省用 useBreakpoint 的 isMobile */
  isMobileOverride?: Ref<boolean>
}

export interface CrossPageSelection<T extends { id: number }> {
  /** 模板 ref：绑定到 el-table（ref="tableRef"） */
  tableRef: Ref<CrossPageTableRef<T> | undefined>
  /** v-bind 到 el-table 的表级选项 */
  tableOpts: {
    rowKey: 'id'
    on: {
      select: (selection: T[], row: T) => void
      'select-all': (selection: T[]) => void
      'selection-change': (selection: T[]) => void
    }
  }
  /** 统一的选择结果：批量操作/导出只认它 */
  selected: ComputedRef<T[]>
  /** 手机卡片复选框勾选/取消 */
  toggleMobile: (row: T, checked: boolean) => void
  /** 手机卡片复选框当前态 */
  isMobileSelected: (row: T) => boolean
  /** 取消选择（双形态统一；Shift 锚点一并重置） */
  clear: () => void
}

export function useCrossPageSelection<T extends { id: number }>(
  options: CrossPageSelectionOptions<T>
): CrossPageSelection<T> {
  const { isMobile: breakpointMobile } = useBreakpoint()
  const isMobile = options.isMobileOverride ?? breakpointMobile

  const tableRef = ref<CrossPageTableRef<T> | undefined>()
  const tableSelected = ref<T[]>([])

  /* 手机端：id 集合即选择集，天然跨页保留 */
  const mobileSelectedIds = ref<Set<number>>(new Set())

  function toggleMobile(row: T, checked: boolean): void {
    const next = new Set(mobileSelectedIds.value)
    if (checked) next.add(row.id)
    else next.delete(row.id)
    mobileSelectedIds.value = next
  }

  function isMobileSelected(row: T): boolean {
    return mobileSelectedIds.value.has(row.id)
  }

  /** 统一两个来源：桌面透传表格选择；手机用「当前页命中 + 其余 id 占位」拼装 */
  const selected = computed(() => {
    if (!isMobile.value) return tableSelected.value
    const pageSelected = options.rows.value.filter((r) => mobileSelectedIds.value.has(r.id))
    const pageIds = new Set(pageSelected.map((r) => r.id))
    const placeholders = [...mobileSelectedIds.value]
      .filter((id) => !pageIds.has(id))
      .map((id) => ({ id }) as unknown as T)
    return [...pageSelected, ...placeholders]
  }) as ComputedRef<T[]>

  /* Shift 区间选择：锚点行（当前页内索引）+ 全局 Shift 按键状态。
     页面数据一旦更换（搜索/翻页/刷新），锚点即失效——自动重置，调用方无须记得 */
  let anchorIndex = -1
  let shiftPressed = false

  watch(options.rows, () => {
    anchorIndex = -1
  })

  function onKeyToggle(e: KeyboardEvent): void {
    if (e.key === 'Shift') shiftPressed = e.type === 'keydown'
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeyToggle)
    window.addEventListener('keyup', onKeyToggle)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', onKeyToggle)
    window.removeEventListener('keyup', onKeyToggle)
  })

  function onSelect(_selection: T[], row: T): void {
    const index = options.rows.value.findIndex((r) => r.id === row.id)
    if (shiftPressed && anchorIndex >= 0 && anchorIndex !== index) {
      const [start, end] = anchorIndex < index ? [anchorIndex, index] : [index, anchorIndex]
      for (let i = start; i <= end; i++) {
        const target = options.rows.value[i]
        if (target) tableRef.value?.toggleRowSelection(target, true)
      }
    }
    anchorIndex = index
  }

  /**
   * 表头全选/取消全选：作用于所有页。
   * - 全选：按当前筛选拉全量 id，把未勾选的补选上（跨页，经 reserve-selection 按 row-key 记录）；
   * - 取消全选：清空所有页的勾选；
   * - 拉取失败：重置 Shift 锚点（Q4 统一语义），并把错误交给 onFetchError。
   */
  async function onSelectAll(selection: T[]): Promise<void> {
    const allPageSelected =
      options.rows.value.length > 0 && options.rows.value.every((r) => selection.some((s) => s.id === r.id))
    if (!allPageSelected) {
      clear()
      return
    }
    try {
      const ids = await options.fetchAllIds()
      for (const id of ids) {
        if (!tableSelected.value.some((s) => s.id === id)) {
          // 不在当前页的行传最小占位对象即可：el-table 按 row-key 记录保留选择
          tableRef.value?.toggleRowSelection({ id } as unknown as T, true)
        }
      }
    } catch (err) {
      anchorIndex = -1
      options.onFetchError?.(err)
    }
  }

  function onSelectionChange(selection: T[]): void {
    tableSelected.value = selection
  }

  function clear(): void {
    if (isMobile.value) {
      mobileSelectedIds.value = new Set()
      return
    }
    tableRef.value?.clearSelection()
    anchorIndex = -1
  }

  return {
    tableRef,
    tableOpts: {
      rowKey: 'id',
      on: {
        select: onSelect,
        'select-all': onSelectAll,
        'selection-change': onSelectionChange
      }
    },
    selected,
    toggleMobile,
    isMobileSelected,
    clear
  }
}
