<template>
  <div class="books-view">
    <div class="page-header">
      <h1>图书编目</h1>
      <div>
        <el-button type="success" :icon="Upload" @click="importVisible = true">Excel 导入</el-button>
        <el-dropdown class="export-dd" @command="handleExport">
          <el-button type="warning" plain :icon="Download" :loading="exporting">
            导出<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="all">全部导出</el-dropdown-item>
              <el-dropdown-item command="page">单页导出</el-dropdown-item>
              <el-dropdown-item command="selected" :disabled="selected.length === 0">
                选中导出（{{ selected.length }}）
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" :icon="Plus" @click="openCreate">添加图书</el-button>
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

      <!-- 批量操作条：勾选图书后出现 -->
      <div v-if="selected.length > 0" class="batch-bar">
        <span class="batch-count">已选 {{ selected.length }} 项</span>
        <el-button size="small" type="success" plain @click="batchSetStatus('ACTIVE')">批量上架</el-button>
        <el-button size="small" type="warning" plain @click="batchSetStatus('INACTIVE')">批量下架</el-button>
        <el-button size="small" type="danger" plain @click="batchDelete">批量删除</el-button>
        <el-button size="small" text @click="clearSelection">取消选择</el-button>
      </div>

      <el-table
        ref="tableRef"
        :data="books"
        v-loading="loading"
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
        @select="handleSelect"
        @select-all="handleSelectAll"
      >
        <!-- reserve-selection + row-key：翻页/搜索后保留已勾选行（跨页多选） -->
        <el-table-column type="selection" width="45" :reserve-selection="true" />
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <!-- 封面缩略图：固定小尺寸不撑高行；点击放大并查看图片信息；无封面可直接上传 -->
        <el-table-column label="封面" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.coverUrl"
              :src="row.coverUrl"
              fit="cover"
              class="cover-thumb"
              title="点击放大"
              @click="openCoverViewer(row)"
            />
            <div
              v-else
              class="cover-thumb cover-thumb-empty"
              title="点击上传封面"
              @click="triggerRowUpload(row)"
            >
              <el-icon><Plus /></el-icon>
            </div>
          </template>
        </el-table-column>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑图书' : '添加图书'" width="640px">
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
        <el-form-item label="出版日期">
          <el-date-picker
            v-model="form.publishDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择出版日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="语言">
          <el-input v-model="form.language" placeholder="如：中文、英文" />
        </el-form-item>
        <el-form-item label="定价">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 160px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="封面">
          <div class="cover-field">
            <div class="cover-uploader">
              <!-- 已有封面：预览 + 移除 -->
              <div v-if="form.coverUrl" class="cover-preview">
                <img :src="form.coverUrl" alt="封面预览" />
                <el-button
                  class="cover-remove"
                  type="danger"
                  circle
                  size="small"
                  :icon="Delete"
                  title="移除封面"
                  @click="form.coverUrl = ''"
                />
              </div>
              <!-- 上传入口：选择本地图片，成功后回填 coverUrl -->
              <el-upload
                v-else
                class="cover-upload"
                accept="image/jpeg,image/png,image/gif,image/webp"
                :show-file-list="false"
                :before-upload="beforeCoverUpload"
                :http-request="uploadCover"
              >
                <div class="cover-trigger" v-loading="uploading">
                  <el-icon :size="22"><Plus /></el-icon>
                  <span>{{ uploading ? '上传中…' : '上传本地图片' }}</span>
                </div>
              </el-upload>
              <span class="cover-tip">
                JPG / PNG / GIF / WebP，不超过 5MB<br />
                建议尺寸 400 × 600（宽 × 高，2:3）
              </span>
            </div>

            <!-- 图片链接方式：直接使用网图地址作为封面 -->
            <div class="cover-link-row">
              <el-input
                v-model="coverLink"
                size="small"
                placeholder="或粘贴图片链接（https://… 或 /uploads/…）"
                clearable
                @keyup.enter="applyCoverLink"
              />
              <el-button size="small" @click="applyCoverLink">使用链接</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="form.description"
            type="textarea"
            resize="none"
            :rows="3"
            placeholder="内容简介（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 行内上传用的隐藏文件选择器 -->
    <input
      ref="rowFileInput"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      style="display: none"
      @change="onRowFileChosen"
    />

    <!-- 封面放大预览：显示图片类型 / 文件大小 / 尺寸，可直接更换封面 -->
    <el-dialog v-model="coverViewer.visible" title="封面预览" width="420px">
      <div class="cover-viewer">
        <img v-if="coverViewer.url" :src="coverViewer.url" class="viewer-img" alt="封面大图" />
        <div class="viewer-meta">
          <span>类型：{{ coverViewer.type }}</span>
          <span>大小：{{ coverViewer.size }}</span>
          <span>尺寸：{{ coverViewer.dimensions }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="coverViewer.visible = false">关闭</el-button>
        <el-button type="primary" :loading="uploading" @click="triggerReplaceCover">更换封面</el-button>
      </template>
    </el-dialog>

    <!-- Excel 批量导入弹窗（与读者管理共用同一组件） -->
    <ExcelImportDialog
      v-model="importVisible"
      template-url="/books/import/template"
      upload-url="/books/import"
      :tips="importTips"
      @success="() => { loadBooks(); loadCategoryOptions() }"
    />

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
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, Upload, Delete, Download, ArrowDown } from '@element-plus/icons-vue'
import { downloadGet } from '@/utils/download'
import { http } from '@/api/http'
import PageBar from '@/components/PageBar.vue'
import ExcelImportDialog from '@/components/ExcelImportDialog.vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Book, BookCopy, CopyStatus, PageResult } from '@/types'

