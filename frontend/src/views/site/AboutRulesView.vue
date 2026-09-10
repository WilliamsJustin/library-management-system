<template>
  <div class="page container">
    <h1 class="page-title">规章制度</h1>
    <AboutSubNav current="rules" />

    <el-card class="block" shadow="never">
      <ul class="rule-list">
        <li v-for="r in rules" :key="r.title">
          <span class="rule-title">{{ r.title }}</span>
          <span class="rule-desc">{{ r.desc }}</span>
        </li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import AboutSubNav from './AboutSubNav.vue'

const rules = [
  { title: '入馆须知', desc: '凭本人读者证或学/工号入馆，请配合门禁与安检，勿携带食品饮料。' },
  { title: '借阅规则', desc: '学生可借 5 册（借期 30 天），教师可借 10 册（借期 60 天），每册可续借 1 次。' },
  { title: '逾期处理', desc: '逾期 0.10 元/天/册；逾期期间暂停借阅权限，缴清罚款后自动恢复。' },
  { title: '文明用馆', desc: '馆内保持安静、手机静音；爱护文献，严禁涂画、撕页、污损。' },
  { title: '电子资源', desc: '校内网络免费使用；禁止批量下载、转发或商业性使用。' },
  { title: '遗失与损坏', desc: '遗失须购赔原书或按价赔偿；损坏视情节处理，严重者按校规追责。' }
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
.block {
  border-radius: 10px;
}
.rule-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.rule-list li {
  display: flex;
  gap: 14px;
  align-items: baseline;
  padding: 12px 4px;
  border-bottom: 1px dashed #eef0f3;
}
.rule-list li:last-child {
  border-bottom: none;
}
.rule-title {
  flex-shrink: 0;
  width: 96px;
  font-weight: 600;
  color: #1f2329;
}
.rule-desc {
  color: #4e5969;
  line-height: 1.7;
}
</style>
