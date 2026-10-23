# work06 - 评论模块接口开发总结（loadComment / doLike / changeTopType）

## 一、概述

本次围绕评论模块补齐三类关键能力：**评论列表加载**、**评论点赞/取消点赞**、**评论置顶/取消置顶**
。这些能力共同支撑文章详情页“可浏览 + 可互动 + 可运营”的评论区闭环。

对应接口如下：

- `GET/POST /comment/loadComment`：分页加载评论（置顶优先、热度/最新排序、加载子评论、登录态点赞状态回显）
- `GET/POST /comment/doLike`：评论点赞/取消点赞（操作记录表驱动，联动评论点赞数与消息）
- `GET/POST /comment/changeTopType`：评论置顶/取消置顶（仅一级评论可置顶，权限校验在 Service 层完成）

文档重点放在设计动机、实现细节与关键业务规则，避免停留在“调用了哪个方法”的层面。

---

## 二、接口实现明细

### 1. `/comment/loadComment` —— 分页加载评论

#### 1.1 接口作用

用于在文章详情页按分页加载评论，默认加载一级评论，并按需携带其子评论列表：

- 一级评论用于构建评论主列表。
- 子评论用于构建“回复”与“楼中楼”的展示。
- 登录态下返回 `likeType`，用于直接渲染“是否点赞”。

#### 1.2 实现位置

- Controller：`horizonhub-web/.../ForumCommentController#loadComment`
- Service：`horizonhub-common/.../ForumCommentServiceImpl#findListByPage / findListByParam`
- Query：`horizonhub-common/.../ForumCommentQuery`
- 枚举与常量：`CommentSortTypeEnum`、`CommentStatusEnum`、`PageSize`
- 系统配置缓存：`SysCacheUtils.getSysSetting().getCommentSetting()`

#### 1.3 入参与约束

- `articleId`（必填）：文章 ID
- `pageNo`（可选）：页码
- `orderType`（可选）：排序类型
    - `null` 或非 `NEW_SORT_TYPE`：按“最热”排序
    - `NEW_SORT_TYPE`：按“最新”排序

额外约束：评论功能开关关闭时，本接口直接阻断（返回业务异常）。

#### 1.4 关键实现逻辑

##### 1）评论功能开关前置校验

接口开始处读取系统设置：

- `SysCacheUtils.getSysSetting().getCommentSetting().getCommentOpen()`

若评论未开启，则抛出：

- `BusinessException(ResponseCodeEnum.CODE_404)`

这一步的价值是把“功能控制”从数据库层与业务层前移到入口处，避免无效查询与无意义的数据返回。

##### 2）构建 `ForumCommentQuery`（只查一级评论 + 可扩展加载子评论）

构建查询对象并设置关键条件：

- `articleId`：限定文章范围
- `status = AUDIT`：仅返回审核通过评论
- `pCommentId = 0`：只查一级评论（主楼）
- `loadChildren = true`：开启子评论加载
- `pageSize = PageSize.SIZE50`：固定每页上限，控制返回体大小

> 这里采用“一级评论分页 + 二级评论按需加载”的方式，避免一次性把树形评论全部展开导致分页错乱或返回体膨胀。

##### 3）排序策略：置顶排序永远优先，然后再做二级排序

排序 SQL 由枚举拼接得到：

- 首段永远拼 `TOP_SORT_TYPE`（`top_type desc ,`），保证置顶评论稳定排前；
- 再根据 `orderType` 拼接：
    - “最热”：`good_count desc , comment_id asc`
    - “最新”：`comment_id desc`

最终把排序字符串交由持久层执行：

- `commentQuery.setOrderBy(commentSortFiled)`

##### 4）登录态回显：是否点赞（likeType）

如果 Session 中存在登录用户：

- `commentQuery.setQueryIsLike(true)`
- `commentQuery.setCurrentUserId(userId)`

Service/Mapper 层会基于 `currentUserId + commentId` 关联 `like_record`（或对应记录表）查询当前用户是否点过赞，并将结果回填到
`ForumComment.likeType`。

