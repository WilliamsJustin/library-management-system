# Tasks: 全站响应式（电脑端 + 手机端网页版）

> 依赖顺序：第 1 期地基 → 第 2 期三处壳 → 第 3/4/5 期页面（可并行）→ 第 6 期验收。每期结束跑一次 `pnpm run type-check`（工作目录 `frontend`）。规格见 `specs/responsive-ui/spec.md`，技术决策见 `design.md`（D1–D9）。

## 1. 响应式地基

- [x] 1.1 [Frontend] 新增 `frontend/src/composables/useBreakpoint.ts`：基于 `matchMedia('(max-width: 768px)')` 导出响应式 `isMobile`，setup 同步初始化并监听 change 事件；验证：`pnpm run type-check` 通过，且 375px 视口下 `isMobile === true`、1440px 下为 `false`
- [x] 1.2 [Frontend] 修改 `frontend/src/style.css`：新增 `@media (max-width: 768px)` 全局块——`.el-dialog { width: 92vw !important }`、隐藏 `.el-pagination__jump`/`.el-pagination__sizes`、`.el-button--small` 最小点击区 40px、`.el-table` 容器横向滚动安全网；验证：任选一个弹窗页面在 375px 下弹窗约 92vw 且无页面横向滚动
- [x] 1.3 [Frontend] 新增 `frontend/src/components/LayoutSideMenu.vue`：接收菜单项数组（index/图标/文案/可见条件），内置激活态与路由跳转，桌面/抽屉两形态共用；验证：分别以桌面与抽屉方式渲染 8 项菜单，激活项高亮一致、点击跳转一致
- [x] 1.4 [Frontend] 封装 `.m-card` 移动端卡片样式与「展开详情」交互规范（摘要字段行 + 定义列表展开 + 操作按钮区，卡片全局 class 放入 `style.css` 或独立样式文件）；验证：用样例数据渲染一张含展开详情的卡片，375px 下无溢出、展开/收起正常、操作按钮区可点

## 2. 三处导航壳

- [x] 2.1 [Frontend] 修改 `frontend/src/views/PublicLayout.vue`：≤768px 隐藏横向 `.nav` 与登录/注册横排，新增汉堡按钮 + `el-drawer`（含全部一级导航、「本馆概况」三个子项平铺、登录/注册或进入后台/用户菜单），页脚 `.footer-info` 纵排并去竖线分隔；验证：375px 点击汉堡展开抽屉、点导航项跳转后抽屉关闭；1440px 布局与改造前一致（依赖 1.1、1.3）
- [x] 2.2 [Frontend] 修改 `frontend/src/views/reader/ReaderLayout.vue`：`el-aside` 桌面常驻（`v-if="!isMobile"`），菜单数据经 `LayoutSideMenu` 渲染进手机 `el-drawer`，头部加汉堡按钮；验证：375px 抽屉含全部菜单项（含教师专属项按身份显隐）、当前页高亮正确、跳转后抽屉关闭（依赖 1.1、1.3）
- [x] 2.3 [Frontend] 修改 `frontend/src/views/reader/ReaderLayout.vue`：新增固定底部 Tab（首页/图书借阅/借阅查询/我的罚款 + 「我的」展开其余菜单），`el-main` 加 Tab 高度内边距与 `env(safe-area-inset-bottom)`；验证：375px 下 Tab 切换与高亮生效、滚动到页面底部内容不被遮挡、「我的」可见我的收藏/帮助与反馈/（教师）发布公告/活动管理（依赖 2.2）
- [x] 2.4 [Frontend] 修改 `frontend/src/views/admin/AdminLayout.vue`：aside 改抽屉（同 2.2 模式，不加底部 Tab），头部保留实时咨询角标、「进入前台」、用户下拉；验证：375px 有未回复消息时角标仍显示且点击进入 `/admin/help?tab=...` 正确页签（依赖 1.1、1.3）
- [x] 2.5 [Frontend] 修改 `frontend/src/views/PublicLayout.vue`、`ReaderLayout.vue`、`AdminLayout.vue`：校验跨断点缩放时抽屉/Tab 无残留（如从 375px 拖到 1440px 时抽屉遮罩不卡留）；验证：对应 spec「跨越断点缩放窗口」场景（依赖 2.1–2.4）

