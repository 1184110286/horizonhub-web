# work05

## 一、第五天工作概述

第五天的开发重点，已经从“论坛首页可浏览”继续推进到“论坛前台可交互、可下载、可感知用户状态”的阶段。

如果说前一天的主要工作，是把论坛板块列表、首页文章分页、文章详情这些基础浏览能力搭起来，那么今天的工作则进一步补齐了论坛前台的互动链路和资源下载链路。当前后端不再只是把文章数据查出来返回，而是开始围绕用户登录态、点赞状态、附件购买与下载状态、积分扣减与增加、消息提醒等行为做联动处理。

从当前代码来看，今天的工作主要集中在以下几个方向：

1. 继续完善 `ForumArticleController`，补齐文章点赞、用户下载信息查询、附件下载等接口。
2. 进一步把论坛文章详情能力从“查文章”扩展为“查文章 + 查附件 + 查当前用户点赞状态”的复合返回结构。
3. 通过 `LikeRecordServiceImpl` 实现文章点赞/取消点赞，并同步更新文章点赞数与消息提醒。
4. 通过 `ForumArticleAttachmentServiceImpl` 实现附件下载相关的积分校验、下载记录写入、附件下载次数更新、积分流转与消息写入。
5. 通过 `ForumArticleAttachmentDownloadServiceImpl`、`ForumBoardServiceImpl`、`ForumArticleServiceImpl`
   等服务层代码，进一步补齐前台接口所依赖的公共查询和业务能力。
6. 持续完善 VO、DTO、Query、枚举、常量、配置类等基础支撑，使论坛前台接口从“能调用”逐步发展到“有清晰边界和业务语义”。

需要特别说明的是：**最近一次 Git 提交中，并不只是包含昨天的工作内容，也已经混入了部分今天完成的论坛接口代码。**

因此，今天这份日报并不是单纯记录“本次提交新增了哪些文件”，而是基于当前仓库代码状态，对今天已经完成或已经在最近一次提交中体现出来的功能进行系统整理。换句话说，
`work05` 更接近于对“当前阶段论坛前台接口能力”的一次完整总结。

当前阶段完成后，论坛前台已经基本形成了如下访问链路：

**板块树加载 -> 首页文章分页 -> 文章详情查看 -> 文章点赞/取消点赞 -> 下载信息查询 -> 附件下载与积分结算**

这意味着 `horizonhub` 后端已经从“账号体系初始化”和“论坛基础浏览”阶段，正式走向了“论坛前台交互能力建设”阶段。

---

## 二、今日完成内容总览

今天围绕论坛前台能力，已经完成或进一步补齐的核心接口包括：

### 2.1 论坛板块接口

控制器位置：`horizonhub-web/src/main/java/com/nlizzard/horizonhub/controller/ForumBoardController.java`

当前已具备的接口：

1. `/board/loadBoard`：加载论坛板块树

### 2.2 论坛文章与互动接口

控制器位置：`horizonhub-web/src/main/java/com/nlizzard/horizonhub/controller/ForumArticleController.java`

当前已具备或继续完善的接口：

1. `/forum/loadArticle`：加载首页文章列表
2. `/forum/getArticleDetail`：获取文章详情
3. `/forum/doLike`：文章点赞/取消点赞
4. `/forum/getUserDownloadInfo`：获取当前用户附件下载信息
5. `/forum/attachmentDownload`：下载附件

其中：

- `/forum/loadArticle` 和 `/forum/getArticleDetail` 属于前一天已经开始具备的论坛基础浏览能力；
- `/forum/doLike`、`/forum/getUserDownloadInfo`、`/forum/attachmentDownload` 则体现了今天重点推进的用户互动与资源下载能力；
- 同时，围绕这些接口所依赖的 Service、Mapper、VO、DTO、枚举、常量与配置也进一步补齐，使这些接口不仅能返回数据，而且具备明确的业务规则控制。

---

## 三、论坛前台接口详细实现说明

### 3.1 板块树加载接口 `/board/loadBoard`

#### 接口作用

该接口用于向前台返回论坛板块的树形结构数据，便于前端构建首页导航、发帖板块选择器、板块筛选菜单等内容。

虽然用户在提问中写的是 `/forum/loadBoard`，但从当前代码实现来看，**实际接口路径是 `/board/loadBoard`**。

#### 实现位置

- 控制器：`ForumBoardController#loadBoard`
- Service：`ForumBoardServiceImpl#getBoardTree`

#### 控制器实现逻辑

控制器层逻辑非常简洁：

1. 接收前台请求；
2. 调用 `forumBoardService.getBoardTree(null)`；
3. 将返回结果包装为统一的 `ResponseVO<List<ForumBoard>>`。

这里传入的参数为 `null`，表示当前查询不额外限制文章类型，直接读取全部板块树。

#### Service 实现逻辑

