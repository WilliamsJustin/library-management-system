# 学校图书馆借阅系统 — 项目长期备忘

## 技术栈与路径
- 前端 `frontend/`（Vue 3 + Vite + Element Plus 2.14.5 + Pinia + vue-router history 模式），主题主色 `#409eff`；页面留白底色 `#f5f7fa`。
- 后端 `backend/`（Spring Boot 3.2.5 + **MyBatis-Plus 3.5.7** + MySQL），包名 `com.school.library`。库 `school_library`@127.0.0.1:3306，root/123456。
- 前台公共页在 `views/site/`，布局 `views/PublicLayout.vue`（单栏，**无侧边导航**）；「本馆概况」用 `views/site/AboutSubNav.vue` 横排胶囊导航（本馆简介 /about、规章制度 /about/rules、开放时间 /about/floors）。

## 前端构建与发布（重要）
- **不要 `rm -rf dist`**（会触发安全拦截）。固定套路：
  `vite build --outDir dist-newN` → `mv dist dist-tmpN && cp -r dist-newN dist`。
- 构建命令（managed node）：
  `cd frontend && "C:/Users/15278/.workbuddy/binaries/node/versions/22.22.2-2/node.exe" node_modules/vite/bin/vite.js build --outDir dist-newN`
- `frontend/` 下累积了大量 `dist-new*` / `dist-tmp*` 临时目录，系统安全策略不允许助手删除，需用户手动清理。

## 前端运行时验证（改完 UI 后强烈建议）
本机未装 agent-browser / playwright，但 **Edge 可直接 headless 渲染**（已验证可用）：
```bash
EDGE="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"
PROF='C:\Users\15278\AppData\Local\Temp\edge-ab-test'   # 必须是 Windows 路径！
"$EDGE" --headless=new --disable-gpu --no-first-run --user-data-dir="$PROF" \
  --virtual-time-budget=8000 --dump-dom "http://localhost:4173/about/rules" > /tmp/out.html
# 截图：把 --dump-dom 换成 --screenshot="$SHOT\x.png" --window-size=1440,1200
```
- 先用 `vite preview --port 4173 --strictPort` 起静态服务（history 模式有 SPA fallback）。
- ⚠️ **坑**：`--user-data-dir` 传 Git Bash 风格的 `/tmp/xxx` 会让 Edge 以 exit 21 静默失败、输出 0 字节；必须传 `C:\...` 形式的 Windows 路径。服务默认只监听 `localhost`(IPv6 ::1)，curl 请用 `http://localhost:4173` 而非 `127.0.0.1`。
- ⚠️ **坑**：本机设了 `http_proxy/https_proxy=127.0.0.1:2246`，curl 访问 localhost 会被代理拦成 502「upstream connect failed」，必须加 `--noproxy '*'`；Edge 加 `--no-proxy-server`。
- 想「快进时间」测定时器（如 30 分钟空闲超时）：给 Edge 加 `--virtual-time-budget=25000`，配合临时把阈值改小（如 6s）的独立构建，即可在秒级内端到端跑完。
- ⚠️ **`--virtual-time-budget` 会让部分 SPA「只出外壳不出内容」**（如 Knife4j 的 doc.html：静态资源全加载但从不发后续 XHR，因为它的异步初始化走 localforage/IndexedDB，属真实 IO，虚拟时间下不 resolve）。**要用 CDP 真实等待复验**：`msedge --headless=new --remote-debugging-port=9223 --no-proxy-server <url>` 后台起，再用 Node 22 **内置 `WebSocket`**（无需 ws 依赖）连 `http://127.0.0.1:9223/json/list` 里该 target 的 `webSocketDebuggerUrl`，`Runtime.enable` → 真实 sleep 若干秒 → `Runtime.evaluate`（读 `#app.innerText`）或 `Page.captureScreenshot`。判断 SPA 是否真渲染，别信虚拟时间的 DOM dump。
- 验证完记得关掉 preview，并清理临时 profile 与截图。

