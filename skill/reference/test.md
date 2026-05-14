# test — 连接测试

## 语法

```bash
roudan-jdbc-cli test
```

## 选项

无。

## 输出（成功）

```json
{
  "success": true,
  "message": "connection ok",
  "dbProduct": "MySQL 8.0.33",
  "timeMs": 120
}
```

## 输出（失败）

```json
{
  "success": false,
  "message": "connection failed",
  "error": "CommunicationsException: ...",
  "errorCode": "CONNECTION_ERROR",
  "timeMs": 5023
}
```

## 示例

```bash
roudan-jdbc-cli -u jdbc:mysql://localhost:3306/test -n root -p 123456 -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test
```
