/**
 * 列表显示口径（CONTEXT.md「列表显示口径」）：列表页共享的显示规则。
 * 全部纯函数——视图层不得再各写一份。
 *
 * 语义来源（2026-09 架构评审收敛）：
 * - 序号递增：BooksView / ReaderManagementView / 各管理页原 indexMethod/rowIndex
 * - 序号递减：admin/LoansView 原 rowIndex（借阅流通页的既有产品行为：第 1 行显示总数）
 * - isLoanOverdue：MyLoansView / LoansView 原 isOverdue（OVERDUE，或 ACTIVE 且已过应还时间）
 * - formatAmount：reader/PenaltyView 原 formatAmount
 */
import type { LoanStatus } from '@/types'

/** 跨页连续序号（递增）：第 2 页从 size+1 开始 */
export function pageIndexAscending(page: number, size: number, index: number): number {
  return (page - 1) * size + index + 1
}

/** 跨页连续序号（递减）：第 1 行显示总数，依次递减（借阅流通页专用语义） */
export function pageIndexDescending(page: number, size: number, index: number, total: number): number {
  return total - (page - 1) * size - index
}

/** 借阅状态文案 */
export function loanStatusText(status: LoanStatus): string {
  return status === 'ACTIVE' ? '在借' : status === 'OVERDUE' ? '逾期' : '已归还'
}

/** 借阅状态标签色：在借 success / 逾期 danger / 已归还 info */
export function loanTagType(status: LoanStatus): 'success' | 'danger' | 'info' {
  return status === 'ACTIVE' ? 'success' : status === 'OVERDUE' ? 'danger' : 'info'
}

/** 逾期判定：状态为 OVERDUE，或仍 ACTIVE 但已过应还时间 */
export function isLoanOverdue(row: { status: LoanStatus; dueDate?: string | null }): boolean {
  return row.status === 'OVERDUE' ||
    (row.status === 'ACTIVE' && !!row.dueDate && new Date(row.dueDate) < new Date())
}

/** 金额格式化：两位小数，空值显示 0.00 */
export function formatAmount(value: number | string | null | undefined): string {
  return value != null ? Number(value).toFixed(2) : '0.00'
}
