# ADR-003: 引入 OpenSpec 规格驱动变更流程

- 状态：已采纳（2026-09，随初版建立，后续所有变更沿用）

## 背景

功能持续演进（初版 → 响应式 → 前端模块深化 → 两次主题改版），缺少"需求为什么这么定、改了哪些行为"的可追溯载体；口头/随手改的方式在多轮重构后无法回答"现在系统到底承诺了什么"。

## 决策

所有实质性变更走 OpenSpec 流程：`openspec/changes/<name>/` 内产出 proposal（为何/改什么）、specs delta（行为契约：需求+场景）、design（技术取舍）、tasks（带验证的任务），实施完成后 validate 并归档。纯重构/文档类可用 `skip_specs: true`。

## 后果

- ✅ 每个变更可审计：CHANGELOG、任务勾选、规格差异都是流程副产物而非额外负担
- ✅ 与 agent 协作顺畅：规格即上下文，新会话可直接续作
- ⚠️ **已知欠账**：截至本文档撰写，5 个 change 均已实施完毕但**尚未归档**，主 `openspec/specs/` 仍为空；归档顺序存在依赖（restyle-warm-library 的 MODIFIED delta 依赖 restyle-visual-system 先归档建立 visual-theme 主规格）。下次归档时按时间序执行即可
- ⚠️ 仪式感对微改动偏重：已用 `skip_specs` 缓解
