package com.nlizzard.horizonhub.controller;

import cn.hutool.core.util.IdUtil;
import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.basecontroller.BaseFileController;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.WebConfig;
import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.entity.enums.UserOperFrequencyTypeEnum;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户侧文件接口。
 * <p>
 * 头像/图片读取等公共能力继承自 {@link BaseFileController}，本类只保留 web 端的图片上传。
 */
@RestController
@RequestMapping("/file")
public class FileController extends BaseFileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Resource
    private WebConfig webConfig;

    @Override
    protected String getProjectFolder() {
        return webConfig.getProjectFolder();
    }

    /**
     * 图片上传
     *
     * @param file 图片文件
     * @return 图片访问路径
     */
    @PostMapping("uploadImage")
    @GlobalInterceptor(checkLogin = true, frequencyType = UserOperFrequencyTypeEnum.IMAGE_UPLOAD)
    public ResponseVO<Map<String, String>> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        // 拿到文件名，后缀名
        String fileName = file.getOriginalFilename();
        String fileSuffixName = null;
        if (fileName != null) {
            fileSuffixName = fileName.substring(fileName.lastIndexOf("."));
        }
        // 判断是否是图片文件
        if (!ArrayUtils.contains(Constants.IMAGE_ALL_SUFFIX, fileSuffixName)) {
            throw new BusinessException("文件格式不支持");
        }
        // 保存文件
        String path = saveFile(file, fileSuffixName);
        Map<String, String> fileMap = new HashMap<>();
        fileMap.put("fileName", path);
        return getSuccessResponseVO(fileMap);
    }

    /**
     * 保存图片文件到服务器
     *
     * @param file           图片文件对象
     * @param fileSuffixName 图片文件后缀
     * @return 文件访问路径
     */
    private String saveFile(MultipartFile file, String fileSuffixName) {
        try {
            // 生成唯一图片名称
            String fileRealName = IdUtil.getSnowflakeNextIdStr() + fileSuffixName;
            // 先把图片存放到 temp 文件夹
            String folderPath = getProjectFolder() + Constants.FILE_FOLDER_FILE + File.separator + Constants.FILE_FOLDER_TEMP;
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if (!folder.mkdirs()) throw new BusinessException(ResponseCodeEnum.CODE_500);
            }
            File uploadFile = new File(folderPath + File.separator + fileRealName);
            // 保存文件
            file.transferTo(uploadFile);
            return Constants.FILE_FOLDER_TEMP + "/" + fileRealName;
        } catch (Exception e) {
            logger.error("上传文件失败", e);
            throw new BusinessException("上传文件失败");
        }
    }
}
