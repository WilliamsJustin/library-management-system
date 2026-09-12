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
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="ISBN / 图书 / 条形码"
            clearable
            style="width: 220px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="未缴" value="UNPAID" />
            <el-option label="已缴" value="PAID" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <div class="date-combo">
            <el-select v-model="filters.dateField" class="combo-field" @change="search">
              <el-option label="生成时间" value="CREATED" />
              <el-option label="缴费时间" value="PAID" />
            </el-select>
            <span class="combo-divider" />
            <el-date-picker
              v-model="filters.dateRange"
              class="combo-picker"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="search"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="penalties" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" :index="rowIndex" />
        <el-table-column prop="isbn" label="ISBN" width="140" show-overflow-tooltip />
        <el-table-column prop="bookTitle" label="图书" min-width="180" show-overflow-tooltip />
        <el-table-column prop="barcode" label="条形码" width="140" />
        <el-table-column width="130">
          <template #header>
            <span class="amount-head">
              金额(元)
              <el-tooltip
                placement="top"
                effect="dark"
                content="罚款金额 = 0.10 元/分钟 × 逾期分钟数；单本图书累计上限 144 元（逾期满 24 小时即封顶，不再累加）。"
              >
                <el-icon class="tip-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import type { PageResult, Penalty } from '@/types'

const penalties = ref<Penalty[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const payingId = ref<number | null>(null)
const filters = reactive<{
  keyword: string
  status: string
  dateField: 'CREATED' | 'PAID'
  dateRange: [string, string] | null
}>({ keyword: '', status: '', dateField: 'CREATED', dateRange: null })

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

/** 序号跨页连续：第 2 页从 pageSize+1 开始 */
function rowIndex(index: number): number {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

async function loadPenalties() {
  loading.value = true
  try {
    const [startDate, endDate] = filters.dateRange || []
    const data = await http.get<PageResult<Penalty>>('/penalties/my', {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      dateField: filters.dateField,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    penalties.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  currentPage.value = 1
  loadPenalties()
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.dateField = 'CREATED'
  filters.dateRange = null
  search()
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
/* 金额列头的规则提示图标 */
.amount-head {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.tip-icon {
  color: #a9aeb8;
  cursor: help;
}
.tip-icon:hover {
  color: #409eff;
}
/* 时间字段下拉 + 日历范围选择合成一个盒子：外层统一描边，内部控件去自身边框 */
.date-combo {
  display: flex;
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: border-color 0.2s;
}
.date-combo:focus-within {
  border-color: #409eff;
}
.date-combo :deep(.el-select .el-select__wrapper),
.date-combo :deep(.el-input .el-input__wrapper) {
  box-shadow: none !important;
  background: transparent;
}
.combo-field {
  width: 112px;
  flex: 0 0 112px;
}
.combo-divider {
  width: 1px;
  height: 20px;
  background: #dcdfe6;
  flex: 0 0 1px;
}
.combo-picker {
  width: 250px;
  flex: 0 0 250px;
}
.paid-text {
  color: #67c23a;
  font-size: 13px;
}
</style>
