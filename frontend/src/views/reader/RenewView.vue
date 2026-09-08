<template>
  <div class="renew-view">
    <h1>续借</h1>
    <el-alert
      type="info"
      :closable="false"
      title="每本图书在借期内最多可续借 1 次，续借后借期顺延一个标准借期；逾期未归还无法续借。"
      style="margin-bottom: 16px"
    />

    <el-card>
      <el-table :data="activeLoans" v-loading="loading" stripe>
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
        <el-table-column label="可续借" width="90">
          <template #default="{ row }">
            <el-tag :type="canRenew(row) ? 'success' : 'info'">
              {{ canRenew(row) ? '可续借' : '不可续借' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :disabled="!canRenew(row)"
              @click="renewLoan(row)"
            >续借</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && activeLoans.length === 0" description="当前没有可续借的图书" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'

const activeLoans = ref([])
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
function canRenew(row) {
  return row.status === 'ACTIVE' && !isOverdue(row) && row.renewedCount < 1
}

async function loadLoans() {
  loading.value = true
  try {
    const data = await http.get('/loans/my', { page: 0, size: 50 })
    activeLoans.value = data.content.filter((l) => l.status === 'ACTIVE' || l.status === 'OVERDUE')
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
.renew-view {
  padding: 20px;
}
.overdue {
  color: #f56c6c;
  font-weight: 600;
}
</style>
