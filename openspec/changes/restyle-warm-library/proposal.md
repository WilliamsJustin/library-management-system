# Proposal: restyle-warm-library

## Why

用户希望在三端"书卷青"主题（restyle-visual-system，已实施）之外更换整体风格。经 ui-ux-pro-max 引擎对 5 个风格家族实跑对比，用户选定"暖木书斋"（Swiss Modernism 2.0，Book & Reading Tracker 命中）：主色书棕 `#78716C`、点缀琥珀 `#D97706`、暖米黄底 `#FFFBEB`、衬线标题。设计系统已落盘 `design-system/school-library/MASTER.md`（旧版留档 `MASTER.book-teal.md`）。上一变更建立的令牌架构（`--sl-*` + EP 桥接）使本次换肤主体收敛为令牌区改写 + 少量主题专属硬编码，风险远低于首次换肤。

## What Changes

- **设计令牌整体替换**：`style.css` 的 `--sl-*` 语义令牌与 EP 桥接阶梯按书棕重推导（EP 混色规则同 design D2 方法）；On Primary 语义翻转——书棕底 `#78716C` 上黑字仅 4.36:1 不达标、白字 4.82:1 达标，主按钮从"青绿底黑字"改为"书棕底白字"，删除上轮的按钮黑字覆盖规则
- **可读性分档重算**：书卷青的标签/小字压深补丁（`#0F766E`、`#92400E`、`#166534`、`#B91C1C`）按新色系重算；书棕 `#78716C` 对白底 4.82:1 达标，链接/小字可用主色或加深一档
- **字体更换**：标题 Cormorant Garamond、正文 Crimson Pro（衬线只作用于标题与展示数字，中文回落系统字栈）；woff2 自托管替换现有 Outfit/Work Sans 资源，`@font-face` 与 `--sl-font-*` 更新
- **主题专属硬编码随新主题改写**：`SiteHomeView` hero 装饰语言（浅青几何 → 暖纸色书卷感装饰）、`FloatingHelp` 的青绿 rgba 阴影、`GlobalReminderPopup`/`SessionList` 等处的青绿/琥珀渐变引用值
- **明确不做**：双主题运行时切换器、暗色模式、信息架构与布局结构变更

## Capabilities

### New Capabilities

（无）

### Modified Capabilities
- `visual-theme`: 品牌色系需求项的色值与字体需求项更换为暖木书斋设计系统（主色书棕/琥珀点缀/暖米黄底/衬线标题），"组件库主题跟随、可访问性底线、换肤不回归约束"三条需求不变。**归档顺序前置条件：需先归档 restyle-visual-system**（其 ADDED delta 建立 `visual-theme` 主规格），本变更的 MODIFIED delta 才有归档目标。

## Impact

- **前端样式**：`frontend/src/style.css`（令牌区、EP 阶梯、可读性补丁、按钮文字规则）、`frontend/src/assets/fonts/`（新 woff2 替换旧字体文件）
- **前端视图**：`SiteHomeView.vue` hero 装饰、`FloatingHelp.vue`/`GlobalReminderPopup.vue`/`components/help/*` 中引用青绿 rgba/渐变的少量规则；其余约 30 个文件因全部走令牌引用而自动跟随、零改动
- **不受影响**：后端、API、路由、store、组件逻辑；21 个前端单测保持绿
- **新依赖**：无 npm 依赖（Cormorant Garamond / Crimson Pro 为静态资源）
- **风险**：衬线中文字体回落需要用"仅标题/大数字应用衬线"控制割裂感；书棕与暖米黄的低饱和组合需逐屏确认层次感（muted `#F6F6F6` 与底色区分度）
