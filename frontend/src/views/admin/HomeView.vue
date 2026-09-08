<template>
  <div class="home-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>欢迎使用图书借阅系统</span>
        </div>
      </template>
      <div class="welcome-content">
        <p>请从左侧菜单选择功能</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-content {
  padding: 20px;
  font-size: 16px;
  color: #606266;
}
</style>