核心逻辑位于：`ForumBoardServiceImpl#getBoardTree`

实现过程如下：

##### 1）构建查询条件

先创建 `ForumBoardQuery`，设置两个关键条件：

- `orderBy = "sort ASC"`：按排序字段升序查询；
- `postType = postType`：允许未来按文章类型过滤板块。

这意味着当前系统已经预留了“不同文章类型对应不同板块”的扩展能力。

##### 2）查询板块线性数据

调用 `forumBoardMapper.selectList(forumBoardQuery)`，先从数据库中查询出板块列表。

此时拿到的是一组线性结构数据，每条板块记录都带有自己的 `boardId` 和父级板块 ID `pBoardId`。

##### 3）把线性结构转换为树形结构

服务层并没有把数据库结果直接返回，而是调用：

`convertLine2Tree(forumBoardList, 0)`

将平铺列表转换为树形结构。

这个方法使用递归方式：

- 从 `pBoardId = 0` 的根板块开始；
- 遍历所有板块；
- 找出父级等于当前节点的子板块；
- 将子板块继续递归挂载到 `children` 字段中；
- 最终返回完整树形列表。

#### 实现细节

1. 使用 `sort ASC` 保证前端每次拿到的板块顺序稳定。
2. 使用递归构建树形结构，代码直观，便于当前阶段快速落地。
3. 代码中已经写了 TODO，说明后续可以把递归优化为非递归实现，以减少层级较深时的递归性能损耗。
4. 该接口返回的是 `ForumBoard` 实体树，而不是额外定义的 VO，说明当前板块结构相对简单，实体本身已足够承载展示数据。

#### 当前价值

该接口构成了论坛首页导航的入口。没有板块树，文章列表只能是“全站平铺”；有了板块树后，前台页面就可以做板块筛选、父子板块展示和后续发帖入口约束。

---

### 3.2 首页文章列表接口 `/forum/loadArticle`

#### 接口作用

该接口用于分页加载论坛首页文章列表，是前台首页、板块页、父板块页等多个场景的基础数据来源。

它不仅负责“查文章”，还承担了：

- 板块筛选；
- 父板块筛选；
- 登录用户与匿名用户可见范围区分；
- 文章排序方式控制；
- 分页结果包装；
- 实体到 VO 的转换。

#### 实现位置

- 控制器：`ForumArticleController#loadArticle`
- Service：`ForumArticleServiceImpl#findListByPage`
- 枚举支撑：`ArticleOrderTypeEnum`、`ArticleStatusEnum`
- 返回对象：`PaginationResultVO<ForumArticleVO>`

#### 入参说明

- `boardId`：板块 ID
- `pBoardId`：父级板块 ID
- `orderType`：排序类型
- `pageNo`：页码
- `session`：当前会话，用于判断是否已登录

#### 控制器实现逻辑

##### 1）组装文章查询对象

控制器首先创建 `ForumArticleQuery articleQuery = new ForumArticleQuery()`，并将前端的筛选条件写入查询对象。

对于 `boardId`，代码做了一个很实用的兼容处理：

- 当 `boardId == null` 或 `boardId == 0` 时，认为前端是在查“全部板块”；
- 此时把 `articleQuery.setBoardId(null)`，避免无意义的 0 值参与 SQL 查询；
- 否则，按具体板块 ID 查询。

接着再设置：

- `pBoardId`
- `pageNo`

##### 2）根据登录状态控制可见文章范围

这是该接口中非常关键的一层业务控制。

控制器会先通过 `getUserInfoFromSession(session)` 获取当前会话中的 `SessionWebUserDto`。

- 如果用户已登录：
    - 调用 `articleQuery.setCurrentUserId(userDto.getUserId())`
    - 这意味着查询时允许“当前用户看到自己的文章”
- 如果用户未登录：
    - 调用 `articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus())`
    - 这意味着匿名用户只能看到审核通过的文章

从这个逻辑可以看出，当前系统已经开始在文章列表层面引入权限边界：

- 公开用户只能看到已审核内容；
- 已登录用户则可以额外看到自己尚未审核的文章。

这对论坛产品来说是很重要的，因为很多帖子在审核期间，作者仍然希望能在“我的视角”中看见自己的内容。

##### 3）处理文章排序规则

接口通过 `ArticleOrderTypeEnum.getByType(orderType)` 获取排序枚举。

如果前端没有传 `orderType`，或者传入值无法匹配枚举，则默认使用：

`ArticleOrderTypeEnum.HOT`

当前枚举中已定义：

1. `HOT(0, "top_type desc,comment_count desc,good_count desc,read_count desc", "热榜")`
2. `SEND(1, "post_time asc", "发布")`
3. `NEW(2, "post_time desc", "最新")`

控制器会把枚举中的 `orderSql` 写入 `articleQuery.setOrderBy(...)`，从而把排序能力下放到 SQL 层。

