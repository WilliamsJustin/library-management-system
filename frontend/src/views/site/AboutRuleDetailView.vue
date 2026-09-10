<template>
  <div class="page container">
    <h1 class="page-title">规章制度</h1>
    <AboutSubNav current="rules" />

    <template v-if="rule">
      <div class="crumb">
        <router-link class="crumb-link" :to="{ name: 'rules' }">规章制度</router-link>
        <span class="crumb-sep">/</span>
        <span class="crumb-cur">正文</span>
      </div>

      <h2 class="doc-title">{{ rule.title }}</h2>
      <div class="doc-meta">最后修改时间：{{ rule.updatedAt }}</div>

      <!-- 长文档（如《实施指南》）自动生成目录 -->
      <div v-if="toc.length >= 3" class="doc-toc">
        <div class="toc-head">目录</div>
        <ul class="toc-list">
          <li v-for="t in toc" :key="t.anchor">
            <router-link :to="{ hash: '#' + t.anchor }">{{ t.num ? t.num + ' ' : '' }}{{ t.text }}</router-link>
          </li>
        </ul>
      </div>

      <div class="doc-body">
        <template v-for="(b, i) in rule.blocks" :key="i">
          <p v-if="b.type === 'p'" class="doc-p">{{ b.text }}</p>

          <h3 v-else-if="b.type === 'h2'" :id="anchorOf(i)" class="doc-h2">
            <span v-if="b.num" class="doc-num">{{ b.num }}</span>{{ b.text }}
          </h3>

          <h4 v-else-if="b.type === 'h3'" class="doc-h3">
            <span v-if="b.num" class="doc-num">{{ b.num }}</span>{{ b.text }}
          </h4>

          <h5 v-else-if="b.type === 'h4'" class="doc-h4">
            <span v-if="b.num" class="doc-num">{{ b.num }}</span>{{ b.text }}
          </h5>

          <div v-else-if="b.type === 'item'" class="doc-item">
            <span class="doc-label">{{ b.label }}</span>
            <span class="doc-text">{{ b.text }}</span>
          </div>
        </template>
      </div>

      <div class="doc-foot">
        <router-link class="back-btn" :to="{ name: 'rules' }">← 返回规章制度列表</router-link>
      </div>
    </template>

    <el-empty v-else description="未找到该规章制度">
      <router-link class="back-btn" :to="{ name: 'rules' }">← 返回规章制度列表</router-link>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AboutSubNav from './AboutSubNav.vue'
import { getRuleById } from '@/data/rules'

const route = useRoute()
const rule = computed(() => getRuleById(String(route.params.id)))

/** 一级章节标题自动生成锚点，供右上目录跳转 */
const anchorOf = (index: number) => `rule-sec-${index}`

const toc = computed(() => {
  const items: { anchor: string; num?: string; text: string }[] = []
  if (!rule.value) return items
  // 用 forEach + 类型收窄（b.type === 'h2'）而不是先 map 再 filter，
  // 否则联合类型 RuleBlock 上的 num 无法被 TS 识别。
  rule.value.blocks.forEach((b, i) => {
    if (b.type === 'h2') items.push({ anchor: anchorOf(i), num: b.num, text: b.text })
  })
  return items
})
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

/* 面包屑 */
.crumb {
  font-size: 13px;
  color: #86909c;
  margin-bottom: 14px;
}
.crumb-link {
  color: #409eff;
  text-decoration: none;
}
.crumb-link:hover {
  text-decoration: underline;
}
.crumb-sep {
  margin: 0 8px;
  color: #c9cdd4;
}

/* 文档标题与元信息 */
.doc-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1.5;
  margin: 0 0 8px;
}
.doc-meta {
  font-size: 12px;
  color: #a8abb2;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f5;
}

/* 目录 */
.doc-toc {
  margin: 20px 0 8px;
  padding: 16px 20px;
  background: #f7f9fc;
  border: 1px solid #eef0f3;
  border-radius: 10px;
}
.toc-head {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 10px;
}
.toc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px 20px;
}
.toc-list a {
  font-size: 13px;
  color: #4e5969;
  text-decoration: none;
}
.toc-list a:hover {
  color: #409eff;
}

/* 正文 */
.doc-body {
  margin-top: 20px;
}
.doc-h2 {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: #1f2329;
  margin: 30px 0 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eef0f3;
  scroll-margin-top: 80px;
}
.doc-h2:first-child {
  margin-top: 6px;
}
.doc-h3 {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin: 20px 0 10px;
}
.doc-h4 {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4e5969;
  margin: 14px 0 8px;
}
.doc-num {
  flex-shrink: 0;
  color: #409eff;
  font-weight: 700;
}
.doc-p {
  font-size: 14px;
  line-height: 1.9;
  color: #4e5969;
  margin: 0 0 10px;
  text-align: justify;
}
.doc-item {
  display: flex;
  gap: 10px;
  font-size: 14px;
  line-height: 1.9;
  color: #4e5969;
  margin: 0 0 10px;
  text-align: justify;
}
.doc-label {
  flex-shrink: 0;
  min-width: 3.6em;
  color: #409eff;
  font-weight: 600;
}
.doc-text {
  flex: 1;
  min-width: 0;
}

/* 底部返回 */
.doc-foot {
  margin-top: 32px;
  padding-top: 18px;
  border-top: 1px solid #f0f2f5;
}
.back-btn {
  display: inline-block;
  font-size: 14px;
  color: #409eff;
  text-decoration: none;
  padding: 7px 18px;
  border: 1px solid #c6e2ff;
  border-radius: 20px;
  background: #f5faff;
  transition: all 0.2s;
}
.back-btn:hover {
  color: #fff;
  background: #409eff;
  border-color: #409eff;
}
</style>
