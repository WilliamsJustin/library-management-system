<template>
  <div class="activity-manage">
    <h1>活动管理</h1>

    <el-card class="form-card">
      <template #header>新增读者活动</template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="活动标题" />
        </el-form-item>
        <el-form-item label="类别" required>
          <el-select v-model="form.tag" placeholder="选择活动类别" style="width: 200px">
            <el-option v-for="t in tagOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="介绍" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="活动介绍"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="publishing" @click="publish">新增活动</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card ref="listCardRef" class="list-card">
      <template #header>
        <div class="list-header">
          <span class="list-title">现有活动</span>
          <div class="list-filters">
            <el-input
              v-model="filters.keyword"
              placeholder="标题搜索"
              clearable
              style="width: 180px"
              @keyup.enter="doSearch"
              @clear="doSearch"
            />
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
            />
            <el-button type="primary" @click="doSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="介绍" min-width="240" show-overflow-tooltip />
        <el-table-column label="类别" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType(row.tag)">{{ row.tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.pinned" type="warning" size="small">置顶</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" :description="hasFilter ? '未找到匹配的活动' : '暂无活动'" />
      <PageBar
        v-if="total > pageSize"
        center
        background
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="editVisible" title="编辑活动" width="560px" append-to-body>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" maxlength="120" show-word-limit placeholder="活动标题" />
        </el-form-item>
        <el-form-item label="类别" required>
          <el-select v-model="editForm.tag" placeholder="选择活动类别" style="width: 200px">
            <el-option v-for="t in tagOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="介绍" required>
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="活动介绍"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="editForm.pinned" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'

const tagOptions = [
  { value: '校级', label: '校级' },
  { value: '培训', label: '培训' },
  { value: '沙龙', label: '沙龙' },
  { value: '活动', label: '活动' },
  { value: '竞赛', label: '竞赛' }
]

const form = ref({ title: '', tag: '', content: '', pinned: false })
const publishing = ref(false)
const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 5
const total = ref(0)
const listCardRef = ref(null)

/** 列表筛选：标题关键词 + 发布时间区间（[开始日期, 结束日期]，格式 YYYY-MM-DD） */
const filters = ref({ keyword: '', dateRange: null })
const hasFilter = computed(() => !!(filters.value.keyword || filters.value.dateRange))

const editVisible = ref(false)
const saving = ref(false)
const editForm = ref({ id: null, title: '', tag: '', content: '', pinned: false })

/** 类别标签与前台时间线颜色的映射（与 ActivitiesView 保持一致） */
function tagType(tag) {
  const map = { 校级: 'danger', 培训: 'primary', 沙龙: 'success', 活动: 'warning', 竞赛: 'info' }
  return map[tag] || 'info'
}

/** 格式化发布时间，精确到分钟 */
function formatDateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function loadList() {
  loading.value = true
  try {
    const [start, end] = filters.value.dateRange || []
    const data = await http.get('/activities', {
      page: page.value - 1,
      size: pageSize,
      keyword: filters.value.keyword || undefined,
      startDate: start || undefined,
      endDate: end || undefined
    })
    list.value = data.content || []
    total.value = data.totalElements || 0
  } catch (err) {
    ElMessage.error(err.message || '加载活动失败')
  } finally {
    loading.value = false
  }
}

/** 提交筛选：回到第一页重新查询 */
function doSearch() {
  page.value = 1
  loadList()
}

/** 清空筛选条件并重新查询 */
function resetSearch() {
  filters.value = { keyword: '', dateRange: null }
  page.value = 1
  loadList()
}

function handlePageChange(p) {
  page.value = p
  loadList()
  // 切换分页后滚动到列表顶部，便于直接查看当前页数据
  // 滚动容器是 el-main（外层页面不滚动），scrollIntoView 会自动找到最近的可滚动祖先
  nextTick(() => {
    const el = listCardRef.value?.$el || listCardRef.value
    el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  })
}

/** 序号跨页连续：第 2 页从 pageSize+1 开始 */
function indexMethod(index) {
  return (page.value - 1) * pageSize + index + 1
}

/** 变更后刷新；若当前页超出范围则回退到最后一页 */
async function refreshAfterMutate() {
  await loadList()
  const maxPage = Math.max(1, Math.ceil(total.value / pageSize))
  if (page.value > maxPage) {
    page.value = maxPage
    await loadList()
  }
}

function resetForm() {
  form.value = { title: '', tag: '', content: '', pinned: false }
}

async function publish() {
  const f = form.value
  if (!f.title.trim() || !f.tag || !f.content.trim()) {
    ElMessage.warning('标题、类别和介绍均不能为空')
    return
  }
  publishing.value = true
  try {
    await http.post('/activities', { ...f })
    ElMessage.success('新增成功')
    resetForm()
    page.value = 1
    await loadList()
  } catch (err) {
    ElMessage.error(err.message || '新增失败')
  } finally {
    publishing.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除活动《${row.title}》？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await http.delete(`/activities/${row.id}`)
    ElMessage.success('删除成功')
    await refreshAfterMutate()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

function openEdit(row) {
  editForm.value = {
    id: row.id,
    title: row.title,
    tag: row.tag,
    content: row.content,
    pinned: !!row.pinned
  }
  editVisible.value = true
}

async function saveEdit() {
  const f = editForm.value
  if (!f.title.trim() || !f.tag || !f.content.trim()) {
    ElMessage.warning('标题、类别和介绍均不能为空')
    return
  }
  saving.value = true
  try {
    await http.put(`/activities/${f.id}`, {
      title: f.title,
      tag: f.tag,
      content: f.content,
      pinned: f.pinned
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    await refreshAfterMutate()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.activity-manage {
  padding: 20px;
}
.form-card {
  margin-bottom: 16px;
}
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.list-header .list-title {
  font-weight: 600;
}
.list-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.act-pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
