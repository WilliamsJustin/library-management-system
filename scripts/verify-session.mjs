/**
 * 会话超时（Spring Security + Spring Session Redis + 自定义拦截器）的端到端验证脚本。
 *
 * 前置：
 *   1. Redis 已启动（docker compose up -d redis），默认 127.0.0.1:6379
 *   2. 后端已用「压缩过的时间参数」起在 8081（见文件末尾的命令），并且**没有**连真实库
 *
 * 用法：node .verify-session.mjs
 * 可用环境变量覆盖：BASE / REDIS_HOST / REDIS_PORT / REDIS_DB / REDIS_NAMESPACE
 *                   ACCOUNT / PASSWORD / IDLE_WAIT_MS / TICK_MS
 *
 * 退出码 0 = 全部通过。
 */
import net from 'node:net'

const BASE = process.env.BASE || 'http://127.0.0.1:8081'
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1'
const REDIS_PORT = Number(process.env.REDIS_PORT || 6379)
const REDIS_DB = Number(process.env.REDIS_DB || 0)
const NS = process.env.REDIS_NAMESPACE || 'school:dev:session'
const ACCOUNT = process.env.ACCOUNT || 'student1'
const PASSWORD = process.env.PASSWORD || 'pass123'
const IDLE_WAIT_MS = Number(process.env.IDLE_WAIT_MS || 70000)
const TICK_MS = Number(process.env.TICK_MS || 20000)

let pass = 0
let fail = 0
const ok = (name, cond, extra = '') => {
  if (cond) { pass++; console.log(`  PASS  ${name}`) }
  else { fail++; console.log(`  FAIL  ${name}${extra ? '  <<< ' + extra : ''}`) }
}
const step = (t) => console.log(`\n== ${t}`)
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/* ---------------- 极简 RESP 客户端（只用到 PING / KEYS / PTTL / SELECT） ---------------- */

function parseResp(buf, pos = 0) {
  const type = buf[pos]
  const end = buf.indexOf('\r\n', pos)
  const line = buf.slice(pos + 1, end)
  let next = end + 2
  if (type === '+') return [line, next]
  if (type === '-') throw new Error(`redis error: ${line}`)
  if (type === ':') return [Number(line), next]
  if (type === '$') {
    const len = Number(line)
    if (len === -1) return [null, next]
    return [buf.slice(next, next + len), next + len + 2]
  }
  if (type === '*') {
    const n = Number(line)
    if (n === -1) return [null, next]
    const arr = []
    let p = next
    for (let i = 0; i < n; i++) { const [v, np] = parseResp(buf, p); arr.push(v); p = np }
    return [arr, p]
  }
  throw new Error(`unexpected RESP: ${JSON.stringify(buf.slice(0, 40))}`)
}

function redisCmd(args) {
  return new Promise((resolve, reject) => {
    const sock = net.connect(REDIS_PORT, REDIS_HOST)
    let acc = ''
    const done = () => { resolve(acc) }
    sock.setEncoding('utf8')
    sock.setTimeout(3000)
    sock.on('data', (d) => { acc += d })
    sock.on('end', done)
    sock.on('timeout', () => { sock.destroy(); reject(new Error('redis timeout')) })
    sock.on('error', reject)
    sock.on('connect', () => {
      const enc = (parts) => {
        let out = `*${parts.length}\r\n`
        for (const p of parts) out += `$${Buffer.byteLength(p)}\r\n${p}\r\n`
        return out
      }
      // 先 SELECT，再发真正的命令
      sock.write(enc(['SELECT', String(REDIS_DB)]) + enc(args))
      setTimeout(() => sock.end(), 300)
    })
  })
}

async function redis(args) {
  const raw = await redisCmd(args)
  // 第一条是 SELECT 的 +OK，把它剥掉再解析真正的回复
  const firstEnd = raw.indexOf('\r\n')
  if (!raw.startsWith('+OK')) throw new Error(`SELECT failed: ${JSON.stringify(raw.slice(0, 60))}`)
  return parseResp(raw.slice(firstEnd + 2))[0]
}

/* ---------------- HTTP 小工具 ---------------- */

async function req(method, path, { cookie, bearer } = {}) {
  const headers = {}
  if (cookie) headers['Cookie'] = cookie
  if (bearer) headers['Authorization'] = `Bearer ${bearer}`
  const res = await fetch(`${BASE}${path}`, { method, headers })
  const text = await res.text()
  let body = null
  try { body = text ? JSON.parse(text) : null } catch { body = text }
  return { status: res.status, body, setCookie: res.headers.get('set-cookie') || '' }
}

const keyOf = (sessionId) => `${NS}:sessions:${sessionId}`

/* ---------------- 主流程 ---------------- */

