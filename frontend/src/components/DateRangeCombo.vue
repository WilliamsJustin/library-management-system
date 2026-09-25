<template>
  <div class="date-combo">
    <el-select v-model="field" class="combo-field" @change="emit('change')">
      <el-option v-for="f in fields" :key="f.value" :label="f.label" :value="f.value" />
    </el-select>
    <span class="combo-divider" />
    <el-date-picker
      v-model="range"
      class="combo-picker"
      type="daterange"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      value-format="YYYY-MM-DD"
      @change="emit('change')"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 时间字段下拉 + 日期区间选择器的合成盒（原 MyLoansView / PenaltyView 各写一份的模板与样式）。
 * 字段选项由使用方传入（借阅查询：借出/应还/归还；罚款：生成/缴费），
 * 任一侧变化即上抛 change，由页面决定是否触发检索。
 */
defineProps<{
  /** 时间字段选项 */
  fields: Array<{ label: string; value: string }>
}>()

const emit = defineEmits<{ (e: 'change'): void }>()

/** 当前选中的时间字段值 */
const field = defineModel<string>('field', { required: true })
/** 日期区间，YYYY-MM-DD 闭区间 */
const range = defineModel<[string, string] | null>('range', { default: null })
</script>

<style scoped>
/* 时间字段下拉 + 日历范围选择合成一个盒子：外层统一描边，内部控件去自身边框 */
.date-combo {
  display: flex;
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: border-color 0.2s;
}
.date-combo:focus-within {
  border-color: var(--sl-primary-strong);
}
/* el-select（el-select__wrapper）与 el-date-picker（el-input__wrapper）都去掉自身边框 */
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

/* ===== 手机端（<=768px）：占满一行，内部允许收缩 ===== */
@media (max-width: 768px) {
  .date-combo {
    width: 100%;
  }
  .combo-field {
    flex: 0 0 104px;
    width: 104px;
  }
  .combo-picker {
    flex: 1 1 auto;
    width: auto;
    min-width: 0;
  }
  .date-combo :deep(.el-range-editor) {
    flex: 1 1 0 !important;
    width: 100% !important;
    min-width: 0 !important;
  }
}
</style>
