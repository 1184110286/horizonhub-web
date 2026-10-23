# 05 写操作接口改为 POST（规避 GET-CSRF）

> 涉及提交：`575321b` refactor: 写操作接口改为 POST 规避 GET-CSRF（后台登录改明文比对）

## 原来的实现

所有接口（读 + 写）统一用 `@RequestMapping`，未限定 HTTP 方法：

```java
@RequestMapping("/postArticle")     // 既接受 POST，也接受 GET
@PostMapping? 不，是 @RequestMapping
public ResponseVO<...> postArticle(...) { ... }

@RequestMapping("/login")
@RequestMapping("/updateArticle")
@RequestMapping("/postComment")
@RequestMapping("/resetPwd")
@RequestMapping("/saveBoard")
@RequestMapping("/delArticle")
...
```

认证基于 `HttpSession`（Cookie），且项目**没有 CSRF Token 防护**。

## 存在的问题

- **GET-CSRF 风险**：`@RequestMapping` 默认接受 GET。结合 Session Cookie 认证 + 无 CSRF Token，在浏览器 SameSite 默认为 Lax 的策略下，**GET 跨站请求会自动携带 Cookie**。攻击者诱导用户访问恶意页面，构造形如 `<img src="https://forum/api/forum/postArticle?...">` 的请求，即可让用户在不知情下**发帖 / 改帖 / 评论 / 重置密码**等。
- 语义上，写操作本就不该接受 GET。
- 引入 AI 代理执行写操作前，必须先把所有写接口收敛到非 GET 方法，并要求 AI 代理用独立 Token（不复用浏览器 Cookie）。

## 改进后的实现

把所有**写操作**接口从 `@RequestMapping` 改为 `@PostMapping`；读操作保留 `@RequestMapping`。改造范围（9 个 controller，38 行变更）：

**web 端：**
- `AccountController`：`sendEmailCode` / `register` / `login` / `logout` / `resetPwd`
- `FileController`：`uploadImage`
- `ForumArticleController`：`postArticle` / `updateArticle` / `doLike` / `attachmentDownload`
- `ForumCommentController`：`doLike` / `changeTopType` / `postComment`
- `UserCenterController`：`updateUserInfo`

**admin 端：**
- `AccountController`：`login`
- `UserInfoController`：`updateUserStatus` / `sendMessage`
- `SysSettingController`：`saveSetting`
- `ForumBoardController`：`saveBoard` / `delBoard` / `changeBoardSort`
- `ForumArticleController`：`delArticle` / `updateBoard` / `topArticle` / `auditArticle` / `delComment` / `auditComment`

示例：

```java
@PostMapping("/postArticle")     // 改前 @RequestMapping
@GlobalInterceptor(checkLogin = true, checkParams = true, frequencyType = UserOperFrequencyTypeEnum.POST_ARTICLE)
public ResponseVO<...> postArticle(...) { ... }
```

附带：本提交同时完成了「后台登录改明文比对」（与 [04-密码BCrypt.md](04-密码BCrypt.md) 配套），见 04 文档说明。

## 如何工作（改造后）

- **无破坏**：前端 `Request.js` 对**所有请求都用 POST**（`instance.post(url, formData, ...)`），因此写接口改 `@PostMapping` 对前端零影响，不会破坏现有调用。
- **GET-CSRF 被阻断**：写接口现在只接受 POST。跨站 GET 请求会被 Spring 以 `405 Method Not Allowed` 拒绝；即便浏览器带上了 Cookie，也不会执行写操作。
- 配合 Session Cookie 的 `SameSite=strict`（见 [03](03-凭据与profile.md) 生产配置），CSRF 面被进一步收窄。

## 验证 / 注意事项

- 改造前已确认前端（web + admin）的 `Request.js` 统一用 `instance.post(...)`，所以无破坏。若将来有第三方 / 移动端按 GET 调用这些接口，会被拒绝——这是预期行为。
- AI 代理接入写接口时，应使用**独立的受限 Token**（如 OAuth2 scope），绝不可复用浏览器 Session Cookie，否则仍可能被 CSRF / 会话劫持利用。
- 读接口（`loadArticle` / `loadComment` / `getArticleDetail` / `loadBoard` 等）保留 `@RequestMapping` 不变。
