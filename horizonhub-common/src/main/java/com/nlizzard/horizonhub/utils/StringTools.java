package com.nlizzard.horizonhub.utils;

import org.apache.commons.lang3.StringUtils;

public class StringTools {

    /**
     * 转义 HTML 标签
     *
     * @param content 原始内容
     * @return 转义后的内容
     */
    public static String escapeHtml(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        content = content.replace(" ", "&nbsp;");
        content = content.replace("\n", "<br>");
        return content;
    }

    /**
     * 截取文件后缀
     *
     * @param fileName 文件全名（后文件后缀）
     */
    public static String getFileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 截取文件名（不带后缀）
     *
     * @param fileName 文件全名（带后缀）
     */
    public static String getFileName(String fileName) {
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        return fileName;
    }

    /**
     * 转义 HTML 标签（适用于标题，空格和换行不转换）
     *
     * @param content 原始内容
     * @return 转义后的内容
     */
    public static String escapeTitle(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        content = content.replace(" ", "&nbsp;");
        // 标题不准换行，直接替换成空格
        content = content.replace("\n", "&nbsp;");
        return content;
    }
}
