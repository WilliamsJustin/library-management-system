// 全局领域模型与接口类型定义。
// 字段命名与后端 DTO / PageResult 严格对齐（后端 Java 用 camelCase，JSON 同名字段）。

/* ------------------------------------------------------------------ *
 * 枚举（后端以 name() 存 VARCHAR，故前端用字符串联合类型）
 * ------------------------------------------------------------------ */

/** 图书状态：在架可流通 / 已下架 */
export type BookStatus = 'ACTIVE' | 'INACTIVE'

/** 副本状态：在库 / 已借出 / 已下架 */
export type CopyStatus = 'IN_STOCK' | 'BORROWED' | 'WITHDRAWN'

/** 借阅状态：在借 / 已还 / 逾期 */
export type LoanStatus = 'ACTIVE' | 'RETURNED' | 'OVERDUE'

/** 罚款状态：未缴 / 已缴 */
export type PenaltyStatus = 'UNPAID' | 'PAID'

/** 读者状态：正常 / 受限（停借） */
export type ReaderStatus = 'NORMAL' | 'RESTRICTED'

/** 读者类型：学生 / 教师 */
export type ReaderType = 'STUDENT' | 'TEACHER'

/** 登录角色（后端返回大写） */
export type UserRole = 'ADMIN' | 'READER'

/* ------------------------------------------------------------------ *
 * 分页
 * ------------------------------------------------------------------ */

/**
 * 统一分页返回结构。
 * 后端 PageResult 沿用 Spring Data 的字段命名，`number` 为 **0 基**（第一页 = 0），
 * 与前端请求参数 `?page=0&size=10` 一致。
 */
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  /** 当前页码，0 基 */
  number: number
  /** 每页条数 */
  size: number
  /** 当前页实际记录数 */
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

/** 列表页统一查询参数 */
export interface PageQuery {
  page?: number
  size?: number
}

/* ------------------------------------------------------------------ *
 * 领域模型
 * ------------------------------------------------------------------ */

export interface Book {
  id: number
  isbn: string
  title: string
  author: string
  publisher: string
  category: string
  /** 出版日期（yyyy-MM-dd），可能为空 */
  publishDate?: string | null
  /** 语言 */
  language?: string | null
  /** 定价（元） */
  price?: number | null
  /** 封面图片地址 */
  coverUrl?: string | null
  /** 内容简介 */
  description?: string | null
  status: BookStatus
  /** 可借副本数 */
  availableCopies: number
  /** 总副本数 */
  totalCopies: number
}

export interface BookCopy {
  id: number
  bookId: number
  barcode: string
  location: string
  status: CopyStatus
}

export interface Reader {
  id: number
  account: string
  name: string
  type: ReaderType
  studentNo: string
  phone: string | null
  status: ReaderStatus
}

export interface Loan {
  id: number
  bookId: number
  bookTitle: string
  isbn: string
  barcode: string
  readerId: number
  readerName: string
  readerAccount: string
  /** 读者学号/工号 */
  readerNo: string | null
  /** 读者类型（学生 / 教师） */
  readerType: ReaderType
  borrowedAt: string
  /** 应还时间（ISO 字符串，精确到分钟：借期为 10 分钟） */
  dueDate: string
  returnedAt: string | null
  renewedCount: number
  status: LoanStatus
}

export interface Penalty {
  id: number
  loanId: number
  isbn: string
  bookTitle: string
  barcode: string
  readerId: number
  readerName: string
  readerAccount: string
  /** 读者学号/工号 */
  readerNo: string | null
  /** 读者类型（学生 / 教师） */
  readerType: ReaderType
  /** 罚款金额（元） */
  amount: number
  status: PenaltyStatus
  createdAt: string
  paidAt: string | null
}

export interface Announcement {
  id: number
  title: string
  content: string
  /** 公告类型：NORMAL 普通 / REMINDER 逾期提醒全局弹窗 */
  type: 'NORMAL' | 'REMINDER'
  pinned: boolean
  publishedAt: string
}

