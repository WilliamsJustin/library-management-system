<template>
  <div class="loans-view">
    <div class="page-header">
      <h1>借阅流通</h1>
      <el-button type="primary" :icon="Plus" @click="openBorrow">借出登记</el-button>
    </div>

    <el-card>
      <el-form inline @submit.prevent>
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
        <el-table-column prop="id" label="借阅ID" width="90" />
        <el-table-column prop="readerName" label="读者" width="100" />
        <el-table-column prop="bookTitle" label="书名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="barcode" label="条形码" width="130" />
        <el-table-column label="借出时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.borrowedAt) }}</template>
        </el-table-column>
        <el-table-column label="应还日期" width="120">
          <template #default="{ row }">
            <span :class="{ overdue: isOverdue(row) }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归还时间" width="170">
          <template #default="{ row }">{{ row.returnedAt ? formatDateTime(row.returnedAt) : '—' }}</template>
        </el-table-column>
        <el-table-column prop="renewedCount" label="续借" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="loanTagType(row.status)">{{ loanStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
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

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
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

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { http } from '@/api/http'

const loans = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const filters = reactive({ status: '' })

const borrowVisible = ref(false)
const borrowing = ref(false)
const borrowRef = ref(null)
const borrowForm = reactive({ readerAccount: '', barcode: '' })
const borrowRules = {
  readerAccount: [{ required: true, message: '请输入读者账号', trigger: 'blur' }],
  barcode: [{ required: true, message: '请输入副本条形码', trigger: 'blur' }]
}

function formatDateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function isOverdue(row) {
  return row.status === 'OVERDUE' ||
    (row.status === 'ACTIVE' && row.dueDate && new Date(row.dueDate) < new Date())
}

function loanStatusText(status) {
  return status === 'ACTIVE' ? '在借' : status === 'RETURNED' ? '已归还' : '逾期'
}
function loanTagType(status) {
  return status === 'ACTIVE' ? 'success' : status === 'OVERDUE' ? 'danger' : 'info'
}

async function loadLoans() {
  loading.value = true
  try {
    const data = await http.get('/loans', {
      page: currentPage.value - 1,
      size: pageSize.value,
      status: filters.status || undefined
    })
    loans.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  currentPage.value = 1
  loadLoans()
}
function reset() {
  filters.status = ''
  search()
}
function handlePageChange(page) {
  currentPage.value = page
  loadLoans()
}

function openBorrow() {
  borrowForm.readerAccount = ''
  borrowForm.barcode = ''
  borrowVisible.value = true
}

async function submitBorrow() {
  await borrowRef.value.validate()
  borrowing.value = true
  try {
    await http.post('/loans', { ...borrowForm })
    ElMessage.success('借出成功')
    borrowVisible.value = false
    loadLoans()
  } catch (err) {
    ElMessage.error(err.message || '借出失败')
  } finally {
    borrowing.value = false
  }
}

async function returnLoan(row) {
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
    ElMessage.error(err.message || '归还失败')
  }
}

async function renewLoan(row) {
  try {
    await http.post(`/loans/${row.id}/renew`)
    ElMessage.success('续借成功')
    loadLoans()
  } catch (err) {
    ElMessage.error(err.message || '续借失败')
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