const books = ref<Book[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filters = reactive({ keyword: '', category: '', publisher: '', status: '' })

/* -------- 多选批量操作（Shift 区间选择 + 跨页保留 + 跨页全选） -------- */
const tableRef = ref<{ clearSelection: () => void; toggleRowSelection: (row: Book, selected?: boolean) => void } | undefined>()
const selected = ref<Book[]>([])

function handleSelectionChange(rows: Book[]) {
  selected.value = rows
}

function clearSelection() {
  tableRef.value?.clearSelection()
  anchorIndex = -1
}

/* Shift 区间选择：锚点行（当前页内索引）+ 全局 Shift 按键状态 */
let anchorIndex = -1
let shiftPressed = false

function onKeyToggle(e: KeyboardEvent) {
  if (e.key === 'Shift') shiftPressed = e.type === 'keydown'
}

/** 勾选复选框时：按住 Shift 则把锚点行到当前行整段选中 */
function handleSelect(_selection: Book[], row: Book) {
  const index = books.value.findIndex((b) => b.id === row.id)
  if (shiftPressed && anchorIndex >= 0 && anchorIndex !== index) {
    const [start, end] = anchorIndex < index ? [anchorIndex, index] : [index, anchorIndex]
    for (let i = start; i <= end; i++) {
      const target = books.value[i]
      if (target) tableRef.value?.toggleRowSelection(target, true)
    }
  }
  anchorIndex = index
}

/**
 * 表头全选/取消全选：作用于所有页。
 * - 全选：按当前筛选条件拉取全部图书 ID，把未勾选的补选上（跨页）；
 * - 取消全选：清空所有页的勾选。
 */
async function handleSelectAll(selection: Book[]) {
  const allPageSelected = books.value.length > 0 &&
    books.value.every((b) => selection.some((s) => s.id === b.id))
  if (!allPageSelected) {
    clearSelection()
    return
  }
  try {
    const ids = await http.get<number[]>('/books/ids', {
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
      publisher: filters.publisher || undefined,
      status: filters.status || undefined
    })
    for (const id of ids) {
      if (!selected.value.some((s) => s.id === id)) {
        // 不在当前页的行传最小占位对象即可：el-table 按 row-key 记录保留选择
        tableRef.value?.toggleRowSelection({ id } as unknown as Book, true)
      }
    }
  } catch (err) {
    ElMessage.error(errorMessage(err, '获取图书列表失败'))
  }
}

/** 批量上架 / 下架 */
async function batchSetStatus(status: 'ACTIVE' | 'INACTIVE') {
  const ids = selected.value.map((b) => b.id)
  if (!ids.length) return
  const action = status === 'ACTIVE' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定${action}选中的 ${ids.length} 本图书吗？`, `批量${action}`, {
      type: 'warning',
      confirmButtonText: action,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    const data = await http.patch<{ count: number }>('/books/batch/status', { ids, status })
    ElMessage.success(`已${action} ${data.count} 本图书`)
    loadBooks()
    clearSelection()
  } catch (err) {
    ElMessage.error(errorMessage(err, '批量操作失败'))
  }
}

/** 批量删除：有副本在借的自动跳过并在结果中提示 */
async function batchDelete() {
  const ids = selected.value.map((b) => b.id)
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${ids.length} 本图书吗？有副本正在借阅的将自动跳过。`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const data = await http.post<{ count: number; skipped: number }>('/books/batch/delete', { ids })
    let msg = `已删除 ${data.count} 本图书`
    if (data.skipped > 0) msg += `，${data.skipped} 本因有副本在借被跳过`
    ElMessage.success(msg)
    loadBooks()
    clearSelection()
  } catch (err) {
    ElMessage.error(errorMessage(err, '批量删除失败'))
  }
}

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
const form = reactive({
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  category: '',
  publishDate: '' as string | null,
  language: '',
  price: undefined as number | undefined,
  coverUrl: '',
  description: ''
})

