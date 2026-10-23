# 04 密码改用 BCrypt（兼容旧版 MD5 惰性升级）

> 涉及提交：`89637b0` refactor: 密码改用 BCrypt，兼容旧版 MD5 惰性升级

## 原来的实现

### 注册 / 重置密码：服务端存 MD5

`UserInfoServiceImpl`：

```java
// register
userInfo.setPassword(SecureUtil.md5(password));
// resetPwd
updateInfo.setPassword(SecureUtil.md5(password));
```

### 登录：直接 `equals` 比对

```java
if (null == userInfo || !userInfo.getPassword().equals(password)) {
    throw new BusinessException("账号或者密码错误");
}
```

### 前端：登录时先 MD5 再发送

`horizonhub-front-web/.../LoginAndRegister.vue`（admin 端 `Login.vue` 同理）：

```js
if (params.password !== cookiePassword) {
    params.password = md5(params.password);   // 前端哈希后传输
}
```

**链路是自洽的、能正常登录**（这条很重要，曾有一份分析误判为「登录恒失败」，经核对前后端确认不成立）：注册时前端传明文 → 后端存 `MD5(明文)`；登录时前端传 `MD5(明文)` → 后端 `equals` 比对 → `MD5(明文)==MD5(明文)` ✓。「记住我」还把 MD5 口令存进 cookie 用于回填。

## 存在的问题

- **MD5 无盐**：彩虹表可秒破，库泄漏即所有口令沦陷。
- **前端哈希 = 哈希即口令**：传输的就是 MD5 值，中间人抓到这个哈希即可重放登录（Pass-the-Hash），无法区分「真实口令」与「哈希凭证」。库泄漏后这个问题不可逆。
- cookie 里存了口令哈希，进一步扩大暴露面。
- AI 时代凭证价值更高，必须先换成服务端加盐哈希。

## 改进后的实现

### 1. 新增 `PasswordUtils`（common）

`horizonhub-common/.../utils/PasswordUtils.java`，统一加密 / 校验，并兼容旧 MD5：

```java
@Component
public class PasswordUtils {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 是否为旧版无盐 MD5（32 位十六进制）。BCrypt 摘要以 $2 开头、约 60 位，不会误判。 */
    public boolean isLegacyMd5(String stored) {
        return stored != null && stored.length() == 32 && stored.matches("[0-9a-fA-F]{32}");
    }

    public String encode(String rawPassword) {           // 明文 → BCrypt
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) return false;
        if (isLegacyMd5(storedPassword)) {
            return SecureUtil.md5(rawPassword).equals(storedPassword);   // 兼容旧库
        }
        return encoder.matches(rawPassword, storedPassword);
    }
}
```

### 2. 服务端 register / resetPwd 改用 `encode`

```java
userInfo.setPassword(passwordUtils.encode(password));     // register
updateInfo.setPassword(passwordUtils.encode(password));   // resetPwd
```

### 3. 登录改用 `matches` + 惰性升级

```java
if (null == userInfo || !passwordUtils.matches(password, userInfo.getPassword())) {
    throw new BusinessException("账号或者密码错误");
}
// 惰性升级：旧版无盐 MD5 校验通过后，重新加密为 BCrypt
if (passwordUtils.isLegacyMd5(userInfo.getPassword())) {
    UserInfo upgrade = new UserInfo();
    upgrade.setPassword(passwordUtils.encode(password));
    this.userInfoMapper.updateByUserId(upgrade, userInfo.getUserId());
}
```

### 4. 前端改传明文，「记住我」只存邮箱

- web / admin 的 `Login.vue` 删除 `md5(params.password)`，直接发送明文。
- web `LoginAndRegister.vue`：`loginInfo` cookie 不再保存 `password`，回填时只恢复 `email`。

### 5. 后台登录改明文比对

`horizonhub-admin/.../AccountController.login`：

```java
// 改前：SecureUtil.md5(adminConfig.getAdminPassword()).equals(password)
// 改后：前端发送明文口令（必须走 HTTPS），与配置中的后台口令直接比对
if (!adminConfig.getAdminAccount().equals(account) || !adminConfig.getAdminPassword().equals(password)) {
    throw new BusinessException("账号或密码错误");
}
```

生产口令由环境变量 `ADMIN_PASSWORD` 注入强随机值（见 [03-凭据与profile.md](03-凭据与profile.md)）。

## 如何工作（改造后）

- **新注册用户**：前端明文（HTTPS）→ 服务端 `BCrypt(明文)` 入库。BCrypt 自带随机盐、强度可调。
- **老用户（库中仍是 MD5）**：登录时前端传明文 → `matches` 识别为旧 MD5，用 `md5(明文)` 比对通过 → 立即用 `encode` 把该用户密码升级为 BCrypt（**惰性升级，无需停机批量迁移**）。下一次登录走 BCrypt 分支。
- **安全性**：哈希只在服务端做，传输的是明文（依赖 HTTPS）；库泄漏后攻击者拿到的是 BCrypt 摘要，无法直接重放。
- 后台口令明文存在于内存配置（来自环境变量），不再有「前端 MD5 当口令」的 Pass-the-Hash 问题。

## 验证 / 注意事项

- **必须走 HTTPS**：前端改传明文后，生产务必启用 TLS，否则口令可被窃听。
- **旧库无需迁移脚本**：惰性升级会随用户逐个登录自动完成；如需立即全量升级，可写一次性脚本遍历 `isLegacyMd5` 的记录。
- `spring-security-crypto`（仅含 BCryptPasswordEncoder，不引入完整 Spring Security）在 `5c9f0d7` 中引入。
- ⚠️ 老用户首次登录后密码才升级；若担心过渡期，可加监控统计仍为 MD5 的账户数。
