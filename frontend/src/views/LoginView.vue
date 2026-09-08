<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
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

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { Reading, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  
  loading.value = true
  try {
    await authStore.login(form.account, form.password)
    router.push(authStore.userRole?.toLowerCase() === 'admin' ? '/admin' : '/reader')
  } catch (error) {
    console.error('登录失败:', error)
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