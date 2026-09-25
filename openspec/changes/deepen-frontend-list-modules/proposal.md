# Proposal: 收敛前端列表页的复制（deepen-frontend-list-modules）

## Why

2026-09 响应式改造在 9 个列表页亲手写下了大量复制：卡片骨架 ×9、展开状态逻辑 ×4、跨页选择逻辑 ×2（约 90 行逐行相同且已分叉）、序号公式 ×8（含两种并存语义）、`formatDateTime` ×9（与现成的 `dateUtils.formatDate` 逐字符等价却 0 引用）。前端目前**零测试运行器**——这些接缝将是第一批可单测的前端接口。架构评审（`%TEMP%\architecture-review-20260925-1602.html`）将本项列为 Top recommendation：全部落在最近 15 次变更的 hot path 上，趁热收敛成本最低。

## What Changes

- 新增 `<MobileCardList>` 骨架组件（Q1/Q2 决议）：接口 = `rows/rowKey/loading/emptyText` props + `head/body/detail/foot` 作用域插槽；展开详情 Set、toggle、展开按钮、空态、v-loading 藏进 implementation，有 `detail` 插槽才渲染展开按钮；expanded 按 rowKey 记忆、翻页/筛选保留
- 新增 `<DateRangeCombo>`（搭车）：时间字段下拉 + 日期区间选择器的合成盒，收敛 MyLoansView / PenaltyView 两份完整复制的模板与约 40 行 scoped CSS
- 新增 `useCrossPageSelection` composable（Q3/Q10 决议）：持有 tableRef，返回表级 `tableBindings`（ref/row-key/三事件）+ `selected/toggle/clear/selectAll`；桌面 el-table 选择与手机 Set 选择是内部两个 adapter；`reserve-selection` 是列级属性，仍由页面书写并在 JSDoc 声明约定
- 新增 `utils/listDisplay.ts`（Q5/Q6 决议）：`pageIndexAscending / pageIndexDescending` 双具名函数（递减是借阅流通页既有产品行为，语义保留、实现收敛）、借阅状态文案 + tag 色映射、`isLoanOverdue`、`formatAmount`；9 份本地 `formatDateTime` 全部改为引用现成 `dateUtils.formatDate`
- 微行为统一（Q4 决议，唯一非等价点）：BooksView 的 `clearSelection` 对齐 ReaderManagementView——全选失败路径也重置 Shift 锚点
- 引入 vitest + @vue/test-utils + happy-dom（Q8 决议）：首批单测覆盖 useCrossPageSelection（约 8 例）、listDisplay（约 7 例）、组件挂载冒烟（展开/收起）
- 9 个页面迁移到上述模块；桌面端渲染**零变化**（只动 `v-else` 手机分支内部与纯函数）

**BREAKING**：无（`npm run test` 为新增脚本，现有脚本不变）。

## Capabilities

### New Capabilities
（无——纯重构 + 测试基建，无规格级用户可见行为变更，`.openspec.yaml` 已声明 `skip_specs: true`）

### Modified Capabilities
（无）

## Impact

- **前端新增**：`components/MobileCardList.vue`、`components/DateRangeCombo.vue`、`composables/useCrossPageSelection.ts`、`utils/listDisplay.ts`、`vitest.config`（并入 vite.config.ts）
- **前端修改**（迁移调用点）：`views/admin/BooksView.vue`、`ReaderManagementView.vue`、`admin/LoansView.vue`、`admin/PenaltiesView.vue`、`admin/HelpAdminView.vue`、`reader/MyLoansView.vue`、`reader/PenaltyView.vue`、`AnnouncementManageView.vue`、`ActivityManageView.vue`、`reader/HelpView.vue`、`site/SiteHomeView.vue`（仅 formatDateTime→dateUtils）；`package.json`、`vite.config.ts`
- **后端**：零改动
- **依赖**：devDependencies 新增 `vitest`、`@vue/test-utils`、`happy-dom`（坐标已在 grilling Q11 确认）
- **CONTEXT.md**：随本变更首建，记录「列表显示口径」「跨页选择集」「移动卡片形态」三个模块术语
- **验收**：`npm run typecheck` 零报错 · `npm run test` 全绿 · 375/1440 双档走查全部改动页（桌面逐像素一致）· 批量操作回归（勾选/跨页保留/全选/Shift 区间/批量下架还原）
