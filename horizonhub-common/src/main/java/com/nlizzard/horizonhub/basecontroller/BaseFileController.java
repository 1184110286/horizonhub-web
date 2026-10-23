package com.nlizzard.horizonhub.basecontroller;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.constants.Constants;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 文件访问公共基类（web 与 admin 共用）。
 * <p>
 * 头像读取、图片读取、无默认图提示等逻辑统一在此，子类只需提供项目根目录
 * （{@link #getProjectFolder()}）。web 端额外保留图片上传能力。
 */
public abstract class BaseFileController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BaseFileController.class);

    /**
     * 子类提供项目根目录（web 取自 {@code WebConfig}，admin 取自 {@code AdminConfig}，
     * 二者均继承自 {@code AppConfig}）。
     */
    protected abstract String getProjectFolder();

    /**
     * 获取用户头像
     *
     * @param userId 用户 ID
     */
    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true)
    public void getAvatar(HttpServletResponse response,
                          @PathVariable @VerifyParam(required = true) String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + File.separator + Constants.FILE_FOLDER_AVATAR;
        String avatarPath = getProjectFolder() + avatarFolderName + File.separator + userId + ".jpg";
        File file = new File(avatarPath);
        String imageFolder = Constants.FILE_FOLDER_AVATAR;
        String imageName = userId + ".jpg";
        if (!file.exists()) {
            imageName = "default_avatar.jpg";
            if (!new File(getProjectFolder() + avatarFolderName + File.separator + imageName).exists()) {
                printNoDefaultImage(response);
                return;
            }
        }
        readImage(response, imageFolder, imageName);
    }

    /**
     * 获取图片
     *
     * @param imageFolder 图片文件夹
     * @param imageName   图片名称
     */
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response,
                         @PathVariable String imageFolder,
                         @PathVariable String imageName) {
        readImage(response, imageFolder, imageName);
    }

    /**
     * 输出无默认图提示
     */
    protected void printNoDefaultImage(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.OK.value());

        try (PrintWriter writer = response.getWriter()) {
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        }
    }

    /**
     * 读取图片并写入响应
     *
     * @param response    HttpServletResponse 响应对象
     * @param imageFolder 图片文件夹
     * @param imageName   图片名称
     */
    protected void readImage(HttpServletResponse response, String imageFolder, String imageName) {
        ServletOutputStream sos = null;
        FileInputStream in = null;
        try {
            if (StringUtils.isBlank(imageFolder) || StringUtils.isBlank(imageName)) {
                return;
            }
            String imageSuffix = imageName.substring(imageName.lastIndexOf("."));
            String filePath = getProjectFolder() + Constants.FILE_FOLDER_FILE + File.separator + Constants.FILE_FOLDER_IMAGE + File.separator + imageFolder + File.separator + imageName;
            if (Constants.FILE_FOLDER_TEMP.equals(imageFolder)) {
                filePath = getProjectFolder() + Constants.FILE_FOLDER_FILE + File.separator + imageFolder + File.separator + imageName;
            } else if (Constants.FILE_FOLDER_AVATAR.equals(imageFolder)) {
                filePath = getProjectFolder() + Constants.FILE_FOLDER_FILE + File.separator + imageFolder + File.separator + imageName;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            imageSuffix = imageSuffix.replace(".", "");
            // 头像文件夹下的图片不设置缓存，其他图片设置1天缓存
            if (!Constants.FILE_FOLDER_AVATAR.equals(imageFolder)) {
                response.setHeader("Cache-Control", "max-age=86400");
            }
            response.setContentType("image/" + imageSuffix);
            in = new FileInputStream(file);
            sos = response.getOutputStream();
            byte[] bytes = new byte[8192];
            int len;
            while ((len = in.read(bytes)) != -1) {
                sos.write(bytes, 0, len);
            }
        } catch (Exception e) {
            logger.error("读取图片异常", e);
        } finally {
            if (sos != null) {
                try {
                    sos.close();
                } catch (IOException e) {
                    logger.error("readImage 方法关闭 ServletOutputStream 流报错", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("readImage 方法关闭 FileInputStream 流报错", e);
                }
            }
        }
    }
}
