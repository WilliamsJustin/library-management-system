<template>
  <div class="register-container">
    <router-link to="/" class="back-home">
      <el-icon><ArrowLeft /></el-icon> 返回首页
    </router-link>

    <el-card class="register-card" shadow="always">
      <div class="reg-header">
        <el-icon class="logo"><Reading /></el-icon>
        <h1>读者注册</h1>
        <p class="subtitle">开通读者账号，畅享借阅与续借服务</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" @submit.prevent="handleSubmit">
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="登录账号（3-50 位）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="真实姓名" :prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item label="学号/工号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="如 20240001 / T2001001" :prefix-icon="Ticket" />
        </el-form-item>
        <el-form-item label="读者类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="STUDENT">学生</el-radio>
            <el-radio value="TEACHER">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-64 位" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" :prefix-icon="Iphone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" block>注册并登录</el-button>
        </el-form-item>
        <div class="to-login">
          已有账号？<router-link to="/login">直接登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { http } from '@/api/http'
import { Reading, User, Postcard, Ticket, Lock, Iphone, ArrowLeft } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { LoginResponse, ReaderType } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<{
  account: string
  name: string
  studentNo: string
  type: ReaderType
  password: string
  confirm: string
  phone: string
}>({
  account: '',
  name: '',
  studentNo: '',
  type: 'STUDENT',
  password: '',
  confirm: '',
  phone: ''
})

const validateConfirm = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const rules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 50, message: '账号长度需在 3-50 位之间', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  studentNo: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]+$/, message: '只能包含字母、数字与连字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6-64 位之间', trigger: 'blur' }
  ],
  confirm: [{ validator: validateConfirm, trigger: 'blur' }]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await http.post<LoginResponse>('/auth/register', {
      account: form.account,
      name: form.name,
      studentNo: form.studentNo,
      type: form.type,
      password: form.password,
      phone: form.phone || undefined
    })
    authStore.setSession(data.token, {
      id: data.userId,
      name: data.name,
      role: data.role,
      type: data.readerType
    })
    ElMessage.success('注册成功，已自动登录')
    router.push('/reader')
  } catch (err) {
    ElMessage.error(errorMessage(err, '注册失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
}

.back-home {
  position: absolute;
  top: 20px;
  left: 24px;
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0.92;
}

.back-home:hover {
  opacity: 1;
}

.register-card {
  width: 440px;
  max-width: 92vw;
  padding: 12px 28px 24px;
  border-radius: 14px;
}

.reg-header {
  text-align: center;
  margin-bottom: 18px;
}

.logo {
  font-size: 44px;
  color: #409eff;
}

.reg-header h1 {
  font-size: 22px;
  font-weight: 600;
  margin: 10px 0 4px;
  color: #1f2329;
}

.subtitle {
  color: #909399;
  font-size: 13px;
}

.to-login {
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.to-login a {
  color: #409eff;
}
</style>
