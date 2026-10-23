package com.nlizzard.horizonhub.utils;

import cn.hutool.core.date.DateUtil;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.AppConfig;
import com.nlizzard.horizonhub.entity.enums.DateTimePatternEnum;
import jakarta.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageUtils {

    public static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    @Resource
    private AppConfig appConfig;

    /**
     * 将临时文件夹中的文章图片移动到正式文件夹中
     *
     * @param html 原始 HTML 内容
     * @return 月份文件夹名称（格式：yyyyMM）
     */
    public String resetImagePathInHtml(String html) {
        String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
        List<String> imageList = getImageList(html);
        for (String img : imageList) {
            resetImagePath(img, month);
        }
        return month;
    }

    /**
     * 处理 Markdown 正文图片：将 temp 临时图片移动到正式月份目录，并返回月份名。
     * <p>
     * Markdown 图片语法为 {@code ![alt](url)}，与 HTML 的 {@code <img src>} 不同，
     * 不能复用 {@link #getImageList(String)}（它只匹配 img 标签）。本方法单独匹配
     * Markdown 图片 URL，保证纯 Markdown 帖子的图片也能被正确移动、路径替换。
     *
     * @param markdown 原始 Markdown 内容
     * @return 月份文件夹名称（格式：yyyyMM）
     */
    public String resetImagePathInMarkdown(String markdown) {
        String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
        List<String> imageList = getMarkdownImageList(markdown);
        for (String img : imageList) {
            resetImagePath(img, month);
        }
        return month;
    }

    /**
     * 将临时文件夹中的文章图片移动到正式文件夹中
     *
     * @param imagePath 原始图片路径
     * @param month     当前月份（格式：yyyyMM），用于生成新的图片路径
     */
    private void resetImagePath(String imagePath, String month) {
        // 只有当图片路径不为空且包含临时文件夹路径temp时才进行处理
        if (StringUtils.isBlank(imagePath) || !imagePath.contains(Constants.FILE_FOLDER_TEMP)) {
            return;
        }
        // "/api/file/getImage/temp/xxx.jpg" -> "temp/xxx.jpg"
        imagePath = imagePath.replace(Constants.READ_IMAGE_PATH, "");
        String imageFileName = month + "/" + imagePath.substring(imagePath.lastIndexOf("/") + 1);
        File targetFile = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + "/" + Constants.FILE_FOLDER_IMAGE + "/" + imageFileName);
        try {
            FileUtils.copyFile(new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + "/" + imagePath), targetFile);
        } catch (IOException e) {
            logger.error("移动图片文件失败，原路径：{}，目标路径：{}", appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + "/" + imagePath, targetFile.getAbsolutePath(), e);
        }
    }

    /**
     * 从 HTML 内容中提取图片路径列表
     *
     * @param content HTML 内容
     * @return 图片路径列表
     */
    private List<String> getImageList(String content) {
        List<String> imageList = new ArrayList<>();
        // 匹配img标签的正则表达式，忽略大小写
        String regEx_img = "(<img.*src\\s*=\\s*(.*?)[^>]*?>)";
        Pattern p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE);
        Matcher m_image = p_image.matcher(content);
        while (m_image.find()) {
            String img = m_image.group();
            // img标签中匹配src属性的正则表达式，忽略大小写
            Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)")
                    .matcher(img);
            while (m.find()) {
                String imageUrl = m.group(1);
                imageList.add(imageUrl);
            }
        }
        return imageList;
    }

    /**
     * 从 Markdown 内容中提取图片 URL 列表，匹配 {@code ![alt](url)} 语法。
     *
     * @param markdown Markdown 内容
     * @return 图片 URL 列表
     */
    private List<String> getMarkdownImageList(String markdown) {
        List<String> imageList = new ArrayList<>();
        if (StringUtils.isBlank(markdown)) {
            return imageList;
        }
        // 匹配 ![任意](url)，捕获 url 部分
        Pattern pattern = Pattern.compile("!\\[[^\\]]*\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(markdown);
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            // markdown 图片 url 可能带 " 标题"，取空格前的部分
            int spaceIdx = url.indexOf(" ");
            if (spaceIdx > 0) {
                url = url.substring(0, spaceIdx);
            }
            imageList.add(url);
        }
        return imageList;
    }
}
