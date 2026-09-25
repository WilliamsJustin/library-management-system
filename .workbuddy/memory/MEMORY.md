# 学校图书馆借阅系统

## 栈 / 账号
- `frontend/`：Vue3 + Vite + TS + Element Plus 2.14.5 + Pinia + vue-router(history)；主色 `#409eff`。
- `backend/`：Boot 3.2.5 + Java17 + MyBatis-Plus 3.5.7 + MySQL + Log4j2 + Spring Security 6 + Spring Session Redis；包 `com.school.library`；库 `school_library`@127.0.0.1:3306 root/123456。
- 账号 admin1 / student1 / teacher1，密码 pass123；登录字段是 **`account`**。

## 认证（已由 JWT 改为服务端会话）
- 迁移套路与坑位已存技能 **`spring-session-from-jwt`**（`~/.workbuddy/skills/`）。
- ⚠️ **Redis 不通时只有「登录」坏**：只读接口（`/api/books` 等）一切正常，登录返回 **503 `SESSION_STORE_UNAVAILABLE`**（已单独映射，不再落兜底 500）；`SessionStoreStartupCheck` 启动时 ping Redis，不通就 ERROR 告警但**不阻止启动**；`/error` 已 permitAll。
- 前端 `LoginView` 的 catch **必须** `ElMessage.error(errorMessage(err, ...))`（曾漏掉、只 console.error → 点登录「没反应」；全项目仅此一处漏）。
- `AppPrincipal implements UserDetails, Serializable`（会话要 JDK 序列化）。
- 凭证**双通道**：Cookie `LIBRARY_SESSION` + `Authorization: Bearer <sessionId>`，由 `CompositeHttpSessionIdResolver` 统一解析（`config/SessionConfig.java`；靠 `@ConditionalOnMissingBean` 让自定义 resolver 顶掉默认 cookie resolver）。
- 登录/注册 → `LoginSessionManager.startSession()`（写 SecurityContext + 防会话固定）；`POST /api/auth/logout` 作废会话（permitAll、幂等返 200）；`SecurityConfig` 必须 `IF_REQUIRED`（**不能 STATELESS**）+ `requestCache.disable()`。
- 超时：`SessionTimeoutInterceptor` 注册在 `/api/**`（排除 login/register/logout），比对 `APP_LAST_ACTIVITY_AT`，空闲 ≥ `app.session.timeout-minutes`(30) → 401 `SESSION_EXPIRED` 并作废会话；`spring.session.timeout` 是 Redis 兜底。前端 `http.ts` 见 `SESSION_EXPIRED` → `/login?expired=1`。
- 测试不依赖 Redis：`support/InMemorySessionConfig`（**必须带 `@EnableSpringHttpSession`** + `MapSessionRepository`，否则 Boot 整体回退 JSESSIONID）。

## 前端 TS（已全量迁移，勿写 .js）
- ts@5.6 + vue-tsc@2.2 + 单文件 `tsconfig.json`（extends `@vue/tsconfig/tsconfig.dom.json`）；`src/` 禁 .js；`.vue` 全 `<script setup lang="ts">`；类型在 `src/types/index.ts`；错误统一 `errorMessage(err, fallback)`。
- strict 坑：`ref(null)`/`ref([])` 要显式泛型；`catch` 的 err 是 unknown；`http.get<T>()` 写泛型（分页 `PageResult<X>`）；校验前先 `if (!formRef.value) return`；EP 语言包引 `element-plus/es/locale/lang/zh-cn`；类型引一律 `import type`。
- **角色比较一律 `toLowerCase()`**（后端返大写）。`vite.config.ts` 的 `API_PROXY` 同时配 `server.proxy` 与 `preview.proxy`。

## 构建 / 验证
- **不要 `rm -rf dist`**：`vite build --outDir dist-newN` → `mv dist dist-tmpN && cp -r dist-newN dist` → 清临时目录。
- node = `C:/Users/15278/.workbuddy/binaries/node/versions/22.22.2-2/node.exe`；**改完 UI 先 `vue-tsc --noEmit` 0 error 再 build**。
- 后端打包：Git Bash 下 mvn 脚本失效 → 直调 `java -classpath <MVN>/boot/plexus-classworlds-2.9.0.jar -Dclassworlds.conf=<MVN>/bin/m2.conf -Dmaven.home=<MVN> org.codehaus.plexus.classworlds.launcher.Launcher -o package -DskipTests`。
- 真机行为验证（点击/跳转/弹窗/新标签/截图）用 **headless Edge + CDP**，见技能 `edge-cdp-e2e-verify`。
- ⚠️ 本机 `http_proxy/https_proxy=127.0.0.1:2246` → curl 打 localhost 必须 `--noproxy '*'`。
- 慢流程（定时任务/分钟级规则）端到端：建临时库 `school_library_e2e`（`sed 's/school_library/school_library_e2e/g' db/schema.sql`）+ 压缩时间参数起 8081 + Node fetch 断言，**勿动真实库**。
- ⚠️ 用户 IDEA 的 8080 常是**旧构建**（新接口 404/参数被忽略）→ **改完必须提醒用户重启 8080**。

