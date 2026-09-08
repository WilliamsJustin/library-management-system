<template>
  <div class="reader-management">
    <div class="page-header">
      <h1>读者管理</h1>
      <div>
        <el-button type="primary" :icon="Plus" @click="openCreate">添加读者</el-button>
      </div>
    </div>

    <el-card>
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="姓名/账号/学号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width: 140px">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="正常" :value="true" />
            <el-option label="停借" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="readers" v-loading="loading" stripe>
        <el-table-column prop="account" label="账号" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column label="类型">
          <template #default="{ row }">
            <el-tag :type="row.type === 'STUDENT' ? 'info' : 'success'">
              {{ row.type === 'STUDENT' ? '学生' : '教师' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号/工号" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'NORMAL' ? 'success' : 'danger'">
              {{ row.status === 'NORMAL' ? '正常' : '停借' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 'NORMAL' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'NORMAL' ? '停借' : '恢复' }}
            </el-button>
            <el-button size="small" type="danger" @click="removeReader(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑读者' : '添加读者'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号/工号" prop="studentNo">
          <el-input v-model="form.studentNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { http } from '@/api/http'

const readers = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filters = reactive({ keyword: '', type: '', status: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editingId = ref(null)
const form = reactive({
  account: '',
  password: '',
  name: '',
  type: 'STUDENT',
  studentNo: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  studentNo: [{ required: true, message: '请输入学号/工号', trigger: 'blur' }]
}

async function loadReaders() {
  loading.value = true
  try {
    const data = await http.get('/readers', {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      status: filters.status === '' ? undefined : filters.status
    })
    readers.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  currentPage.value = 1
  loadReaders()
}

function reset() {
  filters.keyword = ''
  filters.type = ''
  filters.status = ''
  search()
}

function handlePageChange(page) {
  currentPage.value = page
  loadReaders()
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, { account: '', password: '', name: '', type: 'STUDENT', studentNo: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, {
    account: row.account,
    password: '',
    name: row.name,
    type: row.type,
    studentNo: row.studentNo
  })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await http.put(`/readers/${editingId.value}`, {
        account: form.account,
        name: form.name,
        type: form.type,
        studentNo: form.studentNo
      })
      ElMessage.success('更新成功')
    } else {
      await http.post('/readers', { ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadReaders()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 'NORMAL' ? false : true
  try {
    await http.patch(`/readers/${row.id}/status`, { status: next })
    ElMessage.success(next ? '已恢复正常' : '已停借')
    loadReaders()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

async function removeReader(row) {
  try {
    await ElMessageBox.confirm(`确定删除读者「${row.name}」吗？该操作不可撤销。`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await http.delete(`/readers/${row.id}`)
    ElMessage.success('删除成功')
    loadReaders()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

onMounted(loadReaders)
</script>

<style scoped>
.reader-management {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
