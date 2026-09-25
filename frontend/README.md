# 学校图书借阅系统 · 前端

Vue 3.5 `<script setup>` + TypeScript + Element Plus 2.14 + Pinia + Vue Router 的三端单页应用（公共前台 / 读者后台 / 管理后台）。

## 常用命令

```bash
npm install
npm run dev            # 开发服务器（默认 5173；Windows 端口被排除区间占用时加 --port 5342 --strictPort）
npm run build          # 产物构建（dist/）
npm test               # vitest 单测（21 用例）
npx vue-tsc --noEmit   # 类型检查
```

## 结构速览

```
src/
├── views/site/      # 公共前台（首页/检索/详情/概况/服务/活动/注册）
├── views/reader/    # 读者后台（借阅/查询/收藏/罚款/帮助）
├── views/admin/     # 管理后台（编目/读者/流通/罚款/帮助/公告/活动）
├── components/      # MobileCardList、DateRangeCombo、FloatingHelp、help 组件族等
├── composables/     # useBreakpoint（768px 断点）、useCrossPageSelection（跨页多选）
├── utils/           # listDisplay（序号/状态口径）、dateUtils、http
├── stores/          # Pinia（auth 会话态）
└── style.css        # 设计令牌层（--sl-* 语义令牌 + Element Plus 变量桥接）
```

## 约定

- **色值只走令牌**：业务代码禁止裸色值，主题切换只改 `style.css`（规范见 [ADR-004](../docs/adr/ADR-004-frontend-design-tokens.md)）
- 移动端样式只存在于 `@media (max-width: 768px)` 或 `isMobile` 分支；重表格页用「桌面表格 / MobileCardList 卡片」双渲染
- API 走 `utils/http.ts` 统一封装，错误 `{code, message}`；会话 30 分钟空闲过期（401 SESSION_EXPIRED 自动跳登录）

工程文档索引见根 [README.md](../README.md)。
