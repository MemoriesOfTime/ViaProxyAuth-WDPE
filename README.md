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

## Java TAB 延迟

基岩版 `PlayerListPacket` 没有 ping 字段，Java 原版 TAB 会把未知延迟画成 `X`。本插件按下游服分组，把 `ProxiedPlayer.getPing()`（上游 RakNet RTT）编成 `waterdog:player_latency_v1` ScriptMessage 快照发给同服所有在线玩家。ViaBedrock 把它写成 Java `PLAYER_INFO_UPDATE`。

- 默认每 20 tick（1 秒）广播一次
- 进服（`InitialServerConnectedEvent`）和切服（`TransferCompleteEvent`）会立刻再发一次
- **不**用 `ViaProxyAuthToken` / `isJavaClient()` 过滤接收者：正式环境经常不配密钥
- 原版基岩客户端会忽略未知 ScriptMessage
- `onStartup` 里调用 `ProtocolCodecs.addHandledPacket(ScriptMessagePacket.class)`，避免 fast codec 丢掉插件自己构造的包

```yaml
broadcast-player-latency: true
player-latency-interval-ticks: 20
```

## 构建

需要 Java 17+。

```bash
mvn clean package
```

构建产物位于 `target/ViaProxyAuth-WDPE-1.1.0.jar`。

## 安装

将构建产物放入 WaterdogPE 的 `plugins` 目录，重启代理端即可。
