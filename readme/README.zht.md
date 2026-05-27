# roudan-jdbc-cli

[English](../README.md) | [簡體中文](README.zh.md) | [繁體中文](README.zht.md)

JDBC CLI 工具，專為 AI agent 設計。可透過 JDBC 連接任何資料庫執行 SQL，輸出 JSON 到 stdout。

## 一鍵啟動

複製這行貼到 OpenCode / Claude Code / Cursor：

> Install and use roudan-jdbc-cli, a JDBC CLI that lets me execute SQL against any database:  
> https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md

## 快速開始

### 前置條件

- **Java 8+** (JRE 或 JDK，需在 PATH 上) — 從 https://adoptium.net 安裝
- 資料庫對應的 **JDBC 驅動 JAR**（MySQL, PostgreSQL, DM 等）

### 安裝

```bash
npm install -g roudan-jdbc-cli    # 已內含 jar，安裝即用

# 或 Docker
docker pull wsaaaqqq/roudan-jdbc-cli

# 或一行 curl 安裝
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash
```

### 使用

```bash
# ====== 連線持久化（一次登入，隨處使用）======

roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar login
roudan login -f application-dm.yml -j DmJdbcDriver.jar
roudan login -f multi.yml mydb

# 登入後省略連線參數
roudan test
roudan tables
roudan query -s "SELECT * FROM T_USER" --limit 5
roudan use mydb
roudan connections
roudan logout

# ====== 直連模式 ======

roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test
roudan query -s "SELECT * FROM T_USER" --limit 5
roudan query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'
roudan tables
roudan describe -t T_USER
roudan exec -f script.sql
```

## 命令

| 命令 | 說明 |
|------|------|
| `login` | 儲存連線（CLI 參數或 `-f yml`） |
| `logout` | 清除當前連線 |
| `use` | 切換到已儲存的連線 |
| `ls` | 列出已儲存的連線及詳情 |
| `rename` | 重新命名已儲存的連線 |
| `connections` | 列出所有已儲存的連線 |
| `query` | SELECT 查詢 |
| `count` | COUNT 查詢 |
| `modify` | INSERT/UPDATE/DELETE/DDL |
| `tables` | 列出表/檢視 |
| `describe` | 檢視表結構 |
| `test` | 連線測試 |
| `exec` | 執行 SQL 檔案（單交易） |
| `import` | 從 CSV/JSON 匯入資料 |
| `export` | 匯出查詢結果到 CSV/JSON |
| `gen` | 生成 DDL 或 INSERT |
| `tail` | 監控表變化 |
| `begin` | 開始交易 |
| `commit` | 提交交易 |
| `rollback` | 回滾交易 |

`login` 將連線儲存到 `~/.roudan/connections.json`，之後無需重複傳參。

完整參考：[CLI_REFERENCE.md](../doc/CLI_REFERENCE.md)

## License

Apache 2.0
