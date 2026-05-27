# roudan-jdbc-cli

[English](../README.md) | [简体中文](README.zh.md) | [繁體中文](README.zht.md) | [한국어](README.ko.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Français](README.fr.md) | [Italiano](README.it.md) | [Dansk](README.da.md) | [日本語](README.ja.md) | [Polski](README.pl.md) | [Русский](README.ru.md) | [Bosanski](README.bs.md) | [العربية](README.ar.md) | [Norsk](README.no.md) | [Português (Brasil)](README.br.md) | [ไทย](README.th.md) | [Türkçe](README.tr.md) | [Українська](README.uk.md) | [বাংলা](README.bn.md) | [Ελληνικά](README.gr.md) | [Tiếng Việt](README.vi.md)

JDBC 数据�?CLI 工具，专�?AI agent 通过 subprocess 调用设计。输入命令行参数，输�?JSON �?stdout�?
## 一键激活（�?AI Agent 的提示词�?
复制下面这行，发�?OpenCode / Claude Code / Cursor�?
> Install and use roudan-jdbc-cli, a JDBC CLI that lets me execute SQL against any database:  
> https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md

或者一步到位：

```bash
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md
```

## 快速开�?
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
# ====== 连接持久化（登录一次，随处可用�?======

# 通过命令行参数登�?rd -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar login

# 通过 YAML 文件登录（自动识别任意层级的 driver-class-name�?rd login -f application-dm.yml -j DmJdbcDriver.jar

# 多数据源 YAML，指定数据源名称
rd login -f multi.yml mydb

# 登录后，后续命令无需再传连接参数�?rd test
rd tables
rd query -s "SELECT * FROM T_USER" --limit 5

# 切换到另一个已保存的连�?rd use mydb

# 临时使用某个已保存的连接
rd --name mydb tables

# 列出已保存的连接
rd connections

# 登出（清除当前连接）
rd logout

# ====== 直连模式（单次执行，不保存连接） ======

# 测试连接
rd -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test

# 查询
rd query -s "SELECT * FROM T_USER" --limit 5

# 命名参数
rd query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'

# 插入
rd modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"张三"}'

# 查看表列�?rd tables

# 查看表结�?rd describe -t T_USER
```

## 命令参�?
| 命令 | 功能 |
|------|------|
| `login` | 保存连接供后续复用（支持 `-f yml` 和命令行参数�?|
| `logout` | 清除当前已保存的连接 |
| `use` | 切换到另一个已保存的连�?|
| `connections` | 列出所有已保存的连�?|
| `query` | SELECT 查询 |
| `count` | 计数查询 |
| `modify` | INSERT/UPDATE/DELETE/DDL |
| `tables` | 列出�?视图 |
| `describe` | 查看表结�?|
| `test` | 连接测试 |
| `begin` | 开始事�?|
| `commit` | 提交事务 |
| `rollback` | 回滚事务 |
| `--name <name>` | （全局选项）临时使用某个已保存的连�?|

### 配置优先�?
```
命令行参数（最高）> 环境变量 > 已保存连�?> YAML 配置文件
```

`login` 命令将连接参数保存到 `~/.roudan/connections.json`。登录后，后续所有命令自动复用已保存的连接�?
详细用法�?[CLI_REFERENCE.md](doc/CLI_REFERENCE.md)

## OpenCode Skill

本仓库包�?[OpenCode](https://opencode.ai) skill�?
```bash
# 安装 skill
cp -r skill ~/.agents/skills/roudan-jdbc
```

或添加到 `opencode.json`�?
```json
{
  "skills": {
    "paths": ["/path/to/roudan-jdbc-cli/skill"]
  }
}
```

## 构建

```bash
# 前置：构�?roudan-core
cd ../roudan-core && mvn install -DskipTests

# 构建本项�?cd ../roudan-jdbc-cli
mvn package
# 产物：target/roudan-jdbc-cli.jar
```

## 架构

```
roudan-jdbc-cli
  ├── Picocli CLI 入口 �?解析参数
  ├── ConfigLoader    �?配置合并（YAML + 命令行）
  ├── DriverLoader    �?URLClassLoader 动态加�?JDBC 驱动
  ├── DataSourceFactory �?注册�?roudan-core
  └── CommandExecutor �?RD.query() / RD.modify() / JDBC 元数�?```

基于 [roudan-core](https://github.com/wsaaaqqq/xdb) 轻量�?JDBC 封装库�?
## License

Apache 2.0
