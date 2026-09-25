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
          <DateRangeCombo
            v-model:field="filters.dateField"
            v-model:range="filters.dateRange"
            :fields="[
              { label: '生成时间', value: 'CREATED' },
              { label: '缴费时间', value: 'PAID' }
            ]"
            @change="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 桌面：保留原表格（一行不改）；手机：摘要卡片 + 展开详情（design.md D5） -->
      <el-table v-if="!isMobile" :data="penalties" v-loading="loading" stripe>
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
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="缴费时间" width="170">
          <template #default="{ row }">{{ row.paidAt ? formatDate(row.paidAt) : '—' }}</template>
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

      <!-- 手机端卡片列表：骨架藏进 MobileCardList -->
      <MobileCardList
        v-else
        :rows="penalties"
        :row-key="(r: Penalty) => r.id"
        :loading="loading"
        empty-text="暂无罚款记录，继续保持～"
      >
        <template #head="{ row }">
          <span class="m-card-title">{{ row.bookTitle }}</span>
          <el-tag :type="row.status === 'UNPAID' ? 'danger' : 'success'" size="small">
            {{ row.status === 'UNPAID' ? '未缴' : '已缴' }}
          </el-tag>
        </template>
        <template #sub="{ index }">#{{ rowIndex(index) }} · ISBN {{ penalties[index].isbn }}</template>
        <template #body="{ row }">
          <div class="m-field">
            <span class="m-field-label">金额(元)</span>
            <span class="m-field-value">
              {{ formatAmount(row.amount) }}
              <el-tooltip
                placement="top"
                effect="dark"
                content="罚款金额 = 0.10 元/分钟 × 逾期分钟数；单本图书累计上限 144 元（逾期满 24 小时即封顶，不再累加）。"
              >
                <el-icon class="tip-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </div>
          <div class="m-field">
            <span class="m-field-label">条形码</span>
            <span class="m-field-value">{{ row.barcode }}</span>
          </div>
          <div class="m-field m-field--full">
            <span class="m-field-label">生成时间</span>
            <span class="m-field-value">{{ formatDate(row.createdAt) }}</span>
          </div>
        </template>
        <template #detail="{ row }">
          <div class="m-field m-field--full">
            <span class="m-field-label">缴费时间</span>
            <span class="m-field-value">{{ row.paidAt ? formatDate(row.paidAt) : '—' }}</span>
          </div>
        </template>
        <template #foot="{ row }">
          <el-button
            v-if="row.status === 'UNPAID'"
            size="small"
            type="primary"
            :loading="payingId === row.id"
            @click="pay(row)"
          >缴纳</el-button>
          <span v-else class="paid-text">已缴清</span>
        </template>
      </MobileCardList>

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
import { useBreakpoint } from '@/composables/useBreakpoint'
import MobileCardList from '@/components/MobileCardList.vue'
import DateRangeCombo from '@/components/DateRangeCombo.vue'
import { formatAmount } from '@/utils/listDisplay'
import type { PageResult, Penalty } from '@/types'
import { formatDate } from '@/utils/dateUtils'
import { pageIndexAscending } from '@/utils/listDisplay'

const { isMobile } = useBreakpoint()

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


/** 序号跨页连续：第 2 页从 pageSize+1 开始 */
function rowIndex(index: number): number {
  return pageIndexAscending(currentPage.value, pageSize.value, index)
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
  color: var(--el-text-color-placeholder);
  cursor: help;
}
.tip-icon:hover {
  color: var(--sl-primary-strong);
}
.paid-text {
  color: #67c23a;
  font-size: 13px;
}
/* ===== 手机端（<=768px）：概览纵排、内边距收缩 ===== */
@media (max-width: 768px) {
  .penalty-view {
    padding: 12px;
  }
  .penalty-view h1 {
    font-size: 20px;
  }
}
</style>
