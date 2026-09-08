#!/usr/bin/env bash
# 学校图书管理系统 —— 一键启动（全栈 Docker）
set -e
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "提示：未找到 .env，将使用 .env.example 中的默认值。"
  echo "      如需自定义，请先执行：cp .env.example .env"
fi

echo "==> 构建并启动 MySQL + 后端 + 前端..."
docker compose up -d --build

echo ""
echo "✅ 启动完成（首次启动请等待 30~60 秒完成建库与种子数据写入）："
echo "   前端:  http://localhost:5173"
echo "   后端:  http://localhost:8080/api"
echo "   MySQL: localhost:3306  (库名 school_library)"
echo ""
echo "   默认账号: admin1 / student1 / teacher1   密码: pass123"
echo ""
echo "停止: docker compose down    停止并清空数据: docker compose down -v"