这一点对前端非常关键：无需额外接口即可直接展示“点赞按钮是否高亮”。

#### 1.5 Service 层的“加载子评论”实现细节（与 Controller 配合）

子评论加载不在 Controller 层做循环查询，而是在 `ForumCommentServiceImpl#findListByParam` 中做了批量查询与分组挂载：

1. 先按 `ForumCommentQuery` 查询出**一级评论列表**；
2. 若 `loadChildren=true`：
    - 构建 `subQuery`，复用必要条件：`articleId`、`status=AUDIT`，并同步 `queryIsLike/currentUserId`；
    - 从一级评论结果中提取 `parentCommentIdList`（即一级评论的 commentId 列表）；
    - 一次性查询所有二级评论：`selectList(subQuery)`；
    - 按 `pCommentId` 分组：`groupingBy(ForumComment::getPCommentId)`；
    - 将分组结果挂载回各自父评论的 `children` 字段。

这种方式避免了 N+1 查询问题，并且保证子评论只来源于当前页的一级评论集合。

#### 1.6 返回结构与字段说明

- 返回：`ResponseVO<PaginationResultVO<ForumComment>>`
- `PaginationResultVO`：包含总数、页大小、页码、总页数、数据列表
- 评论对象关键字段：
    - `children`：子评论列表
    - `likeType`：当前用户是否点赞（0 未点赞 / 1 已点赞）
    - `topType`：是否置顶（0 未置顶 / 1 置顶）

---

### 2. `/comment/doLike` —— 点赞/取消点赞评论

#### 2.1 接口作用

用于对评论执行“点赞”或“取消点赞”。接口的设计目标是：

- 同一个请求入口同时支持点赞与取消点赞（前端无需区分两个 URL）；
- 点赞行为落到记录表，确保可追溯与可扩展；
- 联动更新评论的点赞数（`good_count`），并在需要时写入消息（点赞通知）。

#### 2.2 实现位置

- Controller：`horizonhub-web/.../ForumCommentController#doLike`
- Service：`horizonhub-common/.../LikeRecordServiceImpl#doLike`（内部会路由到评论点赞逻辑）
- 核心方法：`LikeRecordServiceImpl#commentLike(...)`
- Mapper 更新：`forumCommentMapper.updateCommentCount(±1, commentId)`
- 关联消息：`userMessageMapper`（用于插入评论点赞消息，且做了去重/自赞过滤）

#### 2.3 入参与约束

- `commentId`（必填）：评论 ID
- 必须登录：`@GlobalInterceptor(checkLogin = true, checkParams = true)`

#### 2.4 关键实现逻辑

##### 1）入口统一 + 参数/登录拦截

控制层通过 `@GlobalInterceptor` 统一完成：

- 登录态校验（未登录直接拦截）
- 参数必填校验（commentId 缺失直接拦截）

##### 2）点赞/取消点赞的“状态切换”规则

核心规则在 `LikeRecordServiceImpl#commentLike`：

- 先按 `objectId(commentId) + userId + opType` 查询是否已有点赞记录：
    - 有记录：
        - 删除记录（取消点赞）
        - 评论点赞数 `good_count - 1`
    - 无记录：
        - 插入记录（点赞）
        - 评论点赞数 `good_count + 1`

该模式的好处是天然幂等：同一用户对同一评论的点赞状态只会在“有/无记录”之间切换。

##### 3）点赞消息的联动与去重

在点赞分支下，会构建一条 `UserMessage`：

- 接收人是评论作者 `forumComment.getUserId()`
- 发送人是当前用户

同时做了两类保护：

- **自赞过滤**：如果发送人 == 接收人，则不发消息
- **消息去重**：按文章 ID、评论 ID、发送人、消息类型查询是否已存在，存在则不重复插入

这避免了刷点赞时消息轰炸。

##### 4）接口回显：返回评论实体 + likeType

控制层在执行 `likeRecordService.doLike(...)` 后，会再次查询：

- 当前用户对该评论的操作记录是否存在（用于判断最终状态）
- 评论实体 `ForumComment`

并回填：

