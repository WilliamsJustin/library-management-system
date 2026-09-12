import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useFavoriteStore } from '@/stores/favorites'
import { errorMessage } from '@/utils/error'
import type { Book } from '@/types'

/**
 * 公共书目页（检索结果页、图书详情页、读者后台「我的收藏」）上的「借阅 / 收藏」动作。
 *
 * 两个动作都在**当前页就地完成、不跳页**：
 * - 借阅：这里只做读者身份校验，校验通过后由页面用 `<BorrowDialog>` 弹出副本选择框；
 * - 收藏：直接调接口增 / 删，并同步 favorites store，按钮状态随之变为「已收藏 / 收藏」。
 *
 * 身份规则（四处保持一致）：
 * - 未登录 → 提示需要登录，并带 `redirect` 去登录页（登录后回到本页）；
 * - 已登录但是管理员 → 只提示改用读者账号，**不跳转**（否则会被路由守卫弹回 /admin 形成死循环）。
 */
export function useBookActions() {
  const router = useRouter()
  const route = useRoute()
  const authStore = useAuthStore()
  const favoriteStore = useFavoriteStore()

  /** 校验「当前是登录的读者」；不通过时已就地给出提示并返回 false */
  function ensureReader(): boolean {
    if (favoriteStore.canFavorite) return true
    if (authStore.isAuthenticated) {
      ElMessage.warning('当前是管理员账号，请使用读者账号登录后办理借阅 / 收藏')
      return false
    }
    ElMessage.warning('请先登录读者账号后再办理')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }

  /** 收藏 / 取消收藏：当前页直接执行，不跳页 */
  async function toggleFavorite(book: Book): Promise<void> {
    if (!ensureReader()) return

    // 已收藏时再点 = 取消收藏，先二次确认，避免误点（与「我的收藏」页一致）
    if (favoriteStore.has(book.id)) {
      try {
        await ElMessageBox.confirm(`确定取消收藏《${book.title}》吗？`, '取消收藏', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '再想想'
        })
      } catch {
        return // 用户放弃取消
      }
      try {
        await favoriteStore.remove(book.id)
        ElMessage.success('已取消收藏')
      } catch (err) {
        ElMessage.error(errorMessage(err, '取消收藏失败'))
      }
      return
    }

    try {
      await favoriteStore.add(book.id)
      ElMessage.success('已加入收藏')
    } catch (err) {
      ElMessage.error(errorMessage(err, '收藏失败'))
    }
  }

  /** 是否已收藏（用于按钮文案与样式） */
  const isFavorited = (book: Book): boolean => favoriteStore.has(book.id)

  return { ensureReader, toggleFavorite, isFavorited, favoriteStore }
}
