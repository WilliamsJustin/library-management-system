/**
 * 站内跳转工具：在新浏览器标签页打开目标页面，当前页面保持不变。
 *
 * 首页、读者后台「图书借阅」页等页面上的检索 / 查看详情动作都用它，
 * 这样新页面不会顶掉用户正在看的页面（也便于对照查看、来回切换）。
 */

/** 部署基路径（vite base），去掉末尾斜杠：根部署为 ''，子路径部署为 '/app' */
const BASE = import.meta.env.BASE_URL.replace(/\/+$/, '')

type QueryValue = string | number | null | undefined

/**
 * 在新标签页打开站内页面。
 *
 * @param path  站内路径，以 `/` 开头（不含 base），如 `/search`、`/books/12`
 * @param query 查询参数，值为空字符串 / null / undefined 时会被忽略
 */
export function openInNewTab(
  path: string,
  query?: Record<string, QueryValue>
): Window | null {
  const url = new URL(`${BASE}${path}`, window.location.origin)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null || value === '') continue
      url.searchParams.set(key, String(value))
    }
  }
  // noopener：新页面拿不到本页的 window 引用（安全），因此返回值恒为 null
  return window.open(url.toString(), '_blank', 'noopener')
}
