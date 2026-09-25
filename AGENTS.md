# AGENTS.md — AI 编码工具协作入口

本文件供 AI 编码工具（ZCode / Claude / Cursor 等）在打开仓库时自动读取，是 agent 的协作约定入口。人类开发者请从 [README.md](README.md) 开始。

## 项目一句话

学校图书借阅系统：Vue 3 + Element Plus 三端单页应用（公共前台 / 读者后台 / 管理后台）+ Spring Boot 3 + MyBatis-Plus + MySQL + Redis，Docker Compose 部署。

## 先读哪些文档

| 要做什么 | 先读 |
|---|---|
| 了解业务术语与命名口径 | [CONTEXT.md](CONTEXT.md)（领域词汇表，命名必须与其保持一致） |
| 找功能/部署/回滚/安全等说明 | [README.md](README.md) 的「文档索引」表，或直接看 [docs/](docs/) |
| 做架构级取舍 | [docs/adr/](docs/adr/) 已有 7 篇决策，先查是否已有结论 |
| 改动前查需求口径 | [openspec/](openspec/) 各 change 的 specs（行为契约） |

## 变更流程

实质性变更走 **OpenSpec 规格驱动流程**（提案 → specs/design/tasks → 实施 → validate），约定见 `openspec/config.yaml` 与 [docs/adr/ADR-003](docs/adr/ADR-003-openspec-workflow.md)。纯文档/微改动可直接做。

## 验证基线（改动后必跑）

```bash
cd frontend && npm test && npx vue-tsc --noEmit
cd backend && mvn test
```

视觉相关改动加 375/768/1440 截图走查；768px 断点行为不可回归。

## 硬性约定

- 前端色值只引用 `--sl-*` / `--el-*` 令牌，禁止裸色值（见 [docs/adr/ADR-004](docs/adr/ADR-004-frontend-design-tokens.md)）
- 术语使用 [CONTEXT.md](CONTEXT.md) 的口径，不要自造同义词
- 输出与既有 ADR 冲突时显式指出，不要静默推翻

## 领域文档约定

详见 [docs/agents/domain.md](docs/agents/domain.md)（单上下文仓库：根 `CONTEXT.md` + `docs/adr/`，术语惰性扩充）。
