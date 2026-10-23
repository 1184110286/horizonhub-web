# work09 - 用户中心与文章管理接口技术实现报告

## 1. 目标与范围

本报告基于本次已完成接口开发工作，系统说明以下 8 个接口的实现逻辑、设计思想与工程实践：

- `/ucenter/getUserInfo`
- `/ucenter/updateUserInfo`
- `/ucenter/loadUserIntegralRecord`
- `/ucenter/loadUserArticle`
- `/ucenter/getMessageCount`
- `/ucenter/loadMessageList`
- `/forum/search`
- `/forum/updateArticle`

报告内容覆盖控制层参数接收与校验、服务层核心业务链路、Mapper 查询策略、权限与状态控制、分页与可维护性设计，并补充本次编码过程中的通用设计沉淀。

## 2. 总体实现架构

### 2.1 分层职责

- 控制层（`horizonhub-web`）
  - 负责接口路由、参数接收、会话用户提取、轻量业务编排。
  - 通过 `@GlobalInterceptor` 与 `@VerifyParam` 实现登录校验与参数规则校验。
- 服务层（`horizonhub-common`）
  - 承载核心业务逻辑：权限判断、状态流转、文件处理、聚合统计。
  - 对外提供清晰服务接口，内部使用事务保障一致性。
- 持久层（MyBatis Mapper）
  - 动态 SQL 组合查询条件，支持分页、模糊查询与按类型统计。

### 2.2 核心设计思想

- 不信任前端输入：关键字段在服务层二次校验。
- 会话身份后端注入：用户身份与权限不由请求参数决定。
- 状态驱动访问控制：审核状态、用户状态、消息状态贯穿查询与展示。
- 分页能力统一下沉：复用 `findListByPage` + `SimplePage`，减少重复代码。
- 控制层薄、服务层厚：增强可测试性与后续扩展能力。

## 3. 接口实现详解

## 3.1 `/ucenter/getUserInfo`

### 3.1.1 功能定位

用于用户主页信息展示，返回用户基础资料及扩展统计信息（发帖数、获赞数、当前积分）。

### 3.1.2 控制层逻辑

1. 参数校验：`userId` 必填。
2. 查询用户：`userInfoService.getUserInfoByUserId(userId)`。
3. 状态过滤：用户不存在或已封禁直接返回 404 业务异常。
4. 统计发帖：按 `userId + 审核通过状态` 统计文章数。
5. 统计获赞：按 `authorUserId` 汇总点赞记录数量。
6. 组装返回：将 `UserInfo` 转为 `UserInfoVO` 并补充统计字段。

### 3.1.3 设计要点

- 将基础资料与统计信息在一个接口聚合返回，减少前端多次请求。
- 对封禁用户统一做访问隔离，避免被外部继续读取主页数据。
- 发帖数统计仅计算已审核文章，保证对外展示口径一致。

## 3.2 `/ucenter/updateUserInfo`

### 3.2.1 功能定位

支持当前登录用户更新个人资料（性别、个人简介）及头像。

### 3.2.2 控制层逻辑

1. 登录校验：`checkLogin = true`。
2. 参数校验：`personDescription` 长度上限 100。
3. 会话取当前用户：从 session 获取 `userId`。
4. 构造更新对象：仅设置允许更新字段。
5. 调用服务：`userInfoService.updateUserInfo(userInfo, avatar)`。

### 3.2.3 服务层逻辑

- 先按 `userId` 执行资料更新。
- 如头像不为空，调用文件工具按头像类型上传到本地目录。
- 使用事务保证“资料更新 + 头像更新”的一致性。

### 3.2.4 设计要点

- 强制使用会话 `userId`，避免用户伪造他人身份修改资料。
- 采用“部分字段更新”模型，降低误覆盖风险。
- 资料与头像同入口处理，兼顾体验与接口简洁性。

## 3.3 `/ucenter/loadUserIntegralRecord`

### 3.3.1 功能定位

