<template>
  <div class="announce-manage">
    <h1>公告管理</h1>

    <el-card class="form-card">
      <template #header>发布新公告</template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="公告正文"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="publishing" @click="publish">发布公告</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <template #header>现有公告</template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.pinned" type="warning" size="small">置顶</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无公告" />
      <div v-if="total > pageSize" class="ann-pager">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑公告" width="560px" append-to-body>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" maxlength="120" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="公告正文"
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/http'

const form = ref({ title: '', content: '', pinned: false })
const publishing = ref(false)
const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 5
const total = ref(0)

const editVisible = ref(false)
const saving = ref(false)
const editForm = ref({ id: null, title: '', content: '', pinned: false })

function formatDateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function loadList() {
  loading.value = true
  try {
    const data = await http.get('/announcements', { page: page.value - 1, size: pageSize })
    list.value = data.content || []
    total.value = data.totalElements || 0
  } catch (err) {
    ElMessage.error(err.message || '加载公告失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(p) {
  page.value = p
  loadList()
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
  form.value = { title: '', content: '', pinned: false }
}

async function publish() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  publishing.value = true
  try {
    await http.post('/announcements', { ...form.value })
    ElMessage.success('发布成功')
    resetForm()
    page.value = 1
    await loadList()
  } catch (err) {
    ElMessage.error(err.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除公告《${row.title}》？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await http.delete(`/announcements/${row.id}`)
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
    content: row.content,
    pinned: !!row.pinned
  }
  editVisible.value = true
}

async function saveEdit() {
  if (!editForm.value.title.trim() || !editForm.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  saving.value = true
  try {
    await http.put(`/announcements/${editForm.value.id}`, {
      title: editForm.value.title,
      content: editForm.value.content,
      pinned: editForm.value.pinned
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
.announce-manage {
  padding: 20px;
}
.form-card {
  margin-bottom: 16px;
}
.ann-pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