## 3. 公共前台页面

- [x] 3.1 [Frontend] 修改 `frontend/src/views/site/SiteHomeView.vue`：≤768px Hero 区字号/内边距收缩、公告列表日期换行不挤压标题；验证：375px 无横向滚动，1440px 截图与改造前一致（依赖 1.2）
- [x] 3.2 [Frontend] 修改 `frontend/src/views/site/ServicesView.vue`：`.service-card` 在 ≤768px 改 `width:100%`（桌面保持 350px 不动）；验证：375px 四张卡片各占一行，规则弹窗以 92vw 完整显示（依赖 1.2）
- [x] 3.3 [Frontend] 修改 `frontend/src/views/site/ActivitiesView.vue`：≤768px 时间线与卡片内边距收缩、活动详情弹窗由全局 92vw 规则覆盖；验证：375px 走查无溢出（依赖 1.2）
- [x] 3.4 [Frontend] 修改 `frontend/src/views/site/AboutIntroView.vue`、`AboutRulesView.vue`、`AboutFloorsView.vue`、`AboutRuleDetailView.vue`：≤768px 图文/表格改单列堆叠；验证：375px 四页均无横向滚动（依赖 1.2）
- [x] 3.5 [Frontend] 修改 `frontend/src/views/site/SearchResultsView.vue`：将旧 900px 断点收敛为 768（≤768px 侧栏全宽纵排、筛选区纵向堆叠）；验证：375px 检索结果单列可用，769–900px 按电脑版呈现且不溢出（依赖 1.2）
- [x] 3.6 [Frontend] 修改 `frontend/src/views/site/BookDetailView.vue`：将旧 720px 断点收敛为 768（≤768px 封面居中、信息区全宽）；验证：375px 详情页无横向滚动，1440px 布局不变（依赖 1.2）
- [x] 3.7 [Frontend] 修改 `frontend/src/views/LoginView.vue`、`site/RegisterView.vue`、`ChangePasswordView.vue`：≤768px 表单元素自适应（输入框全宽、按钮点击区 ≥40px），必要时 `label-position="top"`；验证：375px 完成一次登录、一次注册表单提交流程（依赖 1.1、1.2）
- [x] 3.8 [Frontend] 修改 `frontend/src/components/FloatingHelp.vue`：≤768px 面板 `max-width` 收缩为视口内可用宽度、高度不超视口；验证：375px 打开悬浮球面板，常见问题/人工咨询页签可切换、FAQ 检索与转人工可走通（依赖 1.2）
- [x] 3.9 [Frontend] 修改 `frontend/src/components/GlobalReminderPopup.vue`：确认逾期提醒弹窗由全局 92vw 规则覆盖，必要时微调内容布局；验证：375px 下提醒弹窗完整显示、按钮可点（依赖 1.2）

## 4. 读者后台页面

- [x] 4.1 [Frontend] 修改 `frontend/src/views/reader/HomeView.vue`：≤768px 概览卡片与通知盒单列堆叠、欢迎组件信息紧凑；验证：375px 首页布局完整、无横向滚动（依赖 2.3）
- [x] 4.2 [Frontend] 修改 `frontend/src/views/reader/BorrowView.vue`：检索栏与图书结果网格在 ≤768px 自适应（多列网格降为 2 列）；验证：375px 可完成检索与打开借阅弹窗（依赖 1.2）
- [x] 4.3 [Frontend] 修改 `frontend/src/views/reader/FavoritesView.vue`：≤768px 收藏卡网格 2 列、借阅副本弹窗 92vw；验证：375px 收藏/取消收藏与弹出借阅窗可走通（依赖 1.2）
- [x] 4.4 [Frontend] 改造 `frontend/src/views/reader/MyLoansView.vue`：借阅查询表格在 ≤768px 渲染卡片列表（书名/ISBN/条形码/状态/借出、应还、归还时间 + 操作），筛选区纵向堆叠，分页保持；验证：375px 对应 spec「借阅查询手机卡片」场景；筛选后翻页条件不丢（依赖 1.1、1.4）
- [x] 4.5 [Frontend] 改造 `frontend/src/views/reader/PenaltyView.vue`：我的罚款表格手机端卡片化（含 ISBN、图书、条形码、状态、时间、金额），规则提示图标保留可点；验证：375px 罚款列表与在线缴纳入口可用（依赖 1.1、1.4）
- [x] 4.6 [Frontend] 修改 `frontend/src/views/reader/HelpView.vue`：≤768px FAQ/人工咨询/留言三页签与对话输入区自适应；验证：375px 可发送一条咨询消息并收到渲染（依赖 1.2）

