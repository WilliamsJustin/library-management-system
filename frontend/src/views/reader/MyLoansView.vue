<template>
  <div class="my-loans">
    <h1>我的借阅</h1>

    <el-card>
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="ISBN / 书名 / 条形码"
            clearable
            style="width: 220px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="在借" value="ACTIVE" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="逾期" value="OVERDUE" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <DateRangeCombo
            v-model:field="filters.dateField"
            v-model:range="filters.dateRange"
            :fields="[
              { label: '借出时间', value: 'BORROWED' },
              { label: '应还时间', value: 'DUE' },
              { label: '归还时间', value: 'RETURNED' }
            ]"
            @change="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 桌面：保留原表格（一行不改）；手机：摘要卡片列表（design.md D5） -->
      <el-table v-if="!isMobile" :data="loans" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="isbn" label="ISBN" width="140" show-overflow-tooltip />
        <el-table-column prop="bookTitle" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="barcode" label="条形码" width="130" />
        <el-table-column label="借出时间" width="170">
          <template #default="{ row }">{{ formatDate(row.borrowedAt) }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="170">
          <template #default="{ row }">
            <span :class="{ overdue: isLoanOverdue(row) }">{{ formatDate(row.dueDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归还时间" width="170">
          <template #default="{ row }">{{ row.returnedAt ? formatDate(row.returnedAt) : '—' }}</template>
        </el-table-column>
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
              @click="returnLoan(row)"
            >还书</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              size="small"
              type="primary"
              @click="renewLoan(row)"
            >续借</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 手机端卡片列表：骨架藏进 MobileCardList -->
      <MobileCardList
        v-else
        :rows="loans"
        :row-key="(r: Loan) => r.id"
        :loading="loading"
        empty-text="暂无借阅记录"
        :foot-visible="(r) => r.status === 'ACTIVE' || r.status === 'OVERDUE'"
      >
        <template #head="{ row }">
          <span class="m-card-title">{{ row.bookTitle }}</span>
          <el-tag :type="loanTagType(row.status)" size="small">{{ loanStatusText(row.status) }}</el-tag>
        </template>
        <template #sub="{ index }">#{{ pageIndexAscending(currentPage, pageSize, index) }} · ISBN {{ loans[index].isbn }}</template>
        <template #body="{ row }">
          <div class="m-field m-field--full">
            <span class="m-field-label">条形码</span>
            <span class="m-field-value">{{ row.barcode }}</span>
          </div>
          <div class="m-field m-field--full">
            <span class="m-field-label">借出</span>
            <span class="m-field-value">{{ formatDate(row.borrowedAt) }}</span>
          </div>
          <div class="m-field m-field--full">
            <span class="m-field-label">应还</span>
            <span class="m-field-value" :class="{ overdue: isLoanOverdue(row) }">{{ formatDate(row.dueDate) }}</span>
          </div>
          <div class="m-field m-field--full">
            <span class="m-field-label">归还</span>
            <span class="m-field-value">{{ row.returnedAt ? formatDate(row.returnedAt) : '—' }}</span>
          </div>
        </template>
        <template #foot="{ row }">
          <el-button size="small" @click="returnLoan(row)">还书</el-button>
          <el-button v-if="row.status === 'ACTIVE'" size="small" type="primary" @click="renewLoan(row)">续借</el-button>
        </template>
      </MobileCardList>

      <PageBar
        v-if="total > pageSize"
        background
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/http'
import { useBreakpoint } from '@/composables/useBreakpoint'
import PageBar from '@/components/PageBar.vue'
import type { Loan, LoanStatus, PageResult } from '@/types'
import { formatDate } from '@/utils/dateUtils'
import MobileCardList from '@/components/MobileCardList.vue'
import DateRangeCombo from '@/components/DateRangeCombo.vue'
import { isLoanOverdue, loanStatusText, loanTagType, pageIndexAscending } from '@/utils/listDisplay'

const { isMobile } = useBreakpoint()

const loans = ref<Loan[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const loading = ref(false)
const filters = reactive<{
  keyword: string
  status: string
  dateField: 'BORROWED' | 'DUE' | 'RETURNED'
  dateRange: [string, string] | null
}>({ keyword: '', status: '', dateField: 'BORROWED', dateRange: null })

async function loadLoans() {
  loading.value = true
  try {
    const [startDate, endDate] = filters.dateRange || []
    const data = await http.get<PageResult<Loan>>('/loans/my', {
      page: currentPage.value - 1,
      size: pageSize,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      dateField: filters.dateField,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    loans.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载失败'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadLoans()
}

function search() {
  currentPage.value = 1
  loadLoans()
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.dateField = 'BORROWED'
  filters.dateRange = null
  loadLoans()
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

async function returnLoan(row: Loan) {
  try {
    await http.post(`/loans/${row.id}/self-return`)
    ElMessage.success('还书成功')
    loadLoans()
  } catch (err) {
    ElMessage.error(errorMessage(err, '还书失败'))
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
/* ===== 手机端（<=768px）：内边距收缩 ===== */
@media (max-width: 768px) {
  .my-loans {
    padding: 12px;
  }
  .my-loans h1 {
    font-size: 20px;
  }
}
</style>
