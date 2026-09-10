import { onBeforeUnmount, onMounted } from 'vue'

// 视为「有操作」的交互事件：鼠标、键盘、触屏、滚轮、滚动
// 说明：scroll 不冒泡，这里统一用 capture 捕获，保证元素内部滚动也能被监听到
const DEFAULT_EVENTS: readonly string[] = [
  'mousemove',
  'mousedown',
  'mouseup',
  'click',
  'dblclick',
  'wheel',
  'keydown',
  'keyup',
  'scroll',
  'touchstart',
  'touchmove',
  'pointerdown'
]

export interface UseIdleTimeoutOptions {
  /** 空闲阈值（毫秒），默认 30 分钟 */
  timeout?: number
  /** 兜底巡检间隔（毫秒） */
  checkInterval?: number
  /** 参与计数的交互事件 */
  events?: readonly string[]
  /** 空闲判定是否生效，默认恒为 true */
  isEnabled?: () => boolean
  /** 判定为空闲后的回调 */
  onTimeout?: () => void
}

export interface UseIdleTimeoutReturn {
  start: () => void
  stop: () => void
  reset: () => void
  evaluate: () => void
}

/**
 * 会话空闲超时检测。
 *
 * 在任意鼠标 / 键盘等交互发生时刷新「最后活跃时间」；当空闲时长达到
 * timeout 时触发一次 onTimeout（同一段空闲只触发一次）。
 *
 * 设计要点：
 * 1. 采用「时间戳 + 定时巡检」而非纯 setTimeout —— 后台标签页计时器会被
 *    浏览器节流，单纯依赖定时器可能延迟很久才触发。
 * 2. 页面重新可见（visibilitychange）或窗口获得焦点（focus）时立即巡检一次，
 *    避免长时间挂后台后漏判。
 * 3. 记录交互时「先判定上一次是否已超时，再刷新时间」，否则挂后台很久后回到
 *    页面的第一次点击会先重置计时，导致永远判不出空闲。
 */
export function useIdleTimeout(options: UseIdleTimeoutOptions = {}): UseIdleTimeoutReturn {
  const {
    timeout = 30 * 60 * 1000,
    checkInterval = 10 * 1000,
    events = DEFAULT_EVENTS,
    isEnabled = () => true,
    onTimeout = () => {}
  } = options

  let lastActivity = Date.now()
  let timer: number | null = null
  let fired = false // 防止同一段空闲重复触发

  const enabled = (): boolean => {
    try {
      return !!isEnabled()
    } catch {
      return true
    }
  }

  // 巡检：空闲达到阈值且尚未触发过 → 触发回调
  const evaluate = (): void => {
    if (fired || !enabled()) return
    if (Date.now() - lastActivity >= timeout) {
      fired = true
      onTimeout()
    }
  }

  const markActivity = (): void => {
    evaluate() // 先判定「上一次交互」是否已超时
    lastActivity = Date.now()
  }

  // 重新开始计时（登录成功、手动唤醒时调用）
  const reset = (): void => {
    lastActivity = Date.now()
    fired = false
  }

  const onVisibilityChange = (): void => {
    if (document.visibilityState === 'visible') evaluate()
  }

  const start = (): void => {
    if (timer) return
    events.forEach((e) => window.addEventListener(e, markActivity, { passive: true, capture: true }))
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('focus', evaluate)
    timer = window.setInterval(evaluate, checkInterval)
  }

  const stop = (): void => {
    events.forEach((e) => window.removeEventListener(e, markActivity, { capture: true }))
    document.removeEventListener('visibilitychange', onVisibilityChange)
    window.removeEventListener('focus', evaluate)
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  onMounted(start)
  onBeforeUnmount(stop)

  return { start, stop, reset, evaluate }
}
