# work11 - 管理端接口开发技术实现报告

## 1. 目标与范围

本报告基于今日在 `horizonhub-admin` 模块完成的接口开发工作，详细说明管理端核心接口的实现方式、拦截链路、参数校验策略与文件处理方案。

本次覆盖的接口与能力如下：

- 账号与登录：`/checkCode`、`/login`
- 版块管理：`/board/loadBoard`、`/board/saveBoard`、`/board/delBoard`、`/board/changeBoardSort`
- 文章与评论管理：`/forum/loadArticle`、`/forum/delArticle`、`/forum/updateBoard`、`/forum/getAttachment`、`/forum/attachmentDownload`、`/forum/topArticle`、`/forum/auditArticle`、`/forum/loadComment`、`/forum/loadComment4Article`、`/forum/delComment`、`/forum/auditComment`
- 文件访问：`/file/getAvatar/{userId}`、`/file/getImage/{imageFolder}/{imageName}`
- 管理端统一拦截与参数校验：`AppInterceptor` + `OperationAspect`
- 启动期初始化：系统配置预热到内存缓存

报告风格延续前序 work09、work10，重点关注“控制层编排 + 切面治理 + 服务层复用 + 文件存储”的完整实现细节。

## 2. 总体架构设计

### 2.1 分层职责

- 控制层（`horizonhub-admin/controller`）
  - 负责接口路由、参数接收、请求编排与响应包装。
  - 不承载复杂业务，复杂逻辑统一下沉至 Service。
- 切面层（`horizonhub-admin/aspect/OperationAspect`）
  - 统一处理带 `@GlobalInterceptor` 注解接口的参数校验。
  - 通过 `@VerifyParam` 实现基础类型和对象字段级校验。
- 拦截器层（`horizonhub-admin/interceptor/AppInterceptor`）
  - 全局拦截管理端接口，统一执行登录态检查。
  - 在开发模式下支持自动注入管理员会话，降低联调成本。
- 服务层（`horizonhub-common/service`）
  - 实现板块、文章、评论、附件等核心业务能力。
  - 提供分页查询、批量操作、状态更新等复用能力。

### 2.2 关键设计原则

- 管理端统一走登录校验，避免接口级权限遗漏。
- 参数校验通过切面抽离，减少控制器重复代码。
- 控制器尽量保持“薄”，仅做参数整理和服务调用。
- 文件读写路径统一由配置项 `projectFolder` 拼接，便于部署迁移。
- 复用已有通用响应体 `ResponseVO` 与分页结构 `PaginationResultVO`，保持前后端契约稳定。

## 3. 统一拦截与参数校验实现

## 3.1 请求拦截链路

管理端拦截链路由两层组成：

1. `WebAppConfigurer` 注册 `AppInterceptor`，对 `/**` 全路径生效。
2. `AppInterceptor.preHandle()` 执行会话登录校验。
3. 命中 `@GlobalInterceptor` 的控制器方法进入 `OperationAspect`。
4. `OperationAspect` 按 `@VerifyParam` 执行参数校验。
5. 校验通过后进入具体业务方法。

这套方案将“权限校验”和“参数校验”拆分为独立治理点，保证接口行为一致性。

## 3.2 登录校验策略（AppInterceptor）

`AppInterceptor` 对所有管理端请求进行前置处理，核心策略如下：

- 放行登录相关接口：URI 包含 `checkCode` 或 `login` 时直接通过。
- 读取 `SESSION_KEY` 下的 `SessionAdminUserDto` 判断是否登录。
- 若未登录且开发模式开启（`isDev.open=true`），自动注入管理员会话。
- 若仍无会话，抛出 `CODE_901` 业务异常，阻断请求。

设计价值：

- 防止开发过程中因遗漏注解导致未登录可访问敏感接口。
- 通过开发模式开关实现“生产严格、开发高效”的行为隔离。

## 3.3 参数校验策略（OperationAspect）

`OperationAspect` 通过环绕通知拦截 `@GlobalInterceptor` 注解方法：

- `checkParams=true` 时触发参数校验。
- 校验粒度覆盖两类：
  - 基础类型：`String`、`Integer`、`Long`
  - 对象类型：反射遍历字段，检查字段上的 `@VerifyParam`
- 校验规则：
  - 必填校验（`required`）
  - 长度范围校验（`min/max`）
  - 正则校验（`regex`）

异常处理统一转换为业务异常码：

- 参数不合法：`CODE_600`
- 系统异常：`CODE_500`