## 5. 管理后台页面

- [x] 5.1 [Frontend] 修改 `frontend/src/views/admin/HomeView.vue`：≤768px 概览卡片等宽网格降为单列/两列、待咨询行紧凑；验证：375px 仪表盘完整可读（依赖 2.4）
- [x] 5.2 [Frontend] 改造 `frontend/src/views/admin/BooksView.vue`：图书编目表格手机端卡片化（封面缩略图 + 书名/ISBN/状态/副本 + 操作，>8 列按 spec 加「展开详情」），编辑/上传封面/Excel 导入等弹窗 `label-position="top"` 且宽度走全局规则，批量上架/下架/删除入口保留；验证：375px 完整走通「编辑一本书→保存」「导入弹窗打开→下载模板」「批量勾选 2 本书→下架」三条流程（依赖 1.1、1.4）
- [x] 5.3 [Frontend] 改造 `frontend/src/views/ReaderManagementView.vue`：读者表格手机端卡片化（账号/学号/姓名/类型/手机号/状态 + 行内操作），重置密码、批量重置/启用/停借、Excel 导入弹窗可用；卡片保留逐条多选（Shift 区间选择退化为逐条点选，跨页选择集合逻辑不变）；验证：375px 走通「单条重置密码」「勾选两卡→批量停借→恢复」流程（依赖 1.1、1.4、5.2 同批操作模式）
- [x] 5.4 [Frontend] 改造 `frontend/src/views/admin/LoansView.vue`：12 列借阅流通表手机端卡片化（账号/学号/读者/书名/状态/三个时间 + 续借与操作），筛选区纵向堆叠，>8 列加「展开详情」展示 ISBN、条形码等；验证：375px 对应 spec 场景，关键词搜索与状态下拉可用，归还/续借可操作（依赖 1.1、1.4）
- [x] 5.5 [Frontend] 改造 `frontend/src/views/admin/PenaltiesView.vue`：逾期罚款表手机端卡片化（账号/学号/读者/ISBN/金额/状态/时间），罚金规则提示图标保留；验证：375px 罚款列表与筛选可用（依赖 1.1、1.4）
- [x] 5.6 [Frontend] 改造 `frontend/src/views/admin/HelpAdminView.vue`：留言/会话列表手机端卡片化 + 行内回复弹窗 92vw + FAQ 管理编辑弹窗可用；验证：375px 走通「打开一条留言→回复→刷新状态」（依赖 1.1、1.4）
- [x] 5.7 [Frontend] 改造 `frontend/src/views/AnnouncementManageView.vue` 与 `frontend/src/views/ActivityManageView.vue`（管理后台与教师共用）：表格手机端卡片化 + 发布/编辑弹窗自适应；验证：375px 走通「发布一条公告」「编辑一条活动」（依赖 1.1、1.4）

## 6. 验收

- [x] 6.1 [Frontend] 全量执行 `pnpm run type-check`，要求零报错；验证：命令退出码为 0（依赖 3–5 期全部完成）
- [x] 6.2 [Frontend] 按 `specs/responsive-ui/spec.md` 场景清单在 375px 逐条走查（含抽屉、底部 Tab、卡片、展开详情、弹窗、悬浮帮助）；验证：每条场景可复现且通过，记录走查结果
- [x] 6.3 [Frontend] 390px 视图走查 + 768px 边界往返缩放（375↔1440 拖拽）无形态残留；验证：对应 spec「跨越断点缩放窗口」场景
- [x] 6.4 [Frontend] 1440px 桌面回归：对全部路由截图并与改造前同路由截图逐一比对，允许零差异；验证：桌面保持不变的硬约束达成，差异项逐条修复后复跑
- [x] 6.5 [Frontend] 生产构建冒烟：`pnpm build` 成功后 `pnpm preview`，抽查公共首页、登录、读者后台、管理后台四类路由在 375px 与 1440px 的呈现；验证：构建产物两档宽度均正常
