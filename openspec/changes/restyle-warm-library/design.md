# Design: restyle-warm-library

## Context

上一变更（restyle-visual-system）已建成双层令牌架构：`style.css` 的 `--sl-*` 语义令牌 + Element Plus `--el-*` 桥接阶梯，约 30 个视图文件已全部改为令牌引用、零裸品牌色。新设计系统已由引擎落盘 `design-system/school-library/MASTER.md`（暖木书斋：书棕 `#78716C` + 琥珀 `#D97706` + 暖米黄 `#FFFBEB`，Cormorant Garamond / Crimson Pro 衬线）。因此本次换肤的改动面收敛为：令牌区改写、EP 阶梯重推导、字体资源替换、少量主题专属硬编码。

## Goals / Non-Goals

**Goals:**
- 改 `style.css` 令牌区一处 + 少量专属规则即完成三端换肤，验证"令牌架构"的投资回报
- 衬线字体优雅降级：中文不接触衬线，避免宋体割裂感
- 可访问性底线在新色系下重算并保持达标

**Non-Goals:**
- 双主题切换器、暗色模式
- 布局/信息架构变更（hero 装饰仅换视觉语言，结构不动）

## Decisions

### D1. EP 主色阶梯重推导 + On Primary 语义翻转

按 EP 混色规则（向白混 30/50/70/80/90%，向黑混 20%）从书棕 `#78716C` 推导：
`light-3 #948F8A`、`light-5 #B2AFAA`、`light-7 #D1CFCC`、`light-8 #E0DEDC`、`light-9 #EFEEEC`、`dark-2 #605A56`。
（校验：`0.78*120=93.6→94(0x5E)`? 实取 30% 白混：`r=120+0.3*135=160.5→160(0xA0)`? —— 以实现时脚本计算为准，上列为近似值，实现时用一次 Python 混色脚本生成精确六档再写入，避免手工误差。）

**On Primary 翻转**：MASTER 定义 On Primary 为白色。实测对比度：书棕 `#78716C` 上白字 4.82:1 ✓、黑字 4.36:1 ✗——因此删除上一变更的 `.el-button--primary:not(...){color:#000}` 覆盖规则，恢复 EP 默认白字机制。琥珀 `#D97706` 上白字 2.9:1 ✗、黑字 5.6:1 ✓，CTA 琥珀按钮保持黑字。

### D2. 可读性补丁色系重算

书卷青的四个压深补丁替换为暖色系等价物：主色小字/链接直接用 `#78716C`（4.82:1 ✓，无需分档；若截图观感偏灰则加深一档 `#57534E`）；warning 标签文字 `#92400E`（对 light-9 暖底 ≈6:1，保留）；success/danger 补丁色保留（语义色未变）。Muted `#F6F6F6` 与底色 `#FFFBEB` 区分度低 → hover/斑马纹用 `#F5F0E1`（暖纸深一档，实现时实测确认可见性）。

### D3. 字体替换与衬线应用边界

下载 Cormorant Garamond、Crimson Pro 可变字重 woff2（fontsource 流程同上次，预计各 60–100KB）替换 `assets/fonts/` 下旧文件；`--sl-font-display: 'Cormorant Garamond', 'Crimson Pro', ...中文系统栈`，`--sl-font-body: 'Crimson Pro', ...中文系统栈`。**衬线只进 h1/h2、`.hero-title`、时钟/统计大数字**（现有 `.hero-title`/`h1`/`h2` 已走 `--sl-font-display`，自动生效）；表格数字、ISBN、正文中文保持系统栈的可读性——即 body 字体栈以 `'Crimson Pro'` 打头但中文立即回落，效果为拉丁衬线 + 中文黑体混排。若截图确认混排违和，回退方案：body 栈去掉 Crimson Pro 保留系统栈，衬线仅 display（一行令牌改动）。

### D4. 主题专属硬编码清单（grep 驱动）

上轮遗留的青绿专属硬编码须随主题改写，实施前用 grep 盘点：
- `rgba(13, 148, 136, …)`（FloatingHelp 阴影 ×2）
- `SiteHomeView` hero：装饰圆/环用 `--el-color-primary-light-8/9`（自动跟随变暖）、标题下琥珀条用 `--sl-accent`（琥珀不变、自动成立）、hero 渐变底 `--sl-bg → light-9` 自动变暖——仅装饰语言微调（圆环 → 书页横线/纸纹，可选）
- `LoginView` 页面渐变（`--sl-bg` 系，自动跟随）
- `ReaderLayout`/`AdminLayout` 头部渐变（`--sl-primary-strong → dark-2`，自动跟随；深棕渐变白字对比度实现时验证）
- SessionList 头像渐变（自动跟随）

即：真正手改的只有 FloatingHelp 阴影 rgba 与可选的 hero 装饰语言，其余由令牌传导。

### D5. 验证方式

沿用上一变更基线：`vue-tsc` + 21 单测；375/768/1440 三端截图走查；768/769 结构断言（卡片/tabbar）；对比度程序化抽查（body/主按钮/标签）；`grep` 确认青绿品牌色残留为 0。

## Risks / Trade-offs

- [衬线中文混排违和] → D3 回退方案：衬线仅标题（一行令牌改动）
- [书棕偏灰、主按钮辨识度弱于青绿] → 截图阶段确认；可将 `--sl-primary` 加深一档为 `#57534E` 提升对比与分量（令牌一行）
- [暖米黄底与 muted 灰层次不足] → D2 的暖纸色斑马纹/hover 补救
- [EP 阶梯近似值偏差] → 实现时用脚本精确混色，浏览器实测 hover/浅底
- [旧字体文件残留仓库] → 删除 Outfit/Work Sans woff2（git 外无备份必要，MASTER.book-teal.md 已留档色值，字体可随时再取）

## Migration Plan

纯前端变更；按"令牌+字体一次到位 → 截图修正 → 验收"单阶段推进（改动面集中在 style.css，无需三期分期）。回退：恢复令牌区旧值 + 旧字体文件（上一变更产物在案）。

## Open Questions

（无）
