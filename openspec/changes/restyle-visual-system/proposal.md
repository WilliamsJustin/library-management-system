# Proposal: restyle-visual-system

## Why

三端目前是 Element Plus 默认蓝的"裸皮肤"：`#409eff` 等硬编码色散落在约 20 个组件文件里，没有统一的设计令牌层，视觉上与通用后台模板无差别，无法体现图书馆场景的气质。用户已选定 ui-ux-pro-max 引擎生成的"书卷青"设计系统（`design-system/school-library/MASTER.md`，Minimalism & Swiss 风格：主色青绿 `#0D9488`、点缀/CTA 琥珀 `#D97706`、极浅青背景），需要将其落地为全站统一的视觉皮肤。

## What Changes

- **新增设计令牌层**：`frontend/src/style.css` 顶部建立 `:root` CSS 变量（色彩/间距/阴影/圆角/字体栈），值取自 MASTER.md；Element Plus 通过覆盖 `--el-color-primary` 系列变量整体跟随新主题，不引入 Tailwind、不更换组件库
- **自托管字体**：Outfit / Work Sans 拉丁字体 woff2 放入 `frontend/src/assets/fonts/`（校园内网不依赖 Google CDN），中文回落系统字体栈；学号/ISBN/日期/金额等拉丁与数字内容直接获得新字体质感
- **三端分期换肤**（一个变更内分三阶段）：公共前台 → 读者后台 → 管理后台；替换散落的硬编码色为令牌引用，统一圆角/阴影/过渡节奏（150–300ms）
- **公共前台首页局部版式微调**：按 MASTER.md 的 Hero + Features + CTA 模式调整首页 hero 区与功能卡片的视觉层次（不动信息架构与路由结构）
- **可访问性与交互细节**：按 MASTER.md 检查单落地——正文对比度 ≥ 4.5:1、focus 状态可见、hover 过渡 150–300ms、`prefers-reduced-motion` 降级
- **明确不做**：暗色模式（本轮色板仅浅色）、信息架构与交互流程重排、桌面端布局结构变更

## Capabilities

### New Capabilities
- `visual-theme`: 全站统一视觉主题——设计令牌（色彩/字体/间距/阴影/圆角）、Element Plus 主题跟随、三端皮肤一致性约束、可访问性底线（对比度/focus/过渡/reduced-motion），以及"响应式行为与桌面布局结构不回归"的硬约束

### Modified Capabilities

（无——`openspec/specs` 主库当前为空，responsive-ui 约束作为新能力 `visual-theme` 的需求项写入，不单独开 delta）

## Impact

- **前端样式**：`frontend/src/style.css`（令牌层 + EP 变量覆盖 + 全局规则改写）、`frontend/src/main.ts`（字体引入方式视打包方案调整）、新增 `frontend/src/assets/fonts/*.woff2`
- **前端视图**：约 35 个 `.vue` 中散落的硬编码色（`#409eff`、渐变、阴影值）替换为令牌引用；`site/SiteHomeView.vue` hero 区版式微调；布局组件（`PublicLayout` / `ReaderLayout` / `AdminLayout`）配色跟随
- **不受影响**：后端、API、路由、Pinia store、组件交互逻辑；前端 21 个单测必须保持绿
- **新依赖**：无 npm 依赖（字体为静态资源；主题用 EP 原生 CSS 变量机制）
- **风险**：EP 组件内边色（表格、标签、分页等）跟随 `--el-*` 变量自动变化，需逐面验证对比度；768px 断点响应式行为不得回归
