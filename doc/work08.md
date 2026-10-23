# work08 - 文章发布接口技术实现报告

## 1. 目标与范围

本报告聚焦社区模块文章发布接口 `POST /forum/postArticle`（控制器映射为 `@RequestMapping("/postArticle")`
）的实现细节与设计要点，覆盖控制层参数接收与校验、业务层处理链路、状态与权限策略、附件与封面协同处理、异常机制以及可维护性考量。

接口实现入口位于 `horizonhub-web/src/main/java/com/nlizzard/horizonhub/controller/ForumArticleController.java` 的
`postArticle` 方法，核心业务由 `forumArticleService.postArticle(...)` 完成。

## 2. 接口职责定义

文章发布接口承担以下职责：

- 接收发帖页提交的文章主体信息（板块、标题、摘要、正文、编辑器类型）。
- 接收可选上传对象（封面 `cover`、附件 `attachment`）及附件积分配置 `integral`。
- 基于登录态注入作者身份信息，形成可落库的文章实体。
- 执行编辑器与内容一致性约束（Markdown 模式必须存在 `markdownContent`）。
- 将帖子与附件元数据统一交由服务层处理，返回生成后的 `articleId`。

该接口是“发帖场景”的写入入口，既提供业务字段组装，也作为参数安全与业务前置约束的第一道防线。

## 3. 请求模型与参数设计

`postArticle` 的方法签名反映了对富文本与 Markdown 双编辑模式的兼容设计：

- 文件参数
    - `MultipartFile cover`：可选封面。
    - `MultipartFile attachment`：可选附件。
- 数值参数
    - `Integer integral`：附件下载积分；空值按 0 处理。
    - `Integer pBoardId`：父板块 ID（必填）。
    - `Integer boardId`：子板块 ID（可选，允许只在父板块发帖）。
    - `Integer editorType`：编辑器类型（必填）。
- 文本参数
    - `String title`：文章标题（必填，长度上限 50）。
    - `String content`：正文 HTML（可校验）。
    - `String markdownContent`：Markdown 源文。
    - `String summary`：摘要（长度上限 200）。

### 3.1 注解驱动校验

控制器在方法级启用：

- `@GlobalInterceptor(checkLogin = true, checkParams = true)`：要求用户登录，且开启参数统一校验。

参数级通过 `@VerifyParam` 明确约束：

- `pBoardId`：`required = true`
- `title`：`required = true, max = 50`
- `content`：存在校验注解，参与统一参数处理流程
- `editorType`：`required = true`
- `summary`：`max = 200`

这种“方法级开关 + 参数级规则”组合，将通用校验逻辑从业务代码中抽离，控制器主体聚焦业务编排。

## 4. 控制层实现细节

`postArticle` 的执行过程可以拆分为 6 个步骤：

1. **标题净化**  
   `title = StringTools.escapeTitle(title);` 先执行标题转义/净化，降低标题字段携带特殊字符导致展示层风险的概率。

2. **会话用户解析**  
   通过 `getUserInfoFromSession(session)` 获取 `SessionWebUserDto`，后续用于作者身份注入与管理员态判定。

3. **文章实体组装**  
   构造 `ForumArticle` 并注入：`pBoardId`、`boardId`、`title`、`content`、`markdownContent`、`editorType`、`summary`，以及用户侧元数据：
    - `userId`
    - `nickName`
    - `userIpAddress`（当前实现使用会话中的地域信息 `province`）

4. **编辑器一致性校验**  
   当 `editorType == MARKDOWN` 且 `markdownContent` 为空时，抛出
   `BusinessException("编辑器为Markdown编辑器,Markdown内容不能为空")`。  
   该校验属于“跨字段约束”，无法仅通过单字段 `@VerifyParam` 完成，因此放在控制层执行。

5. **附件元数据组装**  
   构造 `ForumArticleAttachment`，设置 `integral`（空值降级为 0），确保服务层拿到完整的附件配置上下文。