const rules: FormRules = {
  isbn: [{ required: true, message: '请输入 ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  publisher: [{ required: true, message: '请输入出版社', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }]
}

/* -------- 封面本地上传 -------- */
const uploading = ref(false)

/** 选择文件后、发起上传前的校验：类型与大小 */
function beforeCoverUpload(file: File): boolean {
  const okTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!okTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG / GIF / WebP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

/** 自定义上传：调后端 /uploads/cover，成功后把返回 URL 回填到表单 */
async function uploadCover(options: { file: File }) {
  uploading.value = true
  try {
    const data = await http.upload<{ url: string }>('/uploads/cover', options.file)
    form.coverUrl = data.url
    ElMessage.success('封面上传成功')
  } catch (err) {
    ElMessage.error(errorMessage(err, '封面上传失败'))
  } finally {
    uploading.value = false
  }
}

/** 图片链接方式：校验格式后直接作为封面地址 */
const coverLink = ref('')

function applyCoverLink() {
  const url = coverLink.value.trim()
  if (!url) {
    ElMessage.warning('请先粘贴图片链接')
    return
  }
  // 仅接受 http(s) 外链或站内 /uploads/ 路径
  if (!/^https?:\/\//i.test(url) && !url.startsWith('/uploads/')) {
    ElMessage.error('链接需以 http(s):// 开头，或使用站内 /uploads/ 路径')
    return
  }
  form.coverUrl = url
  coverLink.value = ''
  ElMessage.success('已使用链接作为封面')
}

/* -------- 列表封面：放大预览 + 行内上传 -------- */
const rowFileInput = ref<HTMLInputElement | null>(null)
const rowUploadTarget = ref<Book | null>(null)
const coverViewer = reactive({
  visible: false,
  book: null as Book | null,
  url: '',
  type: '—',
  size: '—',
  dimensions: '—'
})

/** 无封面时点击格子：直接选本地图片上传并保存到该书 */
function triggerRowUpload(row: Book) {
  rowUploadTarget.value = row
  rowFileInput.value?.click()
}

/** 有封面时点击缩略图：放大预览并读取图片信息 */
function openCoverViewer(row: Book) {
  coverViewer.book = row
  coverViewer.url = row.coverUrl || ''
  coverViewer.type = '—'
  coverViewer.size = '—'
  coverViewer.dimensions = '—'
  coverViewer.visible = true
  loadCoverMeta(coverViewer.url)
}

/** 读取图片的类型 / 文件大小 / 像素尺寸 */
async function loadCoverMeta(url: string) {
  try {
    const res = await fetch(url)
    const blob = await res.blob()
    coverViewer.type = blob.type ? blob.type.replace('image/', '').toUpperCase() : extOf(url)
    coverViewer.size = formatSize(blob.size)

    const img = new Image()
    img.onload = () => {
      // 预览框可能在加载完成前被关闭/更换，校验仍是同一张图再回填
      if (coverViewer.visible && coverViewer.url === url) {
        coverViewer.dimensions = `${img.naturalWidth} × ${img.naturalHeight} px`
      }
    }
    img.src = url
  } catch {
    // 读取失败保持占位符
  }
}

function extOf(url: string): string {
  const m = url.split('?')[0].match(/\.(\w+)$/)
  return m ? m[1].toUpperCase() : '—'
}

function formatSize(bytes: number): string {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(2)} MB`
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${bytes} B`
}

/** 预览框里的「更换封面」：复用隐藏文件选择器，目标是当前预览的书 */
function triggerReplaceCover() {
  if (!coverViewer.book) return
  rowUploadTarget.value = coverViewer.book
  rowFileInput.value?.click()
}

/** 行内上传完成：上传文件 → 保存该书封面（带全量字段调用更新接口） */
async function onRowFileChosen(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!beforeCoverUpload(file)) return

  const target = rowUploadTarget.value
  if (!target) return
  uploading.value = true
  try {
    const data = await http.upload<{ url: string }>('/uploads/cover', file)
    await http.put(`/books/${target.id}`, {
      isbn: target.isbn,
      title: target.title,
      author: target.author,
      publisher: target.publisher,
      category: target.category,
      publishDate: target.publishDate || null,
      language: target.language || null,
      price: target.price ?? null,
      description: target.description || null,
      coverUrl: data.url
    })
    target.coverUrl = data.url
    ElMessage.success('封面上传成功')
    // 预览框开着时同步展示新封面
    if (coverViewer.visible && coverViewer.book?.id === target.id) {
      openCoverViewer(target)
    }
    loadBooks()
  } catch (err) {
    ElMessage.error(errorMessage(err, '封面上传失败'))
  } finally {
    uploading.value = false
  }
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
  anchorIndex = -1
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
  anchorIndex = -1
  loadBooks()
}

/** 序号跨页连续：第 2 页从 pageSize+1 开始 */
function indexMethod(index: number) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, {
    isbn: '',
    title: '',
    author: '',
    publisher: '',
    category: '',
    publishDate: '',
    language: '',
    price: undefined,
    coverUrl: '',
    description: ''
  })
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
    category: row.category,
    publishDate: row.publishDate || '',
    language: row.language || '',
    price: row.price ?? undefined,
    coverUrl: row.coverUrl || '',
    description: row.description || ''
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