## 后端要点
- 日志：pom 里 5 个 starter **各自**排除 `spring-boot-starter-logging`；配置必须叫 `log4j2-spring.xml`；⚠️ XML 注释内禁止连续 `--`（会让配置静默失效）。
- MyBatis-Plus：**无 `repository/` 包**；分页一律 `common/PageResult.java`（前端发 `?page=0&size=10` 0 基，读 `data.content`/`totalElements`）；枚举存 `name()`；关联用外键 ID + Mapper `@Select` join，**别 N+1**。
- 测试无 ddl-auto：`test/resources/sql/schema-h2.sql` + `spring.sql.init.mode=always`；⚠️ 加表须同步 `db/schema.sql`、`schema-h2.sql`（顶部补 DROP，漏了会让第二个上下文报 already exists）、`db/upgrade-*.sql`。
- Knife4j：`/doc.html`；⚠️ `SecurityConfig.DOC_ENDPOINTS` 必须放行 `/doc.html`、`/webjars/**`、`/swagger-ui/**`、`/v3/api-docs/**`（文档 401/白屏先查这里）。
- ⚠️ `spring.jackson.default-property-inclusion: non_null` → null 字段不出现在 JSON，前端可空字段一律声明可选。

## 借阅规则（单位=分钟）
- `app.library.*`：`loan-minutes:10`（学生/教师一致）、`fine-per-minute:0.10`、`reminder-minutes:5`、`check-interval-ms:30000`、`scheduling-enabled:true`。
- ⚠️ `loan.due_date` 是 **DATETIME**；`LoanResponse.dueDate` 是 `LocalDateTime`，前端列名「应还时间」走 `formatDateTime()`。
- 罚款 = `finePerMinute × 分钟差`（不足 1 分钟按 1 分钟，`setScale(2, HALF_UP)`）；到期提醒「只发一次」靠 `loan.due_reminder_sent`，**续借会重置**。
- `OverdueTask` 业务 `checkOnce()` 与触发器 `OverdueTask.Scheduler` 拆开，后者带 `@ConditionalOnProperty(scheduling-enabled)`；⚠️ **测试必须 `scheduling-enabled:false`**（否则 30s 轮询抢测试事务）。

## 共享组件 / 约定
- `PageBar.vue` 统一分页条；`BookSearchBar.vue` 共用检索框（3 处引用，**只改这一个**；清空(×)不自动检索）。
- `utils/navigation.ts` → `openInNewTab()`：凡「不覆盖当前页」的跳转都走它。
- `composables/useIdleTimeout.ts` 30 分钟空闲（**仅前端预警**，真超时以后端为准）。
- 公共页在 `views/site/`，布局 `PublicLayout.vue`；「本馆概况」用 `AboutSubNav.vue` 胶囊导航。

## 前台检索 / 详情 / 收藏
- `/search`、`/books/:id` 挂 PublicLayout；检索条件写回 query；详情页右侧固定 10 行，无封面用 CSS 渐变占位；面包屑只 `{来源} / {书名}`（**无「首页」一级**）。
- 详情页 3 入口（`/search` 条目、借阅页卡片 `?from=borrow`、收藏页行），**三处共用，别再复制后台版**。
- 借阅/收藏**就地执行、不跳转**；未登录提示去 `/login?redirect=`；管理员只提示换读者账号、不跳转。
- `BorrowDialog.vue` 共用副本弹窗（`defineExpose({ open(book) })`）；`POST /api/loans/self` 必须带 `copyId`/`barcode`；⚠️ 借阅记录在借状态是 **`ACTIVE`**（`BORROWED` 是副本状态）。
- 收藏：表 `favorite(reader_id, book_id)`；`GET /api/favorites/my` → `PageResult<BookResponse>`（书目+副本统计，按收藏时间倒序）；前端 `stores/favorites.ts` + `composables/useBookActions.ts`。
- 卡片既「整块可点进详情」又有按钮时，按钮必须 `@click.stop`。

## 易混淆的页面命名
- 「借阅查询」= **读者端** `reader/MyLoansView.vue`（无分页，固定 `size:50`）；「借阅流通」= **后台** `admin/LoansView.vue`（有 PageBar，首列借阅ID）。
- `admin/loans` 列序含 读者/账号/学号·工号/类型；`LoanResponse` 有 `readerNo`/`readerType`；`GET /api/loans` 支持 `readerType` 筛选。

## ⚠️ 本机两个坑
- **D: 盘回收站不可用** → D: 上 `rm -rf`/`Remove-Item`/`fs.rmSync` 全删不掉；可靠办法：先 `mv` 到 C: 临时目录再删。
- 同一条消息里对**同一文件**并行多个 Edit 会「报 success 但磁盘没改」→ 串行改或整份 `Write` 重写，改完 grep/Read 复核。