6. **调用服务层并返回主键**  
   `forumArticleService.postArticle(userDto.getAdmin(), forumArticle, forumArticleAttachment, cover, attachment);`  
   服务处理完成后，控制器返回 `forumArticle.getArticleId()`，作为前端跳转详情页或继续编辑的核心引用。

## 5. 业务设计分析

### 5.1 权限与身份策略

- 通过 `checkLogin = true` 保证发帖用户身份已建立。
- 通过 `userDto.getAdmin()` 传递管理员标识到服务层，为“免审/审核策略分流”预留决策条件。
- 作者身份不由前端传入，而由会话侧注入，避免伪造作者信息。

### 5.2 双编辑器模式

接口同时接收 `content` 与 `markdownContent`，通过 `editorType` 指定主模式：

- 富文本模式：核心内容通常落在 HTML 字段。
- Markdown 模式：强制要求 `markdownContent`，兼容后续渲染或二次转换链路。

这种设计降低了客户端形态切换成本，也为内容持久化提供了格式扩展空间。

### 5.3 文件与主体解耦

控制器将文章实体与附件实体分开组装，再统一交给服务层：

- 文章主体负责内容与作者信息。
- 附件实体负责下载积分等附件域数据。
- 二进制对象（封面/附件）通过 `MultipartFile` 原样透传。

该拆分有利于服务层在事务内实施“文章-附件”协同落库与失败回滚。

### 5.4 异常与响应语义

- 参数问题与业务规则不满足时，抛出 `BusinessException`，由统一异常处理转换为标准响应。
- 成功响应返回 `ResponseVO<String>`，负载为 `articleId`，保持写接口的最小返回原则。

## 6. 调用链与时序说明

发布请求的关键链路如下：

1. 前端提交多部分表单（文本字段 + 可选文件）。
2. 控制器通过拦截器完成登录态与参数合法性预校验。
3. 控制器完成标题净化、跨字段校验、实体组装。
4. 服务层执行文章发布主流程（状态决策、数据写入、文件处理等）。
5. 返回 `articleId`，前端基于该标识执行后续跳转或提示。

该链路将“请求协议处理”与“领域业务处理”分层，符合控制层薄、服务层厚的常见实践。

## 7. 边界条件与风险点

当前实现中的重点边界包括：

- **Markdown 空内容保护**：已覆盖编辑器-内容不一致场景。
- **积分默认值兜底**：`integral` 为空自动置 0，避免空值传播。
- **标题输入安全**：通过 `escapeTitle` 提前净化输入。

需要重点关注的风险点：

- `content` 与 `markdownContent` 的互斥/并存策略依赖上层约定，建议在服务层补充更严格规则，防止数据语义漂移。
- `userIpAddress` 使用 `province` 字段承载，语义上更接近“地域信息”而非严格 IP；建议命名或映射关系进一步澄清。
- 文件大小、类型与存储失败补偿逻辑在本方法未显式体现，依赖服务层实现质量。

## 8. 可维护性与扩展建议

在保持现有接口契约不变的前提下，可沿以下方向增强：

- 为 `editorType` 建立更清晰的内容策略矩阵（必填字段、渲染来源、展示优先级）。
- 在发布成功响应中扩展轻量元数据（如审核状态）以减少前端二次查询。
- 对附件上传补充可观测指标（上传失败原因分类、平均耗时、大小分布）。
- 在服务层明确事务边界与补偿路径，保证“文章与附件”原子一致性。

## 9. 结论

`/forum/postArticle` 接口已形成完整的发布入口能力：具备登录与参数双重校验、支持双编辑器内容模型、能够协同处理封面与附件、并以
`articleId` 作为稳定回执。当前实现结构清晰，控制层职责边界明确，已满足常规发帖业务闭环；后续可在内容策略一致性、字段语义清晰度与可观测性方面持续增强。