/* -------- 导出（全部 / 单页 / 选中） -------- */
const exporting = ref(false)

async function handleExport(scope: string) {
  exporting.value = true
  try {
    await downloadGet('/books/export', {
      scope,
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
      publisher: filters.publisher || undefined,
      status: filters.status || undefined,
      page: scope === 'page' ? currentPage.value - 1 : undefined,
      size: scope === 'page' ? pageSize.value : undefined,
      ids: scope === 'selected' ? selected.value.map((b) => b.id).join(',') : undefined
    }, '图书导出.xlsx')
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(errorMessage(err, '导出失败'))
  } finally {
    exporting.value = false
  }
}

// Excel 导入（弹窗）
const importVisible = ref(false)
const importTips = [
  '后缀名为 xls 或者 xlsx',
  '请下载示例文件，按列顺序填写：图片链接、ISBN、书名、作者、出版社、分类',
  '图片链接可填 http(s):// 或站内 /uploads/ 地址（批量设置封面），留空表示清除该书包封面',
  'ISBN 已存在的行将更新信息，请勿使用合并的单元格',
  '文件大小请勿超过 2MB，每次导入请勿超过 10000 行'
]

onMounted(() => {
  loadBooks()
  loadCategoryOptions()
  // Shift 区间选择：全局监听按键状态（复选框点击事件本身拿不到 shiftKey）
  document.addEventListener('keydown', onKeyToggle)
  document.addEventListener('keyup', onKeyToggle)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeyToggle)
  document.removeEventListener('keyup', onKeyToggle)
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
/* 批量操作条：勾选图书后显示在表格上方 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 6px;
}
.batch-count {
  font-size: 13px;
  font-weight: 600;
  color: #67c23a;
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
/* 表单内单位后缀 */
.unit {
  margin-left: 8px;
  color: #86909c;
}
/* 封面上传：触发块 + 预览 */
.cover-field {
  width: 100%;
}
.cover-uploader {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cover-upload {
  flex: 0 0 auto;
}
.cover-trigger {
  width: 96px;
  height: 128px;
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #86909c;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.cover-trigger:hover {
  border-color: #409eff;
  color: #409eff;
}
.cover-preview {
  position: relative;
  width: 96px;
  height: 128px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #eef0f3;
  flex: 0 0 96px;
}
.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.cover-remove {
  position: absolute;
  top: 4px;
  right: 4px;
}
.cover-tip {
  font-size: 12px;
  color: #a9aeb8;
  line-height: 1.7;
}
/* 图片链接输入行 */
.cover-link-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.cover-link-row .el-input {
  flex: 1;
}
/* 列表封面缩略图：固定小尺寸，不撑高行 */
.cover-thumb {
  width: 36px;
  height: 48px;
  border-radius: 4px;
  cursor: pointer;
  display: block;
  margin: 0 auto;
}
.cover-thumb-empty {
  border: 1px dashed #c0c4cc;
  color: #a9aeb8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: border-color 0.2s, color 0.2s;
}
.cover-thumb-empty:hover {
  border-color: #409eff;
  color: #409eff;
}
/* 放大预览 */
.cover-viewer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}
.viewer-img {
  max-width: 100%;
  max-height: 320px;
  border-radius: 6px;
  object-fit: contain;
}
.viewer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  font-size: 13px;
  color: #4e5969;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 8px 12px;
}
</style>
