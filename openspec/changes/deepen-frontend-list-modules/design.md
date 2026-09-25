# Design: 收敛前端列表页的复制

## Context

动机与范围见 proposal.md。决策来自架构评审（候选 1/2/3）+ 两轮 grilling（8+4 问，全部有推荐并获确认）。项目背景词汇见根目录 `CONTEXT.md`（本变更首建）。约束：桌面端 >768px 渲染零变化；`npm run typecheck` 全程门禁。

## Goals / Non-Goals

**Goals:**
- 把 9 个列表页的移动卡片骨架、跨页选择状态、列表显示口径收进三个深模块：小 interface、大 implementation
- 产出第一批可单测的前端接口（vitest），验证「接口即测试面」
- 消除两处已发生的实现分叉（选择锚点重置、Excel 单元格语义——后者在后端，不在本变更）

**Non-Goals:**
- 不动桌面端模板与样式（>768px 逐像素一致是硬约束）
- 不做后端候选（Excel 流水线、状态机、pendingSummary、AccessPolicy——各自独立成 change）
- 不引入 Pinia 化的全局选择状态、不做通用表格抽象（YAGNI）
- 不改任何产品行为（序号递增/递减双语义保留；Q4 锚点重置是唯一微行为统一，已获确认）

## Decisions

### D1 骨架组件 + 作用域插槽，而非 props 字段驱动
`<MobileCardList>` 的 interface 只有 4 个 props + 4 个插槽。字段编排（封面、金额、时间行）是每页个性，经插槽注入并直接使用全局 `.m-field` 类；骨架共性（head/body/foot 结构、expanded Set、toggle、`m-expand` 按钮、el-empty、v-loading）藏进 implementation。纯 props 字段驱动会造出又宽又僵硬的浅接口；composable 路线会让 9 份 DOM 骨架继续存在。备选均已在 grilling Q1 否决。

### D2 展开详情按插槽存在性可选
`v-if="$slots.detail"` 决定是否渲染「展开详情」按钮——4 个有详情的页面提供 `detail` 插槽，5 个没有的页面零成本。expanded 以 `Set<rowKey>` 记忆、翻页与筛选保留、仅收起时移除，与既有 spec「展开不影响筛选与分页」场景一致。

### D3 useCrossPageSelection 持有 tableRef，接口最小化
返回 `tableBindings`（`ref/row-key/@select/@select-all/@selection-change` 打包对象，页面 `v-bind` 摊开）+ `selected/toggle/clear/selectAll(ids)`。桌面 el-table 选择与手机 Set 选择是接缝上的**两个 adapter**（两个调用方 + 两种选择形态——接缝为真）。`mobileSelectedIds` 的占位对象技巧（`{ id } as unknown as T`）收进 implementation，调用方只见 `selected: T[]`。

### D4 reserve-selection 留在页面（事实修正后的取舍）
`reserve-selection` 是 `el-table-column` 列级属性，tableBindings 盖不住。接受：页面自写选择列 + composable JSDoc 声明「选择列必须带 `:reserve-selection="true"`」。不做 `<SelectionColumn>` 组件——一个属性不值得一个新接口（YAGNI，grilling Q10）。

### D5 列表口径：双具名函数 + 复用 dateUtils
`pageIndexAscending(page,size,index)` 与 `pageIndexDescending(page,size,index,total)` 两个具名函数——递减是借阅流通页既有产品行为，语义保留、实现收敛；不做 mode 参数（布尔参数可读性差）。9 份本地 `formatDateTime` 直接改引用 `utils/dateUtils.ts` 的 `formatDate`（已核实逐字符等价），不新建同名函数。借阅状态文案/tag 色、`isLoanOverdue`、`formatAmount` 一并收编。

### D6 vitest 并入现有 vite.config
devDependencies：`vitest`（3.x）、`@vue/test-utils`（2.x）、`happy-dom`；`package.json` 增 `"test": "vitest run"`；配置以 `test: { environment: 'happy-dom' }` 并入 `vite.config.ts`，不新建配置文件。首批用例只测新模块的 interface（纯状态 + 纯函数 + 组件挂载冒烟），不为旧代码补测。

### D7 迁移顺序：先铺底、后收拢、页面最后
1 `listDisplay` + dateUtils 接回 → 2 `MobileCardList` + `DateRangeCombo` → 3 `useCrossPageSelection` → 4 九页迁移 → 5 测试补齐。每步 typecheck；页面迁移逐页提交，桌面截图可比对。

## Risks / Trade-offs

- [桌面回归] 9 页模板迁移可能误伤桌面 → 所有改动只落在 `v-if="!isMobile"` 的 `v-else` 分支内与纯函数；验收含 1440px 结构断言（各表格列数不变）+ 抽样截图比对
- [插槽类型安全] 作用域插槽的 TS 类型较繁 → 组件用 `defineProps` 泛型约束 + `Slot` 类型显式声明，typecheck 门禁兜底
- [vitest 依赖体积] 三个 dev 依赖 → 均为 devDependencies，不影响产物；坐标已经用户确认（Q11）
- [Q4 行为微变] BooksView 全选失败路径将多一次锚点重置 → 已获用户确认（Q4），无用户可感知差异
- [行数膨胀假象] 迁移初期组件 + 调用点并存，净行数先增后减 → 以「9 页各删 50–100 行模板」为完成判据

## Migration Plan

纯前端变更，无数据迁移。按 D7 顺序实施，每步可独立验证、可暂停；回滚 = 按页反向恢复（无共享状态残留，旧逻辑在 git 历史中）。

## Open Questions

无——两轮 grilling 12 问全部关闭（Q1–Q12 决议已内联到各节）。
