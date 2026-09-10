<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { h, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElConfigProvider, ElNotification } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { useAuthStore } from '@/stores/auth'
import { useIdleTimeout } from '@/composables/useIdleTimeout'

// 全局微调 Element Plus 中文文案：分页跳页提示由「前往」改为「跳至」
const locale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    pagination: { ...zhCn.el.pagination, goto: '跳至' }
  }
}

const router = useRouter()
const authStore = useAuthStore()

// 会话空闲超时：无鼠标 / 键盘操作达 30 分钟，自动退出登录
const IDLE_MINUTES = 30
const IDLE_TIMEOUT = IDLE_MINUTES * 60 * 1000

// 空闲超时处理：清除登录状态，但保留当前页面（不做路由跳转）
const handleIdleTimeout = () => {
  authStore.logout()

  const notification = ElNotification({
    title: '已自动退出登录',
    type: 'warning',
    duration: 0, // 常驻，用户返回页面时仍可见
    message: h('div', { style: 'line-height: 1.6' }, [
      h('span', `检测到已超过 ${IDLE_MINUTES} 分钟无鼠标 / 键盘操作，已自动退出登录。`),
      h(
        'a',
        {
          style: 'color: #409eff; cursor: pointer; margin-left: 4px;',
          onClick: () => {
            notification.close()
            router.push('/login')
          }
        },
        '重新登录'
      )
    ])
  })
}

const { reset: resetIdle } = useIdleTimeout({
  timeout: IDLE_TIMEOUT,
  // 仅登录状态下才需要做空闲判定
  isEnabled: () => authStore.isAuthenticated,
  onTimeout: handleIdleTimeout
})

// 登录成功后重新开始计时
watch(
  () => authStore.isAuthenticated,
  (val) => {
    if (val) resetIdle()
  }
)
</script>