export interface Activity {
  id: number
  title: string
  content: string
  tag: string
  pinned: boolean
  createdAt: string
}

export interface AppNotification {
  id: number
  content: string
  /** 消息类型：NORMAL 普通 / REMINDER 逾期到期提醒（读者端弹窗只弹这一类） */
  type: 'NORMAL' | 'REMINDER'
  createdAt: string
  read: boolean
}

/** 未读消息数 */
export interface UnreadCount {
  count: number
}

/** 收藏状态（收藏 / 取消收藏接口的返回体） */
export interface FavoriteState {
  bookId: number
  /** 当前是否已收藏 */
  favorited: boolean
}

/* ------------------------------------------------------------------ *
 * 认证
 * ------------------------------------------------------------------ */

/** 登录 / 注册接口返回体 */
export interface LoginResponse {
  token: string
  userId: number
  name: string
  role: UserRole
  readerType: ReaderType | null
}

/** 前端本地保存的用户档案（由 LoginResponse 精简而来） */
export interface UserProfile {
  id: number
  name: string
  role: UserRole
  type: ReaderType | null
}

/* ------------------------------------------------------------------ *
 * 请求体
 * ------------------------------------------------------------------ */

/** 借出请求：读者与副本均可通过 ID 或业务标识定位，二选一 */
export interface BorrowPayload {
  readerId?: number
  readerAccount?: string
  copyId?: number
  barcode?: string
}

export interface StatusPayload {
  status: boolean
}

export interface BookPayload {
  isbn: string
  title: string
  author: string
  publisher: string
  category: string
  publishDate?: string | null
  language?: string | null
  price?: number | null
  coverUrl?: string | null
  description?: string | null
}

export interface BookCopyPayload {
  barcode: string
  location: string
}

export interface ReaderPayload {
  account: string
  /** 仅创建时需要密码 */
  password?: string
  name: string
  type: ReaderType
  studentNo: string
}

export interface AnnouncementPayload {
  title: string
  content: string
  pinned: boolean
}

export interface ActivityPayload {
  title: string
  content: string
  tag: string
  pinned: boolean
}

export interface RegisterPayload {
  account: string
  password: string
  name: string
  type: ReaderType
  studentNo: string
  phone?: string
}

/* ------------------------------------------------------------------ *
 * 帮助与反馈（FAQ / 留言 / 在线咨询）
 * ------------------------------------------------------------------ */

export interface Faq {
  id: number
  question: string
  answer: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface FaqPayload {
  question: string
  answer: string
  enabled?: boolean
}

export interface FeedbackMessage {
  id: number
  readerId: number | null
  /** 留言人展示名：读者姓名 / 游客昵称 */
  readerName: string
  /** 读者账号；游客留言为 null */
  readerAccount: string | null
  /** 读者学号/工号；游客留言为 null */
  readerNo: string | null
  content: string
  replyContent: string | null
  status: 'UNREPLIED' | 'REPLIED'
  createdAt: string
  repliedAt: string | null
  repliedBy: string | null
}

export interface FeedbackPayload {
  content: string
  /** 游客留言时的昵称；登录读者可省略 */
  contactName?: string
}

export interface ChatMessage {
  id: number
  readerId: number
  senderRole: 'READER' | 'ADMIN'
  senderName: string
  content: string
  createdAt: string
}

/** 管理员首页仪表盘统计（/stats/overview） */
export interface DashboardStats {
  loans: {
    today: number
    week: number
    total: number
    overdueToday: number
    overdueWeek: number
    overdueTotal: number
  }
  penalty: {
    todayAmount: number
    totalAmount: number
  }
  chat: {
    chatPending: number
    feedbackPending: number
  }
}

export interface ChatSession {
  readerId: number
  readerName: string
  lastMessage: string
  lastSenderRole: 'READER' | 'ADMIN'
  lastTime: string
  messageCount: number
  /** 未回复消息数：最近一条管理员回复之后的读者消息条数 */
  unrepliedCount: number
}
