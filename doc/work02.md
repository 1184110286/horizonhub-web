﻿# work02

## 一、第二天工作概述

在第一天完成后端工程基础骨架搭建之后，第二天的工作重点已经从“工程初始化”逐步转向“业务基础能力建设”。

这一天主要完成了两个方向的内容：

1. 设计并整理 `horizonhub` 论坛项目的数据库表结构，形成完整的建表 SQL，统一存放在 `sql/horizonhub.sql` 中。
2. 在 `horizonhub-common` 模块下，完成论坛项目公共代码层的基础搭建，包括实体类、查询对象、返回对象、Mapper 层、Service
   层、Controller 层、异常处理、枚举类以及工具类等内容。

这一阶段的工作，标志着项目已经从单纯的多模块初始化，进入到面向实际论坛业务的数据结构和通用代码结构建设阶段。后续无论是
`horizonhub-admin` 的后台管理接口，还是 `horizonhub-web` 的前台业务接口，都将基于这一阶段沉淀下来的数据库设计和公共模块继续扩展。

---

## 二、数据库库表设计

当前论坛项目的数据库脚本位于：`sql/horizonhub.sql`

根据现有 SQL 文件，当前已经完成 **11 张核心业务表** 的设计。这些表覆盖了论坛项目在用户、文章、评论、点赞、附件、系统配置、积分以及消息通知等方面的基础数据模型。

下面对每一张表进行详细说明。

### 1. `email_code` —— 邮箱验证码表

该表用于存储用户邮箱验证码信息，主要服务于注册、登录验证、找回密码等需要邮箱校验的业务场景。

| 字段            | 类型             | 描述                        |
|---------------|----------------|---------------------------|
| `email`       | `varchar(150)` | 用户邮箱，联合主键之一               |
| `code`        | `varchar(5)`   | 邮箱验证码，联合主键之一              |
| `create_time` | `datetime`     | 验证码创建时间                   |
| `status`      | `tinyint(1)`   | 验证码状态，`0` 表示未使用，`1` 表示已使用 |

该表采用 `email + code` 作为联合主键，用于保证验证码记录的唯一性。这样的设计能够支持同一个邮箱在不同时间生成多次验证码，同时也方便后续做验证码校验与失效判断。

---

### 2. `forum_article` —— 文章信息表

该表是论坛的核心业务表之一，用于存储帖子或文章的基础信息。一个论坛用户发布的主题帖、技术贴、分享贴等内容，都会最终落入这张表中。

| 字段                 | 类型             | 描述                                     |
|--------------------|----------------|----------------------------------------|
| `article_id`       | `varchar(15)`  | 文章 ID，主键                               |
| `board_id`         | `int(11)`      | 当前所属板块 ID                              |
| `board_name`       | `varchar(50)`  | 当前所属板块名称                               |
| `p_board_id`       | `int(11)`      | 父级板块 ID                                |
| `p_board_name`     | `varchar(50)`  | 父级板块名称                                 |
| `user_id`          | `varchar(15)`  | 发帖用户 ID                                |
| `nick_name`        | `varchar(20)`  | 发帖用户昵称                                 |
| `user_ip_address`  | `varchar(100)` | 用户 IP 对应的归属地信息                         |
| `title`            | `varchar(150)` | 文章标题                                   |
| `cover`            | `varchar(100)` | 文章封面                                   |
| `content`          | `text`         | 富文本内容                                  |
| `markdown_content` | `text`         | Markdown 内容                            |
| `editor_type`      | `tinyint(4)`   | 编辑器类型，`0` 表示富文本编辑器，`1` 表示 Markdown 编辑器 |
| `summary`          | `varchar(200)` | 文章摘要                                   |
| `post_time`        | `datetime`     | 发布时间                                   |
| `last_update_time` | `timestamp`    | 最后更新时间                                 |
| `read_count`       | `int(11)`      | 阅读数量，默认 `0`                            |
| `good_count`       | `int(11)`      | 点赞数量，默认 `0`                            |
| `comment_count`    | `int(11)`      | 评论数量，默认 `0`                            |
| `top_type`         | `tinyint(4)`   | 置顶状态，`0` 表示未置顶，`1` 表示已置顶               |
| `attachment_type`  | `tinyint(4)`   | 是否有附件，`0` 表示没有附件，`1` 表示有附件             |
| `status`           | `tinyint(4)`   | 审核状态，`-1` 表示已删除，`0` 表示待审核，`1` 表示已审核    |

