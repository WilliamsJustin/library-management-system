import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, ref } from 'vue'
import { useCrossPageSelection, type CrossPageSelection } from '../useCrossPageSelection'

interface Row {
  id: number
  title: string
}

const page1: Row[] = [
  { id: 1, title: 'A' },
  { id: 2, title: 'B' },
  { id: 3, title: 'C' }
]

let cleanup: (() => void) | null = null
afterEach(() => {
  cleanup?.()
  cleanup = null
})

/**
 * composable 内部注册了 onMounted/onBeforeUnmount（Shift 全局按键监听），
 * 必须挂在宿主组件上测试——直接调用不会触发生命周期。
 */
function setup(overrides: Partial<{ mobile: boolean; rows: Row[]; fetchAllIds: () => Promise<number[]> }> = {}) {
  const isMobile = ref(overrides.mobile ?? true)
  const rows = ref<Row[]>(overrides.rows ?? page1.map((r) => ({ ...r })))
  const onFetchError = vi.fn()
  const fetchAllIds = overrides.fetchAllIds ?? (() => Promise.resolve([1, 2, 3, 4]))

  let sel!: CrossPageSelection<Row>
  const app = createApp(
    defineComponent({
      setup() {
        sel = useCrossPageSelection<Row>({ rows, fetchAllIds, onFetchError, isMobileOverride: isMobile })
        return () => h('div')
      }
    })
  )
  app.mount(document.createElement('div'))
  cleanup = () => app.unmount()
  return { sel, rows, isMobile, onFetchError }
}

/** 造一个假 el-table ref：记录 toggleRowSelection / clearSelection 调用 */
function fakeTable() {
  const toggled: Array<{ id: number; selected?: boolean }> = []
  let cleared = 0
  return {
    toggled,
    clearedCount: () => cleared,
    ref: {
      toggleRowSelection: (row: Row, selected?: boolean) => toggled.push({ id: row.id, selected }),
      clearSelection: () => {
        cleared++
      }
    }
  }
}

function pressShift(down: boolean) {
  window.dispatchEvent(new KeyboardEvent(down ? 'keydown' : 'keyup', { key: 'Shift' }))
}

describe('useCrossPageSelection', () => {
  it('手机勾选 → selected 含该行；再勾选取消', async () => {
    const { sel } = setup()
    sel.toggleMobile(page1[0], true)
    await nextTick()
    expect(sel.selected.value).toEqual([page1[0]])

    sel.toggleMobile(page1[0], false)
    await nextTick()
    expect(sel.selected.value).toEqual([])
  })

  it('跨页保留：翻页后不在当前页的已选项以占位对象存在', async () => {
    const { sel, rows } = setup()
    sel.toggleMobile(page1[0], true)
    sel.toggleMobile(page1[1], true)

    // 翻到第 2 页：当前页只有 id=3（未勾选），已选的 1/2 都不在当前页
    rows.value = [{ id: 3, title: 'C' }]
    await nextTick()
    expect(sel.selected.value).toEqual([{ id: 1 }, { id: 2 }])
  })

  it('手机 clear 清空选择集', async () => {
    const { sel } = setup()
    sel.toggleMobile(page1[0], true)
    sel.toggleMobile(page1[1], true)
    sel.clear()
    expect(sel.selected.value).toEqual([])
  })

  it('桌面：selection-change 事件同步 tableSelected', async () => {
    const { sel } = setup({ mobile: false })
    sel.tableOpts.on['selection-change']([page1[0]])
    await nextTick()
    expect(sel.selected.value).toEqual([page1[0]])
  })

  it('桌面 Shift 区间选择：锚点 0 → Shift 点第 3 行，整段补选', async () => {
    const { sel } = setup({ mobile: false })
    const fake = fakeTable()
    sel.tableRef.value = fake.ref

    // 第一次点击（无 Shift）：只设置锚点
    sel.tableOpts.on.select([page1[0]], page1[0])
    expect(fake.toggled).toHaveLength(0)

    pressShift(true)
    sel.tableOpts.on.select([page1[2]], page1[2])
    pressShift(false)

    expect(fake.toggled).toHaveLength(3)
    expect(fake.toggled.map((t) => t.id)).toEqual([1, 2, 3])
  })

  it('表头全选：按 fetchAllIds 补选当前页外的 id（占位对象）', async () => {
    const { sel } = setup({ mobile: false, fetchAllIds: () => Promise.resolve([1, 2, 3, 9]) })
    const fake = fakeTable()
    sel.tableRef.value = fake.ref

    // 模拟当前页已全部勾选
    sel.tableOpts.on['selection-change']([...page1])
    await sel.tableOpts.on['select-all']([...page1])

    expect(fake.toggled).toEqual([{ id: 9, selected: true }])
  })

  it('全选拉取失败：重置 Shift 锚点并通知 onFetchError（Q4 统一语义）', async () => {
    const { sel, onFetchError } = setup({
      mobile: false,
      fetchAllIds: () => Promise.reject(new Error('网络错误'))
    })
    const fake = fakeTable()
    sel.tableRef.value = fake.ref

    // 先建立锚点 0
    sel.tableOpts.on.select([page1[0]], page1[0])

    pressShift(true)
    sel.tableOpts.on['selection-change']([...page1])
    await sel.tableOpts.on['select-all']([...page1])
    pressShift(false)

    expect(onFetchError).toHaveBeenCalledTimes(1)
    // 失败后锚点已重置：再 Shift 点击不应触发区间补选
    const togglesBefore = fake.toggled.length
    pressShift(true)
    sel.tableOpts.on.select([page1[2]], page1[2])
    pressShift(false)
    expect(fake.toggled.length).toBe(togglesBefore)
  })

  it('桌面 clear：调用表格 clearSelection 并重置锚点', async () => {
    const { sel } = setup({ mobile: false })
    const fake = fakeTable()
    sel.tableRef.value = fake.ref

    sel.tableOpts.on.select([page1[0]], page1[0])
    sel.clear()
    expect(fake.clearedCount()).toBe(1)

    // 锚点已重置：Shift 点击不再区间补选
    pressShift(true)
    sel.tableOpts.on.select([page1[2]], page1[2])
    pressShift(false)
    expect(fake.toggled).toHaveLength(0)
  })
})
