# roudan-jdbc-cli

[English](README.md) | [简体中文](README.zh.md)

JDBC 数据库 CLI 工具，专为 AI agent 通过 subprocess 调用设计。输入命令行参数，输出 JSON 到 stdout。

## 一键激活（给 AI Agent 的提示词）

复制下面这行，发给 OpenCode / Claude Code / Cursor：

> Install and use roudan-jdbc-cli, a JDBC CLI that lets me execute SQL against any database:  
> https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md

或者一步到位：

```bash
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md
```

## 快速开始

### 前置要求

- Java 8+

### 安装

```bash
# npm（推荐）
npm install -g roudan-jdbc-cli

# Docker
docker pull wsaaaqqq/roudan-jdbc-cli

# 直接下载
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash
```

### 使用

```bash
# 测试连接
roudan-jdbc-cli -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test

# 查询
roudan-jdbc-cli query -s "SELECT * FROM T_USER" --limit 5

# 命名参数
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'

# 插入
roudan-jdbc-cli modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"张三"}'

# 查看表列表
roudan-jdbc-cli tables

# 查看表结构
roudan-jdbc-cli describe -t T_USER
```

## 命令参考

| 命令 | 功能 |
|------|------|
| `query` | SELECT 查询 |
| `count` | 计数查询 |
| `modify` | INSERT/UPDATE/DELETE/DDL |
| `tables` | 列出表/视图 |
| `describe` | 查看表结构 |
| `test` | 连接测试 |
| `begin` | 开始事务 |
| `commit` | 提交事务 |
| `rollback` | 回滚事务 |

详细用法见 [CLI_REFERENCE.md](doc/CLI_REFERENCE.md)

## OpenCode Skill

本仓库包含 [OpenCode](https://opencode.ai) skill：

```bash
# 安装 skill
cp -r skill ~/.agents/skills/roudan-jdbc
```

或添加到 `opencode.json`：

```json
{
  "skills": {
    "paths": ["/path/to/roudan-jdbc-cli/skill"]
  }
}
```

## 构建

```bash
# 前置：构建 roudan-core
cd ../roudan-core && mvn install -DskipTests

# 构建本项目
cd ../roudan-jdbc-cli
mvn package
# 产物：target/roudan-jdbc-cli.jar
```

## 架构

```
roudan-jdbc-cli
  ├── Picocli CLI 入口 → 解析参数
  ├── ConfigLoader    → 配置合并（YAML + 命令行）
  ├── DriverLoader    → URLClassLoader 动态加载 JDBC 驱动
  ├── DataSourceFactory → 注册到 roudan-core
  └── CommandExecutor → RD.query() / RD.modify() / JDBC 元数据
```

基于 [roudan-core](https://github.com/wsaaaqqq/xdb) 轻量级 JDBC 封装库。

## License

Apache 2.0
