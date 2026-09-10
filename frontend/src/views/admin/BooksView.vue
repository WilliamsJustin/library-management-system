<template>
  <div class="books-view">
    <div class="page-header">
      <h1>图书编目</h1>
      <div>
        <el-button type="success" :icon="Upload" @click="triggerImport">Excel 导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">添加图书</el-button>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="handleImport" />
      </div>
    </div>

    <el-card>
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="ISBN/书名/作者" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="filters.category"
            placeholder="全部分类"
            clearable
            filterable
            style="width: 160px"
          >
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="出版社">
          <el-input
            v-model="filters.publisher"
            placeholder="出版社"
            clearable
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="在架" value="ACTIVE" />
            <el-option label="已下架" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="books" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="isbn" label="ISBN" width="160" />
        <el-table-column prop="title" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
        <el-table-column prop="publisher" label="出版社" width="140" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '在架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="副本" width="110">
          <template #default="{ row }">
            可借 {{ row.availableCopies }} / 共 {{ row.totalCopies }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <div class="op-line">
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
            </div>
            <div class="op-line">
              <el-button
                size="small"
                :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
              </el-button>
              <el-button size="small" type="danger" @click="removeBook(row)">删除</el-button>
            </div>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑图书' : '添加图书'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" />
        </el-form-item>
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="form.publisher" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" placeholder="如：计算机、文学" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉：图书信息 + 副本管理 -->
    <el-drawer v-model="detailVisible" :title="detailBook?.title || '图书详情'" size="560px">
      <template v-if="detailBook">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ISBN">{{ detailBook.isbn }}</el-descriptions-item>
          <el-descriptions-item label="书名">{{ detailBook.title }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ detailBook.author }}</el-descriptions-item>
          <el-descriptions-item label="出版社">{{ detailBook.publisher }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detailBook.category }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ detailBook.status === 'ACTIVE' ? '在架' : '已下架' }}
          </el-descriptions-item>
          <el-descriptions-item label="副本">
            可借 {{ detailBook.availableCopies }} / 共 {{ detailBook.totalCopies }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>副本管理</el-divider>
        <el-form inline @submit.prevent>
          <el-form-item label="条形码" prop="barcode">
            <el-input v-model="copyForm.barcode" placeholder="如 C10001-01" />
          </el-form-item>
          <el-form-item label="位置" prop="location">
            <el-input v-model="copyForm.location" placeholder="如 A区1排" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Plus" :loading="addingCopy" @click="addCopy">添加副本</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="copies" v-loading="copiesLoading" stripe>
          <el-table-column prop="barcode" label="条形码" />
          <el-table-column prop="location" label="位置" />
          <el-table-column label="状态">
            <template #default="{ row }">
              <el-tag :type="copyTagType(row.status)">{{ copyStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, Upload } from '@element-plus/icons-vue'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Book, BookCopy, BookPayload, CopyStatus, PageResult } from '@/types'

const books = ref<Book[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filters = reactive({ keyword: '', category: '', publisher: '', status: '' })

// 分类下拉选项（来自现有图书的去重分类）
const categoryOptions = ref<string[]>([])
async function loadCategoryOptions() {
  try {
    categoryOptions.value = await http.get<string[]>('/books/categories')
  } catch {
    categoryOptions.value = []
  }
}

// 新增/编辑
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const form = reactive<BookPayload>({ isbn: '', title: '', author: '', publisher: '', category: '' })

const rules: FormRules = {
  isbn: [{ required: true, message: '请输入 ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  publisher: [{ required: true, message: '请输入出版社', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }]
}

// 详情抽屉
const detailVisible = ref(false)
const detailBook = ref<Book | null>(null)
const copies = ref<BookCopy[]>([])
const copiesLoading = ref(false)
const addingCopy = ref(false)
const copyForm = reactive({ barcode: '', location: '' })

async function loadBooks() {
  loading.value = true
  try {
    const data = await http.get<PageResult<Book>>('/books', {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
      publisher: filters.publisher || undefined,
      status: filters.status || undefined
    })
    books.value = data.content
    total.value = data.totalElements
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  currentPage.value = 1
  loadBooks()
}

function reset() {
  filters.keyword = ''
  filters.category = ''
  filters.publisher = ''
  filters.status = ''
  search()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadBooks()
}

/** 序号跨页连续：第 2 页从 pageSize+1 开始 */
function indexMethod(index: number) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, { isbn: '', title: '', author: '', publisher: '', category: '' })
  dialogVisible.value = true
}

function openEdit(row: Book) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, {
    isbn: row.isbn,
    title: row.title,
    author: row.author,
    publisher: row.publisher,
    category: row.category
  })
  dialogVisible.value = true
}

async function save() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await http.put(`/books/${editingId.value}`, { ...form })
      ElMessage.success('更新成功')
    } else {
      await http.post('/books', { ...form })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadBooks()
    loadCategoryOptions()
  } catch (err) {
    ElMessage.error(errorMessage(err, '保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: Book) {
  const next = row.status !== 'ACTIVE'
  const action = next ? '上架' : '下架'
  try {
    await http.patch(`/books/${row.id}/status`, { status: next })
    ElMessage.success(`已${action}`)
    loadBooks()
  } catch (err) {
    ElMessage.error(errorMessage(err, '操作失败'))
  }
}

async function removeBook(row: Book) {
  try {
    await ElMessageBox.confirm(`确定删除图书「${row.title}」吗？若有副本在借将无法删除。`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await http.delete(`/books/${row.id}`)
    ElMessage.success('删除成功')
    loadBooks()
  } catch (err) {
    ElMessage.error(errorMessage(err, '删除失败'))
  }
}

async function openDetail(row: Book) {
  detailBook.value = row
  detailVisible.value = true
  copyForm.barcode = ''
  copyForm.location = ''
  await loadCopies(row.id)
}

async function loadCopies(bookId: number) {
  copiesLoading.value = true
  try {
    copies.value = await http.get<BookCopy[]>(`/books/${bookId}/copies`)
  } catch (err) {
    ElMessage.error(errorMessage(err, '加载副本失败'))
  } finally {
    copiesLoading.value = false
  }
}

function copyStatusText(status: CopyStatus): string {
  return status === 'IN_STOCK' ? '在库' : status === 'BORROWED' ? '已借出' : '已下架'
}

function copyTagType(status: CopyStatus): 'success' | 'warning' | 'info' {
  return status === 'IN_STOCK' ? 'success' : status === 'BORROWED' ? 'warning' : 'info'
}

async function addCopy() {
  if (!copyForm.barcode || !copyForm.location) {
    ElMessage.warning('请填写条形码与位置')
    return
  }
  if (!detailBook.value) return
  addingCopy.value = true
  try {
    await http.post(`/books/${detailBook.value.id}/copies`, { ...copyForm })
    ElMessage.success('副本已添加')
    copyForm.barcode = ''
    copyForm.location = ''
    await loadCopies(detailBook.value.id)
    loadBooks()
  } catch (err) {
    ElMessage.error(errorMessage(err, '添加失败'))
  } finally {
    addingCopy.value = false
  }
}

// Excel 导入
const fileInput = ref<HTMLInputElement | null>(null)
function triggerImport() {
  fileInput.value?.click()
}
async function handleImport(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    ElMessage.info('正在导入，请稍候…')
    await http.upload('/books/import', file)
    ElMessage.success('导入完成')
    loadBooks()
    loadCategoryOptions()
  } catch (err) {
    ElMessage.error(errorMessage(err, '导入失败'))
  } finally {
    input.value = ''
  }
}

onMounted(() => {
  loadBooks()
  loadCategoryOptions()
})
</script>

<style scoped>
.books-view {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
/* 操作列两行：每行两个按钮 */
.op-line {
  display: flex;
  gap: 8px;
}
.op-line + .op-line {
  margin-top: 8px;
}
/* 抵消 el-button 相邻默认左外边距，避免与 gap 叠加 */
.op-line :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
