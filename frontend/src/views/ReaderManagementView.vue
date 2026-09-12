<template>
  <div class="reader-management">
    <div class="page-header">
      <h1>读者管理</h1>
      <div>
        <el-button type="success" :icon="Upload" @click="importVisible = true">Excel导入</el-button>
        <el-dropdown class="export-dd" @command="handleExport">
          <el-button type="warning" plain :icon="Download" :loading="exporting">
            导出<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="all">全部导出</el-dropdown-item>
              <el-dropdown-item command="page">单页导出</el-dropdown-item>
              <el-dropdown-item command="selected" :disabled="selected.length === 0">
                选中导出（{{ selected.length }}）
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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

      <!-- 批量操作条：勾选读者后出现 -->
      <div v-if="selected.length > 0" class="batch-bar">
        <span class="batch-count">已选 {{ selected.length }} 项</span>
        <el-button size="small" type="primary" plain :icon="Key" @click="batchResetPassword">批量重置密码</el-button>
        <el-button size="small" type="success" plain @click="batchSetStatus(true)">批量启用</el-button>
        <el-button size="small" type="warning" plain @click="batchSetStatus(false)">批量停借</el-button>
        <el-button size="small" text @click="clearSelection">取消选择</el-button>
      </div>

      <el-table
        ref="tableRef"
        :data="readers"
        v-loading="loading"
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
        @select="handleSelect"
        @select-all="handleSelectAll"
      >
        <!-- reserve-selection + row-key：翻页/搜索后保留已勾选行（跨页多选） -->
        <el-table-column type="selection" width="45" :reserve-selection="true" />
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
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
        <el-table-column label="手机号">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'NORMAL' ? 'success' : 'danger'">
              {{ row.status === 'NORMAL' ? '正常' : '停借' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="info" plain @click="resetPassword(row)">重置密码</el-button>
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

      <PageBar
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @change="handlePageChange"
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
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="选填，11 位手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <!-- Excel 批量导入弹窗（与图书编目共用同一组件） -->
    <ExcelImportDialog
      v-model="importVisible"
      template-url="/readers/import/template"
      upload-url="/readers/import"
      :tips="importTips"
      @success="loadReaders"
    />
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, Key, Upload, Download, ArrowDown } from '@element-plus/icons-vue'
import { downloadGet } from '@/utils/download'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'
import ExcelImportDialog from '@/components/ExcelImportDialog.vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { PageResult, Reader, ReaderType } from '@/types'

const readers = ref<Reader[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

/* -------- 多选批量操作 -------- */
const tableRef = ref<{ clearSelection: () => void; toggleRowSelection: (row: Reader, selected?: boolean) => void } | undefined>()
const selected = ref<Reader[]>([])

/* -------- 导出（全部 / 单页 / 选中） -------- */
const exporting = ref(false)

async function handleExport(scope: string) {
  exporting.value = true
  try {
    await downloadGet('/readers/export', {
      scope,
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      status: filters.status === '' ? undefined : filters.status,
      page: scope === 'page' ? currentPage.value - 1 : undefined,
      size: scope === 'page' ? pageSize.value : undefined,
      ids: scope === 'selected' ? selected.value.map((r) => r.id).join(',') : undefined
    }, '读者导出.xlsx')
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(errorMessage(err, '导出失败'))
  } finally {
    exporting.value = false
  }
}

/* -------- Excel 批量导入 -------- */
const importVisible = ref(false)
const importTips = [
  '后缀名为 xls 或者 xlsx',
  '请下载示例文件，按列顺序填写：账号、密码、姓名、类型（学生/教师）、学号/工号、手机号',
  '密码留空时默认为 pass123；请勿使用合并的单元格',
  '文件大小请勿超过 2MB，每次导入请勿超过 10000 行',
  '账号或学号/工号已存在的行将被跳过，并在导入结果中提示'
]

function handleSelectionChange(rows: Reader[]) {
  selected.value = rows
}

function clearSelection() {
  tableRef.value?.clearSelection()
  anchorIndex = -1
}

/* Shift 区间选择：锚点行（当前页内索引）+ 全局 Shift 按键状态 */
let anchorIndex = -1
let shiftPressed = false

function onKeyToggle(e: KeyboardEvent) {
  if (e.key === 'Shift') shiftPressed = e.type === 'keydown'
}

/**
 * 表头全选/取消全选：作用于所有页。
 * - 全选：按当前筛选条件拉取全部读者 ID，把未勾选的都补选上（跨页）；
 * - 取消全选：清空所有页的勾选。
 */
async function handleSelectAll(selection: Reader[]) {
  const allPageSelected = readers.value.length > 0 &&
    readers.value.every((r) => selection.some((s) => s.id === r.id))
  if (!allPageSelected) {
    clearSelection()
    return
  }
  try {
    const ids = await http.get<number[]>('/readers/ids', {
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      status: filters.status === '' ? undefined : filters.status
    })
    for (const id of ids) {
      if (!selected.value.some((s) => s.id === id)) {
        // 不在当前页的行传最小占位对象即可：el-table 按 row-key 记录保留选择
        tableRef.value?.toggleRowSelection({ id } as unknown as Reader, true)
      }
    }
  } catch (err) {
    ElMessage.error(errorMessage(err, '获取读者列表失败'))
  }
}

/** 勾选复选框时：按住 Shift 则把锚点行到当前行整段选中 */
function handleSelect(_selection: Reader[], row: Reader) {
  const index = readers.value.findIndex((r) => r.id === row.id)
  if (shiftPressed && anchorIndex >= 0 && anchorIndex !== index) {
    const [start, end] = anchorIndex < index ? [anchorIndex, index] : [index, anchorIndex]
    for (let i = start; i <= end; i++) {
      const target = readers.value[i]
      if (target) tableRef.value?.toggleRowSelection(target, true)
    }
  }
  anchorIndex = index
}

const filters = reactive<{ keyword: string; type: string; status: string }>({ keyword: '', type: '', status: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const form = reactive<{
  account: string
  password: string
  name: string
  type: ReaderType
  studentNo: string
  phone: string
}>({
  account: '',
  password: '',
  name: '',
  type: 'STUDENT',
  studentNo: '',
  phone: ''
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  studentNo: [{ required: true, message: '请输入学号/工号', trigger: 'blur' }],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号格式不正确（11 位数字）',
      trigger: 'blur'
    }
  ]
}

async function loadReaders() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Reader>>('/readers', {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      status: filters.status === '' ? undefined : filters.status
    })
    readers.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  currentPage.value = 1
  anchorIndex = -1
  loadReaders()
}

function reset() {
  filters.keyword = ''
  filters.type = ''
  filters.status = ''
  search()
}

function handlePageChange(page: number) {
  currentPage.value = page
  anchorIndex = -1
  loadReaders()
}

/** 序号跨页连续：第 2 页从 pageSize+1 开始 */
function indexMethod(index: number) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, { account: '', password: '', name: '', type: 'STUDENT', studentNo: '', phone: '' })
  dialogVisible.value = true
}

