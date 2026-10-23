package com.nlizzard.horizonhub.utils;

import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

// 对象复制工具类
public class CopyTools {
    private static final Logger logger = LoggerFactory.getLogger(CopyTools.class);

    /**
     * 将一个列表复制成另一个列表
     *
     * @param sList
     * @param classz
     * @param <T>
     * @param <S>
     * @return
     */
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
            T t = null;
            try {
                t = classz.getConstructor().newInstance();
            } catch (Exception e) {
                logger.error("对象复制失败", e);
                throw new BusinessException(ResponseCodeEnum.CODE_500);
            }
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    /**
     * 将一个对象复制成另一个对象
     *
     * @param s
     * @param classz
     * @param <T>
     * @param <S>
     * @return
     */
    public static <T, S> T copy(S s, Class<T> classz) {
        T t = null;
        try {
            t = classz.getConstructor().newInstance();
        } catch (Exception e) {
            logger.error("对象复制失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
        BeanUtils.copyProperties(s, t);
        return t;
    }
}
