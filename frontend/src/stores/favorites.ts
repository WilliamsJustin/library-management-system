import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { FavoriteState } from '@/types'

/**
 * 收藏状态（跨页面共享）。
 *
 * 前台检索结果页 / 图书详情页 / 后台「我的收藏」页都要知道「这本书收藏了没」，
 * 如果各页自己请求会重复且状态不同步，统一放这里维护一份 id 集合。
 *
 * 只有「登录的读者」才有收藏概念：管理员账号没有读者侧的收藏入口，
 * 未登录访客点了收藏按钮应先被引导登录（由页面负责跳转），store 里静默跳过。
 */
export const useFavoriteStore = defineStore('favorites', () => {
  const authStore = useAuthStore()

  /** 已收藏的图书 ID */
  const ids = ref<number[]>([])

  /** 当前身份能否收藏（登录 + 读者角色，后端返回的角色是大写，统一转小写比较） */
  const canFavorite = computed(
    () => authStore.isAuthenticated && (authStore.userRole || '').toLowerCase() === 'reader'
  )

  /** 是否已收藏某本书 */
  const has = (bookId: number): boolean => ids.value.includes(bookId)

  /** 拉取收藏 ID 集合（换账号或内容变化后调用） */
  async function refresh(): Promise<void> {
    if (!canFavorite.value) {
      ids.value = []
      return
    }
    try {
      ids.value = await http.get<number[]>('/favorites/my/ids')
    } catch {
      ids.value = []
    }
  }

  /** 收藏（重复收藏由后端幂等处理） */
  async function add(bookId: number): Promise<void> {
    const res = await http.post<FavoriteState>(`/favorites/${bookId}`)
    if (res?.favorited && !has(bookId)) {
      ids.value = [bookId, ...ids.value]
    }
  }

  /** 取消收藏（幂等） */
  async function remove(bookId: number): Promise<void> {
    await http.delete(`/favorites/${bookId}`)
    ids.value = ids.value.filter((id) => id !== bookId)
  }

  /** 退出登录时清空，避免下个账号看到上一个账号的收藏状态 */
  function reset(): void {
    ids.value = []
  }

  return { ids, canFavorite, has, refresh, add, remove, reset }
})