#### Service 分页实现逻辑

核心逻辑位于：`ForumArticleServiceImpl#findListByPage`

实现过程如下：

##### 1）先查总数

通过 `findCountByParam(query)` 统计符合条件的数据总量。

##### 2）决定分页大小

如果前端没有传分页大小，则使用枚举默认值：

`PageSize.SIZE15`

也就是每页默认 15 条数据。

##### 3）构建分页对象

通过 `SimplePage` 统一计算：

- 当前页码
- 总页数
- 每页大小
- 总记录数

##### 4）回填分页条件并查询列表

调用：

- `query.setSimplePage(page)`
- `findListByParam(query)`

最终得到当前页文章列表。

##### 5）统一封装分页返回结果

最终返回 `PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list)`。

#### 返回结果处理

控制器层并没有直接返回 `ForumArticle` 实体，而是调用：

`convert2PaginationVO(resultVO, ForumArticleVO.class)`

将分页结果中的实体对象转换为前台 VO。

这样做有两个好处：

1. 前后端返回结构更明确，不把所有数据库字段直接暴露给前端；
2. VO 可以按前台需要保留展示字段，例如：
    - 标题
    - 封面
    - 摘要
    - 发布时间
    - 点赞数
    - 评论数
    - 阅读数
    - 板块名称
    - 用户昵称
    - 附件标记等

#### 实现细节

1. `boardId == 0` 被视为“全部板块”，这能减少前端对空值处理的复杂度。
2. 通过 Session 判断用户身份，实现匿名用户与登录用户的可见范围差异。
3. 排序规则通过枚举统一管理，避免排序 SQL 散落在控制器中。
4. 分页逻辑放到 Service 层统一处理，便于后续其他列表接口复用。
5. 返回对象使用 `ForumArticleVO`，体现了接口输出与数据库实体解耦的设计思路。

#### 当前价值

该接口是论坛首页和板块页的核心数据来源。它的完成，意味着论坛前台已经不再只是“能查某篇文章”，而是具备了真正的首页信息流组织能力。

---

### 3.3 文章详情接口 `/forum/getArticleDetail`

#### 接口作用

该接口用于获取某篇文章的完整详情数据。

和简单的“查文章主表”不同，这个接口实际返回的是一个复合详情对象，里面不仅包含文章本身，还可能包含：

- 附件信息；
- 当前登录用户是否已点赞该文章。

因此它已经开始体现“详情页聚合接口”的特点。

#### 实现位置

- 控制器：`ForumArticleController#getArticleDetail`
- Service：`ForumArticleServiceImpl#readArticle`
- 依赖服务：`ForumArticleAttachmentService`、`LikeRecordService`
- 返回对象：`FormArticleDetailVO`

#### 控制器实现逻辑

##### 1）读取当前登录用户

先从 Session 中拿到 `SessionWebUserDto`，用于后续判断：

- 当前用户是不是作者；
- 当前用户是不是管理员；
- 当前用户是否点过赞。

##### 2）读取文章详情

控制器调用：

`forumArticleService.readArticle(articleId)`

在 `ForumArticleServiceImpl#readArticle` 中，会先根据文章 ID 查询文章。

如果文章不存在，则直接抛出 `BusinessException(ResponseCodeEnum.CODE_404)`。

##### 3）文章阅读数自增逻辑

`readArticle` 方法里还有一层很重要的处理：

- 如果文章状态为 `AUDIT`（已审核）
- 则调用 `forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.READ_COUNT.getType(), 1, articleId)`
- 对阅读数加 1

这意味着：

- 审核通过的公开文章，每次查看详情都会累计阅读量；
- 未审核或已删除文章不会被错误计入公开阅读数。

##### 4）控制文章可见范围

控制器在拿到文章后，会继续做一次细粒度权限判断：

- 如果文章为空，返回 404；
- 如果文章状态为待审核，且当前访问者既不是作者也不是管理员，返回 404；
- 如果文章状态为已删除，返回 404。

这一段逻辑非常关键，它保证了：

1. 未审核文章不会被普通用户直接通过链接访问；
2. 文章作者本人仍然可以查看自己的待审核内容；
3. 管理员可以查看待审核内容，便于后台审核；
4. 已删除内容不再暴露给前台。

#### VO 组装逻辑

##### 1）复制文章基本信息

先创建 `FormArticleDetailVO detailVO = new FormArticleDetailVO()`，然后通过：

`CopyTools.copy(forumArticle, ForumArticleVO.class)`

把文章实体转换为 `ForumArticleVO` 后写入 `detailVO.setForumArticle(...)`。

##### 2）有附件时补充附件信息

如果 `forumArticle.getAttachmentType() == 1`，说明当前文章带有附件。

这时会：

