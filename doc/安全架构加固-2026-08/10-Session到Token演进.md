# 10 Session → Token 演进（双轨 + Redis 有状态 Token）

> 第二阶段第一步。涉及提交：
> - `eb6fdd6` feat: 引入 Token 基础设施（jjwt + Redis 有状态 Token）
> - `a53b23f` feat: 认证拦截器双轨化，取当前用户兼容 Token 与 Session
> - `bc76175` feat: 新增 Token 登录/登出接口供 AI 与第三方调用

## 原来的实现

认证完全基于 `HttpSession`（Tomcat 内存）：

- **web 端**：`LoginInterceptor` 从 `session.getAttribute(SESSION_KEY)` 取 `SessionWebUserDto` 判断登录态。
- **admin 端**：`AppInterceptor` 从 session 取 `SessionAdminUserDto`。
- 登录接口校验密码后 `session.setAttribute(...)`；前端靠 Cookie 携带 `JSESSIONID`。
- 取当前用户：`FrequencyLimitAspect`、`BaseController.getUserInfoFromSession` 都直接读 session。

Session 的读写点共 9 处，集中在 2 个拦截器、2 个 `AccountController`、`FrequencyLimitAspect`、`BaseController`、2 个 dev 自动登录 handler。另：图片验证码 `CHECK_CODE_KEY` 也借 session 存。

## 存在的问题

- **不利于跨服务**：Session 是有状态的服务端内存，引入 AI 助手后，AI 作为独立服务 / 外部 LLM 编排调用后端，Session 共享成本高。
- **不利于水平扩展**：多实例部署需 Session 粘性或共享，麻烦。
- **不可主动吊销**：Session 过期前无法强制下线（除非维护黑名单）。
- AI 代理需要「独立、可控、可封禁」的凭证，Session Cookie 不合适。

## 改进后的实现

### 设计决策（采纳明确要求：用 Redis 记录登录状态）

采用 **JWT 外壳 + Redis 登录态（有状态 Token）**，而非纯 JWT 自验证：

| | 纯 JWT（无状态） | 本方案（Redis 有状态）✓ |
| --- | --- | --- |
| 登录态载体 | JWT 自包含 | JWT 载 `tokenId` + Redis 存用户信息 |
| 校验 | 验签即可 | 验签 + 查 Redis |
| 主动吊销 | 难（需黑名单） | 删 Redis key 即下线 |
| 多实例 | 天然支持 | 天然支持（Redis 共享） |

JWT 仅作为「载 tokenId + HMAC 签名防伪造」的令牌外壳；真正的登录态（`LoginUserContext`）以 JSON 存 Redis，key = `horizonHub:token:{tokenId}`，带 TTL。校验 = 验签 + 查 Redis，**Redis 无记录即失效**。

### 双轨并存（低风险渐进）

现有 web/admin 前端**继续走 Session Cookie，零改动**；AI / 第三方 / 移动端走 `Authorization: Bearer <token>`。拦截器**优先认 Token，回落 Session**。

### 新增组件（均在 horizonhub-common）

| 组件 | 职责 |
| --- | --- |
| `TokenConfig` | 密钥 / TTL / 签发者，走 `${ENV}`（dev 给默认密钥，prod 无默认 fail-fast） |
| `TokenScope`（枚举） | `WEB` / `ADMIN` / `AI_AGENT`，区分调用方、防串用 |
| `LoginUserContext` | 统一登录上下文（tokenId/userId/nickName/account/isAdmin/scope） |
| `TokenService` | 签发 `createToken` / 校验 `parseToken` / 续期 `renew` / 吊销 `invalidate` |
| `TokenContextHolder` | 请求级 ThreadLocal，存当前请求的登录上下文 |

### TokenService 核心逻辑