该表已经针对高频查询场景设计了多个索引，包括板块、父板块、发布时间、置顶状态、标题以及用户 ID
等字段。整体设计既能满足论坛列表页、详情页、搜索页的查询需求，也为后续的审核、置顶、统计等功能预留了扩展空间。

---

### 3. `forum_article_attachment` —— 文章附件表

该表用于存储论坛文章所关联的附件信息，例如压缩包、源码文件、文档资料、图片资源等。

| 字段               | 类型             | 描述         |
|------------------|----------------|------------|
| `file_id`        | `varchar(15)`  | 文件 ID，主键   |
| `article_id`     | `varchar(15)`  | 所属文章 ID    |
| `user_id`        | `varchar(15)`  | 上传附件的用户 ID |
| `file_size`      | `bigint(20)`   | 文件大小       |
| `file_name`      | `varchar(200)` | 文件名称       |
| `download_count` | `int(11)`      | 下载次数       |
| `file_path`      | `varchar(100)` | 文件存储路径     |
| `file_type`      | `tinyint(4)`   | 文件类型       |
| `integral`       | `int(11)`      | 下载该附件所需积分  |

通过该表，论坛后续可以实现“帖子附件上传与下载”的完整业务能力，同时还能结合积分系统做出“付费下载”或“积分下载”的功能设计。

---

### 4. `forum_article_attachment_download` —— 用户附件下载记录表

该表用于记录用户对文章附件的下载行为，主要作用是避免重复扣积分，同时方便统计某个用户对某个附件的下载情况。

| 字段               | 类型            | 描述                 |
|------------------|---------------|--------------------|
| `file_id`        | `varchar(15)` | 文件 ID，联合主键之一       |
| `user_id`        | `varchar(15)` | 用户 ID，联合主键之一       |
| `article_id`     | `varchar(15)` | 所属文章 ID            |
| `download_count` | `int(11)`     | 该用户下载该文件的次数，默认 `1` |

该表使用 `file_id + user_id` 作为联合主键，用来约束同一个用户对同一个附件的记录唯一性。这一设计在积分扣减、附件权限控制以及下载日志统计方面都很有价值。

---

### 5. `forum_board` —— 板块信息表

该表用于存储论坛板块信息，是整个论坛内容分类体系的基础。通过该表可以构建一级板块和二级板块，形成完整的论坛栏目结构。

| 字段           | 类型             | 描述                              |
|--------------|----------------|---------------------------------|
| `board_id`   | `int(11)`      | 板块 ID，主键，自增                     |
| `p_board_id` | `int(11)`      | 父级板块 ID                         |
| `board_name` | `varchar(50)`  | 板块名称                            |
| `cover`      | `varchar(50)`  | 板块封面                            |
| `board_desc` | `varchar(150)` | 板块描述                            |
| `sort`       | `int(11)`      | 排序值                             |
| `post_type`  | `tinyint(1)`   | 发帖权限，`0` 表示仅管理员可发帖，`1` 表示所有人可发帖 |

通过 `p_board_id`，系统可以支持父子板块结构；通过 `sort`，可以灵活控制板块在页面中的展示顺序；通过 `post_type`
，可以对某些板块设置发帖权限限制。

---

### 6. `forum_comment` —— 评论表

该表用于存储论坛文章的评论信息，同时也支持评论回复，因此它既承担一级评论，也承担二级回复的存储职责。

