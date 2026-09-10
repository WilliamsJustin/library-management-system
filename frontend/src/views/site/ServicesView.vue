<template>
  <div class="page container">
    <h1 class="page-title">读者服务</h1>

    <div class="service-grid">
      <el-card
        v-for="s in services"
        :key="s.title"
        class="service-card"
        shadow="hover"
        :class="{ clickable: s.to }"
        @click="s.to && openService(s)"
      >
        <div class="service-icon">
          <el-icon :size="28"><component :is="s.icon" /></el-icon>
        </div>
        <div class="service-title">{{ s.title }}</div>
        <div class="service-desc">{{ s.desc }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Component } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { Reading, Refresh, Bell, Document } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

interface ServiceItem {
  title: string
  icon: Component
  desc: string
  /** 目标路由；不填表示仅作说明（如「逾期提醒」） */
  to?: string
}

const services: ServiceItem[] = [
  { title: '图书借阅', icon: Reading, desc: '凭读者账号在线检索馆藏，到馆或自助借还机办理借还。', to: '/reader/borrow' },
  { title: '借阅查询', icon: Document, desc: '随时查看在借、已借与逾期记录，掌握借阅动态。', to: '/reader/my-loans' },
  { title: '在线续借', icon: Refresh, desc: '图书到期前可在线续借 1 次，自动顺延借阅期限。', to: '/reader/renew' },
  { title: '逾期提醒', icon: Bell, desc: '到期前 3 天自动推送提醒，避免产生滞纳金。' }
]

function openService(s: ServiceItem) {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录后再办理')
    // 登录后直接回到该服务页面，而不是跳后台首页
    router.push({ path: '/login', query: { redirect: s.to } })
    return
  }
  if (s.to) router.push(s.to)
}
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 8px;
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2329;
  margin: 0 0 12px;
}
.lead {
  font-size: 15px;
  color: #4e5969;
  line-height: 1.8;
  margin: 0 0 24px;
}
.service-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.service-card {
  width: 350px;
  height: 182px;
  margin-bottom: 16px;
  border-radius: 10px;
  text-align: center;
  box-sizing: border-box;
}
.service-card :deep(.el-card__body) {
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.service-card.clickable {
  cursor: pointer;
}
.service-go {
  margin-top: 10px;
  font-size: 13px;
  color: #409eff;
}
.service-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}
.service-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 8px;
}
.service-desc {
  font-size: 13px;
  color: #86909c;
  line-height: 1.6;
}
.tip {
  margin-top: 16px;
  border-radius: 8px;
}
</style>