- 创建 `ForumArticleAttachmentQuery`
- 按 `articleId` 查询附件列表
- 如果查到了附件，则取第一条记录
- 转换为 `ForumArticleAttachmentVo`
- 写入 `detailVO.setAttachment(...)`

这说明当前文章与附件的关系，至少在详情页聚合时已经打通。

##### 3）补充当前用户点赞状态

如果当前用户已登录，则调用：

`likeRecordService.getUserOperRecordByObjectIdAndUserIdAndOpType(articleId, userId, OperRecordOpTypeEnum.ARTICLE_LIKE.getType())`

查询点赞记录。

如果能查到记录，就执行：

`detailVO.setHaveLike(true)`

这样前端在拿到详情数据后，就可以直接决定详情页点赞按钮的展示状态，而不需要额外再发一次“我是否点过赞”的请求。

#### 实现细节

1. 阅读数自增逻辑放在 `readArticle` 中，保证文章详情读取与阅读统计行为一致。
2. 权限控制不是简单按“是否存在”判断，而是进一步结合文章状态、作者身份、管理员身份共同决定。
3. 详情接口不是只返回文章主表，而是聚合了附件和点赞态，减少前端请求次数。
4. `FormArticleDetailVO` 中包含 `forumArticle`、`attachment`、`haveLike` 三个维度数据，结构清晰，适合详情页直接消费。

#### 当前价值

该接口标志着论坛详情页不再只是静态信息展示，而是具备了“附件感知”和“用户交互态感知”的能力，是后续继续开发评论、回复、收藏等功能的重要基础。

---

### 3.4 文章点赞接口 `/forum/doLike`

#### 接口作用

该接口用于实现文章点赞与取消点赞。

需要注意的是，这个接口并不是单向“只能点赞一次”，而是基于已有记录实现“点赞/取消点赞”切换：

- 没有点赞记录时，执行点赞；
- 已有点赞记录时，执行取消点赞。

因此它本质上是一个“切换型”交互接口。

#### 实现位置

- 控制器：`ForumArticleController#doLike`
- Service：`LikeRecordServiceImpl#doLike`
- 核心枚举：`OperRecordOpTypeEnum.ARTICLE_LIKE`

#### 控制器实现逻辑

该接口使用了：

- `@GlobalInterceptor(checkLogin = true, checkParams = true)`
- `@VerifyParam(required = true)`

这说明在进入方法体之前，框架已经帮它完成了两层校验：

1. 用户必须已登录；
2. `articleId` 参数必须存在。

控制器层逻辑非常轻量：

1. 从 Session 中获取当前用户信息；
2. 把 `articleId`、`userId`、`nickName` 和操作类型 `ARTICLE_LIKE` 传给业务层；
3. 最终返回统一成功结果。

这符合控制器只负责“接参和转发”的设计原则。

#### Service 实现逻辑

核心逻辑位于：`LikeRecordServiceImpl#doLike`

该方法使用了：

`@Transactional(rollbackFor = Exception.class)`

说明点赞/取消点赞流程中的多步数据库操作必须作为一个事务整体执行。

##### 1）先校验文章是否存在

服务层会先根据 `articleId` 查询文章：

`forumArticleMapper.selectByArticleId(objectId)`

如果文章不存在，则直接抛出 `BusinessException("文章不存在")`。

##### 2）按操作类型分发处理

当前 `OperRecordOpTypeEnum` 中定义了两类操作：

1. `ARTICLE_LIKE(0, "文章点赞")`
2. `COMMENT_LIKE(1, "评论点赞")`

本次接口使用的是 `ARTICLE_LIKE`，因此会进入文章点赞逻辑。

##### 3）判断当前是否已有点赞记录

在 `articleLike(...)` 方法中，会按：

- `objectId`
- `userId`
- `opType`

查询点赞记录。

如果记录已存在，说明用户此前已经点过赞，则执行：

- 删除点赞记录；
- 调用 `forumArticleMapper.updateArticleCount(..., -1, objectId)` 把文章点赞数减 1。

如果记录不存在，说明用户是首次点赞，则执行：

- 新建 `LikeRecord`；
- 写入文章 ID、用户 ID、操作类型、创建时间、文章作者 ID；
- 插入点赞记录表；
- 调用 `forumArticleMapper.updateArticleCount(..., 1, objectId)` 把文章点赞数加 1。

### 消息提醒逻辑

点赞成功后，系统还会尝试构建一条 `UserMessage` 消息，通知文章作者有人对其内容进行了点赞。

消息内容包括：

- 文章 ID
- 文章标题
- 消息类型：`ARTICLE_LIKE`
- 发送人 ID
- 发送人昵称
- 接收人 ID
- 消息状态：未读

但这里做了两个重要限制：

##### 1）不给自己发消息

如果点赞人就是文章作者本人，则不会生成提醒消息。

##### 2）避免重复插入同类消息

