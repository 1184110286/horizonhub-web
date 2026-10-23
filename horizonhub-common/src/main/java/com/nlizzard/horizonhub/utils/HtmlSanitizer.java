package com.nlizzard.horizonhub.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * 富文本 XSS 净化器。
 * <p>
 * 用 jsoup 白名单清洗用户提交的 HTML 正文（文章/评论），剥离 {@code <script>}、
 * 内联事件（onclick 等）、javascript: 协议等存储型 XSS 载荷，只保留论坛富文本需要的
 * 结构、排版、图片、链接等标签。图片/链接采用相对路径（指向本系统 /api/file/...），
 * 因此开启 {@code preserveRelativeLinks} 以避免相对 URL 被误删。
 * <p>
 * 安全权衡：不放开任意内联 {@code style}（任意 CSS 可用于点击劫持/隐藏覆盖），仅保留
 * 标签结构带来的格式。
 */
@Component
public class HtmlSanitizer {

    /**
     * 用于校验相对 URL 的安全基准地址。该地址不会写入已净化的正文，因为白名单配置会保留相对路径。
     */
    private static final String SANITIZE_BASE_URI = "https://horizonhub.invalid/";

    /**
     * 论坛富文本白名单：relaxed（含图片、表格、列表、标题、链接等常用标签），保留相对路径。
     */
    private static final Safelist SAFELIST = Safelist.relaxed()
            .preserveRelativeLinks(true);

    /**
     * 清洗 HTML；入参为 null/空时原样返回。
     */
    public String clean(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        // Jsoup 校验 URL 协议时需要基准地址解析相对路径；未提供 baseUri 会将
        // /api/file/getImage/temp/xxx.jpg 判为无效并删除 src，后续图片迁移也随之失效。
        return Jsoup.clean(html, SANITIZE_BASE_URI, SAFELIST);
    }
}
