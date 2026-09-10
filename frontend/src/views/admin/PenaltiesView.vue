<template>
  <div class="penalties-view">
    <h1>逾期罚款</h1>

    <el-card>
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 160px">
            <el-option label="未缴" value="UNPAID" />
            <el-option label="已缴" value="PAID" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="penalties" v-loading="loading" stripe>
        <el-table-column prop="id" label="罚款ID" width="90" />
        <el-table-column prop="readerName" label="读者" width="100" />
        <el-table-column prop="bookTitle" label="图书" min-width="160" show-overflow-tooltip />
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
import { Search } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'
import type { PageResult, Penalty } from '@/types'

const penalties = ref<Penalty[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const filters = reactive<{ status: string }>({ status: '' })

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
    const data = await http.get<PageResult<Penalty>>('/penalties', {
      page: currentPage.value - 1,
      size: pageSize.value,
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
</style>
