<template>
  <div class="penalties-view">
    <h1>逾期罚款</h1>

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
            <el-option label="未缴" value="UNPAID" />
            <el-option label="已缴" value="PAID" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 桌面：保留原表格；手机：摘要卡片 + 展开详情（design.md D5） -->
      <el-table v-if="!isMobile" :data="penalties" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" :index="rowIndex" />
        <el-table-column prop="readerAccount" label="账号" width="120" />
        <el-table-column label="学号" width="120">
          <template #default="{ row }">{{ row.readerNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="readerName" label="读者" width="100" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.readerType" :type="row.readerType === 'TEACHER' ? 'success' : 'info'">
              {{ row.readerType === 'TEACHER' ? '教师' : '学生' }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="isbn" label="ISBN" width="140" show-overflow-tooltip />
        <el-table-column prop="bookTitle" label="图书" min-width="160" show-overflow-tooltip />
        <el-table-column prop="barcode" label="条形码" width="130" />
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
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="缴费时间" width="170">
          <template #default="{ row }">{{ row.paidAt ? formatDate(row.paidAt) : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'UNPAID'"
              size="small"
              type="primary"
              @click="pay(row)"
            >标记已缴</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 手机端卡片列表：骨架藏进 MobileCardList -->
      <MobileCardList
        v-else
        :rows="penalties"
        :row-key="(r: Penalty) => r.id"
        :loading="loading"
        empty-text="暂无罚款记录"
      >
        <template #head="{ row }">
          <span class="m-card-title">{{ row.bookTitle }}</span>
          <el-tag :type="row.status === 'UNPAID' ? 'danger' : 'success'" size="small">
            {{ row.status === 'UNPAID' ? '未缴' : '已缴' }}
          </el-tag>
        </template>
        <template #sub="{ index }">#{{ rowIndex(index) }} · {{ penalties[index].readerName }}（{{ penalties[index].readerAccount }}）</template>
        <template #body="{ row }">
          <div class="m-field">
            <span class="m-field-label">金额(元)</span>
            <span class="m-field-value">{{ formatAmount(row.amount) }}</span>
          </div>
          <div class="m-field">
            <span class="m-field-label">ISBN</span>
            <span class="m-field-value">{{ row.isbn }}</span>
          </div>
          <div class="m-field m-field--full">
            <span class="m-field-label">生成时间</span>
            <span class="m-field-value">{{ formatDate(row.createdAt) }}</span>
          </div>
        </template>
        <template #detail="{ row }">
          <div class="m-field">
            <span class="m-field-label">类型</span>
            <span class="m-field-value">{{ row.readerType === 'TEACHER' ? '教师' : row.readerType === 'STUDENT' ? '学生' : '—' }}</span>
          </div>
          <div class="m-field">
            <span class="m-field-label">学号</span>
            <span class="m-field-value">{{ row.readerNo || '—' }}</span>
          </div>
          <div class="m-field m-field--full">
            <span class="m-field-label">条形码</span>
            <span class="m-field-value">{{ row.barcode }}</span>
          </div>
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
            @click="pay(row)"
          >标记已缴</el-button>
        </template>
      </MobileCardList>

      <PageBar
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
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import { useBreakpoint } from '@/composables/useBreakpoint'
import MobileCardList from '@/components/MobileCardList.vue'
import { formatAmount } from '@/utils/listDisplay'
import PageBar from '@/components/PageBar.vue'
import type { PageResult, Penalty } from '@/types'
import { formatDate } from '@/utils/dateUtils'
import { pageIndexAscending } from '@/utils/listDisplay'

const { isMobile } = useBreakpoint()

const penalties = ref<Penalty[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const filters = reactive<{ keyword: string; readerType: string; status: string }>({
  keyword: '',
  readerType: '',
  status: ''
})


/** 序号跨页连续：第 2 页从 pageSize+1 开始 */
function rowIndex(index: number): number {
  return pageIndexAscending(currentPage.value, pageSize.value, index)
}

async function loadPenalties() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Penalty>>('/penalties', {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      readerType: filters.readerType || undefined,
      status: filters.status || undefined
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
  filters.readerType = ''
  filters.status = ''
  search()
}
function handlePageChange(page: number) {
  currentPage.value = page
  loadPenalties()
}

async function pay(row: Penalty) {
  try {
    await http.post(`/penalties/${row.id}/pay`)
    ElMessage.success('已标记缴费，读者借阅资格已恢复')
    loadPenalties()
  } catch (err) {
    ElMessage.error(errorMessage(err, '操作失败'))
  }
}

onMounted(loadPenalties)
</script>

<style scoped>
.penalties-view {
  padding: 20px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
/* ===== 手机端（<=768px） ===== */
@media (max-width: 768px) {
  .penalties-view {
    padding: 12px;
  }
  .penalties-view h1 {
    font-size: 20px;
  }
}
</style>
