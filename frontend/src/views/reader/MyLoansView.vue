<template>
  <div class="my-loans">
    <h1>我的借阅</h1>

    <el-card>
      <el-table :data="loans" v-loading="loading" stripe>
        <el-table-column prop="bookTitle" label="书名" min-width="180" show-overflow-tooltip />
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              size="small"
              type="primary"
              @click="renewLoan(row)"
            >续借</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && loans.length === 0" description="暂无借阅记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'

const loans = ref([])
const loading = ref(false)

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
    const data = await http.get('/loans/my', { page: 0, size: 50 })
    loans.value = data.content
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
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
.my-loans {
  padding: 20px;
}
.overdue {
  color: #f56c6c;
  font-weight: 600;
}
</style>