服务层会先调用：

`userMessageMapper.selectByArticleIdAndSendUserIdAndMessageType(...)`

检查是否已经存在同一发送人、同一文章、同一消息类型的提醒。

如果已存在，则不再重复插入消息。

这能有效避免用户反复点击点赞按钮时，给作者制造大量重复通知。

#### 实现细节

1. 点赞和取消点赞复用同一个接口，减少前端接口数量。
2. 通过事务控制保证“点赞记录表”和“文章点赞数字段”保持一致。
3. 点赞消息不会发给自己，也会做重复消息检查。
4. 枚举 `OperRecordOpTypeEnum` 已为未来扩展评论点赞预留统一模式。

#### 当前价值

该接口让论坛从“可浏览”走向“可互动”。它不仅让前台具备真实的点赞行为，也同时打通了内容计数与消息通知机制，是论坛活跃度建设的重要一步。

---

### 3.5 获取用户下载信息接口 `/forum/getUserDownloadInfo`

#### 接口作用

该接口用于在用户真正点击下载之前，先返回当前用户与该附件之间的下载关系信息，便于前端决定如何展示下载弹窗或下载按钮。

它主要返回两类信息：

1. 用户当前积分；
2. 用户是否已经下载过该附件。

这使前端在发起正式下载前，就能明确知道：

- 当前用户积分够不够；
- 是否已经购买/下载过该附件；
- 是否应该提示再次扣积分。

#### 实现位置

- 控制器：`ForumArticleController#getUserDownloadInfo`
- Service：`UserInfoService`、`ForumArticleAttachmentDownloadServiceImpl`

#### 控制器实现逻辑

该接口同样使用：

- `@GlobalInterceptor(checkLogin = true, checkParams = true)`
- `@VerifyParam(required = true)`

说明：

1. 只有登录用户才能查询自己的下载信息；
2. `fileId` 必须传入。

方法体内部逻辑如下：

##### 1）创建返回 Map

先创建：

`Map<String, Object> result = new HashMap<>()`

当前返回结构中主要放两个字段。

##### 2）查询用户当前积分

通过 Session 拿到当前用户 ID 后，调用：

`userInfoService.getUserInfoByUserId(...)`

获取用户对象，并把：

`userInfo.getCurrentIntegral()`

写入：

`result.put("userIntegral", userInfo.getCurrentIntegral())`

##### 3）查询是否已下载过该附件

控制器接着调用：

`forumArticleAttachmentDownloadService.getForumArticleAttachmentDownloadByFileIdAndUserId(fileId, userId)`

如果能查到记录，则说明用户曾经下载过该附件。

最终通过：

`result.put("haveDownload", attachmentDownload != null)`

将布尔值写入结果。

#### 依赖的 Service 说明

`ForumArticleAttachmentDownloadServiceImpl` 目前已经提供了一个非常清晰的主键式查询方法：

`getForumArticleAttachmentDownloadByFileIdAndUserId(String fileId, String userId)`

它直接通过下载记录表查询当前用户是否下载过指定文件，是这一接口最直接的数据支撑。

#### 实现细节

1. 该接口不直接发起下载，只负责为下载弹窗或按钮提供前置信息。
2. 返回的是轻量级 `Map`，适合前台快速判断，不需要额外定义复杂 VO。
3. “是否已下载过”这个字段非常重要，因为它决定后续下载是否需要再次扣积分。
4. 接口与真正的下载接口分离，体现了“查询态”和“执行态”分层设计。

#### 当前价值

该接口让下载行为更可控。前端可以先展示用户积分和下载资格，再决定是否调用真正的下载接口，从而提升交互体验，也避免用户在未了解积分情况时直接触发下载失败。

---

### 3.6 附件下载接口 `/forum/attachmentDownload`

#### 接口作用

该接口用于完成论坛附件的真实下载流程。

需要强调的是，这个接口并不只是“把服务器上的文件流输出给浏览器”这么简单。它实际上同时承担了如下职责：

1. 校验附件是否存在；
2. 判断当前用户是否需要扣积分；
3. 判断用户积分是否足够；
4. 写入下载记录；
5. 更新附件下载次数；
6. 执行积分扣减与增加；
7. 写入消息提醒；
8. 最终输出文件流。

因此，这个接口是当前阶段业务规则最复杂的前台接口之一。

#### 实现位置

- 控制器：`ForumArticleController#attachmentDownload`
- 核心 Service：`ForumArticleAttachmentServiceImpl#downloadAttachment`
- 配置类：`WebConfig`
- 常量类：`Constants`

#### 控制器实现逻辑

##### 1）基础校验

接口使用：

- `@GlobalInterceptor(checkLogin = true, checkParams = true)`
- `@VerifyParam(required = true)`

说明：

- 必须登录后才能下载附件；
- `fileId` 必传。

