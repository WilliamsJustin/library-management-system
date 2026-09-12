<template>
  <div class="loans-view">
    <div class="page-header">
      <h1>借阅流通</h1>
      <el-button type="primary" :icon="Plus" @click="openBorrow">借出登记</el-button>
    </div>

    <el-card>
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="账号 / 学号 / 姓名 / ISBN / 书名"
            clearable
            style="width: 240px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.readerType" placeholder="全部" clearable style="width: 140px">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 160px">
            <el-option label="在借" value="ACTIVE" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="逾期" value="OVERDUE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="loans" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" :index="rowIndex" />
        <el-table-column prop="readerAccount" label="账号" width="120" />
        <el-table-column label="学号" width="120">
          <template #default="{ row }">{{ row.readerNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="readerName" label="读者" width="100" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.readerType" :type="readerTypeTag(row.readerType)">
              {{ readerTypeText(row.readerType) }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="isbn" label="ISBN" width="140" show-overflow-tooltip />
        <el-table-column prop="bookTitle" label="书名" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="loanTagType(row.status)">{{ loanStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="借出时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.borrowedAt) }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="170">
          <template #default="{ row }">
            <span :class="{ overdue: isOverdue(row) }">{{ formatDateTime(row.dueDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归还时间" width="170">
          <template #default="{ row }">{{ row.returnedAt ? formatDateTime(row.returnedAt) : '—' }}</template>
        </el-table-column>
        <el-table-column prop="barcode" label="条形码" width="130" />
        <el-table-column prop="renewedCount" label="续借" width="70" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE' || row.status === 'OVERDUE'"
              size="small"
              type="success"
              @click="returnLoan(row)"
            >归还</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              size="small"
              @click="renewLoan(row)"
            >续借</el-button>
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

    <el-dialog v-model="borrowVisible" title="借出登记" width="460px">
      <el-form ref="borrowRef" :model="borrowForm" :rules="borrowRules" label-width="90px">
        <el-form-item label="读者账号" prop="readerAccount">
          <el-input v-model="borrowForm.readerAccount" placeholder="如 student1" />
        </el-form-item>
        <el-form-item label="副本条码" prop="barcode">
          <el-input v-model="borrowForm.barcode" placeholder="如 C1-001" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowVisible = false">取消</el-button>
        <el-button type="primary" :loading="borrowing" @click="submitBorrow">确认借出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Loan, LoanStatus, PageResult, ReaderType } from '@/types'

const loans = ref<Loan[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const filters = reactive<{ keyword: string; status: string; readerType: string }>({
  keyword: '',
  status: '',
  readerType: ''
})

const borrowVisible = ref(false)
const borrowing = ref(false)
const borrowRef = ref<FormInstance>()
const borrowForm = reactive({ readerAccount: '', barcode: '' })
const borrowRules: FormRules = {
  readerAccount: [{ required: true, message: '请输入读者账号', trigger: 'blur' }],
  barcode: [{ required: true, message: '请输入副本条形码', trigger: 'blur' }]
}

function formatDateTime(value?: string | null) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 序号倒序且跨页连续：第 1 行显示总数，依次递减 */
function rowIndex(index: number): number {
  return total.value - (currentPage.value - 1) * pageSize.value - index
}

function isOverdue(row: Loan): boolean {
  return row.status === 'OVERDUE' ||
    (row.status === 'ACTIVE' && !!row.dueDate && new Date(row.dueDate) < new Date())
}

/** 读者类型文案 / 标签色（与「读者管理」页保持一致：学生 info、教师 success） */
function readerTypeText(type: ReaderType): string {
  return type === 'TEACHER' ? '教师' : '学生'
}
function readerTypeTag(type: ReaderType): 'success' | 'info' {
  return type === 'TEACHER' ? 'success' : 'info'
}

function loanStatusText(status: LoanStatus): string {
  return status === 'ACTIVE' ? '在借' : status === 'RETURNED' ? '已归还' : '逾期'
}
function loanTagType(status: LoanStatus): 'success' | 'danger' | 'info' {
  return status === 'ACTIVE' ? 'success' : status === 'OVERDUE' ? 'danger' : 'info'
}

async function loadLoans() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Loan>>('/loans', {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      readerType: filters.readerType || undefined
    })
    loans.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  currentPage.value = 1
  loadLoans()
}
function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.readerType = ''
  search()
}
function handlePageChange(page: number) {
  currentPage.value = page
  loadLoans()
}

function openBorrow() {
  borrowForm.readerAccount = ''
  borrowForm.barcode = ''
  borrowVisible.value = true
}

async function submitBorrow() {
  if (!borrowRef.value) return
  await borrowRef.value.validate()
  borrowing.value = true
  try {
    await http.post('/loans', { ...borrowForm })
    ElMessage.success('借出成功')
    borrowVisible.value = false
    loadLoans()
  } catch (err) {
    ElMessage.error(errorMessage(err, '借出失败'))
  } finally {
    borrowing.value = false
  }
}

async function returnLoan(row: Loan) {
  try {
    await ElMessageBox.confirm(`确认归还《${row.bookTitle}》？`, '归还确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await http.post(`/loans/${row.id}/return`)
    ElMessage.success('归还成功')
    loadLoans()
  } catch (err) {
    ElMessage.error(errorMessage(err, '归还失败'))
  }
}

async function renewLoan(row: Loan) {
  try {
    await http.post(`/loans/${row.id}/renew`)
    ElMessage.success('续借成功')
    loadLoans()
  } catch (err) {
    ElMessage.error(errorMessage(err, '续借失败'))
  }
}

onMounted(loadLoans)
</script>

<style scoped>
.loans-view {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.overdue {
  color: #f56c6c;
  font-weight: 600;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