```java
// 签发：生成 tokenId，登录态写 Redis（TTL），签 JWT 外壳
public String createToken(LoginUserContext context) {
    String tokenId = IdUtil.simpleUUID();
    context.setTokenId(tokenId);
    stringRedisTemplate.opsForValue().set(redisKey(tokenId), JsonUtils.object2Json(context), ttlSeconds, SECONDS);
    return Jwts.builder().issuer(...).subject(tokenId).claim("scope", ...).signWith(secretKey).compact();
}

// 校验：验签 + 查 Redis；无记录即失效
public LoginUserContext parseToken(String token) {
    Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    String json = stringRedisTemplate.opsForValue().get(redisKey(claims.getSubject()));
    return json == null ? null : JsonUtils.json2Object(json, LoginUserContext.class);
}
```

### 拦截器双轨化

`LoginInterceptor`（web）/ `AppInterceptor`（admin）的 `preHandle`：

```
1. 优先 Token：从 Authorization 头取 Bearer token
   - parseToken 成功 → 校验 scope → set TokenContextHolder → renew（滑动续期）→ 放行
   - scope 校验：web 接受 WEB/AI_AGENT（拒绝 ADMIN 串用）；admin 仅接受 ADMIN
2. 回落 Session：原 session.getAttribute 逻辑（现有前端）
3. 都不满足：dev 自动登录，否则抛 CODE_901
afterCompletion：TokenContextHolder.clear()（防线程复用泄漏）
```

### 取当前用户兼容双轨

`BaseController.getUserInfoFromSession` 与 `FrequencyLimitAspect` 改为：优先 `TokenContextHolder.get()`，为空再回落 session。Token 路径的用户也能正确取到 `userId`（限频、业务逻辑一致）。

### Token 登录 / 登出接口

- `POST /tokenLogin`（web / admin 各一）：账号 + 密码（明文、走 HTTPS，复用第一阶段 BCrypt 校验），**免图形验证码**（API 友好），签发对应 scope 的 Token，返回 `{token, userInfo/account}`。
- `POST /tokenLogout`：从 Authorization 头取 token → `invalidate`（删 Redis，立即失效）。
- admin 的 `/tokenLogin` 加入 `AppInterceptor` 白名单。

## 如何工作（改造后）

**典型 AI / 第三方调用流程：**

1. `POST /api/account/tokenLogin`（email + password）→ 拿到 `token`
2. 后续请求带 `Authorization: Bearer <token>`
3. 拦截器 `parseToken` 验签 + 查 Redis → set 上下文 → 放行
4. 业务 / 限频从 `TokenContextHolder` 取用户
5. `POST /api/account/tokenLogout` 或 Redis TTL 到期 → 失效

**现有前端流程：** 不变，仍走 Session Cookie（拦截器回落 Session 分支）。

**安全特性：**
- Token 防伪造（HMAC 签名）+ 可吊销（删 Redis）
- scope 隔离：前台 token 不能调后台、后台 token 不能调前台
- 滑动续期（每次有效请求续 TTL）

## 验证 / 注意事项

- 编译：`mvn compile` 三模块通过。
- **必须走 HTTPS**：tokenLogin 传明文密码，生产务必 TLS。
- **Redis 必须可用**：Token 校验依赖 Redis；Redis 宕则所有 token 用户无法认证（Session 用户不受影响）。
- 本地验证：`POST /api/account/tokenLogin`（dev 默认密钥开箱可用）拿 token → 带 `Authorization: Bearer <token>` 调 `GET /api/account/getUserInfo`（或任意需登录接口）应放行 → `POST /api/account/tokenLogout` 后该 token 立即失效。
- 图片验证码仍用 session（短生命周期，未纳入 Token；纯 Token 调用方不走验证码流程）。

## 刻意延后（本阶段未做）

- **Refresh Token**：当前是单 Token + 滑动续期。若要「access 短期 + refresh 长期」双 Token 体系，后续再加。
- **AI_AGENT scope 细化**：AI 模块接入时，再为 AI 代理设计更细的权限 / 配额（结合第一阶段提到的「AI 代理纳入限频」）。
- **限频对 Token 用户的 IP 维度**：当前限频按 userId；未登录接口（tokenLogin）暂无限频，后续可加 IP 维度防爆破。
- **前端迁移到 Token**：双轨下前端仍用 Session；若将来要统一为纯 Token，可在前端切到 localStorage + Authorization 头，届时下线 Session 分支。