| 字段                | 类型             | 描述                       |
|-------------------|----------------|--------------------------|
| `comment_id`      | `int(11)`      | 评论 ID，主键，自增              |
| `p_comment_id`    | `int(11)`      | 父级评论 ID                  |
| `article_id`      | `varchar(15)`  | 所属文章 ID                  |
| `content`         | `varchar(800)` | 评论内容                     |
| `img_path`        | `varchar(150)` | 评论图片                     |
| `user_id`         | `varchar(15)`  | 评论用户 ID                  |
| `nick_name`       | `varchar(20)`  | 评论用户昵称                   |
| `user_ip_address` | `varchar(100)` | 用户 IP 对应的归属地信息           |
| `reply_user_id`   | `varchar(15)`  | 被回复用户 ID                 |
| `reply_nick_name` | `varchar(20)`  | 被回复用户昵称                  |
| `top_type`        | `tinyint(4)`   | 置顶状态，`0` 表示未置顶，`1` 表示置顶  |
| `post_time`       | `datetime`     | 评论发布时间                   |
| `good_count`      | `int(11)`      | 点赞数量，默认 `0`              |
| `status`          | `tinyint(4)`   | 审核状态，`0` 表示待审核，`1` 表示已审核 |

该表设计已经覆盖了评论审核、回复关系、评论点赞、评论置顶等论坛评论场景的核心需求，为后续构建完整评论树和消息提醒功能打下了基础。

---

### 7. `like_record` —— 点赞记录表

该表用于记录用户对文章或评论的点赞行为，既能支持去重，也能为后续的点赞统计、提醒通知提供基础数据。

| 字段               | 类型            | 描述                         |
|------------------|---------------|----------------------------|
| `op_id`          | `int(11)`     | 自增 ID，主键                   |
| `op_type`        | `tinyint(4)`  | 操作类型，`0` 表示文章点赞，`1` 表示评论点赞 |
| `object_id`      | `varchar(15)` | 点赞目标对象 ID                  |
| `user_id`        | `varchar(15)` | 点赞用户 ID                    |
| `create_time`    | `datetime`    | 点赞时间                       |
| `author_user_id` | `varchar(15)` | 被点赞内容作者的用户 ID              |

该表中设计了唯一索引 `(object_id, user_id, op_type)`，用于防止重复点赞。同时也建立了 `(user_id, op_type)`
索引，方便后续做用户点赞记录查询。

---

### 8. `sys_setting` —— 系统设置表

该表用于保存系统运行过程中的一些全局配置项，配置内容以 JSON 形式存储，能够兼顾灵活性和可扩展性。

| 字段             | 类型             | 描述            |
|----------------|----------------|---------------|
| `code`         | `varchar(10)`  | 配置编码，主键       |
| `json_content` | `varchar(500)` | 配置内容，JSON 字符串 |

从现有 SQL 初始化数据来看，该表已经预置了多个系统设置项，例如：

- `audit`：审核相关配置
- `comment`：评论相关配置
- `email`：邮件发送内容配置
- `like`：点赞相关限制配置
- `post`：发帖相关限制配置
- `register`：注册欢迎信息配置

这种配置存储方式有利于后续通过后台管理模块动态维护论坛系统参数。

---

### 9. `user_info` —— 用户信息表

该表用于存储论坛用户的核心账号信息，是用户注册、登录、积分、状态控制等功能的基础数据来源。

| 字段                      | 类型             | 描述                     |
|-------------------------|----------------|------------------------|
| `user_id`               | `varchar(15)`  | 用户 ID，主键               |
| `nick_name`             | `varchar(20)`  | 用户昵称                   |
| `email`                 | `varchar(150)` | 用户邮箱                   |
| `password`              | `varchar(50)`  | 用户密码                   |
| `sex`                   | `tinyint(1)`   | 性别，`0` 表示女，`1` 表示男     |
| `person_description`    | `varchar(200)` | 个人描述                   |
| `join_time`             | `datetime`     | 注册时间                   |
| `last_login_time`       | `datetime`     | 最后登录时间                 |
| `last_login_ip`         | `varchar(15)`  | 最后登录 IP                |
| `last_login_ip_address` | `varchar(100)` | 最后登录 IP 归属地            |
| `total_integral`        | `int(11)`      | 累计积分                   |
| `current_integral`      | `int(11)`      | 当前积分                   |
| `status`                | `tinyint(4)`   | 账号状态，`0` 表示禁用，`1` 表示正常 |

该表针对 `email` 和 `nick_name` 都建立了唯一索引，能够从数据库层面保证邮箱和昵称的唯一性。这对于论坛系统中的注册校验、账户识别以及用户资料展示都非常重要。

