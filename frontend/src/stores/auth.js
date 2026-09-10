import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http, getToken, setAuth, clearAuth, getStoredUser } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(getStoredUser())
  const token = ref(getToken())
  const isAuthenticated = computed(() => !!token.value)
  const userRole = computed(() => user.value?.role || null)
  const userReaderType = computed(() => user.value?.type || null)

  const login = async (account, password) => {
    const data = await http.post('/auth/login', { account, password })
    const profile = {
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

  const logout = () => {
    token.value = null
    user.value = null
    clearAuth()
  }

  // 注册成功等场景下，用后端返回的信息直接建立会话（自动登录）
  const setSession = (tokenValue, profile) => {
    token.value = tokenValue
    user.value = profile
    setAuth(profile, tokenValue)
    return profile
  }

  const changePassword = async (oldPassword, newPassword) => {
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
    changePassword
  }
})
