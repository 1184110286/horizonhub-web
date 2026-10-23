package com.nlizzard.horizonhub.utils;

import com.nlizzard.horizonhub.entity.enums.VerifyRegexEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * java 正则表达式工具类
 */
public class VerifyUtils {

    // 验证正则表达式
    public static boolean verify(String regex, String value) {
        if (value == null || StringUtils.isBlank(value)) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    // 验证正则表达式
    public static boolean verify(VerifyRegexEnum regex, String value) {
        return verify(regex.getRegex(), value);
    }
}