---

### 10. `user_integral_record` —— 用户积分记录表

该表用于存储用户积分变化的流水记录，用来反映用户在发帖、评论、下载附件等行为中产生的积分变动。

| 字段            | 类型            | 描述          |
|---------------|---------------|-------------|
| `record_id`   | `int(11)`     | 记录 ID，主键，自增 |
| `user_id`     | `varchar(15)` | 用户 ID       |
| `oper_type`   | `tinyint(4)`  | 积分操作类型      |
| `integral`    | `int(11)`     | 本次积分变动值     |
| `create_time` | `datetime`    | 创建时间        |

该表对于构建完整的积分体系至关重要。通过积分流水表，后续不仅可以展示用户积分记录，也便于定位积分异常问题和进行积分运营分析。

---

### 11. `user_message` —— 用户消息表

该表用于存储论坛中的消息通知信息，是实现“评论提醒”“点赞提醒”“系统通知”“附件下载提醒”等消息中心功能的重要基础表。

| 字段                 | 类型              | 描述                                                        |
|--------------------|-----------------|-----------------------------------------------------------|
| `message_id`       | `int(11)`       | 消息 ID，主键，自增                                               |
| `received_user_id` | `varchar(15)`   | 接收消息的用户 ID                                                |
| `article_id`       | `varchar(15)`   | 关联文章 ID                                                   |
| `article_title`    | `varchar(150)`  | 关联文章标题                                                    |
| `comment_id`       | `int(11)`       | 关联评论 ID                                                   |
| `send_user_id`     | `varchar(15)`   | 发送消息的用户 ID                                                |
| `send_nick_name`   | `varchar(20)`   | 发送消息用户昵称                                                  |
| `message_type`     | `tinyint(4)`    | 消息类型，`0` 表示系统消息，`1` 表示评论，`2` 表示文章点赞，`3` 表示评论点赞，`4` 表示附件下载 |
| `message_content`  | `varchar(1000)` | 消息内容                                                      |
| `status`           | `tinyint(4)`    | 消息状态，`1` 表示未读，`2` 表示已读                                    |
| `create_time`      | `datetime`      | 创建时间                                                      |

该表建立了唯一索引 `(article_id, comment_id, send_user_id, message_type)`
，用于在一定程度上避免重复生成相同类型的消息记录。同时也针对接收用户、状态、消息类型建立了普通索引，以便后续支持消息列表、未读统计、分类筛选等业务功能。

---

## 三、`horizonhub-common` 模块建设情况

`horizonhub-common` 是当前项目中最重要的公共模块之一。它承担的是“前后台通用基础能力沉淀”的职责，也就是说，无论未来是
`horizonhub-admin` 还是 `horizonhub-web`，很多共享的实体定义、查询结构、数据访问接口、业务接口和工具能力，都会优先放在这个模块中。

在第二天的开发工作中，该模块已经完成了较为完整的基础分层搭建。

### 3.1 实体类（Entity / POJO）

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/entity/pojo/`

当前已经定义了 **11 个 POJO 实体类**，并且基本与数据库表保持一一对应关系：

| 类名                               | 对应表                                 |
|----------------------------------|-------------------------------------|
| `EmailCode`                      | `email_code`                        |
| `ForumArticle`                   | `forum_article`                     |
| `ForumArticleAttachment`         | `forum_article_attachment`          |
| `ForumArticleAttachmentDownload` | `forum_article_attachment_download` |
| `ForumBoard`                     | `forum_board`                       |
| `ForumComment`                   | `forum_comment`                     |
| `LikeRecord`                     | `like_record`                       |
| `SysSetting`                     | `sys_setting`                       |
| `UserInfo`                       | `user_info`                         |
| `UserIntegralRecord`             | `user_integral_record`              |
| `UserMessage`                    | `user_message`                      |

这一层的完成，说明项目已经把数据库表结构正式映射到了 Java 实体层，为后续 Mapper 查询、Service 处理以及 Controller
返回数据提供了统一的数据载体。

---

### 3.2 查询类（Query）

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/entity/query/`

