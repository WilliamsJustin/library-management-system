/// <reference types="vite/client" />

// Vue SFC 单文件组件的类型由 vue-tsc / Volar 原生解析，
// 这里无需声明 '*.vue' 通配模块（声明反而会掩盖真实推导）。
// Element Plus 语言包改从 `element-plus/es/locale/lang/*` 引入（该路径带 .d.ts 类型），
// 因此也不需要为 `dist/locale/*.mjs` 补模块声明。
