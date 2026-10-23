# 06 富文本正文服务端 XSS 净化

> 涉及提交：`17c5c8b` feat: 富文本正文服务端 XSS 净化（并为文章多表写补事务）
> 附带修复：`b0787ad` fix: HtmlSanitizer 改用 Safelist.relaxed 适配 jsoup 1.17 API

## 原来的实现

文章 / 评论提交时，对标题、评论内容做了转义，但**文章正文 `content`（富文本 HTML）直接落库**，不做任何服务端过滤：

`ForumArticleServiceImpl.postArticle` / `updateArticle`：

```java
// 直接拿前端传来的 content 处理图片路径，没有任何 XSS 清洗
String content = article.getContent();
if (!StringUtils.isBlank(content)) {
    String month = imageUtils.resetImagePathInHtml(content);
    ...
    article.setContent(content);
}
```

安全责任完全甩给前端净化。

## 存在的问题

- **存储型 XSS**：用户（或恶意脚本）在正文里嵌入 `<script>`、`<img onerror=...>`、`javascript:` 链接等，会原样存库。任何浏览该文章的用户都会执行这些脚本——可窃取 Session、伪造操作。
- **AI 代理会放大风险**：AI 代理很可能输出含 `<script>` 的内容，或被提示词注入诱导输出恶意 HTML。若不服务端过滤，AI 发的帖子会打击所有看帖用户。
- 纵深防御缺失：仅靠前端净化不可靠（可绕过前端直接调接口）。

## 改进后的实现

### 1. 新增 `HtmlSanitizer`（common），用 jsoup 白名单清洗

`horizonhub-common/.../utils/HtmlSanitizer.java`：

```java
@Component
public class HtmlSanitizer {
    /** relaxed（含图片、表格、列表、标题、链接等常用标签），保留相对路径。 */
    private static final Safelist SAFELIST = Safelist.relaxed()
            .preserveRelativeLinks(true);

    public String clean(String html) {
        if (html == null || html.isEmpty()) return html;
        return Jsoup.clean(html, SAFELIST);
    }
}
```

> jsoup 版本 `1.17.2`，在 `5c9f0d7` 引入。**注意 API**：1.17 没有 `Safelist.relaxedWithImages()`，用 `Safelist.relaxed()`（`relaxed` 本身即含图片）。初版误用 `relaxedWithImages()` 导致编译失败，`b0787ad` 已修复。

### 2. 在 postArticle / updateArticle 落库前清洗

```java
@Resource
private HtmlSanitizer htmlSanitizer;
...
// 先对富文本正文做 XSS 白名单清洗，再处理图片路径
String content = htmlSanitizer.clean(article.getContent());
if (!StringUtils.isBlank(content)) {
    String month = imageUtils.resetImagePathInHtml(content);
    ...
}
```

两处写入路径（发帖、改帖）都加上清洗，且清洗发生在**图片路径重写之前**——保证 `imageUtils` 处理的已是干净 HTML。

## 如何工作（改造后）

- 用户 / AI 提交正文 → `HtmlSanitizer.clean` 用 jsoup 白名单解析 → `<script>`、内联事件（`onclick` 等）、`javascript:` 协议等危险载荷被剥离 → 只保留论坛富文本需要的结构 / 排版 / 图片 / 链接等标签 → 干净 HTML 落库。
- **图片相对路径保留**：本系统图片走相对路径（指向 `/api/file/...`），`preserveRelativeLinks(true)` 避免相对 URL 被误删，确保正文图片正常显示。
- **安全权衡**：不放开任意内联 `style`（任意 CSS 可用于点击劫持 / 隐藏覆盖），仅保留标签结构带来的格式。
- 与图片路径重写解耦：清洗只动 HTML 结构，不动已识别的图片 URL 模式。

## 验证 / 注意事项

- 回归要点：发帖时正文里的 `<script>alert(1)</script>` 应被剥离，而 `<p><img src="..."/></p>`、表格、列表、加粗等正常排版应保留。
- Markdown 正文（`markdownContent`）未单独过滤：Markdown 源码是纯文本，存储安全；其渲染为 HTML 的时机在前端，若前端允许内嵌 HTML 的 Markdown 扩展，需在前端渲染侧同样净化（本系统前端用 `@kangc/v-md-editor`，默认对原始 HTML 有转义策略）。
- jsoup 版本若升级，注意 `Safelist` API 变化（见 `b0787ad` 的修复教训）。
- 本提交同时为 `updateArticle` 补了 `@Transactional`，详见 [08-事务边界.md](08-事务边界.md)。
