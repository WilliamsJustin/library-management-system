<template>
  <div class="penalty-view">
    <h1>我的罚款</h1>

    <!-- 概览卡片：未缴总额 + 未缴笔数 -->
    <el-row :gutter="16" class="summary">
      <el-col :span="12">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">未缴总额（元）</div>
          <div class="summary-value danger">{{ formatAmount(unpaidTotal) }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">未缴笔数</div>
          <div class="summary-value">{{ unpaidCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="penalties" v-loading="loading" stripe>
        <el-table-column prop="id" label="罚款ID" width="90" />
        <el-table-column prop="bookTitle" label="图书" min-width="180" show-overflow-tooltip />
        <el-table-column prop="barcode" label="条形码" width="140" />
        <el-table-column label="金额(元)" width="110">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'UNPAID' ? 'danger' : 'success'">
              {{ row.status === 'UNPAID' ? '未缴' : '已缴' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="缴费时间" width="170">
          <template #default="{ row }">{{ row.paidAt ? formatDateTime(row.paidAt) : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'UNPAID'"
              size="small"
              type="primary"
              :loading="payingId === row.id"
              @click="pay(row)"
            >缴纳</el-button>
            <span v-else class="paid-text">已缴清</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && penalties.length === 0" description="暂无罚款记录，继续保持～" />

      <el-pagination
        v-if="total > 0"
        class="pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'
import type { PageResult, Penalty } from '@/types'

const penalties = ref<Penalty[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const payingId = ref<number | null>(null)

// 概览：仅统计当前页加载到的未缴记录（读者自身罚款量通常很少，单页足够）
const unpaidTotal = computed(() =>
  penalties.value
    .filter((p) => p.status === 'UNPAID')
    .reduce((sum, p) => sum + Number(p.amount || 0), 0)
)
const unpaidCount = computed(() =>
  penalties.value.filter((p) => p.status === 'UNPAID').length
)

function formatAmount(value: number | string | null | undefined) {
  return value != null ? Number(value).toFixed(2) : '0.00'
}
function formatDateTime(value?: string | null) {
  if (!value) return ''
  const d = new Date(value)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function loadPenalties() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Penalty>>('/penalties/my', {
      page: currentPage.value - 1,
      size: pageSize.value
    })
    penalties.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载失败'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadPenalties()
}

async function pay(row: Penalty) {
  try {
    payingId.value = row.id
    await http.post(`/penalties/${row.id}/pay`)
    ElMessage.success('缴纳成功，借阅资格已恢复（如全部缴清）')
    loadPenalties()
  } catch (err) {
    ElMessage.error(errorMessage(err, '缴纳失败'))
  } finally {
    payingId.value = null
  }
}

onMounted(loadPenalties)
</script>

<style scoped>
.penalty-view {
  padding: 20px;
}
.summary {
  margin-bottom: 16px;
}
.summary-card {
  border-radius: 8px;
}
.summary-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.summary-value.danger {
  color: #f56c6c;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.paid-text {
  color: #67c23a;
  font-size: 13px;
}
</style>
