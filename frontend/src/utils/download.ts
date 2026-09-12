import { getToken } from '@/api/http'

/** 查询参数取值（与 http util 同规则：null/undefined/空串跳过） */
export type QueryValue = string | number | boolean | null | undefined

/**
 * GET 下载二进制文件（带登录态）：请求 /api 下接口，按响应头
 * Content-Disposition 还原文件名（后端用 filename*=UTF-8'' 编码），保存到本地。
 * headers 与 blob 解析失败时退回 fallbackName。
 */
export async function downloadGet(
  path: string,
  params: Record<string, QueryValue> = {},
  fallbackName = '导出文件.xlsx'
): Promise<void> {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      sp.append(key, String(value))
    }
  })
  const qs = sp.toString()

  const token = getToken()
  const res = await fetch(`/api${path}${qs ? `?${qs}` : ''}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!res.ok) {
    throw new Error(`下载失败 (${res.status})`)
  }
  const blob = await res.blob()

  const dispo = res.headers.get('Content-Disposition') || ''
  const m = dispo.match(/filename\*=UTF-8''([^;]+)/i)
  const name = m ? decodeURIComponent(m[1]) : fallbackName

  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
  URL.revokeObjectURL(url)
}