## 4. 账号与登录接口实现

## 4.1 `/checkCode` 图片验证码接口

实现位置：`AccountController.checkCode`

核心流程：

1. 创建验证码对象 `CreateImageCode(130, 38, 5, 10)`。
2. 设置响应头禁止缓存。
3. 将验证码文本写入 session：`Constants.CHECK_CODE_KEY`。
4. 将验证码图片流写入响应输出流。

实现要点：

- 验证码由服务端生成并存储于 session，防止客户端伪造。
- 禁止缓存避免浏览器复用旧验证码图片。

## 4.2 `/login` 管理员登录接口

实现位置：`AccountController.login`

输入参数：

- `account`
- `password`（前端传入 MD5 后的密文）
- `checkCode`

核心流程：

1. 校验验证码是否与 session 一致（忽略大小写）。
2. 读取配置 `adminConfig` 中管理账号与密码。
3. 使用 `SecureUtil.md5(adminConfig.getAdminPassword())` 与传入密码比对。
4. 校验通过后写入 `SessionAdminUserDto` 到 session。
5. 在 `finally` 中移除验证码，保证一次性使用。

设计要点：

- 验证码失效逻辑放在 `finally`，无论成功失败都销毁，防爆破。
- 登录态只保存必要字段（账号），控制 session 数据规模。

## 5. 版块管理接口实现

## 5.1 `/board/loadBoard` 加载版块树

实现位置：`ForumBoardController.loadBoard`

- 直接调用 `forumBoardService.getBoardTree(null)` 获取完整树。
- 返回树结构用于后台版块管理页渲染。

## 5.2 `/board/saveBoard` 新增/修改版块

实现位置：`ForumBoardController.saveBoard`

入参能力：

- `boardId` 为 null 时新增，非 null 时更新。
- `pBoardId`、`boardName` 为必填。
- 支持 `cover` 封面图上传。

核心流程：

1. 组装 `ForumBoard` 实体。
2. 若上传 `cover`，调用 `fileUtils.uploadFile2Local` 存储到图片目录。
3. 将返回的本地相对路径写入 `forumBoard.cover`。
4. 调用 `forumBoardService.saveForumBoard` 执行保存。

设计要点：

- 封面上传与版块保存在同接口完成，降低前端交互复杂度。
- 文件路径只存相对路径，实际根目录由配置拼接，便于环境迁移。

## 5.3 `/board/delBoard` 删除版块

实现位置：`ForumBoardController.delBoard`

- 接收 `boardId`，调用 `deleteForumBoardByBoardId`。
- 删除校验与级联逻辑下沉服务层，控制器不耦合业务细节。

## 5.4 `/board/changeBoardSort` 板块排序

实现位置：`ForumBoardController.changeSort`

- 接收逗号分隔的 `boardIds`（按新顺序排列）。
- 服务层按数组顺序重写排序值。

设计要点：

- 使用“全量顺序提交”替代“单条上移下移”，前端实现更直观。

## 6. 文章与评论管理接口实现

## 6.1 `/forum/loadArticle` 分页加载文章

实现位置：`ForumArticleController.loadArticle`

- 控制层统一设置排序：`post_time desc`。
- 调用 `forumArticleService.findListByPage` 返回分页结果。

设计要点：

- 排序口径统一，避免前端传入不可信排序字段。

## 6.2 `/forum/delArticle` 批量删除文章

实现位置：`ForumArticleController.delArticle`

- 接收逗号分隔 `articleIds`。
- 服务层执行批量删除及关联清理。

## 6.3 `/forum/updateBoard` 调整文章所属版块

实现位置：`ForumArticleController.updateBoard`

- 必填 `articleId`、`pBoardId`。
- `boardId` 空值自动兜底为 `0`。
- 调用服务层更新文章版块归属。

设计要点：

- 对 `boardId` 空值兜底，降低前端参数差异导致的异常。

## 6.4 `/forum/getAttachment` 获取文章附件信息

实现位置：`ForumArticleController.getAttachment`

- 按 `articleId` 查询附件列表。
- 列表为空抛出“附件不存在”业务异常。
- 返回首个附件对象用于后台展示与下载按钮构建。

## 6.5 `/forum/attachmentDownload` 下载附件

实现位置：`ForumArticleController.attachmentDownload`

核心流程：

1. 按 `fileId` 查询附件元数据。
2. 拼接本地文件真实路径：`projectFolder + attachment 子目录 + filePath`。
3. 设置下载响应头与文件名。
4. 按浏览器类型处理中文文件名编码（IE 与非 IE 分支）。
5. 以流式方式输出文件内容。

