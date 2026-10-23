package com.nlizzard.horizonhub.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    private final HtmlSanitizer htmlSanitizer = new HtmlSanitizer();

    @Test
    void keepsRelativeImageUrlUsedByThePostEditor() {
        String html = "<p><img src=\"/api/file/getImage/temp/123.jpg\" alt=\"测试图片\"></p>";

        assertThat(htmlSanitizer.clean(html))
                .isEqualTo("<p><img src=\"/api/file/getImage/temp/123.jpg\" alt=\"测试图片\"></p>");
    }

    @Test
    void removesJavascriptImageUrl() {
        String html = "<p><img src=\"javascript:alert(1)\" alt=\"恶意图片\"></p>";

        assertThat(htmlSanitizer.clean(html))
                .isEqualTo("<p><img alt=\"恶意图片\"></p>");
    }
}
