<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="620px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <!-- 说明列表 -->
    <ol class="import-tips">
      <li v-for="(t, i) in tipList" :key="i">{{ t }}</li>
    </ol>

    <!-- 导入结果（上传后显示） -->
    <el-alert
      v-if="result"
      :type="result.failed > 0 ? 'warning' : 'success'"
      :closable="false"
      class="import-result"
    >
      <template #title>
        共读取 {{ result.total }} 行：成功 {{ result.success }} 条，失败 {{ result.failed }} 条
      </template>
      <ul v-if="result.errors.length" class="result-errors">
        <li v-for="(e, i) in result.errors.slice(0, 20)" :key="i">{{ e }}</li>
        <li v-if="result.errors.length > 20">……其余 {{ result.errors.length - 20 }} 条省略</li>
      </ul>
    </el-alert>

    <!-- 双栏操作区：左下载示例文件，右上传 Excel 文件 -->
    <div class="import-panes">
      <div class="pane">
        <el-button :loading="downloading" @click="downloadTemplate">下载Excel示例文件</el-button>
      </div>
      <div class="pane-divider">»</div>
      <div class="pane">
        <el-button type="primary" :loading="uploading" @click="fileInput?.click()">
          上传Excel文件
        </el-button>
      </div>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept=".xlsx,.xls"
      style="display: none"
      @change="onFileChosen"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="emit('update:modelValue', false)">返回</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { errorMessage } from '@/utils/error'
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getToken } from '@/api/http'
import { http } from '@/api/http'

/**
 * 「上传 Excel 文件」导入弹窗（图书编目 / 读者管理共用），样式参照需求图：
 * 顶部编号说明列表；下方左右两栏——左侧白底「下载Excel示例文件」、
 * 右侧蓝底「上传Excel文件」，中间以 » 分隔；右下角「返回」关闭弹窗。
 *
 * 上传成功后展示导入结果（成功/失败计数与逐行错误），并 emit('success', result)
 * 由父页面刷新列表。
 */
interface ImportResult {
  total: number
  success: number
  failed: number
  errors: string[]
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  /** 弹窗标题 */
  title?: string
  /** 模板下载接口地址（GET，返回 xlsx） */
  templateUrl: string
  /** 导入上传接口地址（POST multipart，字段名 file） */
  uploadUrl: string
  /** 说明列表；不传时使用通用提示 */
  tips?: string[]
}>(), { title: '上传 Excel 文件' })

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'success', result: ImportResult): void
}>()

const defaultTips = [
  '后缀名为 xls 或者 xlsx',
  '请按示例文件的列顺序填写，请勿增删列、请勿使用合并的单元格',
  '文件大小请勿超过 2MB',
  '每次上传的数据行数请勿超过 10000 行'
]
const tipList = computed(() => props.tips?.length ? props.tips : defaultTips)

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const downloading = ref(false)
const result = ref<ImportResult | null>(null)

/** 下载示例模板：带 token 请求二进制，按响应头还原文件名 */
async function downloadTemplate() {
  downloading.value = true
  try {
    const token = getToken()
    const res = await fetch(`/api${props.templateUrl}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!res.ok) {
      ElMessage.error('模板下载失败，请稍后重试')
      return
    }
    const blob = await res.blob()
    const dispo = res.headers.get('Content-Disposition') || ''
    const m = dispo.match(/filename\*=UTF-8''([^;]+)/i)
    const name = m ? decodeURIComponent(m[1]) : '导入模板.xlsx'
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    downloading.value = false
  }
}

async function onFileChosen(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  const name = file.name.toLowerCase()
  if (!name.endsWith('.xls') && !name.endsWith('.xlsx')) {
    ElMessage.error('后缀名需为 xls 或 xlsx')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('文件大小请勿超过 2MB')
    return
  }

  uploading.value = true
  try {
    const data = await http.upload<ImportResult>(props.uploadUrl, file)
    result.value = data
    emit('success', data)
  } catch (err) {
    ElMessage.error(errorMessage(err, '导入失败'))
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.import-tips {
  margin: 0 0 16px;
  padding-left: 18px;
  font-size: 14px;
  color: #1f2329;
  line-height: 2;
}
.import-result {
  margin-bottom: 16px;
}
/* 错误明细固定高度内部滚动：行数再多也不把弹窗往下撑开（标题保持可见） */
.result-errors {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.8;
  max-height: 140px;
  overflow-y: auto;
}
/* 双栏操作区：左右两个浅灰半格，中间 » 分隔 */
.import-panes {
  display: flex;
  align-items: stretch;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.pane {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 0;
  background: #f5f7fa;
}
.pane-divider {
  flex: 0 0 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #909399;
  font-size: 18px;
  font-weight: 700;
}
.dialog-footer {
  text-align: right;
}
</style>
