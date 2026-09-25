<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <div class="login-header" @click="goHome">
        <el-icon class="logo"><Reading /></el-icon>
        <h1>学校图书借阅系统</h1>
        <p class="subtitle">Library Management System</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleSubmit">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" block>
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/utils/error'
import { useRouter, useRoute } from 'vue-router'
import { Reading, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 服务端会话空闲超时后，http 层会带着 expired=1 跳回登录页 —— 这里说明原因，
// 否则用户会以为登录态是被莫名清掉的（会话存在 Redis 里，与浏览器关没关窗口无关）。
onMounted(() => {
  if (route.query.expired === '1') {
    ElMessage.warning('会话已超时（30 分钟无操作），请重新登录')
  }
})

/**
 * 解析登录后的跳转目标：
 * - 前台页面点击登录时携带 redirect = 当前路由，登录后回到该页面，不强跳后台首页；
 * - 无 redirect（直接访问登录页）时，按角色进入各自后台首页。
 * 仅接受站内绝对路径，且排除 // 开头的协议相对地址，避免开放重定向。
 */
function resolveRedirect() {
  const target = route.query.redirect
  if (typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')) {
    return target
  }
  return null
}

const goHome = () => {
  router.push('/')
}

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  account: '',
  password: ''
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  
  loading.value = true
  try {
    await authStore.login(form.account, form.password)
    const redirect = resolveRedirect()
    if (redirect) {
      router.replace(redirect)
    } else {
      router.replace(authStore.userRole?.toLowerCase() === 'admin' ? '/admin' : '/reader')
    }
  } catch (error) {
    // 登录失败必须让用户看见原因（账号密码错误 / 会话服务 503 / 网络不通），
    // 之前这里只 console.error，界面上毫无反馈，会被当成「点了没反应」的 bug
    ElMessage.error(errorMessage(error, '登录失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
}

.login-card {
  width: 380px;
  max-width: 90vw;
  padding: 16px 30px 30px;
  border-radius: 14px;
}

.login-header {
  text-align: center;
  margin-bottom: 20px;
  cursor: pointer;
}

.logo {
  font-size: 44px;
  color: #409eff;
}

.login-header h1 {
  font-size: 22px;
  font-weight: 600;
  margin: 10px 0 4px;
  color: #1f2329;
}

.subtitle {
  color: #909399;
  font-size: 13px;
  letter-spacing: 1px;
}
</style>