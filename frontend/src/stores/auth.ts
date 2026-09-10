import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http, getToken, setAuth, clearAuth, getStoredUser } from '@/api/http'
import type { LoginResponse, ReaderType, UserProfile, UserRole } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(getStoredUser())
  const token = ref<string | null>(getToken())
  const isAuthenticated = computed(() => !!token.value)
  const userRole = computed<UserRole | null>(() => user.value?.role ?? null)
  const userReaderType = computed<ReaderType | null>(() => user.value?.type ?? null)

  const login = async (account: string, password: string): Promise<UserProfile> => {
    const data = await http.post<LoginResponse>('/auth/login', { account, password })
    const profile: UserProfile = {
      id: data.userId,
      name: data.name,
      role: data.role,
      type: data.readerType
    }
    token.value = data.token
    user.value = profile
    setAuth(profile, data.token)
    return profile
  }

  const logout = (): void => {
    token.value = null
    user.value = null
    clearAuth()
  }

  // 注册成功等场景下，用后端返回的信息直接建立会话（自动登录）
  const setSession = (tokenValue: string, profile: UserProfile): UserProfile => {
    token.value = tokenValue
    user.value = profile
    setAuth(profile, tokenValue)
    return profile
  }

  const changePassword = async (oldPassword: string, newPassword: string): Promise<void> => {
    await http.post('/auth/change-password', { oldPassword, newPassword })
  }

  return {
    user,
    token,
    isAuthenticated,
    userRole,
    userReaderType,
    login,
    logout,
    // 注意：RegisterView 依赖 setSession 完成注册后自动登录，必须对外暴露，
    // 否则调用处会「is not a function」（原 JS 版本漏返回，TS 迁移时一并修正）。
    setSession,
    changePassword
  }
})