- `comment.setLikeType(userOperRecord == null ? 0 : 1)`

这样前端无需额外请求即可立刻更新点赞按钮状态。

#### 2.5 关键边界场景

- 未登录：由拦截器统一阻断。
- 重复请求：通过“记录存在/不存在”切换实现自然幂等。
- 消息重复：通过“自赞过滤 + 唯一性查询”避免重复插入。

---

### 3. `/comment/changeTopType` —— 置顶/取消置顶

#### 3.1 接口作用

用于对评论执行置顶/取消置顶。置顶能力属于“运营能力”，并且为了保持列表易读性，当前规则限定为：**仅一级评论允许置顶**。

置顶后在 `/comment/loadComment` 中通过 `TOP_SORT_TYPE` 排序立即体现。

#### 3.2 实现位置

- Controller：`horizonhub-web/.../ForumCommentController#changeTopType`
- Service：`horizonhub-common/.../ForumCommentServiceImpl#changeTopType`
- 枚举校验：`CommentTopTypeEnum`
- 文章校验：`ForumArticleMapper.selectByArticleId(...)`

#### 3.3 入参与约束

- `commentId`（必填）：评论 ID
- `topType`（必填）：目标置顶状态
    - `0`：取消置顶
    - `1`：置顶
- 必须登录：`@GlobalInterceptor(checkLogin = true, checkParams = true)`

#### 3.4 Service 层的完整校验链路（核心规则）

置顶接口的校验集中在 Service 层，保证 Controller 轻量，规则可持续演进。当前校验链路如下：

1. **置顶参数合法性**：
    - `CommentTopTypeEnum.getByType(topType)` 为空则抛 `CODE_600`
2. **评论存在性**：
    - 评论不存在抛 `CODE_404`（文案：评论不存在）
3. **仅一级评论可置顶**：
    - `pCommentId != 0` 直接拒绝（文案：只能置顶一级评论）
4. **文章存在性**：
    - 文章不存在抛 `CODE_404`（文案：文章不存在）
5. **权限校验**（当前规则：仅文章作者可操作）：
    - `forumArticle.getUserId()` 不等于当前用户 `userId`，抛异常（文案：没有权限操作）
6. **禁止重复操作**：
    - 若评论当前 `topType` 与目标 `topType` 一致，抛 `CODE_600`（无需重复操作）

通过以上校验后，才落库更新：

- `updateInfo.setTopType(topType)`
- `forumCommentMapper.updateByCommentId(updateInfo, commentId)`

#### 3.5 关键边界场景

- 非法 topType：直接拒绝，避免脏数据。
- 二级评论置顶：直接拒绝，避免列表结构紊乱。
- 非文章作者调用：直接拒绝，避免越权置顶。
- 重复置顶/重复取消：直接拒绝，避免产生无意义写操作。

---

## 三、与通用能力的协同点（今日内容范围内）

本次 3 个接口能保持简洁且规则清晰，依赖于以下通用能力的支撑：

1. **统一拦截器**：`@GlobalInterceptor` 标准化登录校验与参数校验，控制层无需重复判空/判断登录。
2. **统一请求参数校验**：`@VerifyParam` 用于声明必填字段，降低遗漏风险。
3. **系统设置缓存**：评论开关来自 `SysCacheUtils`，实现“运行期可控”。
4. **枚举语义化**：排序策略、评论状态、置顶类型等均通过枚举表达，避免散落魔法值。
5. **分页与返回包装统一**：`PaginationResultVO` + `ResponseVO` 保持前端消费稳定。

---

## 四、自测与联调关注点（与今日接口对应）

1. 评论开关关闭：`/comment/loadComment` 直接被拒绝，且不触发数据库查询。
2. 匿名访问评论列表：能够正常分页返回，但不会回填 `likeType`。
3. 登录访问评论列表：能够回填 `likeType`，并携带子评论树。
4. 点赞切换：同一用户重复点击可在点赞/取消之间切换，并联动 `good_count` 变化。
5. 置顶切换：仅一级评论可置顶；置顶后列表排序立即体现；重复操作会被拒绝。