function openEdit(row: Reader) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, {
    account: row.account,
    password: '',
    name: row.name,
    type: row.type,
    studentNo: row.studentNo,
    phone: row.phone || ''
  })
  dialogVisible.value = true
}

async function save() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await http.put(`/readers/${editingId.value}`, {
        account: form.account,
        name: form.name,
        type: form.type,
        studentNo: form.studentNo,
        phone: form.phone.trim()
      })
      ElMessage.success('更新成功')
    } else {
      await http.post('/readers', {
        ...form,
        phone: form.phone.trim()
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadReaders()
  } catch (err) {
    ElMessage.error(errorMessage(err, '保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: Reader) {
  const next = row.status === 'NORMAL' ? false : true
  try {
    await http.patch(`/readers/${row.id}/status`, { status: next })
    ElMessage.success(next ? '已恢复正常' : '已停借')
    loadReaders()
  } catch (err) {
    ElMessage.error(errorMessage(err, '操作失败'))
  }
}

async function removeReader(row: Reader) {
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
    ElMessage.error(errorMessage(err, '删除失败'))
  }
}

/** 重置单个读者密码：恢复为初始密码 pass123 */
async function resetPassword(row: Reader) {
  try {
    await ElMessageBox.confirm(
      `确定将「${row.name}（${row.account}）」的密码重置为初始密码吗？`,
      '重置密码',
      { type: 'warning', confirmButtonText: '重置', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await http.post(`/readers/${row.id}/reset-password`)
    ElMessage.success('密码已重置为 pass123')
  } catch (err) {
    ElMessage.error(errorMessage(err, '重置失败'))
  }
}

/** 批量重置所选读者密码 */
async function batchResetPassword() {
  const ids = selected.value.map((r) => r.id)
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${ids.length} 位读者密码全部重置为初始密码吗？`,
      '批量重置密码',
      { type: 'warning', confirmButtonText: '重置', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const data = await http.post<{ count: number }>('/readers/batch/reset-password', { ids })
    ElMessage.success(`已重置 ${data.count} 位读者的密码（初始密码 pass123）`)
    clearSelection()
  } catch (err) {
    ElMessage.error(errorMessage(err, '批量重置失败'))
  }
}

/** 批量启用 / 停借所选读者 */
async function batchSetStatus(next: boolean) {
  const ids = selected.value.map((r) => r.id)
  if (!ids.length) return
  const action = next ? '启用' : '停借'
  try {
    await ElMessageBox.confirm(`确定${action}选中的 ${ids.length} 位读者吗？`, `批量${action}`, {
      type: 'warning',
      confirmButtonText: action,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    const data = await http.patch<{ count: number }>('/readers/batch/status', { ids, status: next })
    ElMessage.success(`已${action} ${data.count} 位读者`)
    loadReaders()
    clearSelection()
  } catch (err) {
    ElMessage.error(errorMessage(err, '批量操作失败'))
  }
}

onMounted(() => {
  loadReaders()
  // Shift 区间选择：全局监听按键状态（复选框点击事件本身拿不到 shiftKey）
  document.addEventListener('keydown', onKeyToggle)
  document.addEventListener('keyup', onKeyToggle)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeyToggle)
  document.removeEventListener('keyup', onKeyToggle)
})
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
/* 批量操作条：勾选读者后显示在表格上方 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
}
.batch-count {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}
</style>
