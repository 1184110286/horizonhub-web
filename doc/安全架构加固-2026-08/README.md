# 安全架构加固记录（2026-08）

本目录记录 HorizonHub 在**引入 AI 问答助手之前**进行的「第一阶段」安全与架构加固。每份文档遵循统一结构：

- **原来的实现**——改造前的代码长什么样、如何工作
- **存在的问题**——为什么要改
- **改进后的实现**——改造后的代码长什么样、如何工作
- **涉及的提交**——对应的 git commit
- **验证 / 注意事项**

## 为什么做这一轮加固

后续要引入的 AI 对话助手有一个特殊性质：**它会代用户执行写操作（发帖、删帖等）**。这把许多原本「仅靠使用方自律」或「风险潜伏」的问题直接激活成了必须先解决的阻塞项。本轮按「先打地基再加楼层」的顺序，把引入 AI 前的结构性/安全性债务清理掉。

## 提交清单与文档映射

| 提交 | 类型 | 说明 | 详细文档 |
| --- | --- | --- | --- |
| `5c9f0d7` | chore | 引入密码加密、富文本净化、参数校验依赖 | 散见于 04 / 06 / 09 |
| `b7b999c` | refactor | 抽取公共参数校验切面与文件访问基类到 common 模块 | [01-公共模块抽取.md](01-公共模块抽取.md) |
| `ca2a47a` | fix | 用 @Profile 物理隔离开发环境自动登录后门 | [02-开发后门隔离.md](02-开发后门隔离.md) |
| `f791169` | chore | 敏感配置外置化并拆分 dev/prod profile | [03-凭据与profile.md](03-凭据与profile.md) |
| `89637b0` | refactor | 密码改用 BCrypt，兼容旧版 MD5 惰性升级 | [04-密码BCrypt.md](04-密码BCrypt.md) |
| `575321b` | refactor | 写操作接口改为 POST 规避 GET-CSRF（后台登录改明文比对） | [05-写接口POST.md](05-写接口POST.md) |
| `17c5c8b` | feat | 富文本正文服务端 XSS 净化（并为文章多表写补事务） | [06-富文本XSS净化.md](06-富文本XSS净化.md)、[08-事务边界.md](08-事务边界.md) |
| `8618a17` | fix | 评论批量删除/审核补外层事务 | [08-事务边界.md](08-事务边界.md) |
| `dc2bf33` | refactor | orderBy 排序字段白名单校验，消除 SQL 注入面 | [07-orderBy白名单.md](07-orderBy白名单.md) |
| `fa888fb` | feat | 引入 JSR-303 校验基础设施与全局异常增强 | [09-JSR303校验.md](09-JSR303校验.md) |
| `b0787ad` | fix | HtmlSanitizer 改用 Safelist.relaxed 适配 jsoup 1.17 API | [06-富文本XSS净化.md](06-富文本XSS净化.md) |
| `eb6fdd6` | feat | 引入 Token 基础设施（jjwt + Redis 有状态 Token） | [10-Session到Token演进.md](10-Session到Token演进.md) |
| `a53b23f` | feat | 认证拦截器双轨化，取当前用户兼容 Token 与 Session | [10-Session到Token演进.md](10-Session到Token演进.md) |
| `bc76175` | feat | 新增 Token 登录/登出接口供 AI 与第三方调用 | [10-Session到Token演进.md](10-Session到Token演进.md) |
| `b79f2cd` | feat | 新增 horizonhub-ai 模块骨架与 Spring AI 配置 | [11-AI助手模块.md](11-AI助手模块.md) |
| `3dd9450` | feat | 实现 AI 对话（检索增强 + SSE 流式）与论坛上下文检索 | [11-AI助手模块.md](11-AI助手模块.md) |
| `6049ddc` | feat | 前端悬浮 AI 助手组件（流式对话、登录检测） | [11-AI助手模块.md](11-AI助手模块.md) |

## 第二阶段：Session → Token 演进（已完成）

第一阶段清理了引入 AI 前的安全 / 架构债务；第二阶段着手**认证演进**，为 AI 服务铺路。采用 **双轨 + Redis 有状态 Token**：现有前端继续 Session（零改动），AI / 第三方走 `Authorization: Bearer <token>`，登录态存 Redis（可主动吊销）。详见 [10-Session到Token演进.md](10-Session到Token演进.md)。

## 编译验证

本轮全部改动已通过 `mvn compile`（三模块 common / web / admin）。项目原本不含自动化测试，下列核心流程建议本地手动回归：

- 登录：明文密码登录；旧用户的 MD5 密码应在登录时**惰性升级**为 BCrypt
- 发帖：富文本正文里的 `<script>` 等被剥离，正常图片 / 排版保留
- 评论 / 点赞 / 管理端审核：写操作走 POST，事务正确
- 本地开箱：`application.yml` 默认 `dev` profile，可直接启动

## 刻意延后（非遗漏）

- **`postArticle` 的文件 IO 移出事务**：该方法事务边界已存在（`@Transactional`），但封面 / 附件等文件上传仍发生在事务内（长事务持有连接）。原计划改为「先事务外上传、仅 DB 写入库走事务」，但因该法逻辑复杂（封面 / 附件 / 正文图片路径重写 / 积分交织）且当时无法可靠编译验证，判定为高风险重构，暂缓。
- **JSR-303 仅落地基础设施**：依赖 + 全局异常处理已就位，但现有接口仍用 `@VerifyParam`；未来的 AI 模块 DTO 才使用 `@Valid`。

## 遗留小清理

两个旧的 `OperationAspect.java`（web / admin）因提交期间无法物理删除文件，被降级为空 `@Deprecated` 桩类（不影响功能与编译，不参与拦截）。可在后续提交中物理删除。