异常与资源处理：

- 下载异常统一抛出“下载失败”。
- 输入输出流在 `finally` 中显式关闭，避免句柄泄露。

## 6.6 `/forum/topArticle` 置顶/取消置顶

实现位置：`ForumArticleController.topArticle`

- 入参 `topType`：`0` 取消置顶，`1` 置顶。
- 通过 `updateForumArticleByArticleId` 更新文章置顶状态。

## 6.7 `/forum/auditArticle` 批量审核文章

实现位置：`ForumArticleController.auditArticle`

- 接收 `articleIds` 批量审核。
- 审核细节和后续状态流转由服务层处理。

## 6.8 `/forum/loadComment` 分页加载评论

实现位置：`ForumArticleController.loadComment`

- 开启 `loadChildren=true`，返回评论及其子评论。
- 统一按 `post_time desc` 排序。
- 返回分页结构，适配后台评论管理场景。

## 6.9 `/forum/loadComment4Article` 加载文章评论树

实现位置：`ForumArticleController.loadComment4Article`

- 限定 `pCommentId=0` 仅查一级评论。
- 同时设置 `loadChildren=true`，由服务层补齐子评论。
- 返回非分页列表用于单文评论详情。

## 6.10 `/forum/delComment` 与 `/forum/auditComment`

实现位置：`ForumArticleController.delComment`、`ForumArticleController.auditComment`

- 均采用批量 ID 入参。
- 控制层仅负责请求入口，具体删除、审核、副作用处理全部在服务层完成。

## 7. 文件访问接口实现

## 7.1 `/file/getAvatar/{userId}` 用户头像读取

实现位置：`FileController.getAvatar`

核心流程：

1. 按约定路径查找头像文件：`avatar/{userId}.jpg`。
2. 若不存在则回退到 `default_avatar.jpg`。
3. 若默认头像也不存在，返回友好提示文本。
4. 调用 `readImage` 输出图片二进制流。

设计要点：

- 通过默认头像兜底，避免前端出现 404 图像破损体验。
- 默认图缺失时返回明确提示，便于部署排查。

## 7.2 `/file/getImage/{imageFolder}/{imageName}` 通用图片读取

实现位置：`FileController.getImage`

- 支持读取业务图片、临时图片、头像图片。
- 内部统一委托 `readImage`，避免重复代码。

## 7.3 `readImage` 文件读取策略

关键实现点：

- 非头像图片设置 1 天缓存：`Cache-Control: max-age=86400`。
- 根据后缀动态设置 `Content-Type: image/{suffix}`。
- 使用 8KB 缓冲区分段输出，降低大图读取开销。
- 异常仅记录日志，不向客户端暴露底层路径信息。

## 8. 配置与启动阶段实现

## 8.1 配置项驱动能力

`application.yml` 中关键配置与作用：

- `projectFolder`：统一文件根目录。
- `admin`：管理端登录账号密码。
- `isDev.open`：开发模式自动登录开关。
- `inner.api.*`：内部接口密钥与地址（用于跨模块协同）。

## 8.2 启动预热（InitRun）

管理端启动后执行 `InitRun.run()`：

- 调用 `sysSettingService.initSysSettingToCache()`。
- 将系统配置提前加载至缓存，避免首次请求触发冷加载。

设计价值：

- 降低运行期首次读取配置的延迟。
- 保证管理端与业务模块共享一致的配置视图。

## 9. 本次接口开发的工程化沉淀

本次在管理端接口开发中形成了以下可复用实践：

- 统一入口拦截：登录校验不依赖控制器开发者手动添加。
- 统一参数规范：`@GlobalInterceptor + @VerifyParam` 可持续复用。
- 文件路径可配置：避免硬编码绝对路径带来的部署问题。
- 批量接口约定：删除、审核、排序统一使用逗号分隔 ID 语义。
- 响应结构统一：全部走 `ResponseVO`，降低前端适配成本。

## 10. 结论

本次 `horizonhub-admin` 模块接口开发已完成管理后台核心能力闭环，覆盖登录鉴权、版块管理、文章评论治理、附件下载与静态文件访问，并通过拦截器与切面实现了接口级统一治理。

整体实现遵循“控制层轻量、服务层聚合、切面化校验、配置化驱动”的设计方向，具备较好的可维护性与可扩展性，可直接支撑管理后台后续功能迭代。
