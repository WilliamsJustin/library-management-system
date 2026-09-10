<template>
  <div class="page container">
    <h1 class="page-title">开放时间</h1>
    <AboutSubNav current="floors" />

    <el-card class="block" shadow="never">
      <table class="hours-table">
        <thead>
          <tr>
            <th>楼层</th>
            <th>开放区域</th>
            <th>服务时间</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>一楼</td>
            <td>总服务台 · 自助借还机 · 新书展厅</td>
            <td>08:30 - 21:00</td>
          </tr>
          <tr>
            <td>二楼</td>
            <td>社科与文学书库 · 阅览区</td>
            <td>08:30 - 22:00</td>
          </tr>
          <tr>
            <td>三楼</td>
            <td>科技与工具书库 · 电子阅览室</td>
            <td>08:30 - 22:00</td>
          </tr>
          <tr>
            <td>四楼</td>
            <td>研讨间 · 特藏与古籍阅览室</td>
            <td>09:00 - 17:30</td>
          </tr>
        </tbody>
      </table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import AboutSubNav from './AboutSubNav.vue'
</script>

<style scoped>
.container {
  width: calc(100% - 32px);
  max-width: 1200px;
  margin: 24px auto 40px;
  padding: 28px 32px 36px;
  /* 主内容区独立底色块，与页面两侧浅灰留白形成视觉隔离 */
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2329;
  margin: 0 0 12px;
}
/* 内容已处于白色面板内，去掉内层卡片边框与内边距，避免白底叠白底 */
.block {
  border: none;
  background: transparent;
}
.block :deep(.el-card__body) {
  padding: 0;
}
.hours-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 15px;
  color: #1f2329;
}
.hours-table th,
.hours-table td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}
.hours-table thead th {
  background: #f5f7fa;
  font-weight: 600;
  color: #303133;
}
/* 斑马条纹效果 */
.hours-table tbody tr:nth-child(odd) {
  background: #ffffff;
}
.hours-table tbody tr:nth-child(even) {
  background: #f7f9fc;
}
.hours-table tbody tr:hover {
  background: #ecf3ff;
}
</style>
