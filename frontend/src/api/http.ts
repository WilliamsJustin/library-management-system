import type { UserProfile } from '@/types'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

/** 读取本地保存的 token */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** 保存登录态（用户档案 + token） */
export function setAuth(user: UserProfile, token: string): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  localStorage.setItem(TOKEN_KEY, token)
}

/** 清除登录态 */
export function clearAuth(): void {
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
}

/** 读取本地保存的用户档案（解析失败时返回 null） */
export function getStoredUser(): UserProfile | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserProfile
  } catch {
    return null
  }
}

/** 统一的接口错误对象，携带后端返回的业务 code 与 HTTP status */
export class ApiError extends Error {
  code?: string
  status: number

  constructor(message: string, code: string | null | undefined, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code ?? undefined
    this.status = status
  }
}

/** 查询参数取值类型（null / undefined / 空串会被自动跳过） */
export type QueryValue = string | number | boolean | null | undefined
export type QueryParams = Record<string, QueryValue>

function buildQuery(params?: QueryParams): string {
  const sp = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      sp.append(key, String(value))
    }
  })
  const s = sp.toString()
  return s ? `?${s}` : ''
}

interface RequestOptions {
  params?: QueryParams
  data?: unknown
  isForm?: boolean
}

async function request<T>(
  method: string,
  url: string,
  { params, data, isForm }: RequestOptions = {}
): Promise<T> {
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`
  if (!isForm) headers['Content-Type'] = 'application/json'

  const init: RequestInit = { method, headers }
  if (data !== undefined) init.body = isForm ? (data as BodyInit) : JSON.stringify(data)

  const res = await fetch(`/api${url}${buildQuery(params)}`, init)

  if (res.status === 401) {
    clearAuth()
    window.location.href = '/login'
    throw new ApiError('登录已过期，请重新登录', 'UNAUTHORIZED', 401)
  }

  if (res.status === 204) return undefined as unknown as T

  const text = await res.text()
  const body = text ? (JSON.parse(text) as Record<string, unknown> | null) : null

  if (!res.ok) {
    const message = (body && (body.message as string)) || `请求失败 (${res.status})`
    const code = body && (body.code as string | undefined)
    throw new ApiError(message, code, res.status)
  }
  return body as T
}

export const http = {
  get: <T = unknown>(url: string, params?: QueryParams) => request<T>('GET', url, { params }),
  post: <T = void>(url: string, data?: unknown) => request<T>('POST', url, { data }),
  put: <T = void>(url: string, data?: unknown) => request<T>('PUT', url, { data }),
  patch: <T = void>(url: string, data?: unknown) => request<T>('PATCH', url, { data }),
  delete: <T = void>(url: string) => request<T>('DELETE', url),
  upload: <T = unknown>(url: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<T>('POST', url, { isForm: true, data: form })
  }
}
