# test — 连接测试

## 语法

```bash
roudan test
```

## 选项

无。

仅在新连接或连接排查时使用；当前任务已验证连接就不必每次查询前再 test。连接选项放在子命令前。

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
roudan --name dev test
```

`dev` 须为已确认的连接；先排除环境变量/YAML 冲突，见 [connections.md](connections.md)。成功只证明可连接并给出数据库产品，不证明当前数据库、用户、schema 就是用户要求的目标。认证/网络失败不要盲目换驱动重试。