分页加载当前用户积分流水，支持时间区间筛选。

### 3.3.2 实现逻辑

1. 登录校验后，固定查询条件中的 `userId` 为当前会话用户。
2. 接收 `pageNo/createTimeStart/createTimeEnd` 作为筛选条件。
3. 指定排序 `record_id desc`，保证最新流水优先展示。
4. 调用 `userIntegralRecordService.findListByPage` 返回分页结果。

### 3.3.3 设计要点

- 积分记录只允许查询本人，避免越权读取他人资产流水。
- 分页逻辑由通用 Service 模板实现，统一页大小与总数计算行为。
- 时间筛选参数保留扩展空间，便于后续做账单导出或统计报表。

## 3.4 `/ucenter/loadUserArticle`

### 3.4.1 功能定位

按用户维度加载文章列表，支持三种模式：

- `type=0`：我发布的
- `type=1`：我评论的
- `type=2`：我点赞的

### 3.4.2 控制层逻辑

1. 校验目标用户存在且未封禁。
2. 构建 `ForumArticleQuery`，默认 `post_time desc`。
3. 根据 `type` 注入不同过滤字段：
   - 发布：`userId`
   - 评论：`commentUserId`
   - 点赞：`likeUserId`
4. 根据登录态设置可见性：
   - 已登录：`currentUserId`，可看到本人待审 + 全部已审。
   - 未登录：仅 `status = AUDIT`。
5. 分页查询并转换为 `ForumArticleVO`。

### 3.4.3 Mapper 关键策略

`ForumArticleMapper.xml` 中通过动态 SQL 实现差异过滤：

- `currentUserId` 存在时：`((当前用户 = 作者 and status = 0) or status = 1)`。
- 评论模式：`article_id in (select article_id from forum_comment where user_id=...)`。
- 点赞模式：`article_id in (select object_id from like_record where user_id=... and op_type = 0)`。

### 3.4.4 设计要点

- 同一接口覆盖三类用户行为轨迹，降低前端接入复杂度。
- 通过状态可见性规则兼顾审核机制与作者自查诉求。
- 查询条件高度参数化，便于后续增加收藏、浏览历史等模式。

## 3.5 `/ucenter/getMessageCount`

### 3.5.1 功能定位

返回当前用户各消息类型未读数量及总未读数。

### 3.5.2 实现逻辑

1. 登录校验后读取当前用户 ID。
2. 服务层调用 `selectUserMessageCount(userId)`，按 `message_type` 分组聚合未读数。
3. 将聚合结果映射到 `UserMessageCountDto`：
   - 系统消息
   - 评论回复
   - 文章点赞
   - 评论点赞
   - 附件下载
4. 计算并返回 `total`。

### 3.5.3 设计要点

- 使用数据库分组聚合，避免拉取全量消息后在应用层统计。
- DTO 字段按业务语义拆分，前端可直接绑定角标显示。
- 总数与分项同时返回，兼顾顶部总提醒和分栏提醒场景。

## 3.6 `/ucenter/loadMessageList`

### 3.6.1 功能定位

按消息类型分页加载消息列表，并在第一页触发该类型消息已读。

### 3.6.2 控制层逻辑

1. 校验 `code` 必填并映射为 `MessageTypeEnum`。
2. 构建查询：`receivedUserId + messageType + pageNo`。
3. 设置排序 `message_id desc`，保证新消息在前。
4. 执行分页查询。
5. 若 `pageNo` 为空或为 1，调用 `readMessageByType` 批量已读。
6. 转换为 `UserMessageVO` 返回。

### 3.6.3 Mapper 关键策略

- 列表查询：动态 where + 分页 limit。
- 批量已读：`updateMessageStatusBatch` 按接收人和消息类型更新状态。

### 3.6.4 设计要点

- “查看第一页即已读”符合常见消息中心交互预期。
- 已读更新按类型批量执行，降低逐条更新开销。
- 类型码先做枚举校验，防止非法类型进入查询链路。