当前已经定义了 **11 个 Query 查询类**，分别对应各业务实体的查询场景。同时，在 `basequery/` 子目录下还定义了通用查询基础类：

- `BaseQuery`：基础查询类，用于封装通用查询条件
- `SimplePage`：分页对象，用于处理分页参数和分页偏移量

各业务查询类如下：

- `EmailCodeQuery`
- `ForumArticleQuery`
- `ForumArticleAttachmentQuery`
- `ForumArticleAttachmentDownloadQuery`
- `ForumBoardQuery`
- `ForumCommentQuery`
- `LikeRecordQuery`
- `SysSettingQuery`
- `UserInfoQuery`
- `UserIntegralRecordQuery`
- `UserMessageQuery`

这一层的搭建说明项目已经开始按照“查询对象封装查询条件”的方式组织代码，这种方式有利于后续统一分页、模糊查询、区间查询等逻辑，也能让
Service 和 Mapper 的调用更加清晰。

---

### 3.3 视图对象（VO）

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/entity/vo/`

当前已经定义了两个通用 VO：

| 类名                      | 说明                                              |
|-------------------------|-------------------------------------------------|
| `ResponseVO<T>`         | 通用接口响应对象，封装 `status`、`code`、`info`、`data` 等返回字段 |
| `PaginationResultVO<T>` | 通用分页结果对象，封装总记录数、页大小、当前页、总页数和列表数据                |

VO 层的搭建说明项目已经开始统一接口返回格式。后续无论前台还是后台接口，都可以基于这些通用返回对象形成风格一致的响应结构，从而降低前后端联调成本。

---

### 3.4 Mapper 层

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/mappers/`

当前已经定义了 **11 个业务 Mapper 接口**，并抽取了通用基础 Mapper：`BaseMapper<T, P>`。

`BaseMapper<T, P>` 当前已经封装了如下通用方法：

| 方法                             | 说明         |
|--------------------------------|------------|
| `insert(T)`                    | 新增单条数据     |
| `insertOrUpdate(T)`            | 新增或更新单条数据  |
| `insertBatch(List<T>)`         | 批量新增       |
| `insertOrUpdateBatch(List<T>)` | 批量新增或更新    |
| `selectList(P)`                | 根据查询条件查询列表 |
| `selectCount(P)`               | 根据查询条件统计数量 |

业务 Mapper 包括：

- `EmailCodeMapper`
- `ForumArticleMapper`
- `ForumArticleAttachmentMapper`
- `ForumArticleAttachmentDownloadMapper`
- `ForumBoardMapper`
- `ForumCommentMapper`
- `LikeRecordMapper`
- `SysSettingMapper`
- `UserInfoMapper`
- `UserIntegralRecordMapper`
- `UserMessageMapper`

这一层已经具备了较明显的 MyBatis 风格基础封装思路，能够减少重复的 CRUD 接口定义，并为后续 XML 映射编写提供统一规范。

---

### 3.5 Service 层

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/service/`

当前已经定义了 **11 个业务 Service 接口**，并在 `impl/` 目录下提供了对应的实现类：

- `EmailCodeService` / `EmailCodeServiceImpl`
- `ForumArticleService` / `ForumArticleServiceImpl`
- `ForumArticleAttachmentService` / `ForumArticleAttachmentServiceImpl`
- `ForumArticleAttachmentDownloadService` / `ForumArticleAttachmentDownloadServiceImpl`
- `ForumBoardService` / `ForumBoardServiceImpl`
- `ForumCommentService` / `ForumCommentServiceImpl`
- `LikeRecordService` / `LikeRecordServiceImpl`
- `SysSettingService` / `SysSettingServiceImpl`
- `UserInfoService` / `UserInfoServiceImpl`
- `UserIntegralRecordService` / `UserIntegralRecordServiceImpl`
- `UserMessageService` / `UserMessageServiceImpl`

Service 层的落地，说明项目已经完成了从数据访问层到业务层的结构搭建。虽然现阶段更多还是基础骨架和通用方法定义，但这一层已经为后续封装用户注册、发帖、评论、点赞、积分结算、消息通知等业务逻辑预留好了位置。

---

### 3.6 Controller 层

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/controller/`