##### 2）先走业务层，再走文件流输出

控制器不会一上来就去读磁盘文件，而是先调用：

`forumArticleAttachmentService.downloadAttachment(fileId, getUserInfoFromSession(session))`

这一步非常关键，因为真正的业务规则都在这里完成，包括积分和下载记录处理。

业务层返回 `ForumArticleAttachment attachment` 后，控制器才继续进行文件输出。

##### 3）拼接磁盘文件路径

控制器根据配置和常量拼接出附件真实路径：

- `webConfig.getProjectFolder()`：项目文件根目录
- `Constants.FILE_FOLDER_FILE`：`/file/`
- `Constants.FILE_FOLDER_ATTACHMENT`：`attachment/`
- `attachment.getFilePath()`：具体文件相对路径

最终组合成完整磁盘路径。

这说明当前项目已将上传文件与业务数据分离存储，数据库只记录元信息和相对路径，真实文件位于服务器磁盘中。

##### 4）输出文件流

控制器使用：

- `FileInputStream` 读取文件；
- `response.getOutputStream()` 向浏览器输出；
- `Content-Type` 设置为 `application/x-msdownload; charset=UTF-8`；
- `Content-Disposition` 设置为附件下载响应头。

##### 5）处理中文文件名编码

为了兼容浏览器下载时的文件名显示，代码对文件名做了分浏览器编码处理：

- 如果是 IE 浏览器，使用 `URLEncoder.encode(downloadFileName, "UTF-8")`；
- 否则，将 UTF-8 字节转为 `ISO8859-1`。

这能在一定程度上避免中文附件名在下载时出现乱码。

##### 6）异常与资源释放

如果下载过程中发生异常：

- 记录日志 `logger.error("下载异常", e)`；
- 抛出 `BusinessException("下载失败")`。

最后在 `finally` 中关闭输入流和输出流，并分别捕获 `IOException` 写日志，保证 IO 资源不会泄漏。

#### Service 核心业务逻辑

核心实现位于：`ForumArticleAttachmentServiceImpl#downloadAttachment`

该方法使用事务控制：

`@Transactional(rollbackFor = Exception.class)`

说明只要积分处理、下载记录写入、消息写入等任一环节失败，整个下载前置业务都可以回滚。

##### 1）先查附件是否存在

按 `fileId` 查询附件记录：

`forumArticleAttachmentMapper.selectByFileId(fileId)`

如果附件不存在，则抛出：

`BusinessException(ResponseCodeEnum.CODE_404.getCode(), "附件不存在")`

##### 2）判断是否需要积分校验

如果满足以下两个条件：

- 附件积分 `attachment.getIntegral() > 0`
- 当前用户不是附件提供者

则进入积分校验流程。

##### 3）判断用户是否已下载过

先查下载记录：

`forumArticleAttachmentDownloadMapper.selectByFileIdAndUserId(fileId, currentUserId)`

如果有记录，说明用户此前已经下载过，那么本次无需再次扣积分。

##### 4）首次下载时校验积分是否足够

如果用户之前没下载过，则查询用户当前积分：

`userInfoService.getUserInfoByUserId(currentUserId)`

并执行判断：

`userInfo.getCurrentIntegral() - attachment.getIntegral() < 0`

如果结果为真，则直接抛出“积分不够”异常。

##### 5）写入下载记录

无论是否需要扣积分，只要下载动作发生，系统都会构造 `ForumArticleAttachmentDownload` 对象，并执行：

`forumArticleAttachmentDownloadMapper.insertOrUpdate(updateDownload)`

这说明下载记录表不仅承担“是否下载过”的判断作用，也承担下载次数更新或幂等记录作用。

##### 6）更新附件下载次数

调用：

`forumArticleAttachmentMapper.updateDownloadCount(fileId)`

把附件下载次数加 1。

##### 7）自己下载自己的附件不扣积分

如果当前用户就是附件作者本人，则直接返回附件信息，不再往下走积分流转逻辑。

##### 8）已下载过的用户再次下载不扣积分

如果前面已经查到 `download != null`，说明用户历史上已经下载过该附件，也会直接返回附件信息。

这样就形成了很清晰的积分规则：

- 作者自己下载自己的附件：不扣积分；
- 已购买/已下载过的用户再次下载：不扣积分；
- 其他用户首次下载：需要判断积分并完成扣减。

##### 9）执行积分扣减与积分增加

对于首次付费下载的用户：

- 调用 `userInfoService.updateUserIntegral(currentUserId, USER_DOWNLOAD_ATTACHMENT, REDUCE, attachment.getIntegral())`
  扣减下载方积分；
- 调用 `userInfoService.updateUserIntegral(attachment.getUserId(), DOWNLOAD_ATTACHMENT, ADD, attachment.getIntegral())`
  给附件提供者增加积分。

