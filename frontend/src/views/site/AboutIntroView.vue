<template>
  <div class="page container">
    <h1 class="page-title">本馆简介</h1>
    <AboutSubNav current="intro" />

    <p class="lead">
      学校图书借阅系统服务于全校师生，致力于建设开放、便捷、智能的文献资源中心，
      为教学科研与课外阅读提供坚实支撑。
    </p>

    <el-row :gutter="16" class="stats">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-num">{{ s.num }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="block" shadow="never">
      <template #header><span class="block-title">馆藏与服务</span></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="开放时间">周一至周日 8:00 – 22:00（法定节假日另行通知）</el-descriptions-item>
        <el-descriptions-item label="馆藏范围">中外文图书、期刊、电子资源与特色馆藏</el-descriptions-item>
        <el-descriptions-item label="借阅权限">学生 5 册 / 30 天，教师 10 册 / 60 天，可续借 1 次</el-descriptions-item>
        <el-descriptions-item label="滞纳金">逾期 0.10 元 / 天 / 册</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import AboutSubNav from './AboutSubNav.vue'

const stats = [
  { num: '60万+', label: '馆藏文献（册）' },
  { num: '1200+', label: '阅览座位（个）' },
  { num: '8:00–22:00', label: '每日开放' },
  { num: '365天', label: '全年服务' }
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
.stats {
  margin-bottom: 24px;
}
.stat-card {
  text-align: center;
  border-radius: 10px;
}
.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #409eff;
}
.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 6px;
}
.block {
  border-radius: 10px;
}
.block-title {
  font-weight: 600;
  color: #1f2329;
}
</style>
