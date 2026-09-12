# 学校图书馆借阅系统 — 项目备忘

## 栈 / 账号
- 前端 `frontend/`：Vue3 + Vite + **TS** + Element Plus 2.14.5 + Pinia + vue-router(history)；主色 `#409eff`，留白 `#f5f7fa`。
- 后端 `backend/`：Boot 3.2.5 + Java17 + **MyBatis-Plus 3.5.7** + MySQL + Log4j2，包 `com.school.library`；库 `school_library`@127.0.0.1:3306 root/123456。
- 账号 `admin1`/`student1`/`teacher1`，密码均 `pass123`；登录字段是 **`account`**。
- 公共页在 `views/site/`，布局 `PublicLayout.vue`（单栏无侧栏）；「本馆概况」用 `site/AboutSubNav.vue` 胶囊导航。

## 前端 TS（已全量迁移，勿写 .js）
- ts@5.6 + vue-tsc@2.2 + **单文件 `tsconfig.json`**（extends `@vue/tsconfig/tsconfig.dom.json`，`paths @/*`，**不用 project references**）；`env.d.ts` 只放 vite/client 引用。
- `src/` 禁 .js；`.vue` 全 `<script setup lang="ts">`；类型在 `src/types/index.ts`；错误统一 `errorMessage(err, fallback)`（`utils/error.ts`）。
- **改完 UI 必须 `vue-tsc --noEmit` 0 error 再 build**（命令见「构建 / 验证」）。
- strict 坑：① `ref(null)`/`ref([])` 显式泛型，否则模板报 `never`；② `catch` 的 err 是 unknown → `errorMessage()`；③ `http.get<T>()` 写泛型（分页 `PageResult<X>`）；④ 校验前先 `if (!formRef.value) return`；⑤ EP 语言包引 `element-plus/es/locale/lang/zh-cn`（**别引** `dist/locale/*.mjs`）；⑥ 类型引一律 `import type`。
- 曾修 bug：`auth.ts` 的 `setSession` 漏 return；`PublicLayout.vue` 用 `userRole === 'admin'` 比较（后端返大写）→ **角色比较一律 `toLowerCase()`**。
- `vite.config.ts` 的 `API_PROXY` **同时**配 `server.proxy` 与 `preview.proxy`。
- ⚠️ 同一条消息里对**同一文件**并行多个 Edit 会「报 success 但磁盘没改」→ 串行改，或改完 grep 复核。

## 构建 / 验证
- **不要 `rm -rf dist`**：`vite build --outDir dist-newN` → `mv dist dist-tmpN && cp -r dist-newN dist` → 清临时目录。
- node = `C:/Users/15278/.workbuddy/binaries/node/versions/22.22.2-2/node.exe`：
  `cd frontend && "$node" node_modules/vue-tsc/bin/vue-tsc.js --noEmit`；同路径 `node_modules/vite/bin/vite.js build --outDir dist-newN`。
- 真机行为验证（点击/跳转/弹窗/新标签/截图）用 **headless Edge + CDP**；模板与坑位见技能 **`edge-cdp-e2e-verify`**（`~/.workbuddy/skills/`），**别重复踩**。速记：`vite preview --port 4173 --strictPort`（`API_PROXY_TARGET` 可指 8081）。
- ⚠️ 本机 `http_proxy/https_proxy=127.0.0.1:2246` → curl 打 localhost 必须 `--noproxy '*'`。
- **慢流程（定时任务 / 分钟级规则）端到端验法，勿动用户真实库**：`sed 's/school_library/school_library_e2e/g' backend/.../db/schema.sql | mysql ...` 建临时库（脚本写死了 `CREATE DATABASE`/`USE`）→ 用新打的 jar 起 8081 覆盖参数压缩时间：`--spring.datasource.url=...e2e --app.library.loan-minutes=1 --app.library.reminder-minutes=1 --app.library.check-interval-ms=5000` → Node fetch 跑断言 → `DROP DATABASE`。断言「只发一次」须按 `id > 基线最大值` 过滤新增，否则读到上轮残留。

## 后端
### 打包 / 运行
- 打包（Git Bash 下 mvn 脚本因 MSYS 路径转换失败，须直调 java + Launcher）：
  `cd backend && java -classpath "D:/Software/Programming/myEnvironment/Maven/apache-maven-3.9.12/boot/plexus-classworlds-2.9.0.jar" "-Dclassworlds.conf=$MVN_HOME/bin/m2.conf" "-Dmaven.home=$MVN_HOME" org.codehaus.plexus.classworlds.launcher.Launcher -q -o package -DskipTests`（跑测试换成 `test`）。
