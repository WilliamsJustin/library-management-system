# Design: restyle-visual-system

## Context

前端为 Vue 3 + Element Plus 2.14，全局样式集中在 `frontend/src/style.css`（227 行，含移动卡片 `.m-card` 系列与 ≤768px 全局规则），无令牌层；`#409eff` 等 EP 默认蓝硬编码散布在约 20 个 `.vue` 文件的 `<style>` 中。设计系统源头已持久化在 `design-system/school-library/MASTER.md`（"书卷青"：主色 `#0D9488`、点缀 `#D97706`、背景 `#F0FDFA`、前景 `#134E4A`，Outfit/Work Sans 字体，Swiss 极简风）。响应式体系（768px 断点、移动卡片、useBreakpoint）刚稳定，约束见 spec「换肤不回归约束」。

## Goals / Non-Goals

**Goals:**
- 一处定义、三端生效的设计令牌层，EP 组件整体跟随
- 字体内网离线可用
- MASTER.md 交付检查单（对比度/focus/过渡/reduced-motion）落地
- 分期可验证：每期结束可截图对比、可回退

**Non-Goals:**
- 暗色模式（仅浅色；`color-scheme: light`）
- 信息架构/路由/交互流程变更（`SiteHomeView` hero 仅视觉层微调）
- 引入 Tailwind / SCSS 构建链改造 / 任何新 npm 依赖

## Decisions

### D1. 双层令牌：自有语义令牌 + EP 桥接

`style.css` 的 `:root` 定义自有令牌（`--sl-primary`、`--sl-primary-strong`、`--sl-accent`、`--sl-bg`、`--sl-text`、`--sl-muted`、`--sl-border`、`--sl-radius`、`--sl-shadow-*`、`--sl-font-display`、`--sl-font-body`、`--sl-transition`），再用 EP 官方变量机制桥接：`--el-color-primary` 系列指到自有令牌的函数值。业务代码只允许引用 `--sl-*`（及少量 `--el-*`），不得再出现裸色值。

*备选*：SCSS 主题（需改构建链，弃）；Tailwind（违反项目技术栈硬性规定，弃）；直接散改各文件（无令牌层，后续维护差，弃）。

### D2. EP 主色阶梯映射（含可读性分档）

按 EP 混色规则（向白混 30/50/70/80/90%，向黑混 20%）从 `#0D9488` 推导阶梯，写入 `:root`：
`light-3 #56B4AC`、`light-5 #86CAC4`、`light-7 #B6DFDB`、`light-8 #CFEAE7`、`light-9 #E7F4F3`、`dark-2 #0A766D`。

可读性分档（关键决策）：`#0D9488` 对白底对比度仅 ≈3.75:1，**小号文字（链接、辅助强调）一律用深一档青 `#0F766E`（≈5.5:1）**，即 `--sl-primary-strong`；主按钮等大号/加粗填充场景用 `#0D9488`，按钮文字按 MASTER 的 On Primary 用黑 `#000`（≈5.6:1）而非白字。

语义色与主题协调：`--el-color-danger: #DC2626`（MASTER destructive）、`--el-color-warning: #D97706`（与琥珀点缀同源）、`success/info` 保持 EP 语义基调仅微调明度；状态标签的语义（在借/逾期/停借）不变。

*备选*：全局把 primary 定成 `#0F766E` 保白字达标——但视觉偏暗失去"书卷青"的清爽感，且 MASTER 明确 On Primary 为黑，弃。

### D3. 功能性边框中性化

MASTER 的 `Border #5EEAD4` 是装饰性强调边框色，直接用于表单边框过于饱和。功能性边框（输入框、表格线）用中性灰青 `#CBD5E1` 系；青绿描边仅用于卡片 hover、选中卡、装饰分隔线。 MASTER `Muted #E8F1F4` 用作 hover 浅底/斑马纹。

### D4. 字体自托管方案

从 Google Fonts 下载 Outfit、Work Sans 的**可变字重 woff2**（各 1 个文件，约 40–80KB，覆盖 300–700）放入 `frontend/src/assets/fonts/`，`@font-face` 用 `font-weight: 300 700` + `font-display: swap` 声明；字体栈：正文 `'Work Sans', -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif`，标题/展示数字 `'Outfit', 'Work Sans', ...`。中文零网络加载。`main.ts` 不引 CDN，字体经 Vite 静态资源打包。

*备选*：Google CDN 直连（内网不可靠，弃）；中文 webfont（体积 MB 级，弃）。

### D5. 分期实施与回归防线

三阶段，每阶段独立可验证、可单独提交：
1. **令牌层 + 公共前台**：style.css 令牌与 EP 桥接、字体、`PublicLayout` + `site/*` + `LoginView`/`RegisterView` + 公共组件（`FloatingHelp`、`GlobalReminderPopup`）
2. **读者后台**：`ReaderLayout` + `reader/*`
3. **管理后台 + 收尾**：`AdminLayout` + `admin/*` + 其余共享组件（`MobileCardList`、`DateRangeCombo`、`PageBar` 等）+ 全局检查单规则收口

每阶段收尾跑 `vitest run`（21 用例）+ 375/768/1440 截图对比；硬规则：**只改 `<style>` 块与样式类，不改 `<template>` 结构与 `<script>` 逻辑**（唯一例外：`SiteHomeView` hero 区视觉微调，见 D6）。

### D6. SiteHomeView hero 微调边界

保留现有区块顺序（hero+检索框 / 功能卡片 / 公告·活动），仅做视觉层：背景由渐变蓝改为极浅青底 + 青绿几何装饰（纯 CSS）、标题用 Outfit、检索框与 CTA 按钮换新色系、卡片阴影/圆角统一到令牌。不新增区块、不调整路由入口。

### D7. 检查单的全局落点

`style.css` 集中落地：`a` 与可点击元素 `cursor: pointer`；全局 `:focus-visible` 用 `--sl-ring`（青绿 3px 光环）；过渡统一 `var(--sl-transition)`（180ms ease）；`@media (prefers-reduced-motion: reduce)` 下关闭位移/动画类效果。对比度抽查清单：正文 `#134E4A`/浅青底、muted `#475569`/白底、表头文字、状态标签文字。

## Risks / Trade-offs

- [EP 阶梯推导值与 EP 实际混色有偏差] → 浏览器实测 hover/浅底色，偏差明显时微调阶梯值（只动 `:root` 一处）
- [`#0D9488` 大面积用于按钮黑字观感未知] → 截图对比阶段确认；不可接受则整表降为 `#0F766E` 底 + 白字（一行令牌切换）
- [35 文件逐个替换硬编码色，遗漏部分残留 EP 蓝] → 收尾用 `grep -rn "#409eff\|#409EFF" frontend/src` 全局清零检查 + 三端截图走查
- [字体文件入库增大仓库] ≈150KB，可接受；后续可改 CDN+本地回退
- [过渡动画与 EP 内部动画叠加] → 只统一自定义元素过渡节奏，不覆盖 EP 组件内部动画时长

## Migration Plan

纯前端变更，随前端一并部署（docker-compose 构建物不变）；字体为静态资源随构建产物分发。回退策略：整个变更按阶段提交，任一阶段可独立 `git revert`；令牌层隔离在 `style.css` 顶部，极端情况可整段移除恢复 EP 默认蓝。

## Open Questions

（无）
