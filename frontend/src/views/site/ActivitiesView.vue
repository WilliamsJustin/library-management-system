<template>
  <div class="page container">
    <h1 class="page-title">读者活动</h1>
    <p class="lead">图书馆全年举办丰富多彩的阅读推广与素养培训活动，欢迎师生积极参与。</p>

    <el-timeline v-loading="loading" class="timeline">
      <el-timeline-item
        v-for="(a, i) in activities"
        :key="a.id ?? i"
        :timestamp="a.dateText"
        :type="tagType(a.tag)"
        placement="top"
      >
        <el-card class="act-card clickable" shadow="hover" @click="openActivity(a)">
          <div class="act-head">
            <span class="act-title">{{ a.title }}</span>
            <el-tag size="small" :type="tagType(a.tag)">{{ a.tag }}</el-tag>
          </div>
          <div class="act-desc">{{ a.content }}</div>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-if="!loading && activities.length === 0" description="暂无活动，敬请期待" />

    <el-dialog
      v-model="dialogVisible"
      :title="activeActivity?.title"
      width="520px"
      append-to-body
    >
      <div v-if="activeActivity" class="act-detail">
        <div class="act-detail-meta">
          <el-tag size="small" :type="tagType(activeActivity.tag)">{{ activeActivity.tag }}</el-tag>
          <span class="act-detail-date">{{ activeActivity.dateText }}</span>
        </div>
        <div class="act-detail-content">{{ activeActivity.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { http } from '@/api/http'

const activities = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const activeActivity = ref(null)

/** 类别标签与时间线/标签颜色的映射 */
function tagType(tag) {
  const map = { 校级: 'danger', 培训: 'primary', 沙龙: 'success', 活动: 'warning', 竞赛: 'info' }
  return map[tag] || 'info'
}

function openActivity(a) {
  activeActivity.value = a
  dialogVisible.value = true
}

/** 从后端拉取活动列表（公开接口）；接口不可用时回退到本地示例数据 */
async function loadActivities() {
  loading.value = true
  try {
    const data = await http.get('/activities', { page: 0, size: 20 })
    activities.value = (data.content || []).map((a) => ({
      id: a.id,
      dateText: a.dateText,
      title: a.title,
      tag: a.tag,
      content: a.content
    }))
  } catch {
    // 后端不可用时展示默认示例，保证前台页面仍可浏览
    activities.value = [
      { dateText: '9月 · 全天', title: '读书月启动仪式', tag: '校级',
        content: '年度读书月开幕，发布共读书单与打卡挑战，参与即有机会获得阅读礼包。' },
      { dateText: '9月15日 14:00', title: '文献检索技能培训', tag: '培训',
        content: '图书馆员主讲：中外文数据库使用、核心期刊查找与参考文献管理工具实操。' },
      { dateText: '9月22日 19:00', title: '经典共读会 · 《百年孤独》', tag: '沙龙',
        content: '师生共读拉美文学经典，分享阅读心得，现场设有自由讨论环节。' },
      { dateText: '10月 · 每周三', title: '亲子绘本故事会', tag: '活动',
        content: '面向教职工子女的绘本讲读与手工活动，培养早期阅读兴趣。' },
      { dateText: '11月 · 全天', title: '信息素养大赛', tag: '竞赛',
        content: '以赛促学，提升学生检索、甄别与利用信息的能力，设校级奖项。' }
    ]
  } finally {
    loading.value = false
  }
}

onMounted(loadActivities)
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
.act-card.clickable {
  cursor: pointer;
  transition: background 0.15s ease;
}
.act-card.clickable:hover {
  background: #f5f8ff;
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
.act-more {
  margin-top: 8px;
  font-size: 13px;
  color: #409eff;
}
.act-detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.act-detail-date {
  font-size: 13px;
  color: #c0c4cc;
}
.act-detail-content {
  font-size: 15px;
  color: #1f2329;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
