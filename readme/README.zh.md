# roudan-jdbc-cli

[English](../README.md) | [简体中文](README.zh.md) | [繁體中文](README.zht.md)

JDBC CLI 工具，专为 AI agent 设计。可通过 JDBC 连接任何数据库执行 SQL，输出 JSON 到 stdout。

## 一键激活

复制这行粘贴到 OpenCode / Claude Code / Cursor：

> Install and use roudan-jdbc-cli, a JDBC CLI that lets me execute SQL against any database:  
> https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md

## 快速开始

### 前置条件

- **Java 8+** (JRE 或 JDK，需在 PATH 上) — 从 https://adoptium.net 安装
- 数据库对应的 **JDBC 驱动 JAR**（MySQL, PostgreSQL, DM 等）

### 安装

\`\`\`bash
npm install -g roudan-jdbc-cli    # 已内含 jar，安装即用
\`\`\`

### 使用

```bash
# ====== 连接持久化（一次登录，随处使用）======

roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar login
roudan login -f application-dm.yml -j DmJdbcDriver.jar
roudan login -f multi.yml mydb

# 登录后省略连接参数
roudan test
roudan tables
roudan query -s "SELECT * FROM T_USER" --limit 5
roudan use mydb
roudan connections
roudan logout

# ====== 直连模式 ======

roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test
roudan query -s "SELECT * FROM T_USER" --limit 5
roudan query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'
roudan tables
roudan describe -t T_USER
roudan exec -f script.sql
```

## 命令

| 命令 | 说明 |
|------|------|
| `login` | 保存连接（CLI 参数或 `-f yml`） |
| `logout` | 清除当前连接 |
| `use` | 切换到已保存的连接 |
| `ls` | 列出已保存的连接及详情 |
| `rename` | 重命名已保存的连接 |
| `connections` | 列出所有已保存的连接 |
| `query` | SELECT 查询 |
| `count` | COUNT 查询 |
| `modify` | INSERT/UPDATE/DELETE/DDL |
| `tables` | 列出表/视图 |
| `describe` | 查看表结构 |
| `test` | 连接测试 |
| `exec` | 执行 SQL 文件（单事务） |
| `import` | 从 CSV/JSON 导入数据 |
| `export` | 导出查询结果到 CSV/JSON |
| `gen` | 生成 DDL 或 INSERT |
| `tail` | 监控表变化 |
| `begin` | 开始事务 |
| `commit` | 提交事务 |
| `rollback` | 回滚事务 |

`login` 将连接保存到 `~/.roudan/connections.json`，之后无需重复传参。

完整参考：[CLI_REFERENCE.md](../doc/CLI_REFERENCE.md)

## License

Apache 2.0
