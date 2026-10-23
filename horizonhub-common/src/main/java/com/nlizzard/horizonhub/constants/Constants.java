package com.nlizzard.horizonhub.constants;

public class Constants {

    public static final Integer LENGTH_200 = 200;
    public static final Integer LENGTH_190 = 190;
    public static final Integer LENGTH_15 = 15;

    // 注册赠送积分
    public static final Integer REGISTER_GIFT_INTEGRAL = 5;
    // 邮箱验证码长度
    public static final Integer EMAIL_CODE_LENGTH = 5;
    // 邮箱验证码过期时间，单位：分钟
    public static final Integer EMAIL_CODE_EXPIRED_MINUTE = 15;
    // session 中登录注册的图片验证码的key
    public static final String CHECK_CODE_KEY = "check_code";
    // session 中发送邮箱验证码的图片验证码的key
    public static final String CHECK_CODE_KEY_EMAIL = "check_code_email";
    // 用户 ID 长度
    public static final Integer USER_ID_LENGTH = 19;
    // redis中系统设置的key
    public static final String SYS_SETTING_KEY = "horizonHub:sys_setting";
    // redis中系统设置的过期时间(分钟)
    public static final Integer SYS_SETTING_KEY_EXPIRE = 10;
    // session 中用户信息的 key
    public static final String SESSION_KEY = "session_key";
    // ip 省份默认未知
    public static final String IP_PROVINCE_DEFAULT = "未知";
    // 论坛文件存放位置的文件夹路径前缀
    public static final String FILE_FOLDER_FILE = "/file";
    // 论坛文件临时存放路径前缀
    public static final String FILE_FOLDER_TEMP = "temp";
    // 论坛用户头像存放位置的文件夹路径前缀
    public static final String FILE_FOLDER_AVATAR = "avatar";
    // 论坛文章附件存放位置的文件夹路径前缀
    public static final String FILE_FOLDER_ATTACHMENT = "attachment";
    // 图片存放位置的文件夹路径前缀
    public static final String FILE_FOLDER_IMAGE = "images";
    // 图片文件后缀列表
    public static final String[] IMAGE_ALL_SUFFIX = new String[]{".png", ".PNG", ".jpg", ".JPG", ".jpeg", ".JPEG", ".gif", ".GIF", ".bmp", ".BMP"};
    // 文章附件文件后缀列表
    public static final String[] ATTACHMENT_ALL_SUFFIX = new String[]{".zip", ".ZIP", ".rar", ".RAR"};
    // temp图片访问地址前缀
    public static final String READ_IMAGE_PATH = "/api/file/getImage/";
    // 1M 文件大小，单位：字节
    public static final Integer FILE_SIZE_1M = 1024 * 1024;
    // 成功响应的状态信息
    public static final String STATUS_SUCCESS = "success";
    // 频率计数的前缀key
    public static final String FREQUENCY_KEY = "horizonHub:frequency_key";
    // Token 登录态在 Redis 中的 key 前缀，完整 key 为 horizonHub:token:{tokenId}
    public static final String TOKEN_REDIS_KEY_PREFIX = "horizonHub:token:";
    // 请求头中的认证 scheme：Authorization: Bearer <token>
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
}