- ⚠️ **反复踩**：用户 IDEA 的 8080 常是**旧构建**（新接口 404/500、参数被忽略、`dueDate` 仍按日期返回）→ **改完必须提醒用户重启 8080**。
- 自验另起 `--server.port=8081`；`nohup &` 会随 shell 退出消失 → 用受管后台任务。启动后先查 stderr 有无 `[Fatal Error]`，再看 `logs/`。
### 日志 Log4j2
- `pom.xml`：**5 个 starter 各自**排除 `spring-boot-starter-logging`（exclusion 只作用于自身传递树），再引 `spring-boot-starter-log4j2`。
- `log4j2-spring.xml`（**必须 -spring 后缀**）+ `application.yml` 的 `logging.config`；输出 控制台彩色 / `logs/school-library.log`（按天+100MB，gz 进 history/，留 30 天）/ `-error.log`（仅 ERROR）；异步用原生 `<Async>`（**不需要 Disruptor**）。
- ⚠️ ① **XML 注释内禁止连续 `--`** → 整个配置**静默失效**（退回默认、不报错）；② `<Property>` 的 name 不能与它引用的 lookup key 同名。
### 持久层 MyBatis-Plus（已由 JPA 全量迁移）
- **无 `repository/` 包，别再建 JPA Repository**；Mapper 在 `mapper/`、实体在 `entity/`（已无 `@Entity`）。
- **分页契约硬约束**：前端读 `data.content`/`totalElements`、发 `?page=0&size=10`（0 基）→ 一律 `common/PageResult.java`（复刻 JPA `Page` 字段，`number`=current-1）+ Controller `int page,int size`，**别返回 `Page`/`IPage`**；plugin 在 `config/MybatisPlusConfig.java`（`@MapperScan`，拦截器顺序 乐观锁→分页）。
- **枚举存 `name()`(VARCHAR)** 兼容旧数据 → `default-enum-type-handler: org.apache.ibatis.type.EnumTypeHandler`。
- **关联用外键 ID**（`Long bookId` + `@TableField`），跨表详情写 Mapper `@Select` join；批量统计用 `<script>` IN 查询（**不要 N+1**）；`map-underscore-to-camel-case: true` 已开。
- `BookCopy`/`Loan` 有 `@Version` 乐观锁；`updateById` 返 0 = 版本冲突。
- 测试无 ddl-auto：`src/test/resources/sql/schema-h2.sql`（BOOLEAN 代 TINYINT(1)、独立 CREATE INDEX）+ `spring.sql.init.mode=always`；用 `TestData` + Mapper 造数。
- ⚠️ **加表/改表同步三处**：`db/schema.sql`、`test/.../sql/schema-h2.sql`、`db/upgrade-*.sql`；且 **schema-h2.sql 顶部 `DROP TABLE IF EXISTS` 必须补一行** —— 脚本每个 Spring 上下文都重跑、H2 靠 `DB_CLOSE_DELAY` 长驻 JVM，漏 DROP 会在第二个上下文报 `Table "xxx" already exists`（报错指向 CREATE，易误判；症状是整片 `Failed to load ApplicationContext`）。
- MySQL 中文：heredoc 内联会乱码 → 写 SQL 文件后 `mysql ... --default-character-set=utf8mb4 < f.sql`，用 `CHAR_LENGTH()` 校验；BCrypt 哈希由 Anaconda python 生成后把 `$2b$` 改 `$2a$`。

## 接口文档 Knife4j
- `knife4j-openapi3-jakarta-spring-boot-starter:4.5.0`（内 springdoc 2.3.0）；`/doc.html`、`/swagger-ui/index.html`、`/v3/api-docs`；必须 `knife4j.enable: true`。
- ⚠️ `SecurityConfig.DOC_ENDPOINTS` 必须放行 `/doc.html`、`/webjars/**`、`/swagger-ui/**`、`/v3/api-docs/**` —— **文档 401/白屏先查这里**。

