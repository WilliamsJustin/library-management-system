import { ref } from 'vue'

/**
 * 全站唯一断点：<=768px 为手机版形态，>768px 为电脑版形态（不分平板档）。
 * 规格来源：openspec/changes/responsive-mobile-web/spec.md「断点与形态切换」。
 */
export const MOBILE_BREAKPOINT = 768
const MOBILE_QUERY = `(max-width: ${MOBILE_BREAKPOINT}px)`

// 模块级单例：整个应用共享一份 matchMedia 监听，避免每个页面重复创建。
// 同步初始化 matches，CSR 下无首屏闪烁；不支持 matchMedia 的环境退化为电脑版。
const mql =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia(MOBILE_QUERY)
    : null

const isMobile = ref(mql ? mql.matches : false)

if (mql) {
  mql.addEventListener('change', (e) => {
    isMobile.value = e.matches
  })
}

/**
 * 响应式断点：模板中用 v-if="!isMobile" 保留桌面结构、v-else 渲染手机结构；
 * 样式级差异仍走 CSS 媒体查询（桌面优先、只增不改）。
 */
export function useBreakpoint() {
  return { isMobile }
}
