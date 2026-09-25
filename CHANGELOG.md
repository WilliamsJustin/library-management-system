# 更新日志

本项目的变更遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式。
变更明细与需求规格见 [openspec/changes/](openspec/changes/) 目录（规格驱动开发）。

## [Unreleased]

### 2026-09-25 · 视觉主题 v2：「暖木书斋」（restyle-warm-library）

- 三端整体换肤：主色书棕 `#78716C`、点缀/CTA 琥珀 `#D97706`、暖米黄底 `#FFFBEB`（Swiss Modernism 2.0，Book & Reading Tracker）
- 标题字体更换为衬线 Cormorant Garamond / Crimson Pro（自托管 woff2，中文回落系统字体栈）
- On Primary 语义翻转为「书棕底白字」；补齐 plain/text/link 按钮变体的对比度压深规则（导出/Excel 导入等按钮文字对比度由 2.2–2.9 提升至 4.5 以上）
- 上一主题「书卷青」设计系统留档于 `design-system/school-library/MASTER.book-teal.md`

### 2026-09-25 · 视觉主题 v1：「书卷青」（restyle-visual-system）

- 建立 `frontend/src/style.css` 设计令牌层（`--sl-*` 语义令牌 + Element Plus `--el-*` 变量桥接），清零约 20 个文件中的 EP 默认蓝硬编码
- 三端（公共前台/读者后台/管理后台）分期换肤：主色青绿 `#0D9488`、点缀琥珀、极浅青底
- 自托管 Outfit/Work Sans 可变字体；落地可访问性底线：正文对比度 ≥ 4.5:1、focus 可见、统一 150–300ms 过渡、`prefers-reduced-motion` 降级、`color-scheme: light`
- 公共前台首页 hero 改为浅青底 + 几何装饰 + 琥珀点缀条（区块结构不变）

### 2026-09-25 · 前端列表模块深化（deepen-frontend-list-modules）

- 收敛响应式改造遗留的重复代码：`<MobileCardList>` 移动卡片组件、`<DateRangeCombo>` 日期范围组合筛选、`useCrossPageSelection` 跨页多选（含 Shift 区间与跨页全选）、`utils/listDisplay.ts` 序号/状态口径工具
- 消除约 500 行重复；建立前端测试基线：vitest + @vue/test-utils + happy-dom，21 个单测
- 首建 `CONTEXT.md` 领域术语表

### 2026-09-22 · 全站响应式（responsive-mobile-web）

- 以 768px 统一断点实现桌面/移动双形态：`useBreakpoint`（matchMedia 单例）
- 三处导航壳改汉堡抽屉（公共头部/读者后台/管理后台），读者后台新增底部 Tab 导航
- 约 10 个重表格页实现「桌面表格 / 手机摘要卡片（可展开详情）」双渲染；全局弹窗 ≤768px 降为 92vw，按钮触控区 ≥40px
- 仅前端约 35 个文件，后端零改动；桌面端逐像素保持原布局

### 2026-09-07 · 系统初版（school-library-system）

- 三端一体单页应用：公共前台（检索/详情/公告/活动/规章/自助注册）、读者后台（自助借阅/续借/收藏/罚款缴纳/站内通知/帮助反馈）、管理后台（编目/副本/读者/流通/罚款/公告/活动/帮助管理/统计概览）
- 按副本（book_copy）建模馆藏流通；借阅规则引擎 `CirculationPolicy`
- 认证采用 Spring Session + Redis 服务端会话（Cookie + Bearer 双通道），BCrypt 密码，角色 ADMIN/READER
- Excel 批量导入导出（图书/读者）、逾期定时扫描与罚款、到期站内提醒
- 12 表 schema（幂等建表）+ 幂等种子账号；Knife4j 接口文档；Docker Compose 四容器部署
- （后续口径调整）借期单位由「天」改为「分钟」（10 分钟，演示节奏），迁移脚本 `db/upgrade-loan-minutes.sql`

## 图例

- 本日志按「实施完成日期」倒序记录；每次变更的动机、规格与任务清单见 `openspec/changes/<变更名>/`
