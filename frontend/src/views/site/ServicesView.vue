<template>
  <div class="page container">
    <h1 class="page-title">读者服务</h1>

    <div class="service-grid">
      <el-card
        v-for="s in services"
        :key="s.title"
        class="service-card"
        shadow="hover"
        :class="{ clickable: s.to || s.dialog }"
        @click="onCardClick(s)"
      >
        <div class="service-icon">
          <el-icon :size="28"><component :is="s.icon" /></el-icon>
        </div>
        <div class="service-title">{{ s.title }}</div>
        <div class="service-desc">{{ s.desc }}</div>
      </el-card>
    </div>

    <!-- 逾期与提醒规则弹窗：点击「逾期与提醒规则」卡片打开 -->
    <el-dialog v-model="rulesVisible" title="逾期与提醒规则" width="560px">
      <div class="rules-body">
        <section class="rule-section">
          <h3>一、怎样算逾期</h3>
          <p>
            超过应还时间仍未归还的图书即为逾期。应还时间 = 借出时间 + 借期，
            在线续借成功后借期自动顺延，应还时间相应推迟。
          </p>
        </section>
        <section class="rule-section">
          <h3>二、到期前多久会提醒</h3>
          <p>
            图书到期前 <b>5 分钟</b>，系统会向您推送一次站内提醒（每笔借阅只提醒一次；
            续借成功后提醒标记重置，新借期到期前会再次提醒）。
          </p>
        </section>
        <section class="rule-section">
          <h3>三、逾期后会怎样处理、罚金计算规则</h3>
          <ul>
            <li>逾期后立即生成罚款，并在罚款缴清前<b>限制借阅资格</b>（无法借出新书）；</li>
            <li>罚款 = 0.10 元/分钟 × 逾期分钟数，不足 1 分钟按 1 分钟计；</li>
            <li>单本图书累计罚金上限 <b>144 元</b>（逾期满 24 小时封顶，不再累加）；</li>
            <li>在「我的罚款」页在线缴纳，缴清全部罚款后借阅资格自动恢复。</li>
          </ul>
        </section>
      </div>
      <template #footer>
        <el-button type="primary" @click="rulesVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Component } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { Reading, Bell, Document, Star } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

interface ServiceItem {
  title: string
  icon: Component
  desc: string
  /** 目标路由；不填表示仅作说明 */
  to?: string
  /** 点击打开说明弹窗（无跳转目标时使用） */
  dialog?: 'rules'
}

const services: ServiceItem[] = [
  { title: '图书借阅', icon: Reading, desc: '凭读者账号在线检索馆藏，到馆或自助借还机办理借还。', to: '/reader/borrow' },
  { title: '借阅查询', icon: Document, desc: '随时查看在借、已借与逾期记录，掌握借阅动态。', to: '/reader/my-loans' },
  { title: '我的收藏', icon: Star, desc: '把感兴趣的书加入收藏，随时回看馆藏与可借状态。', to: '/reader/favorites' },
  { title: '逾期与提醒规则', icon: Bell, desc: '怎样算逾期、到期前多久提醒、逾期罚金如何计算。', dialog: 'rules' }
]

const rulesVisible = ref(false)

function onCardClick(s: ServiceItem) {
  if (s.dialog === 'rules') {
    rulesVisible.value = true
    return
  }
  openService(s)
}

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
/* 逾期与提醒规则弹窗 */
.rules-body {
  line-height: 1.8;
}
.rule-section + .rule-section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f2f3f5;
}
.rule-section h3 {
  margin: 0 0 6px;
  font-size: 15px;
  color: #1f2329;
}
.rule-section p,
.rule-section ul {
  margin: 0;
  font-size: 13px;
  color: #4e5969;
}
.rule-section ul {
  padding-left: 18px;
}
.rule-section b {
  color: #e6a23c;
}
</style>