当前已经定义了 **11 个业务 Controller**，同时还定义了基础控制器与全局异常处理控制器。

基础控制器位于：`controller/basecontroller/`

| 类名                                 | 说明                        |
|------------------------------------|---------------------------|
| `BaseController`                   | 基础控制器，封装统一成功返回结果等公共方法     |
| `GlobalExceptionHandlerController` | 全局异常处理控制器，用于统一处理系统异常和业务异常 |

业务 Controller 包括：

- `EmailCodeController`
- `ForumArticleController`
- `ForumArticleAttachmentController`
- `ForumArticleAttachmentDownloadController`
- `ForumBoardController`
- `ForumCommentController`
- `LikeRecordController`
- `SysSettingController`
- `UserInfoController`
- `UserIntegralRecordController`
- `UserMessageController`

虽然目前这些 Controller 更多体现为基础接口骨架，但它们已经把论坛项目的主要业务边界明确出来了，为后续在 `horizonhub-admin`
和 `horizonhub-web` 中继续扩展 API 提供了清晰结构。

---

### 3.7 异常处理

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/exception/`

当前已经定义了：

| 类名                  | 说明                      |
|---------------------|-------------------------|
| `BusinessException` | 自定义业务异常类，用于处理业务逻辑中的异常情况 |

业务异常类的存在，说明项目已经开始把“系统异常”和“业务异常”做出区分。这对于后续接口错误码、统一异常响应和日志排查都非常重要。

---

### 3.8 枚举类（Enums）

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/enums/`

当前已经定义了以下枚举类：

| 枚举类                   | 说明                     |
|-----------------------|------------------------|
| `ResponseCodeEnum`    | 响应码枚举，用于统一接口返回状态码和提示信息 |
| `DateTimePatternEnum` | 日期格式枚举，用于统一管理日期时间格式    |
| `PageSize`            | 分页大小枚举，用于统一定义分页常量      |

枚举类的引入，说明项目已经开始将系统中的常量语义化、结构化，能够降低硬编码带来的维护成本。

---

### 3.9 工具类（Utils）

路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/utils/`

当前已经定义了：

| 类名          | 说明                       |
|-------------|--------------------------|
| `DateUtils` | 日期工具类，用于统一处理日期格式化与解析相关操作 |

工具类虽然数量还不多，但已经体现出公共模块的定位。后续随着开发推进，项目中与字符串处理、文件处理、时间处理、分页处理相关的通用能力，也都适合继续沉淀在这一层。

---

## 四、第二天工作总结

综合来看，第二天的工作已经不再局限于“把项目跑起来”这一层面，而是进一步完成了论坛项目后端的核心基础建设。

具体来说，当前已经完成了以下内容：

| 工作项            | 完成情况                       |
|----------------|----------------------------|
| 数据库表结构设计       | 已完成，当前共整理 11 张核心业务表        |
| 实体类定义          | 已完成，POJO 与表结构基本一一对应        |
| 查询对象定义         | 已完成，支持后续分页与条件查询扩展          |
| 通用返回对象定义       | 已完成，统一了接口响应结构              |
| Mapper 层搭建     | 已完成，包含基础 Mapper 和业务 Mapper |
| Service 层搭建    | 已完成，接口与实现类均已建立             |
| Controller 层搭建 | 已完成，业务边界已经初步明确             |
| 异常处理设计         | 已完成，已引入业务异常类               |
| 枚举与工具类整理       | 已完成，具备进一步扩展基础              |

这一阶段的成果，为后续开发提供了非常扎实的基础：

- 对外，后续可以在 `horizonhub-web` 中逐步实现论坛前台接口，如发帖、列表、评论、点赞、消息中心等能力。
- 对内，后续可以在 `horizonhub-admin` 中逐步实现后台管理接口，如板块管理、文章审核、评论审核、系统配置维护、用户管理等能力。
- 对共用能力而言，`horizonhub-common` 已经具备继续沉淀论坛公共代码的基础，能够持续服务前后台两个模块。

整体来看，第二天的工作已经把论坛项目从“工程初始化阶段”推进到了“业务基础层搭建阶段”，为后续正式进入论坛业务功能开发创造了良好条件。