## 接口文档（Knife4j）
- 依赖：`knife4j-openapi3-jakarta-spring-boot-starter:4.5.0`（内部用 springdoc-openapi 2.3.0，适配 Boot 3.2.5）。
- 地址：`http://localhost:8080/doc.html`（Knife4j）、`http://localhost:8080/swagger-ui/index.html`（原生 Swagger UI）、`/v3/api-docs`（JSON）。
- 配置：`application.yml` 必须有 `knife4j.enable: true`（4.x 不显式开启则增强 UI 不生效）；`config/OpenApiConfig.java` 提供标题/版本与全局 `bearerAuth` 方案。
- ⚠️ **必须放行**：`SecurityConfig.DOC_ENDPOINTS`（`/doc.html`、`/webjars/**`、`/swagger-ui/**`、`/v3/api-docs/**` 等）。少了 `/webjars/**` 或 `/v3/api-docs/**`，文档页就是 401/白屏——**文档打不开先查这里**。

## 后端改动后的必做动作
- 编译/打包（maven 脚本在 Git Bash 下会因 MSYS 路径转换失败，需用 Windows 路径直调 java + plexus-classworlds Launcher）：
  `cd backend && java -classpath "D:/Software/Programming/myEnvironment/Maven/apache-maven-3.9.12/boot/plexus-classworlds-2.9.0.jar" -Dclassworlds.conf=.../m2.conf -Dmaven.home=... Launcher -q package -DskipTests`
- **反复出现的坑**：用户 IDE(IDEA) 里跑的 8080 后端常是**旧构建**，新增接口/查询参数会表现为 500 或「参数被忽略」。已多次踩到（图书编目的 categories/publishers、公告活动的 keyword/startDate）。源码正确时，除重新打包外必须**提醒用户在 IDEA 重启 8080 后端**。
- 本地自验可另起 `java -jar target/library-0.1.0-SNAPSHOT.jar --server.port=8081`（用完关掉）。
- `nohup ... &` 起的进程会随 shell 退出而消失，要用受管后台任务方式启动。
- 改完启动一次，**先看 stderr 有没有 `[Fatal Error]`**（配置文件语法/依赖问题会静默退化），再看 `logs/` 是否生成。

## 日志（Log4j2）
- 实现是 Log4j2（不是 Logback）。`pom.xml` 里 **5 个 starter 各自**排除 `spring-boot-starter-logging`（Maven exclusion 只作用于自身传递树，只排 web 不够，data-jpa/security 仍会拖进 logback + `log4j-to-slf4j` 导致日志丢失），再引 `spring-boot-starter-log4j2`（Boot 3.2.5 管理版本 = log4j 2.21.1）。
- 配置：`src/main/resources/log4j2-spring.xml`（**必须 -spring 后缀**）+ `application.yml` 的 `logging.config: classpath:log4j2-spring.xml`。输出：控制台彩色 / `logs/school-library.log`（按天+100MB，`.gz` 存 `logs/history/`，留 30 天）/ `logs/school-library-error.log`（仅 ERROR）。文件写入包 `<Async>`（原生 AsyncAppender，**不需要 Disruptor**）。改级别可用 `-Dlogging.level.com.school.library=INFO`，换目录用 `-Dlibrary.log.dir=...`。
- ⚠️ 两个坑：① **XML 注释内禁止连续 `--`**，`<!-- ---- x ---- -->` 会让整个配置**静默失效**（退回默认配置：无文件、无自定义格式，但不报错）；② `<Property>` 的 name 不能和它自己引用的 lookup key 同名（`name="LOG_HOME"` 配 `${sys:LOG_HOME:-logs}` 会刷 Infinite loop 告警）。
- 验证手段：`unzip -l target/*.jar | grep -iE "logback|log4j|slf4j"` 确认无 logback；「同端口二次启动」可低成本制造一条 ERROR 来验证 error appender 的 ThresholdFilter。