## 共享组件 / 约定
- `components/PageBar.vue` 统一分页条（跳页框默认空；App.vue 把「前往」改「跳至」）。
- `components/BookSearchBar.vue` **共用检索框**，3 处引用：`/`、`/search`、`/reader/borrow`；`v-model:keyword`/`v-model:field` + `@search`。**只改这一个组件**；仅回车/点按钮时 emit，**清空(×)不自动检索**。
- `utils/navigation.ts` → `openInNewTab(path, query)`：**凡「不覆盖当前页」的跳转都走它**（自动拼 `BASE_URL`、跳过空参数）。
- `composables/useIdleTimeout.ts` 空闲超时 `IDLE_MINUTES=30`（10s 巡检；事件须 `{passive,capture:true}`，scroll 不冒泡；纯前端）。
- `src/data/rules.ts` 规章制度数据；`src/utils/dateUtils.ts` 日期格式化。

## 前台检索 / 详情 / 收藏
- `/search`(检索结果)、`/books/:id`(详情) 挂 `PublicLayout`；检索条件写回 query（`keyword/field/category/publisher/page`）；结果页左侧分类/出版社侧边导航（`/books/categories`、`/books/publishers`）+ 右侧结果 + PageBar。
- 详情页：左封面（无 `coverUrl` 用 CSS 渐变占位），右侧固定 10 行：ISBN / 书名 / 作者-译者 / 出版社 / 出版日期 / 分类 / 语言 / 定价 / 封面 / 简介。
- 详情页 3 入口：`/search` 条目、`/reader/borrow` 卡片（`?from=borrow`，面包屑二级换成「图书借阅」）、`/reader/favorites` 行；**有意挂 PublicLayout 下**（从后台点进去会换掉后台布局，靠面包屑返回），三处共用，**别再复制后台版**。面包屑 `<nav class="crumb">` 只两项 `{来源} / {书名}`（**用户要求删掉「首页」一级**）。
- **新标签页打开（用户要求）**：首页点「检索」/馆藏卡片、借阅页点「检索」/书目 → 全部 `openInNewTab`，**当前页不被覆盖**。副作用：借阅页检索框不再本页过滤（列表改由 URL query 决定；页内 `search()` 保留给 `?keyword=&field=` 入口和借完后刷新）。
- **借阅/收藏一律就地执行、不跳转（用户要求）**：`/search`、`/books/:id` 点借阅→页内弹副本框直接借出；点收藏→直接调接口增删、按钮就地切换（**取消收藏带二次确认**）。未登录→提示去 `/login?redirect=<fullPath>`；管理员→只提示换读者账号、**不跳转**（否则守卫弹回 /admin 死循环）。
- `components/BorrowDialog.vue` **共用副本选择弹窗**：`defineExpose({ open(book) })`，成功 `emit('borrowed')` 由父刷新。3 处引用（借阅页原内联弹窗已删，**别再加回去**）。
- `POST /api/loans/self` 必须带 `copyId`/`barcode` → **没有「一键借出」接口**。⚠️ **借阅记录在借状态是 `ACTIVE`**（`BORROWED` 是**副本**的状态）。
- `book` 已扩列 `publish_date/language/price/cover_url/description`（老库用 `db/upgrade-book-metadata.sql`）；后台编目表单与列表序号已同步。
- ⚠️ 后端 `spring.jackson.default-property-inclusion: non_null` → **null 字段不出现在 JSON**，前端可空字段一律声明可选。
- ⚠️ 卡片既「整块可点进详情」又有按钮时，按钮必须 `@click.stop`。
- **我的收藏**：表 `favorite(reader_id, book_id, created_at)`，唯一键 `(reader_id, book_id)`。接口（需登录）：`GET /api/favorites/my?page&size` → `PageResult<BookResponse>`（**是书目+副本统计，不是收藏记录**）、`/my/ids`、`POST|DELETE /api/favorites/{bookId}`（**都幂等**）、`/{bookId}/status`。列表**按收藏时间倒序**（`loadBooksInOrder`：`selectBatchIds` 后重排；副本统计复用 `bookService.toResponses`，**别重写统计**）。
- 收藏前端：`stores/favorites.ts`、`composables/useBookActions.ts`（`ensureReader`/`toggleFavorite`/`isFavorited`/`goBorrow`）——**公共页的借阅/收藏统一走它**；`goBorrow` 现**只有「我的收藏」页在用**。`auth.logout()` 调 `favoriteStore.reset()`。页面 `/reader/favorites`。种子 `DataSeeder.seedFavorites()`；线上 `db/upgrade-favorite.sql`。

