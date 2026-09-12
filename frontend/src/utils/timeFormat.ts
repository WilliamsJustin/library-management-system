/**
 * 消息/留言时间的智能显示格式（对比消息日期与当前系统日期，按自然日判断，
 * 而非简单的 24 小时差值；自然周以周一为一周开始）：
 * 1. 今天（同年月日）：HH:mm，例：15:30
 * 2. 昨天：昨天 HH:mm，例：昨天 09:12
 * 3. 本周（昨天之后、同一自然周内）：星期X，例：星期三
 * 4. 其余（不在本周）：YYYY/MM/DD，例：2026/09/12
 */

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function hhmm(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

/** 本周周一 0 点（getDay: 周日=0 … 周六=6，换算成周一为一周开始） */
function mondayOfThisWeek(now: Date): Date {
  const day = startOfDay(now)
  const offset = (day.getDay() + 6) % 7
  day.setDate(day.getDate() - offset)
  return day
}

export function formatChatTime(value?: string | null): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''

  const now = new Date()
  const diffDays = Math.round(
    (startOfDay(now).getTime() - startOfDay(d).getTime()) / 86_400_000
  )

  if (diffDays <= 0) return hhmm(d) // 今天
  if (diffDays === 1) return `昨天 ${hhmm(d)}` // 昨天
  if (startOfDay(d).getTime() >= mondayOfThisWeek(now).getTime()) {
    return WEEKDAYS[d.getDay()] // 本周（昨天之后）
  }
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${p(d.getMonth() + 1)}/${p(d.getDate())}` // 不在本周
}
