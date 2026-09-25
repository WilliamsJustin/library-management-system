# 业务流程与状态机

> 本文档描述系统的核心业务域：**借阅流通**（检索 → 借出 → 续借/归还 → 逾期 → 罚款 → 停借/恢复），以及内容运营（公告/活动/帮助）的一笔带过说明。
> 参数来源：`application.yml` 的 `app.library.*` + `service/CirculationPolicy.java`，与 [database.md](database.md) 的数据字典一一对应。

## 1. 流通规则速查

| 规则 | 值 | 配置项/位置 |
|---|---|---|
| 借期 | 学生/教师均 **10 分钟**（演示口径） | `app.library.loan-minutes` |
| 可借数量 | 学生 **5** 本、教师 **10** 本（在借计入） | 代码内常量 |
| 续借 | 每本 **1 次**，续借顺延一个标准借期；**逾期不可续借** | `CirculationPolicy.maxRenewCount` |
| 逾期罚款 | **0.10 元/分钟**，不足 1 分钟按 1 分钟计 | `app.library.fine-per-minute` |
| 单本罚款封顶 | **144.00 元**（= 0.10 × 1440 分钟） | `app.library.max-fine-minutes` |
| 到期提醒 | 到期前 **5 分钟**下发一次站内 REMINDER（`due_reminder_sent` 幂等，续借后重置） | `app.library.reminder-minutes` |
| 逾期扫描 | 每 **30 秒**一轮（归还/续借时另有兜底结算） | `app.library.check-interval-ms` |
| 停借 | 存在 **UNPAID** 罚单 → 读者 RESTRICTED，借阅被拒 | `PenaltyServiceImpl` |
| 恢复 | 缴清全部罚单 → 自动恢复 NORMAL 并发通知 | 同上 |

## 2. 借阅主流程

```mermaid
flowchart TD
    A[检索图书<br>前台/读者后台/管理端] --> B{选择副本借出<br>读者自助 / 管理员代借}
    B --> C{借前校验}
    C -->|读者 RESTRICTED| X1[拒绝：停借中，请先缴清罚款]
    C -->|在借数达上限<br>学生5/教师10| X2[拒绝：超出可借上限]
    C -->|副本状态 ≠ IN_STOCK| X3[拒绝：副本已被借出]
    C -->|校验通过| D[创建借阅<br>loan=ACTIVE, due=now+借期<br>副本→BORROWED（乐观锁）]
    D --> E{OverdueTask 30s 轮询}
    E -->|距到期 <5 分钟且未提醒| F[下发站内 REMINDER<br>due_reminder_sent=true]
    E -->|due_date 已过| G[loan→OVERDUE<br>生成/递增 UNPAID 罚款<br>读者→RESTRICTED + 通知]
    D --> H{归还方式}
    H -->|读者自助归还| I
    H -->|管理员代还| I[结算：未逾期→直接 RETURNED<br>已逾期→先按分钟计罚款]
    I --> J[loan→RETURNED, returned_at=now<br>副本→IN_STOCK]
    G --> K{缴纳罚款}
    K -->|读者在线缴 / 管理员代缴| L[penalty→PAID]
    L --> M{该读者还有未缴罚单?}
    M -->|否| N[读者恢复 NORMAL + 通知]
    M -->|是| O[保持 RESTRICTED]
```

## 3. 实体状态机

### 3.1 借阅 Loan

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: 借出（校验通过）
    ACTIVE --> OVERDUE: 定时扫描 due_date 已过
    ACTIVE --> RETURNED: 归还（未逾期）
    OVERDUE --> RETURNED: 归还（逾期→先结算罚款）
    RETURNED --> [*]
    note right of OVERDUE: 逾期期间罚款金额<br>随轮询递增（封顶144）
    note left of ACTIVE: 可续借 ≤1 次<br>（due += 借期，重置提醒标记）
```

### 3.2 副本 book_copy

```mermaid
stateDiagram-v2
    [*] --> IN_STOCK: 编目/Excel 导入
    IN_STOCK --> BORROWED: 借出（乐观锁扣减）
    BORROWED --> IN_STOCK: 归还
    IN_STOCK --> WITHDRAWN: 下架副本
    WITHDRAWN --> IN_STOCK: 恢复上架
```

### 3.3 读者 reader（借阅资格）

```mermaid
stateDiagram-v2
    [*] --> NORMAL: 注册/管理员创建
    NORMAL --> RESTRICTED: 产生 UNPAID 罚单
    RESTRICTED --> NORMAL: 缴清全部罚单（自动+通知）
    note right of RESTRICTED: 借阅请求被拒<br>其余功能（检索/收藏/查询）不受限
```

### 3.4 罚款 penalty

```mermaid
stateDiagram-v2
    [*] --> UNPAID: 逾期结算（每借阅仅一张，幂等）
    UNPAID --> UNPAID: 轮询递增金额（封顶144）
    UNPAID --> PAID: 缴纳（在线/代缴）
    PAID --> [*]: 不再变更
```

## 4. 关键并发与幂等设计

- **并发借出**：同一副本两人同时点借阅 → `book_copy.version` 乐观锁，后提交者收到「副本已被借出」
- **罚款幂等**：`penalty.loan_id` 唯一约束 + 服务层"存在 UNPAID 且金额更大则更新"——定时任务与归还兜底双入口不会产生双份账单
- **提醒幂等**：`loan.due_reminder_sent` 标记保证到期提醒只发一次；续借后重置，续借后的新到期日仍会提醒
- **轮询 + 兜底**：逾期状态以 30s 定时任务为主；归还/续借接口内同步结算，保证即使调度延迟数据也一致

## 5. 认证与会话流程

```mermaid
sequenceDiagram
    participant FE as 前端
    participant API as Spring Security
    participant RD as Redis
    FE->>API: POST /api/auth/login (账号+密码)
    API->>API: BCrypt 校验
    API->>RD: 创建会话（LIBRARY_SESSION Cookie，30 分钟）
    API-->>FE: 用户档案 + 会话
    FE->>API: 后续请求携带 Cookie（或 Authorization: Bearer 会话ID）
    API->>RD: 校验会话；30 分钟空闲失效 → 401 SESSION_EXPIRED
    FE->>API: POST /api/auth/logout → 会话即吊销
```

会话三处超时配置必须一致（`app.session.timeout-minutes` / `spring.session.timeout` / Cookie Max-Age），详见 [security.md](security.md)。

## 6. 内容运营域（简述）

- **公告 announcement / 活动 activity**：管理员（或教师）发布、置顶、撤下；前台匿名可见，读者端公告/活动管理页复用同一套接口（学生仅读）
- **帮助反馈**：FAQ（管理员维护，前台悬浮窗与读者后台共用检索）→ 留言（游客可发，管理员回复）→ 实时对话（reader_id 会话 + 前端轮询，管理员侧"待处理"聚合）
- **通知 notification**：系统生成的私有站内消息（区别于公告），读者端时间线展示 + 未读数 + 一键已读
