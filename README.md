# ViaProxyAuth-WDPE

WaterdogPE 插件，用于验证通过 ViaProxy 连接的 Java 版玩家身份。

## 工作原理

当 Java 版玩家通过 ViaProxy 连接到 WaterdogPE 代理端时，ViaProxy 会在 clientData 中附带一个基于 HMAC-SHA256 签名的认证 Token。本插件在 `PlayerPreAuthEvent` 阶段拦截并验证该 Token，确保连接来自受信任的 ViaProxy 实例。

- **原版基岩版玩家** — 不受影响，继续使用 Xbox 认证
- **ViaProxy 玩家** — 通过共享密钥 + 时间戳签名验证身份，防止重放攻击

## Token 格式

```
Base64(HMAC-SHA256(secret, UUID:Name:Timestamp)):Timestamp
```

## 配置

`config.yml`:

```yaml
# 必须与 ViaProxy viabedrock.yml 中的 viaproxy-auth-secret 保持一致
auth-secret: ""
# Token 有效期（秒），防止重放攻击
token-timeout: 30
```

> 如果 `auth-secret` 为空，插件将跳过验证，允许所有 ViaProxy 连接。

## 构建

需要 Java 17+。

```bash
mvn clean package
```

构建产物位于 `target/ViaProxyAuth-WDPE-1.0.0.jar`。

## 安装

将构建产物放入 WaterdogPE 的 `plugins` 目录，重启代理端即可。
