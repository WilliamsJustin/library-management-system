# 领域文档约定（Domain Docs）

本文件约定 agent 在探索本仓库时应如何消费领域文档。

## 探索前先读

- 仓库根的 **`CONTEXT.md`**（若存在 **`CONTEXT-MAP.md`** 则以它为索引，按主题读相关的各 `CONTEXT.md`）
- **`docs/adr/`**：阅读与本次工作领域相关的 ADR（本项目当前 7 篇，见 [docs/adr/](../adr/)）

如果这些文件不存在，**静默继续**：不要提示缺失，也不要主动建议创建。`/domain-modeling` 技能（经 `/grill-with-docs`、`/improve-codebase-architecture` 触达）会在术语或决策实际沉淀时惰性创建它们。

## 文件结构

单上下文仓库（本项目即此形态）：

```
/
├── CONTEXT.md
├── docs/adr/
│   └── ADR-00x-*.md
└── src/
```

多上下文仓库（根目录出现 `CONTEXT-MAP.md` 时）：

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← 全系统级决策
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← 上下文专属决策
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## 使用词汇表的词汇

输出中提到领域概念时（issue 标题、重构提案、假设、测试命名），一律使用 `CONTEXT.md` 定义的术语，不要漂移到词汇表刻意回避的同义词。

如果需要的概念词汇表里还没有，这是一个信号：要么你在发明项目不使用的语言（请重新斟酌），要么是真实的词汇缺口（记下来交给 `/domain-modeling` 补充）。

## 标记 ADR 冲突

如果输出与既有 ADR 矛盾，显式指出而不是静默推翻：

> _与 ADR-001（Spring Session 替代 JWT）冲突，但值得重新讨论，因为……_