这说明当前系统已经把附件下载设计为一种积分流转行为，而不是单纯做一个是否可下的开关。

##### 10）写入消息提醒

如果下载者不是附件作者，则系统还会：

- 按文章 ID 查到所属文章；
- 构造 `UserMessage`；
- 消息类型设为 `DOWNLOAD_ATTACHMENT`；
- 记录发送人、接收人、文章标题、未读状态；
- 调用 `userMessageService.add(userMessage)` 写入消息表。

这意味着作者后续可以在消息中心中看到“有人下载了我的附件”。

#### 实现细节

1. 下载动作被拆成“业务处理”和“文件输出”两部分，层次清晰。
2. 业务处理部分使用事务，保证积分、记录、消息的一致性。
3. 下载记录与积分判断解耦，便于支持“首次扣费，多次免费下载”的规则。
4. 文件路径由配置类与常量统一拼接，减少硬编码路径散落。
5. 控制器对中文文件名做了兼容处理，考虑了实际下载体验。
6. 该接口业务成功并不绝对等于磁盘文件一定存在；如果文件读取异常，控制器仍会捕获并返回“下载失败”。

#### 当前价值

该接口的完成，意味着论坛已经具备了较完整的附件变现与资源分发基础能力。它不仅支持“下载”，更重要的是把积分体系、消息提醒、下载记录和附件统计串成了一条完整业务链路。

---

## 四、今日补充的支撑代码与公共能力

除了接口本身，今天的工作还体现在大量支撑代码的补充与协同上。这部分虽然不直接暴露为单个接口，但实际上决定了这些前台接口能否真正稳定运行。

### 4.1 Service 层能力继续补齐

#### `ForumArticleServiceImpl`

该类已经承担论坛文章分页查询、文章详情读取等核心职责：

- `findListByPage`：统一处理文章分页；
- `readArticle`：读取文章详情并在已审核状态下增加阅读量。

这意味着文章列表与文章详情不再只是简单的 Mapper 调用，而是已经具备一定业务语义封装。

#### `ForumBoardServiceImpl`

该类提供：

- 板块线性数据查询；
- 板块树形结构转换。

它把前台真正需要的树形结构在服务层完成转换，减轻了前端处理复杂度。

#### `LikeRecordServiceImpl`

该类是本次工作中非常重要的互动支撑类，已经具备：

- 文章点赞/取消点赞；
- 评论点赞能力预留；
- 点赞记录写入与删除；
- 文章/评论点赞数联动更新；
- 用户消息写入。

#### `ForumArticleAttachmentServiceImpl`

该类承担了附件下载最关键的业务逻辑：

- 附件是否存在校验；
- 首次下载与重复下载判断；
- 积分充足校验；
- 下载记录维护；
- 附件下载数更新；
- 积分流转；
- 消息通知。

#### `ForumArticleAttachmentDownloadServiceImpl`

该类为“是否下载过附件”这一能力提供了直接的数据支撑，特别是：

- `getForumArticleAttachmentDownloadByFileIdAndUserId`

这个方法在前台下载弹窗和下载业务中都非常关键。

---

### 4.2 VO、DTO 与聚合返回结构逐步清晰

#### `SessionWebUserDto`

该 DTO 当前至少承载：

- `userId`
- `nickName`
- `province`
- `isAdmin`

它在论坛接口中的作用非常重要，因为它决定了：

- 用户是否已登录；
- 当前用户是否是作者；
- 当前用户是否为管理员；
- 当前用户是否具备某些特殊可见权限。

#### `ForumArticleVO`

文章列表和详情页返回并不是直接暴露数据库实体，而是通过 `ForumArticleVO` 向前端提供更加适合展示的字段，包括但不限于：

- 文章 ID
- 板块信息
- 作者信息
- 标题、封面、摘要、内容
- 发布时间
- 阅读数、点赞数、评论数
- 置顶状态
- 附件标记
- 状态等

#### `FormArticleDetailVO`

这是文章详情页最重要的聚合返回对象，当前包含：

- `forumArticle`：文章主信息
- `attachment`：附件信息
- `haveLike`：当前用户是否已点赞

它使文章详情页可以通过一次请求拿到大部分必要数据。

#### `ForumArticleAttachmentVo`

该 VO 用于向前端返回附件基本信息，在详情页中承担“附件展示卡片”的作用。

#### `PaginationResultVO`

该对象统一承载：

- 总记录数
- 当前页码
- 每页大小
- 总页数
- 当前页数据列表

这让分页接口拥有统一输出结构，也便于前端通用分页组件复用。

---

### 4.3 枚举与常量让业务语义更加清晰

#### `ArticleOrderTypeEnum`

该枚举统一管理文章列表排序方式：

- 热榜排序
- 发布时间排序
- 最新排序

它把排序逻辑从零散判断中抽离出来，避免控制器中出现过多硬编码。

