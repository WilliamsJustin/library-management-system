# Delta Spec: visual-theme

## Purpose

定义全站（公共前台、读者后台、管理后台）统一视觉主题的行为契约：品牌色系与设计系统一致、组件库主题跟随、字体离线可用、可访问性底线达标，且响应式行为与桌面布局结构不因换肤回归。设计系统源头为 `design-system/school-library/MASTER.md`。

## ADDED Requirements

### Requirement: 统一品牌色系

系统 SHALL 在三端呈现统一品牌色系，以设计系统 MASTER.md 为唯一色值来源：主色青绿 `#0D9488`（主操作、链接、选中态），强调/CTA 琥珀 `#D97706`（营销性主按钮、重点引导），页面背景极浅青 `#F0FDFA`（前台与后台工作区），文字主色深青 `#134E4A`。三端 SHALL NOT 残留 Element Plus 默认蓝 `#409EFF` 作为品牌色使用。

#### Scenario: 三端主操作色一致

- **WHEN** 用户在公共前台、读者后台、管理后台任意界面查看主要操作按钮、链接与选中态
- **THEN** 呈现均为主题青绿色系，无 Element Plus 默认蓝残留

#### Scenario: 管理后台工作区底色

- **WHEN** 用户打开管理后台任意列表页
- **THEN** 工作区背景为极浅青色，内容卡片为白色，层次分明

### Requirement: 组件库主题跟随

Element Plus 组件（按钮、链接、标签、表格高亮、分页、开关、单选/复选、消息提示等）SHALL 通过主题变量覆盖整体跟随新色系，页面代码不得以逐处硬编码方式修正组件颜色。

#### Scenario: 主按钮与链接跟随主题

- **WHEN** 页面渲染任意 `type="primary"` 按钮、文字链接或表单聚焦态
- **THEN** 颜色来自主题变量（青绿系），覆盖 EP 默认蓝

#### Scenario: 状态色语义不变

- **WHEN** 系统渲染成功/警告/危险语义色（在借/逾期/停借等状态标签）
- **THEN** 语义含义保持不变（成功绿/警告橙/危险红），仅明度与色调与主题协调

### Requirement: 字体自托管与离线可用

拉丁与数字内容 SHALL 以自托管的 Outfit / Work Sans 字体渲染，字体文件随前端静态资源部署；部署环境无法访问公网字体 CDN 时字体呈现 SHALL 不退化。中文内容 SHALL 回落系统字体栈，SHALL NOT 引入中文字体网络加载。

#### Scenario: 内网环境字体加载

- **WHEN** 部署环境无法访问 fonts.googleapis.com 等外部 CDN
- **WHEN** 用户打开任意页面
- **THEN** 学号、ISBN、日期、金额等拉丁与数字内容仍以 Outfit/Work Sans 渲染，无字体加载失败导致的无样式闪烁

### Requirement: 交互过渡与可访问性

系统 SHALL 满足设计系统交付检查单：正文文字对比度不低于 4.5:1；可点击元素 SHALL 呈现 `cursor: pointer`；键盘导航的 focus 态 SHALL 可见；悬停/状态变化 SHALL 使用 150–300ms 过渡；系统 SHALL 尊重 `prefers-reduced-motion`（动画降级为静态呈现）。

#### Scenario: 正文对比度达标

- **WHEN** 对三端页面正文文字与背景取色计算对比度
- **THEN** 对比度不低于 4.5:1（含表头、辅助文字所在区域）

#### Scenario: 减少动态偏好

- **WHEN** 用户系统开启 prefers-reduced-motion
- **THEN** 首页 hero 及各处过渡动画不再播放位移类效果，静态终态直接呈现

### Requirement: 换肤不回归约束

视觉主题改造 SHALL NOT 改变桌面端（>768px）页面布局结构、信息架构、路由与交互流程；SHALL NOT 使响应式行为回归——768px 断点的桌面/移动双形态切换、移动端卡片化重排、底部导航等既有响应式约束 SHALL 继续成立。暗色模式不在本主题范围内，系统 SHALL 仅提供浅色主题。

#### Scenario: 桌面结构不变

- **WHEN** 分别以 1440px 视口对比换肤前后三端任意页面
- **THEN** 页面结构（导航、卡片/表格布局、区块顺序）一致，仅颜色、字体、圆角、阴影等视觉层不同

#### Scenario: 移动端行为不变

- **WHEN** 分别以 375px、768px、769px 视口检查三端页面
- **THEN** 断点切换行为与换肤前一致：≤768px 呈现移动卡片形态与底部导航（读者后台），>768px 呈现桌面布局

#### Scenario: 既有功能不回归

- **WHEN** 运行前端全部单元测试
- **THEN** 全部通过（组件结构与交互逻辑未被换肤改动）
