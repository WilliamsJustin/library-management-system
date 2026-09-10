<template>
  <div class="sub-nav">
    <router-link class="sub-nav-item" :class="{ on: current === 'intro' }" to="/about">本馆简介</router-link>
    <router-link class="sub-nav-item" :class="{ on: current === 'rules' }" to="/about/rules">规章制度</router-link>
    <router-link class="sub-nav-item" :class="{ on: current === 'floors' }" to="/about/floors">开放时间</router-link>
  </div>
</template>

<script setup>
defineProps({
  current: { type: String, default: 'intro' }
})
</script>

<style scoped>
.sub-nav {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.sub-nav-item {
  text-decoration: none;
  color: #4e5969;
  font-size: 14px;
  padding: 7px 18px;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  background: #fff;
  transition: all 0.2s;
}
.sub-nav-item:hover,
.sub-nav-item.on {
  color: #fff;
  background: #409eff;
  border-color: #409eff;
}
</style>