#### `ArticleStatusEnum`

该枚举统一描述文章状态：

- `DEL`：已删除
- `NO_AUDIT`：待审核
- `AUDIT`：已审核

它直接参与了文章列表与文章详情可见范围控制。

#### `OperRecordOpTypeEnum`

该枚举统一管理操作记录类型：

- 文章点赞
- 评论点赞

当前虽然主要用于文章点赞，但已经为后续评论互动预留扩展。

#### `Constants`

常量类中与本次工作强相关的内容包括：

- `FILE_FOLDER_FILE = "/file/"`
- `FILE_FOLDER_ATTACHMENT = "attachment/"`
- `SESSION_KEY`
- 其它会话与系统配置相关常量

尤其是附件下载时，文件磁盘路径的拼接就是依赖这些常量完成的。

---

### 4.4 配置类开始参与实际业务流程

#### `WebConfig`

虽然这个配置类前面已经用于邮件发送等能力，但在当前阶段，它还承担了文件存储根目录等配置读取职责。

在附件下载接口中，控制器通过：

`webConfig.getProjectFolder()`

获取项目文件根目录，再结合常量与附件相对路径定位真实文件。

这说明当前项目已经开始形成“配置类统一提供环境信息，业务代码只负责消费配置”的结构。

这种设计比把磁盘路径直接写死在控制器里要更安全，也更方便后续部署到不同环境。

---

### 4.5 Mapper 层配合业务规则逐步完善

从当前实现可以看出，今天接口背后的 Mapper 层也已经承担了更细粒度的数据操作职责，例如：

- `ForumArticleMapper`
    - 查询文章
    - 更新阅读数
    - 更新点赞数
- `ForumArticleAttachmentMapper`
    - 查询附件
    - 更新附件下载次数
- `ForumArticleAttachmentDownloadMapper`
    - 按 `fileId + userId` 查询下载记录
    - 插入或更新下载记录
- `LikeRecordMapper`
    - 查询点赞记录
    - 新增或删除点赞记录
- `UserMessageMapper`
    - 查询是否已存在同类消息
    - 写入点赞或下载消息

这说明当前项目的持久层不再只是“通用 CRUD 代码生成结果”，而是已经围绕论坛业务规则开始出现针对性很强的方法设计。

---

## 五、当前阶段的业务链路总结

从当前仓库代码状态来看，论坛前台已经不再停留在“账号注册登录”或者“简单读取文章”的阶段，而是逐步形成了比较完整的用户访问闭环：

1. 前端先通过 `/board/loadBoard` 获取板块树；
2. 再通过 `/forum/loadArticle` 按板块、父板块、排序方式分页加载文章列表；
3. 点击文章后，通过 `/forum/getArticleDetail` 获取文章详情、附件信息和点赞状态；
4. 用户可以通过 `/forum/doLike` 对文章执行点赞或取消点赞；
5. 如果文章带附件，前端可以先通过 `/forum/getUserDownloadInfo` 查看当前积分和下载状态；
6. 最终通过 `/forum/attachmentDownload` 完成真实下载，并触发积分与消息联动。

这条链路说明：

- 浏览能力已经具备；
- 用户互动能力已经开始具备；
- 附件资源下载能力已经开始具备；
- 积分体系与消息体系也已开始真正参与业务流转。

对论坛项目而言，这已经不是单纯的“接口初始化”，而是在逐步搭建真实可用的前台业务骨架。

---

## 六、第五天工作总结

第五天的工作，可以理解为在第四天“前台可浏览”的基础上，继续向“前台可互动、可下载、可形成行为闭环”推进。

今天最重要的价值，不只是新增了几个接口，而是把这些接口背后的业务规则真正串联了起来：

1. 文章列表支持按板块、父板块、排序方式和登录状态查询；
2. 文章详情不仅返回文章本身，还会聚合附件信息与用户点赞态；
3. 文章点赞已经具备切换逻辑、计数更新和消息提醒；
4. 附件下载已经具备下载前信息查询、首次扣积分、多次免扣、作者免扣、消息通知、文件流输出等完整规则；
5. Service、Mapper、VO、DTO、枚举、配置类等公共支撑代码进一步完善，为后续评论、发帖、消息中心、个人中心等模块开发打下了稳定基础。

同时，也需要明确一点：由于最近一次 Git
提交中已经混入了部分今天完成的接口代码，所以今天这份日报并不是机械地按照“提交记录时间”拆分内容，而是根据当前仓库中已经落地的能力进行整体总结。这样的整理方式更符合项目当前的真实开发状态。

到目前为止，`horizonhub` 后端已经逐步具备一个论坛系统应有的核心前台能力雏形。后续如果继续推进评论、回复、发帖、消息列表、个人主页、附件上传等功能，就可以直接基于当前这套结构继续扩展。
