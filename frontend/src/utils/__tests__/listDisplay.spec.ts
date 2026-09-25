import { describe, expect, it } from 'vitest'
import {
  formatAmount,
  isLoanOverdue,
  loanStatusText,
  loanTagType,
  pageIndexAscending,
  pageIndexDescending
} from '../listDisplay'
import { formatDate } from '@/utils/dateUtils'

describe('listDisplay', () => {
  it('pageIndexAscending：第 2 页从 size+1 开始', () => {
    expect(pageIndexAscending(1, 10, 0)).toBe(1)
    expect(pageIndexAscending(1, 10, 9)).toBe(10)
    expect(pageIndexAscending(2, 10, 0)).toBe(11)
    expect(pageIndexAscending(4, 10, 5)).toBe(36)
  })

  it('pageIndexDescending：第 1 行显示总数，依次递减（借阅流通语义）', () => {
    expect(pageIndexDescending(1, 10, 0, 34)).toBe(34)
    expect(pageIndexDescending(1, 10, 9, 34)).toBe(25)
    expect(pageIndexDescending(2, 10, 0, 34)).toBe(24)
    expect(pageIndexDescending(1, 10, 0, 0)).toBe(0)
  })

  it('借阅状态口径：文案与标签色一一对应', () => {
    expect(loanStatusText('ACTIVE')).toBe('在借')
    expect(loanStatusText('OVERDUE')).toBe('逾期')
    expect(loanStatusText('RETURNED')).toBe('已归还')
    expect(loanTagType('ACTIVE')).toBe('success')
    expect(loanTagType('OVERDUE')).toBe('danger')
    expect(loanTagType('RETURNED')).toBe('info')
  })

  it('isLoanOverdue：OVERDUE 直接算逾期', () => {
    expect(isLoanOverdue({ status: 'OVERDUE', dueDate: '2099-01-01T00:00:00' })).toBe(true)
  })

  it('isLoanOverdue：ACTIVE 且已过应还时间算逾期，未过期不算', () => {
    const past = new Date(Date.now() - 60_000).toISOString()
    const future = new Date(Date.now() + 60_000).toISOString()
    expect(isLoanOverdue({ status: 'ACTIVE', dueDate: past })).toBe(true)
    expect(isLoanOverdue({ status: 'ACTIVE', dueDate: future })).toBe(false)
  })

  it('isLoanOverdue：已归还不算逾期（即使缺 dueDate）', () => {
    expect(isLoanOverdue({ status: 'RETURNED' })).toBe(false)
  })

  it('formatAmount：两位小数，空值 0.00', () => {
    expect(formatAmount(144)).toBe('144.00')
    expect(formatAmount('3.6')).toBe('3.60')
    expect(formatAmount(null)).toBe('0.00')
    expect(formatAmount(undefined)).toBe('0.00')
  })

  it('dateUtils.formatDate 与原 9 份本地实现等价：空值空串、精确到分钟', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate(undefined)).toBe('')
    expect(formatDate('2026-09-25T15:30:00')).toMatch(/^2026-09-25 15:30$/)
  })
})