## 借阅规则（单位=分钟，用户要求）
- 全在 `app.library.*`（`CirculationPolicy` 读，**别再写死天数**）：`loan-minutes:10`（学生/教师**一致**）、`fine-per-minute:0.10`、`reminder-minutes:5`、`check-interval-ms:30000`、`scheduling-enabled:true`。
- ⚠️ **`loan.due_date` 已是 DATETIME**（原 DATE，天粒度表达不了 10 分钟借期）。三处同步：`db/schema.sql`、`schema-h2.sql`（`due_reminder_sent` 用 BOOLEAN）、`db/upgrade-loan-minutes.sql`（`MODIFY due_date DATETIME` + `ADD due_reminder_sent`，**老库必跑**）。
- 罚款 = `finePerMinute × ChronoUnit.MINUTES.between(dueDate, now)`（**不足 1 分钟按 1 分钟**），`setScale(2, HALF_UP)`；`PenaltyServiceImpl.settleOverdue` 兼管幂等（`uk_penalty_loan`）与停借。
- 到期提醒「只发一次」靠 **`loan.due_reminder_sent`**；**续借会重置该标记**（新到期时间重新享有一次）。文案含「还剩 N 分钟」。
- `task/OverdueTask.java`：业务 `checkOnce()` 与定时触发器**拆开**——触发器是内部静态类 `OverdueTask.Scheduler`，带 `@ConditionalOnProperty(app.library.scheduling-enabled, matchIfMissing=true)`。⚠️ **测试必须 `scheduling-enabled:false`**（30s 轮询会在上下文启动后立刻跑并抢测试事务，断言随机失败），测试改显式调 `checkOnce()`；`LibraryApplicationTests.schedulerIsDisabledInTests` 守住这条。
- ⚠️ `LoanResponse.dueDate` 由 `LocalDate` 变 `LocalDateTime`；前端三个列表（`admin/LoansView`、`reader/MyLoansView`、`reader/RenewView`）列名「应还日期」→「**应还时间**」并走 `formatDateTime(row.dueDate)`。**旧构建的 8080 仍按日期返回** → `new Date('2026-10-11')` 按 UTC 解析、前端显示 `08:00`；重启 8080 后即正常。

## 借阅流通列表的读者信息（用户要求）
- `admin/loans` 表格列序：借阅ID / 读者(姓名) / **账号** / **学号/工号** / **类型** / 书名 / 条形码 / 借出·应还·归还时间 / 续借 / 状态 / 操作。三列紧随「读者」之后，文案与「读者管理」页对齐（列名用「学号/工号」，学生标签 `info`、教师 `success`）。
- 契约：`LoanResponse` 增 `readerNo`、`readerType`（枚举 `ReaderType`），由 `LoanMapper.DETAIL_SELECT` 的 `r.student_no AS reader_no` / `r.type AS reader_type` 自动映射（**靠 mp 的 `EnumTypeHandler` 转 `STUDENT/TEACHER` 字符串，已验证**）。
- 筛选：`GET /api/loans` 增 `readerType` 参数（`ReaderType` 枚举，与 `/readers?type=` 一致）→ `selectDetailPage(..., readerType)` 内 `<if>AND r.type = #{readerType}</if>`。`getMyLoans` 传 `null`。前端「类型」下拉与读者管理页同构（学生/教师）。

## 易混淆的页面命名（歧义时先确认）
- 「借阅查询」= **读者端** `views/reader/MyLoansView.vue`（/reader/my-loans，标题「我的借阅」，**无分页**，固定 `size:50`）。
- 「借阅流通」= **后台** `views/admin/LoansView.vue`（/admin/loans，有 PageBar，首列「借阅ID」）。
- 用户口语的「后台」不一定等于 admin 端（曾出现「后台借阅查询界面」实指读者端页面）。

## ⚠️ 本机删除限制：D: 盘回收站不可用
- `rm/unlink/rmdir` 被 `safe-bin` shim 包装成「先入回收站、失败即 fail-closed」，D: 盘回收站操作必失败 → **D: 上 `rm -rf`、`fs.rmSync`、`Remove-Item` 全删不掉**（C: 正常）。`mv` 未被 shim。
- 可靠办法：先 `mv` 到 C: 临时目录再在 C: 上 `rm -rf`。项目在 `D:\Users\15278\Desktop\test`，**所有删除都走 mv→C:→rm**。

## ⚠️ 工具坑：同一文件的多处 Edit 别放在同一条消息里
- 曾出现在一条消息里对**同一个文件**连发 2~3 个 Edit，全部回报成功，但**只有 1~2 处真正落盘**（其余被并行写覆盖），差点把「改了一半」的代码当成改完。
- 做法：**同一文件的改动串行**（一次消息一处），或对小文件直接 `Write` 整份重写（原子、最稳）；改完**必须 grep/Read 复核**再继续。