const pong = await redis(['PING']).catch((e) => `ERR:${e.message}`)
if (pong !== 'PONG') {
  console.error(`无法连接 Redis ${REDIS_HOST}:${REDIS_PORT} → ${pong}`)
  console.error('请先启动 Redis：docker compose up -d redis')
  process.exit(2)
}

step('1. 登录建立服务端会话')
const login = await req('POST', '/api/auth/login', {})
login.body = login.body || {}
const sessionId = login.body.token
ok('登录返回 200', login.status === 200, `status=${login.status} body=${JSON.stringify(login.body)}`)
ok('返回非空会话 ID（复用 token 字段）', typeof sessionId === 'string' && sessionId.length > 0)
ok('下发会话 Cookie LIBRARY_SESSION', login.setCookie.startsWith(`LIBRARY_SESSION=${sessionId}`), login.setCookie)
const cookie = login.setCookie.split(';')[0]

step('2. 会话真的落在 Redis 里')
ok('Redis 存在会话键', (await redis(['EXISTS', keyOf(sessionId)])) === 1, keyOf(sessionId))
const ttl1 = await redis(['PTTL', keyOf(sessionId)])
ok('会话键带 TTL（约等于空闲超时）', ttl1 > 0, `pttl=${ttl1}`)

step('3. 两条凭证通道都能通过认证')
const viaCookie = await req('GET', '/api/loans/my', { cookie })
ok('Cookie 通道 200', viaCookie.status === 200, `status=${viaCookie.status}`)
const viaBearer = await req('GET', '/api/loans/my', { bearer: sessionId })
ok('Bearer 通道 200', viaBearer.status === 200, `status=${viaBearer.status} body=${JSON.stringify(viaBearer.body)}`)

step('4. 无凭证 / 伪造凭证一律 401')
ok('不带凭证 401', (await req('GET', '/api/loans/my')).status === 401)
ok('伪造会话 ID 401', (await req('GET', '/api/loans/my', { bearer: 'forged-session-id' })).status === 401)

step(`5. 持续活跃不会被登出（每 ${TICK_MS / 1000}s 打一次，跨过超时阈值）`)
const ticks = Math.ceil((IDLE_WAIT_MS + 20000) / TICK_MS)
let alive = true
for (let i = 1; i <= ticks; i++) {
  await sleep(TICK_MS)
  const r = await req('GET', '/api/loans/my', { cookie })
  if (r.status !== 200) { alive = false; ok(`第 ${i} 次心跳（t≈${(i * TICK_MS) / 1000}s）仍 200`, false, `status=${r.status}`); break }
  console.log(`  心跳 ${i}/${ticks} @${(i * TICK_MS) / 1000}s → 200`)
}
if (alive) ok(`${ticks} 次心跳全部 200（活跃即续期）`, true)
const ttl2 = await redis(['PTTL', keyOf(sessionId)])
ok('活跃后 Redis TTL 被刷新（> 阈值的一半）', ttl2 > IDLE_WAIT_MS / 2, `pttl=${ttl2}`)

step(`6. 空闲 ${IDLE_WAIT_MS / 1000}s 后判定超时`)
await sleep(IDLE_WAIT_MS)
const expired = await req('GET', '/api/loans/my', { cookie })
ok('超时后返回 401', expired.status === 401, `status=${expired.status}`)
ok('错误码是 SESSION_EXPIRED', expired.body?.code === 'SESSION_EXPIRED', JSON.stringify(expired.body))
ok('超时后 Bearer 通道同样失效', (await req('GET', '/api/loans/my', { bearer: sessionId })).status === 401)
ok('超时后 Redis 会话键已删除', (await redis(['EXISTS', keyOf(sessionId)])) === 0)

step('7. 退出登录立即作废会话')
const login2 = await req('POST', '/api/auth/login', {})
const sid2 = login2.body?.token
const cookie2 = login2.setCookie.split(';')[0]
ok('二次登录成功', login2.status === 200 && !!sid2)
ok('会话键存在', (await redis(['EXISTS', keyOf(sid2)])) === 1)
const logout = await req('POST', '/api/auth/logout', { cookie: cookie2, bearer: sid2 })
ok('退出登录返回 200', logout.status === 200, `status=${logout.status}`)
ok('退出后 Redis 会话键已删除', (await redis(['EXISTS', keyOf(sid2)])) === 0)
ok('退出后 Cookie 通道 401', (await req('GET', '/api/loans/my', { cookie: cookie2 })).status === 401)
ok('退出后 Bearer 通道 401', (await req('GET', '/api/loans/my', { bearer: sid2 })).status === 401)

console.log(`\n结果：${pass} passed, ${fail} failed`)
process.exit(fail === 0 ? 0 : 1)
