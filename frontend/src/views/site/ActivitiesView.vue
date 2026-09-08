<template>
  <div class="page container">
    <h1 class="page-title">读者活动</h1>
    <p class="lead">图书馆全年举办丰富多彩的阅读推广与素养培训活动，欢迎师生积极参与。</p>

    <el-timeline class="timeline">
      <el-timeline-item
        v-for="(a, i) in activities"
        :key="i"
        :timestamp="a.date"
        :type="a.type"
        placement="top"
      >
        <el-card class="act-card" shadow="hover">
          <div class="act-head">
            <span class="act-title">{{ a.title }}</span>
            <el-tag size="small" :type="a.tagType">{{ a.tag }}</el-tag>
          </div>
          <div class="act-desc">{{ a.desc }}</div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup>
const activities = [
  { date: '9月 · 全天', title: '读书月启动仪式', tag: '校级', tagType: 'danger', type: 'danger',
    desc: '年度读书月开幕，发布共读书单与打卡挑战，参与即有机会获得阅读礼包。' },
  { date: '9月15日 14:00', title: '文献检索技能培训', tag: '培训', tagType: 'primary', type: 'primary',
    desc: '图书馆员主讲：中外文数据库使用、核心期刊查找与参考文献管理工具实操。' },
  { date: '9月22日 19:00', title: '经典共读会 · 《百年孤独》', tag: '沙龙', tagType: 'success', type: 'success',
    desc: '师生共读拉美文学经典，分享阅读心得，现场设有自由讨论环节。' },
  { date: '10月 · 每周三', title: '亲子绘本故事会', tag: '活动', tagType: 'warning', type: 'warning',
    desc: '面向教职工子女的绘本讲读与手工活动，培养早期阅读兴趣。' },
  { date: '11月 · 全天', title: '信息素养大赛', tag: '竞赛', tagType: 'info', type: 'info',
    desc: '以赛促学，提升学生检索、甄别与利用信息的能力，设校级奖项。' }
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
.timeline {
  max-width: 720px;
  padding: 8px 0 0 4px;
}
.act-card {
  border-radius: 10px;
}
.act-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.act-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}
.act-desc {
  font-size: 14px;
  color: #4e5969;
  line-height: 1.6;
}
</style>
