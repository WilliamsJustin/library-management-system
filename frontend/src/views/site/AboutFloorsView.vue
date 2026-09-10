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

<script setup>
import AboutSubNav from './AboutSubNav.vue'
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
