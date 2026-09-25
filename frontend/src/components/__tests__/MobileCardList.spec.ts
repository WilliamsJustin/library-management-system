import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import MobileCardList from '../MobileCardList.vue'

interface Row {
  id: number
  title: string
}

const rows: Row[] = [
  { id: 1, title: '算法导论' },
  { id: 2, title: '红楼梦' }
]

function mountList(slots: Record<string, unknown> = {}, props: Record<string, unknown> = {}) {
  return mount(MobileCardList<Row>, {
    props: { rows, rowKey: (r: Row) => r.id, ...props },
    slots,
    global: { plugins: [ElementPlus] }
  })
}

describe('MobileCardList', () => {
  it('按 rows 渲染一张卡片，插槽内容携带行数据与索引', () => {
    const wrapper = mountList({
      head: '<template #head="{ row, index }"><b>{{ row.title }}#{{ index }}</b></template>',
      body: '<template #body="{ row }"><span class="b">{{ row.title }}</span></template>',
      foot: '<template #foot="{ row }"><em>{{ row.title }}</em></template>'
    })
    expect(wrapper.findAll('.m-card')).toHaveLength(2)
    expect(wrapper.find('.m-card-head b').text()).toBe('算法导论#0')
    expect(wrapper.find('.m-card-body .b').text()).toBe('算法导论')
    expect(wrapper.find('.m-card-foot em').text()).toBe('算法导论')
  })

  it('rows 为空时渲染空态并带上描述文案', () => {
    const wrapper = mount(MobileCardList<Row>, {
      props: { rows: [], rowKey: (r: Row) => r.id, emptyText: '暂无图书' },
      global: { plugins: [ElementPlus] }
    })
    expect(wrapper.findAll('.m-card')).toHaveLength(0)
    expect(wrapper.text()).toContain('暂无图书')
  })

  it('无 detail 插槽时不渲染展开按钮与详情区', () => {
    const wrapper = mountList({ body: '<template #body="{ row }">{{ row.title }}</template>' })
    expect(wrapper.find('.m-expand').exists()).toBe(false)
    expect(wrapper.find('.m-card-detail').exists()).toBe(false)
  })

  it('有 detail 插槽时：展开按钮切换详情区，逐卡独立', async () => {
    const wrapper = mountList({
      body: '<template #body="{ row }">{{ row.title }}</template>',
      detail: '<template #detail="{ row }"><span class="d">{{ row.title }}-详情</span></template>'
    })
    const buttons = wrapper.findAll('.m-expand')
    expect(buttons).toHaveLength(2)
    expect(buttons[0].text()).toBe('展开详情')

    await buttons[0].trigger('click')
    expect(wrapper.findAll('.m-card-detail')).toHaveLength(1)
    expect(wrapper.find('.m-card-detail .d').text()).toBe('算法导论-详情')
    expect(wrapper.findAll('.m-expand')[0].text()).toBe('收起')

    // 第二张卡独立展开 → 两张都展开
    await wrapper.findAll('.m-expand')[1].trigger('click')
    expect(wrapper.findAll('.m-card-detail')).toHaveLength(2)

    // 再点收起第一张
    await wrapper.findAll('.m-expand')[0].trigger('click')
    expect(wrapper.findAll('.m-card-detail')).toHaveLength(1)
    expect(wrapper.find('.m-card-detail .d').text()).toBe('红楼梦-详情')
  })

  it('expanded 按 rowKey 记忆：数据换序/翻页后展开态保留', async () => {
    const wrapper = mountList(
      {
        detail: '<template #detail="{ row }">{{ row.title }}</template>'
      },
      { emptyText: '' }
    )
    await wrapper.findAll('.m-expand')[1].trigger('click')
    expect(wrapper.findAll('.m-card-detail')).toHaveLength(1)

    // 翻页：换成另外的数据，id=2 的新数据仍处于展开态
    await wrapper.setProps({ rows: [{ id: 2, title: '红楼梦(第2页)' }, { id: 9, title: '新书' }] })
    const details = wrapper.findAll('.m-card-detail')
    expect(details).toHaveLength(1)
    expect(details[0].text()).toBe('红楼梦(第2页)')
  })
})
