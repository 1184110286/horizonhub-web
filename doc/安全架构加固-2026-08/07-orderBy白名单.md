# 07 orderBy 排序字段白名单（消除 SQL 注入面）

> 涉及提交：`dc2bf33` refactor: orderBy 排序字段白名单校验，消除 SQL 注入面

## 原来的实现

### Mapper XML 用 `${}` 字符串拼接排序

**11 个** Mapper XML 都用 `${query.orderBy}` 拼接（而非预编译 `#{}`），例如 `ForumArticleMapper.xml`：

```xml
<select id="selectList" resultMap="base_result_map">
    SELECT ... FROM forum_article
    <include refid="query_condition"/>
    <if test="query.orderBy!=null">order by ${query.orderBy}</if>   <!-- 字符串拼接 -->
    <if test="query.simplePage!=null">limit #{query.simplePage.start},#{query.simplePage.end}</if>
</select>
```

### BaseQuery 的 setter 不做任何校验

```java
public class BaseQuery {
    private String orderBy;
    ...
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;     // 原样接收，Spring 可直接绑定请求参数
    }
}
```

## 存在的问题

`${}` 是**字符串直接拼接进 SQL**，是 SQL 注入的经典入口。

- **当前暂不可利用**：审计确认所有调用点的 `orderBy` 都来自服务端枚举 / 常量（`ArticleOrderTypeEnum.getOrderSql()`、`CommentSortTypeEnum`，以及 `"post_time desc"` / `"sort ASC"` 等硬编码），暂无用户输入直达 `${}`。
- **但这是「靠使用方纪律」的防御**：一旦新增 Controller 忘记覆盖 orderBy、或把请求参数写进 `query.setOrderBy(...)`，立刻就是注入点。
- **AI 触发风险最高**：AI 助手「按热度查文章」「按时间排序」天然会把自然语言映射成排序字段，若映射结果直接灌进 `setOrderBy`，就是直接的 SQL 注入。这正是引入 AI 前必须消除的面。

## 改进后的实现

在 `BaseQuery.setOrderBy` 处加**白名单正则校验**，从源头阻断：

```java
private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
        "^\\s*[a-zA-Z_][a-zA-Z0-9_]*(\\s+(?i:asc|desc))?" +
        "(\\s*,\\s*[a-zA-Z_][a-zA-Z0-9_]*(\\s+(?i:asc|desc))?)*\\s*$");

public void setOrderBy(String orderBy) {
    if (orderBy == null || orderBy.isBlank()) {     // null/空视为不排序
        this.orderBy = null;
        return;
    }
    if (!ORDER_BY_PATTERN.matcher(orderBy).matches()) {   // 仅放行白名单片段
        throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "非法的排序参数");
    }
    this.orderBy = orderBy;
}
```

> 顺带删除了原文件里的死代码 `setPagoNo`（拼写错误的重复 setter，正下方已有正确的 `setPageNo`）。

## 如何工作（改造后）

- **白名单语义**：只允许「列名 + 可选 asc/desc」，列名限定为 `字母/下划线开头、由字母数字下划线组成`，支持逗号分隔的多列排序。点、引号、分号、括号、注释符等任何注入字符都不可能通过。
- **现有合法值全部通过**：
  - `ArticleOrderTypeEnum.HOT` = `"top_type desc,comment_count desc,good_count desc,read_count desc"`（多列）✓
  - `"post_time desc"` / `"sort ASC"` / `"comment_id asc"` ✓
- **注入载荷被拒**：任何含 `'` / `;` / `--` / `()` / 子查询的串都抛 `CODE_600`，在进入 Mapper 前就被拦截。
- 校验在 setter（数据入口）而非 Mapper，意味着无论哪个调用方设置 orderBy，都过同一道闸。

## 验证 / 注意事项

- 改造前已逐一核对了所有 `setOrderBy` 调用点（`ForumCommentServiceImpl`、`ForumBoardServiceImpl`、评论 Controller 组合排序等），确认现有取值均匹配白名单，不会误伤。
- 评论排序有一处组合：`TOP_SORT_TYPE`（`"top_type desc ,"`）+ `HOT/NEW`。其中 `TOP_SORT_TYPE` 单独是带尾逗号的片段，但它**从不单独传入** `setOrderBy`——Controller 总是拼接 `HOT`/`NEW` 后再设值（拼接结果如 `"top_type desc ,good_count desc , comment_id asc"`，匹配白名单）。若未来单独使用 `TOP_SORT_TYPE`，需调整组合方式（去掉尾逗号）。
- 这是「纵深防御 + 为 AI 铺路」：即使将来 AI 把自然语言映射成 orderBy，也无法注入。
