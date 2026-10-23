package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.pojo.EmailCode;
import com.nlizzard.horizonhub.entity.query.EmailCodeQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:邮箱验证码Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface EmailCodeService {

    /**
     * 根据条件查询列表
     */
    List<EmailCode> findListByParam(EmailCodeQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(EmailCodeQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery query);

    /**
     * 新增
     */
    Integer add(EmailCode bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<EmailCode> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<EmailCode> listBean);

    /**
     * 根据EmailAndCode查询
     */
    EmailCode getEmailCodeByEmailAndCode(String email, String code);

    /**
     * 根据EmailAndCode更新
     */
    void updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code);

    /**
     * 根据EmailAndCode删除
     */
    void deleteEmailCodeByEmailAndCode(String email, String code);

    /**
     * 发送邮箱验证码
     *
     * @param toEmail 接收验证码的邮箱地址
     * @param type    验证码类型（0：注册验证码，1：找回密码验证码），根据业务需求定义不同的类型
     */
    void sendEmailCode(String toEmail, Integer type);

    void checkCode(String email, String code);
}