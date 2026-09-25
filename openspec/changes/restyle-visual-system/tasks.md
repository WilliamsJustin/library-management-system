# Tasks: restyle-visual-system

## 1. 令牌层与字体基座

- [x] 1.1 下载 Outfit、Work Sans 可变字重 woff2 至 `frontend/src/assets/fonts/`（校验：文件存在且 <100KB/个，`npm run dev` 无 404）
- [x] 1.2 `style.css` 建立令牌层：`--sl-*` 语义令牌（色彩/圆角/阴影/过渡/字体栈，值取 MASTER.md 与 design D2/D3 分档）+ EP 桥接（`--el-color-primary` 阶梯、danger/warning 映射、按钮黑字），`html { color-scheme: light }`；校验：主按钮呈青绿底黑字，无编译报错
- [x] 1.3 `@font-face` 声明（`font-weight: 300 700`，`font-display: swap`）+ body/标题字体栈切换；校验：断网状态下刷新页面，学号/ISBN/日期/金额以新字体渲染（DevTools Network 无外部字体请求）
- [x] 1.4 检查单全局规则：`a` 与 `.m-expand` 等 cursor:pointer、`:focus-visible` 青绿光环、`--sl-transition` 统一过渡、`prefers-reduced-motion` 降级；校验：Tab 键焦点可见，系统开启减动效后动画停止
- [x] 1.5 跑 `npm test`（21 用例）确认基座不影响组件逻辑；校验：全绿

## 2. 公共前台换肤

- [x] 2.1 `PublicLayout`（头部/导航/底部）与全站锚点配色换令牌；校验：1440/375 截图中导航选中态、链接均为青绿系
- [x] 2.2 `SiteHomeView` hero 微调（design D6 边界内：浅青底+几何装饰、Outfit 标题、琥珀 CTA、卡片阴影统一）；校验：1440 与 375 截图对比区块顺序未变
- [x] 2.3 `site/*` 其余页面（SearchResults/BookDetail/Services/About*/Activities）硬编码色替换为令牌引用；校验：`grep -n "#409eff\|#409EFF" frontend/src/views/site frontend/src/views/PublicLayout.vue` 无结果
- [x] 2.4 前台阶段收尾：375/768/1440 三档截图走查 + `npm test`；校验：对比度抽查项达标、测试全绿、桌面结构未变

## 3. 读者后台换肤

- [x] 3.1 `ReaderLayout`（侧栏/底部 tabbar/抽屉）与 `reader/HomeView` 配色换令牌；校验：底部 tabbar 选中态为青绿
- [x] 3.2 `reader/*` 其余页面（MyLoans/Borrow/Penalty/Favorites/Help）硬编码色替换；校验：`grep -n "#409eff\|#409EFF" frontend/src/views/reader` 无结果
- [x] 3.3 读者后台收尾：三档截图走查 + `npm test`；校验：移动卡片形态与换肤前一致、测试全绿

## 4. 管理后台换肤与收尾

- [x] 4.1 `AdminLayout` 与 `admin/*`（Home/Books/Loans/Penalties/HelpAdmin）硬编码色替换；校验：`grep -n "#409eff\|#409EFF" frontend/src/views/admin` 无结果
- [x] 4.2 其余共享视图与组件（LoginView/RegisterView/ChangePassword/NotFound/ActivityManage/AnnouncementManage/ReaderManagement、MobileCardList/DateRangeCombo/PageBar/FloatingHelp/GlobalReminderPopup/help 组件）替换；校验：全仓 `grep -rn "#409eff\|#409EFF" frontend/src` 无品牌色残留（EP 内部样式除外）
- [x] 4.3 全端验收：375/768/1440 三端截图对比、对比度与检查单逐项核对、`npm test` 全绿；校验：spec「换肤不回归约束」三个场景全部通过
