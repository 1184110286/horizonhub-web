package com.nlizzard.horizonhub.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DateUtils {

    // 用于同步的对象
    private static final Object lockObj = new Object();
    // 存储线程局部变量的Map，用于保存不同日期格式对应的SimpleDateFormat对象
    private static Map<String, ThreadLocal<SimpleDateFormat>> sdfMap = new HashMap<>();

    // 根据日期格式获取对应的SimpleDateFormat对象
    private static SimpleDateFormat getSdf(final String pattern) {
        // 从sdfMap中获取对应格式的ThreadLocal对象
        ThreadLocal<SimpleDateFormat> t1 = sdfMap.get(pattern);
        if (t1 == null) {
            // 如果ThreadLocal对象不存在，则进入同步块
            synchronized (lockObj) {
                // 再次检查ThreadLocal对象是否存在，由于在多线程环境下可能有多个线程同时通过第一次检查
                t1 = sdfMap.get(pattern);
                if (t1 == null) {
                    // 创建一个新的ThreadLocal对象，并将其初始化为一个SimpleDateFormat对象
                    t1 = new ThreadLocal<SimpleDateFormat>() {
                        @Override
                        protected SimpleDateFormat initialValue() {
                            return new SimpleDateFormat(pattern);
                        }
                    };
                    // 将ThreadLocal对象存储在sdfMap中
                    sdfMap.put(pattern, t1);
                }
            }
        }
        return t1.get();
    }

    // 将给定的日期按照指定的格式进行格式化，并返回格式化后的日期字符串
    public static String format(Date date, String pattern) {
        return getSdf(pattern).format(date);
    }

    // 将给定的日期字符串按照指定的格式进行解析，并返回解析后得到的日期对象
    public static Date parse(String dateStr, String pattern) {
        try {
            return getSdf(pattern).parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
