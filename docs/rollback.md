# 版本回滚方案

> 原则：本系统为单机 Docker Compose 部署、无灰度/多实例，回滚 = **应用镜像回退 + 数据库脚本回退**两件事。数据库永远先备份再动手。

## 1. 应用回滚（后端/前端）

应用无状态（会话在 Redis，业务数据在 MySQL），镜像回退是安全的：

```bash
# 情形 A：镜像还在本地
docker compose down app frontend
docker tag <旧镜像ID> school-library-app:rollback     # docker images 里找上一版
# 修改 docker-compose.yml 中 app/frontend 的 image 指向 rollback tag 后：
docker compose up -d

# 情形 B：重新构建上一版本（git 场景）
git checkout <上一个版本点>          # 或对应 change 实施前的提交
docker compose build app frontend
docker compose up -d
```

**回退后必查**：旧版本代码 + 新数据库结构是否兼容（见 §2/§3——**只增不改不删**的升级脚本原则上向后兼容，旧代码忽略新列即可运行）。

前端回滚特有注意：`assets/fonts/` 与设计令牌随版本走，回退后无需额外处理；若仅回滚前端不回滚后端，注意接口契约（本项目升级均为向后兼容的新增接口）。

## 2. 数据库回滚

### 2.1 快照回滚（最可靠，推荐）

```bash
# 部署前必备（DEPLOYMENT_CHECKLIST E 项）
mysqldump -uroot -p school_library > backup-<日期>.sql

# 回滚 = 恢复快照
mysql -uroot -p -e "DROP DATABASE school_library; CREATE DATABASE school_library DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p school_library < backup-<日期>.sql
```

> 代价：回滚点之后的业务数据丢失（借阅记录/罚款）。演示/课程场景可接受；真实运营需评估停机窗口。

### 2.2 定向回滚脚本

`backend/src/main/resources/db/upgrade-*.sql` 的既定规范是**只增不改不删**（新增列带默认值、新增表），因此多数升级无需逆向脚本。确实需要定向回退时使用 [`docs/sql/rollback.sql`](sql/rollback.sql)，按需执行其中带注释的语句块（当前覆盖 6 个 upgrade 脚本的逆操作；**先备份再执行**）。

⚠️ 两个不可简单逆向的口径，执行 rollback.sql 前必须确认：
- `upgrade-loan-minutes.sql`：借期单位从"天"改"分钟"——回滚会把 DATETIME 借期改回天数口径，**存量 loan.due_date 会失真**
- `upgrade-book-metadata.sql` / `upgrade-help.sql` 若含数据迁移（把旧列数据搬到新列），逆向脚本只删列不还原数据

### 2.3 幂等性说明

- `schema.sql` 全部 `CREATE TABLE IF NOT EXISTS`，重跑无害
- 种子数据 `APP_SEED_ENABLED=true` 幂等（按账号存在性判断），重跑无害
- 定时任务对罚款按 `penalty.loan_id` 唯一约束幂等，回滚窗口期重复扫描不会产生重复账单

## 3. 配置回滚

- `.env` / 环境变量：回退到上一版本值后 `docker compose up -d` 即可（注意改 `DB_PASSWORD` 必须连数据卷一起重建才会重新初始化 mysql 密码——这会删数据，优先保持密码不变）
- 前端主题：视觉层回退 = 随应用镜像回退；单换主题可把 `design-system/school-library/MASTER.book-teal.md` 的色值写回 `frontend/src/style.css` 令牌区（历史验证：改动面就是这一个文件 + 字体资源）

## 4. 回滚演练检查单

- [ ] 备份文件存在且可解压导入（定期演练 `mysql < backup.sql`）
- [ ] 上一版本镜像在本地或可重新构建
- [ ] 回滚后冒烟：`/api/ping`、登录、借阅查询三件事通过
- [ ] 回滚记录写入 [CHANGELOG.md](../CHANGELOG.md)
