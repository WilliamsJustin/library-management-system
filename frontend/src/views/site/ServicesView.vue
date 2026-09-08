<template>
  <div class="page container">
    <h1 class="page-title">读者服务</h1>
    <p class="lead">面向全校读者提供一站式借阅与信息服务，登录后即可在线办理。</p>

    <el-row :gutter="16">
      <el-col :span="8" v-for="s in services" :key="s.title">
        <el-card class="service-card" shadow="hover">
          <div class="service-icon">
            <el-icon :size="28"><component :is="s.icon" /></el-icon>
          </div>
          <div class="service-title">{{ s.title }}</div>
          <div class="service-desc">{{ s.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      type="info"
      :closable="false"
      title="温馨提示"
      description="借阅、续借、预约等服务需在登录后使用。如尚未注册读者账号，请点击页面右上角「注册」自助开通。"
      show-icon
      class="tip"
    />
  </div>
</template>

<script setup>
import { Reading, Refresh, Bell, Document, Notebook, User } from '@element-plus/icons-vue'

const services = [
  { title: '图书借阅', icon: Reading, desc: '凭读者账号在线检索馆藏，到馆或自助借还机办理借还。' },
  { title: '在线续借', icon: Refresh, desc: '图书到期前可在线续借 1 次，自动顺延借阅期限。' },
  { title: '图书预约', icon: Notebook, desc: '心仪的在借图书可预约，归还后优先通知您取书。' },
  { title: '逾期提醒', icon: Bell, desc: '到期前 3 天自动推送提醒，避免产生滞纳金。' },
  { title: '读者证管理', icon: User, desc: '维护个人资料与联系方式，修改登录密码。' },
  { title: '借阅查询', icon: Document, desc: '随时查看在借、已借与逾期记录，掌握借阅动态。' }
]
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
.service-card {
  margin-bottom: 16px;
  border-radius: 10px;
  text-align: center;
  padding: 12px;
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
