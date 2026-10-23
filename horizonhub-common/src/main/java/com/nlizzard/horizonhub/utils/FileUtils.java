package com.nlizzard.horizonhub.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.IdUtil;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.AppConfig;
import com.nlizzard.horizonhub.entity.dto.FileUploadDto;
import com.nlizzard.horizonhub.entity.enums.DateTimePatternEnum;
import com.nlizzard.horizonhub.entity.enums.FileUploadTypeEnum;
import com.nlizzard.horizonhub.exception.BusinessException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.File;
import java.util.Date;

// 文件工具类
@Component
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    @Resource
    private AppConfig appConfig;

    /**
     * 文件上传到本地
     *
     * @param file           文件对象
     * @param uploadTypeEnum 上传类型枚举
     * @param folder         文件夹名称(特殊：头像的话，这个值传用户ID)
     */
    public FileUploadDto uploadFile2Local(MultipartFile file,
                                          FileUploadTypeEnum uploadTypeEnum,
                                          String folder) {
        try {
            FileUploadDto uploadDto = new FileUploadDto();
            // 拿到文件名+后缀名
            String originalFilename = file.getOriginalFilename();
            String fileSuffix = StringTools.getFileSuffix(originalFilename);
            if (originalFilename.length() > Constants.LENGTH_200) {
                originalFilename = StringTools.getFileName(originalFilename).substring(0, Constants.LENGTH_190) + fileSuffix;
            }
            if (!ArrayUtils.contains(uploadTypeEnum.getSuffixArray(), fileSuffix)) {
                throw new BusinessException("文件类型不正确");
            }
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
            String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFileFolder = new File(baseFolder + "/" + folder + "/" + month + "/");
            String fileName = IdUtil.getSnowflakeNextIdStr() + fileSuffix;
            File targetFile = new File(targetFileFolder.getPath() + "/" + fileName);
            String localPath = month + "/" + fileName;

            // 头像特殊处理，直接放在avatar文件夹下
            if (uploadTypeEnum == FileUploadTypeEnum.AVATAR) {
                targetFileFolder = new File(baseFolder + "/" + Constants.FILE_FOLDER_AVATAR);
                targetFile = new File(targetFileFolder.getPath() + "/" + folder + ".jpg");
                localPath = folder + ".jpg";
            }
            if (!targetFileFolder.exists()) {
                targetFileFolder.mkdirs();
            }
            file.transferTo(targetFile);
            //  缩放评论区图片
            if (uploadTypeEnum == FileUploadTypeEnum.COMMENT_IMAGE) {
                // 缩放图名称最后加一个_
                String scaledImageName = targetFile.getName().replace(".", "_.");
                File scaledImageFile = new File(targetFile.getParent() + "/" + scaledImageName);
                ImgUtil.scale(targetFile, scaledImageFile, 200, 200, Color.WHITE);
            } else if (uploadTypeEnum == FileUploadTypeEnum.AVATAR || uploadTypeEnum == FileUploadTypeEnum.ARTICLE_COVER) {
                // 头像和文章封面直接缩放原图
                ImgUtil.scale(targetFile, targetFile, 200, 200, Color.WHITE);
            }
            uploadDto.setLocalPath(localPath);
            uploadDto.setOriginalFilename(originalFilename);
            return uploadDto;
        } catch (BusinessException e) {
            logger.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
}
