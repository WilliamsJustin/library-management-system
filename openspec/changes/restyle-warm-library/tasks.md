# Tasks: restyle-warm-library

## 1. 令牌与字体基座

- [x] 1.1 下载 Cormorant Garamond、Crimson Pro 可变字重 woff2 替换 `frontend/src/assets/fonts/` 下旧字体并删除 Outfit/Work Sans 文件；校验：`npm run dev` 无 404、字体文件 <120KB/个
- [x] 1.2 用混色脚本从 `#78716C` 精确生成 EP 六档阶梯，改写 `style.css` 令牌区：`--sl-*`（primary/accent/bg/text/muted/border/ring/shadow）+ EP 桥接 + `@font-face` 与 `--sl-font-*` 更新；校验：主按钮书棕底白字、页面底色暖米黄
- [x] 1.3 可读性补丁重算：删除按钮黑字覆盖规则、warning 标签文字 `#92400E` 确认、muted/hover 用暖纸色 `#F5F0E1`；校验：程序化对比度抽查 body ≥4.5:1、主按钮白字对书棕 ≈4.8:1
- [x] 1.4 `npx vue-tsc --noEmit` + `npm test`（21 用例）；校验：全绿

## 2. 主题专属硬编码与装饰

- [x] 2.1 grep 盘点并改写青绿专属硬编码（`rgba(13,148,136,…)` → 书棕 rgba、FloatingHelp 阴影等）；校验：全仓 grep 无青绿品牌色残留（`#0d9488|#0f766e|rgba(13, 148, 136`）
- [x] 2.2 `SiteHomeView` hero 装饰语言微调（可选：圆环 → 书页横线/纸纹；令牌引用部分自动跟随不手动改）；校验：1440/375 hero 截图，区块结构未变
- [x] 2.3 三端截图走查（1440 首页/检索页/读者首页/借阅查询/管理首页/图书编目 + 375 移动端抽查 + 768/769 结构断言）；校验：桌面结构与响应式行为不回归、衬线混排观感确认（违和则执行 D3 回退）
- [x] 2.4 全端验收：`npm test` 全绿 + 检查单逐项（对比度/focus/过渡/reduced-motion/字体 0 外网请求）；校验：spec 四个场景全部通过
