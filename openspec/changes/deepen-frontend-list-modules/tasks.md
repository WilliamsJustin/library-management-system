# Tasks: 收敛前端列表页的复制

> 依赖顺序：第 1 期口径模块铺底 → 第 2 期组件 → 第 3 期 composable → 第 4 期页面迁移 → 第 5 期测试。每期结束跑 `npm run typecheck`。桌面端零变化是全程硬约束。

## 1. 列表口径模块（候选 3）

- [x] 1.1 [Frontend] 新增 `frontend/src/utils/listDisplay.ts`：`pageIndexAscending(page,size,index)`、`pageIndexDescending(page,size,index,total)`、`loanStatusText/loanTagType`、`isLoanOverdue(row)`、`formatAmount`，全部纯函数 + JSDoc 标注语义来源（哪个页面原实现）；验证：typecheck 通过
- [x] 1.2 [Frontend] 修改 9 个文件的本地 `formatDateTime` 为引用 `utils/dateUtils` 的 `formatDate`（MyLoansView、reader/PenaltyView、LoansView、admin/PenaltiesView、AnnouncementManageView、ActivityManageView、HelpAdminView、reader/HelpView、SiteHomeView），删除各页本地实现；验证：typecheck 通过 + 全局搜索 `formatDateTime` 无残留本地定义
- [x] 1.3 [Frontend] 将 8 处序号公式调用点切换到 `pageIndexAscending/Descending`（LoansView 用 Descending，其余用 Ascending，含 MyLoansView 内联版）；验证：typecheck 通过

## 2. 卡片骨架组件（候选 1）

- [x] 2.1 [Frontend] 新增 `frontend/src/components/MobileCardList.vue`：props（rows/rowKey/loading/emptyText）+ head/body/detail/foot 作用域插槽；implementation 内置 expanded Set（按 rowKey 记忆）、toggle、`m-expand` 按钮（`v-if="$slots.detail"`）、el-empty、v-loading；验证：typecheck 通过
- [x] 2.2 [Frontend] 新增 `frontend/src/components/DateRangeCombo.vue`：v-model 双绑定（dateField/dateRange）+ 字段选项 props，样式收编 MyLoansView 现有 40 行 scoped CSS；验证：typecheck 通过
- [x] 2.3 [Frontend] 编写 MobileCardList 挂载冒烟测试（有/无 detail 插槽两种渲染、展开收起切换、空态）；验证：`npm run test` 该文件全绿（依赖 5.1 的 vitest 基建，可先写后跑）

## 3. 跨页选择 composable（候选 2）

- [x] 3.1 [Frontend] 新增 `frontend/src/composables/useCrossPageSelection.ts`：持有 tableRef，返回 `tableBindings`（ref/row-key/@select/@select-all/@selection-change）+ `selected/toggle/clear/selectAll(ids)`；JSDoc 声明「选择列必须带 `:reserve-selection="true"`」约定（design D4）；统一 Q4 语义：clearSelection 一律重置 Shift 锚点；验证：typecheck 通过
- [x] 3.2 [Frontend] 编写 useCrossPageSelection 单测（约 8 例：勾选/取消/跨页保留/占位对象仅含 id/全选补选/清空/Shift 锚点区间/失败路径锚点重置）；验证：`npm run test` 全绿

## 4. 页面迁移（9 页）

- [x] 4.1 [Frontend] 迁移 `admin/BooksView.vue`：卡片模板→MobileCardList 插槽、选择逻辑→useCrossPageSelection（保留封面点击上传/预览、Excel 导入导出、批量操作入口）；验证：typecheck + 375/1440 双档人工走查
- [x] 4.2 [Frontend] 迁移 `ReaderManagementView.vue`：同 4.1 模式（保留手机逐条多选退化、批量重置/启用/停借）；验证：typecheck + 双档走查
- [x] 4.3 [Frontend] 迁移 `admin/LoansView.vue` 与 `admin/PenaltiesView.vue`：卡片→插槽（含展开详情 detail 插槽）、时间组合框→DateRangeCombo、口径函数→listDisplay；验证：typecheck + 双档走查
- [x] 4.4 [Frontend] 迁移 `reader/MyLoansView.vue` 与 `reader/PenaltyView.vue`：同 4.3 模式；验证：typecheck + 双档走查
- [x] 4.5 [Frontend] 迁移 `AnnouncementManageView.vue`、`ActivityManageView.vue`、`admin/HelpAdminView.vue`（FAQ 与留言两张卡）：卡片→插槽、口径→listDisplay；验证：typecheck + 双档走查

## 5. 测试基建与验收

- [x] 5.1 [Frontend] 引入 devDependencies：`vitest`（3.x）、`@vue/test-utils`（2.x）、`happy-dom`；`package.json` 增 `"test": "vitest run"`；`vite.config.ts` 增 `test: { environment: 'happy-dom' }`；验证：`npm run test` 可运行（先建最小占位用例）
- [x] 5.2 [Frontend] 完成第 2/3 期用例的最终断言并补 listDisplay 单测（约 7 例：双序号公式/状态映射/isLoanOverdue 的 OVERDUE 与 ACTIVE 过期两分支/金额格式）；验证：`npm run test` 全绿
- [x] 5.3 [Frontend] 回归验收：`npm run typecheck` 零报错；1440px 结构断言（各表格页列数与改造前一致、无 m-card、侧栏在位）；375px 走查 9 页卡片形态不变；批量操作回归（勾选/跨页保留/全选/Shift 区间/批量下架后上架还原）；验证：全部通过并记录结果