## 3.7 `/forum/search`

### 3.7.1 功能定位

提供文章标题关键字搜索能力。

### 3.7.2 实现逻辑

1. 校验 `keyword` 必填。
2. 构造 `ForumArticleQuery` 并设置 `titleFuzzy`。
3. 调用 `forumArticleService.findListByPage(query)` 返回分页数据。

### 3.7.3 Mapper 关键策略

在 `ForumArticleMapper.xml` 中通过：

- `title like concat('%', #{query.titleFuzzy}, '%')`

实现标题模糊匹配。

### 3.7.4 设计要点

- 搜索条件简洁，适合作为社区首页快速检索入口。
- 基于通用分页查询能力复用，减少搜索接口重复实现成本。
- 与文章状态过滤规则联动，确保输出结果遵循可见性约束。

## 3.8 `/forum/updateArticle`

### 3.8.1 功能定位

提供文章编辑更新能力，支持内容、封面、附件、板块、摘要、附件积分等字段同步修改。

### 3.8.2 控制层逻辑

1. 登录与参数校验（文章 ID、板块、标题、正文、编辑器类型、附件类型）。
2. 标题净化：`StringTools.escapeTitle(title)`。
3. 会话用户注入：`userId`、地理信息。
4. 组装 `ForumArticle` 与 `ForumArticleAttachment`（积分默认值 0）。
5. 调用 `forumArticleService.updateArticle(...)`。

### 3.8.3 服务层核心链路

1. 所属权校验：非管理员仅可修改本人文章。
2. 内容合法性校验：编辑器类型、摘要长度、板块发布权限。
3. 更新时间刷新：`lastUpdateTime = now`。
4. 封面处理：有新封面则上传并替换引用路径。
5. 附件处理：
   - 有新附件：上传新文件并按更新模式替换旧附件。
   - 无新附件但附件类型改为无：删除历史附件记录与本地文件。
   - 无新附件且仍有附件：仅在积分变化时更新积分。
6. 审核状态重算：管理员直审；普通用户按系统审核配置决定待审/已审。
7. 内容图片路径修正：将正文和 Markdown 中临时目录路径替换为正式月份目录。
8. 持久化更新文章主表。

### 3.8.4 设计要点

- 编辑权限与板块权限双重校验，避免越权修改和越权发帖。
- 附件更新流程覆盖“替换/删除/仅改积分”三种场景，业务闭环完整。
- 文本内容与文件存储路径同步修正，保证发布后资源可访问性。
- 状态重算保证编辑后仍符合审核制度，不绕过平台治理规则。

## 4. 其他代码编写工作与工程实践

本次接口开发除功能实现外，还完成了以下工程化工作：

- 统一接入拦截器式校验：
  - 登录校验与参数校验通过注解声明，降低样板代码。
- 统一分页返回模型：
  - 列表接口均返回 `PaginationResultVO`，前端分页协议一致。
- 统一 VO 转换策略：
  - 使用 `CopyTools` 完成实体到 VO 的字段映射，减少手工复制错误。
- 统一异常语义：
  - 参数非法、资源不存在、权限不足等场景通过 `BusinessException` 归一输出。
- 查询能力扩展预留：
  - 通过 Query 对象与动态 SQL 机制，为后续过滤条件扩展提供低成本路径。

## 5. 本次实现的价值总结

本次 8 个接口形成了“用户中心 + 文章管理 + 消息体系 + 搜索能力”的闭环：

- 用户侧：资料维护、积分可视化、个人内容轨迹可视化。
- 内容侧：文章编辑更新、搜索检索、审核可见性治理。
- 通知侧：未读统计与分类型消息拉取。
- 工程侧：分层职责清晰、校验统一、分页统一、异常统一，具备持续迭代基础。

整体实现遵循了“安全优先、状态驱动、能力复用、便于扩展”的设计原则，可支撑后续继续扩展用户中心与社区互动能力。
