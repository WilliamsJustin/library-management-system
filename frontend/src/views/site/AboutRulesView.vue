<template>
  <div class="page container">
    <h1 class="page-title">规章制度</h1>
    <AboutSubNav current="rules" />

    <ul class="rule-list">
      <li v-for="r in rules" :key="r.id">
        <router-link class="rule-item" :to="{ name: 'rule-detail', params: { id: r.id } }">
          <span class="rule-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path
                d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path d="M14 3v5h5M9 13h7M9 17h5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="rule-main">
            <span class="rule-title">{{ r.title }}</span>
            <span class="rule-desc">{{ r.summary }}</span>
            <span class="rule-time">最后修改时间：{{ r.updatedAt }}</span>
          </span>
          <span class="rule-arrow" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M9 5l7 7-7 7" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
        </router-link>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import AboutSubNav from './AboutSubNav.vue'
import { rules } from '@/data/rules'
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
.rule-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 20px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
  text-decoration: none;
  transition: all 0.2s;
}
.rule-item:hover {
  border-color: #409eff;
  background: #f5faff;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}
.rule-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 9px;
  background: #ecf5ff;
  color: #409eff;
}
.rule-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.rule-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.5;
}
.rule-item:hover .rule-title {
  color: #409eff;
}
.rule-desc {
  font-size: 13px;
  color: #86909c;
  line-height: 1.6;
}
.rule-time {
  font-size: 12px;
  color: #c0c4cc;
}
.rule-arrow {
  flex-shrink: 0;
  align-self: center;
  display: inline-flex;
  color: #c0c4cc;
  transition: all 0.2s;
}
.rule-item:hover .rule-arrow {
  color: #409eff;
  transform: translateX(3px);
}
</style>
