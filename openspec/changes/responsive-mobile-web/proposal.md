# Proposal: 全站响应式（电脑端 + 手机端网页版）

## Why

当前系统（公共前台 + 读者后台 + 管理后台）全部按桌面浏览器设计：头部横向导航、200px 固定侧栏、`el-table` 宽表格、写死 px 宽的卡片与弹窗。师生用手机浏览器访问时，导航挤压溢出、读者服务卡片（350px）超出 375px 屏宽、560px 规则弹窗被裁切、借阅查询等表格无法完整查看，基本不可用。本次改造让同一套网站在手机宽度下自动切换为手机版，同时电脑端布局一行不变。

## What Changes

- 建立全站响应式地基：新增 `useBreakpoint` 组合式函数（`matchMedia('(max-width: 768px)')` 响应式 `isMobile`）；`style.css` 增加全局手机规则（`el-dialog` 92vw、分页简化、表单 label 上浮、小屏点击区域）
- 统一断点约定为 **768px**：全部手机样式只写在 `@media (max-width: 768px)` 内，桌面样式零改动（桌面优先、纯增量）；收敛现有的 900px（检索页）与 720px（详情页）两个旧断点；不设平板档（769–1024px 按电脑版显示）
- 三处导航壳改造（ReaderLayout 与 AdminLayout 骨架相同，一套模式覆盖）：
  - 公共头部：≤768px 隐藏横向导航，改汉堡按钮 + 抽屉菜单；页脚纵向堆叠
  - 读者后台：侧栏菜单改为汉堡抽屉；新增底部 Tab（首页/图书借阅/借阅查询/我的罚款四 + 我的入口）
  - 管理后台：侧栏菜单改为汉堡抽屉（底部不放 Tab，保留顶部实时咨询角标与进入前台入口）
- 约 10 个 `el-table` 重表格页面（图书编目、读者管理、借阅流通、逾期罚款、公告/活动管理、借阅查询、我的罚款、帮助后台留言）实现「桌面表格 / 手机卡片」双渲染：≤768px 时渲染摘要卡片（核心字段 + 操作按钮），重列页面（12–20 列）提供「展开详情」查看全部字段；筛选工具栏纵向堆叠
- 纯布局页面（首页、读者服务、读者活动、本馆概况、图书检索、图书详情、登录/注册/修改密码）通过媒体查询改单列堆叠、字号收缩、卡片 100% 宽
- 全局弹窗（逾期规则、公告/活动详情、逾期提醒、Excel 导入、封面预览）在 ≤768px 降为 92vw，不出现横向滚动条
- **BREAKING**（仅手机端视觉，不影响桌面）：≤768px 下表格行不再是表格行而是卡片；读者后台/管理后台侧栏从常驻改为抽屉呼出

## Capabilities

### New Capabilities
- `responsive-ui`: 全站响应式呈现规格——断点约定、导航壳（公共头部汉堡抽屉 / 两端后台抽屉 / 读者后台底部 Tab）、表格卡片化双渲染与展开详情、弹窗与小屏适配规则

### Modified Capabilities
（无——`openspec/specs/` 尚无已归档主规格；本变更为全新能力规格，不修改任何既有需求）

## Impact

- **影响代码（仅前端，约 35 个文件）**：
  - 新增：`frontend/src/composables/useBreakpoint.ts`、`frontend/src/components/LayoutSideMenu.vue`（桌面 aside 与手机抽屉同源菜单）
  - 壳层：`PublicLayout.vue`、`reader/ReaderLayout.vue`、`admin/AdminLayout.vue`、`components/FloatingHelp.vue`、`components/GlobalReminderPopup.vue`、`components/PageBar.vue`、`components/BookSearchBar.vue`
  - 公共前台：`site/SiteHomeView.vue`、`site/ServicesView.vue`、`site/ActivitiesView.vue`、`site/SearchResultsView.vue`、`site/BookDetailView.vue`、`site/AboutIntroView.vue`、`site/AboutRulesView.vue`、`site/AboutFloorsView.vue`、`site/AboutRuleDetailView.vue`、`site/RegisterView.vue`、`LoginView.vue`、`ChangePasswordView.vue`
  - 读者后台：`reader/HomeView.vue`、`reader/BorrowView.vue`、`reader/FavoritesView.vue`、`reader/MyLoansView.vue`、`reader/PenaltyView.vue`、`reader/HelpView.vue`
  - 管理后台：`admin/HomeView.vue`、`admin/BooksView.vue`、`ReaderManagementView.vue`、`admin/LoansView.vue`、`admin/PenaltiesView.vue`、`admin/HelpAdminView.vue`、`AnnouncementManageView.vue`、`ActivityManageView.vue`
- **后端**：零改动，无 API 变更，无数据库变更（因此无 DDL / 回滚脚本 / 接口联调项）
- **依赖**：不引入新依赖；断点切换复用浏览器原生 `matchMedia`，Element Plus 组件用法不变
- **验收方式**：`pnpm run type-check` 零报错；在 375 / 390 / 768 / 1440 四档宽度逐路由截图走查，1440 档与改造前逐像素一致（桌面回归）