## 持久层（MyBatis-Plus）
- 已由 Spring Data JPA 全量迁移到 **MyBatis-Plus 3.5.7**（starter：`mybatis-plus-spring-boot3-starter`，Boot 3 专用）。**不再有 `repository/` 包，不要新建 JPA Repository**；持久层都在 `mapper/`，实体在 `entity/`，实体已无 `@Entity/@ManyToOne/@Enumerated`。
- **前端分页契约是硬约束**：前端读 `data.content`(+`data.totalElements` 等)，发 `?page=0&size=10`（0 基）。MP 的 `IPage` 字段名不兼容 → 统一用 `common/PageResult.java`（复刻 JPA `Page` 的字段名，`number`=current-1）持出；Controller 一律 `int page,int size` + `PageResult<T>`，**别再返回 `Page`/`IPage`**。分页 plugin 在 `config/MybatisPlusConfig.java`（`@MapperScan("com.school.library.mapper")`，拦截器顺序：乐观锁→分页）。
- **枚举必须存 `name()`（VARCHAR）** 以兼容旧数据 → `application.yml` 里 `mybatis-plus.configuration.default-enum-type-handler: org.apache.ibatis.type.EnumTypeHandler`（用 MP 默认的 MybatisEnumTypeHandler 会不兼容）。
- **关联用外键 ID（`Long bookId` + `@TableField("book_id")`），不用对象引用**。跨表详情（借阅/罚款带书名/读者名）写在 Mapper 的 `@Select` join SQL 里（`DETAIL_SELECT`/`selectDetailPage`）；批量统计用 `<script>` 的 IN 查询（见 `BookCopyMapper` + `dto/BookCopyStat`），**不要写 N+1**。列名不符的字段（如 `is_read`→`read`）用 `@TableField`/`@Results` 映射；`map-underscore-to-camel-case: true` 已开。
- **乐观锁**：`BookCopy`/`Loan` 有 `@Version`，`updateById` 返回 0 表示版本冲突（借书时抛 `ConflictException`）。
- **测试无 ddl-auto**：`src/test/resources/sql/schema-h2.sql` 是 H2 建表脚本（BOOLEAN 代 TINYINT(1)、独立 CREATE INDEX、无 ENGINE/CHARSET），`application.yml` 用 `spring.sql.init.mode=always` + `schema-locations` 加载；测试里 `TestData` 等直接用 Mapper 造数。

## MySQL 中文写入
- 直接用 mysql 客户端 heredoc 内联中文会因连接字符集不是 utf8mb4 而存成乱码（HEX 呈双编码）。**必须**把 SQL 写成文件后 `mysql ... --default-character-set=utf8mb4 < file.sql` 导入；用 `CHAR_LENGTH()` 校验（中文 1 字算 1）而非字节数。
- 测试密码统一 `pass123`；BCrypt 哈希用 Anaconda python（`pip install bcrypt` → `$2b$12$...`，把前缀改成 `$2a$` 供 Spring BCryptPasswordEncoder 使用）。

## 已有共享组件 / 约定
- `src/components/PageBar.vue`：统一分页条，渲染「共 N 页 … 跳至 [ ] 页」，props `total/pageSize/currentPage/background/center`。跳页输入框**默认为空**（自定义实现，未用 el-pagination 内置 jumper）。App.vue 用 el-config-provider 覆盖 locale，把「前往」改成「跳至」。
- 分页列表的「序号」列统一用 `type="index"` + `:index` 函数做**跨页连续**编号：`(page-1)*pageSize + index + 1`。
- `src/data/rules.js`：规章制度数据（列表页 + 详情页共用）。
- `src/composables/useIdleTimeout.js`：会话空闲超时。时间戳 + 10s 巡检；事件用 `{passive,capture:true}` 挂 window（scroll 不冒泡，必须 capture）；`markActivity` 要**先 evaluate 再刷新时间**；visibilitychange/focus 立即兜底巡检；`isEnabled` 传 `()=>authStore.isAuthenticated`，`onTimeout` 调 `authStore.logout()`。App.vue 挂载，阈值 `IDLE_MINUTES=30`，弹常驻 ElNotification（duration:0）提示 + 原生 `<a>` 重新登录链接。纯前端方案，后端无会话失效。

## 易混淆的页面命名（需求澄清时注意）
- 「借阅查询」= **读者端** `views/reader/MyLoansView.vue`（/reader/my-loans，菜单名「借阅查询」，页面标题「我的借阅」）。该页**无分页**，固定 `size: 50` 一次性加载。
- 「借阅流通」= **后台** `views/admin/LoansView.vue`（/admin/loans，菜单名「借阅流通」，有 PageBar 分页，首列是「借阅ID」）。
- 用户口语里的「后台」不一定等于 admin 端：曾出现「后台借阅查询界面」实指读者端页面的情况，涉及歧义时先确认再改。
- ⚠️ **工具坑**：同一条消息里对**同一个文件**并行发多个 Edit 时，出现过工具报 success 但磁盘未改的静默丢失。改完必须 grep 复核，或对同一文件改为串行编辑。
