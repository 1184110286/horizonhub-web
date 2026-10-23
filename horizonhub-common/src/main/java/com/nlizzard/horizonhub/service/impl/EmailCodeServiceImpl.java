package com.nlizzard.horizonhub.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.WebConfig;
import com.nlizzard.horizonhub.entity.dto.SysSetting4EmailDto;
import com.nlizzard.horizonhub.entity.enums.PageSize;
import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.entity.pojo.EmailCode;
import com.nlizzard.horizonhub.entity.pojo.UserInfo;
import com.nlizzard.horizonhub.entity.query.EmailCodeQuery;
import com.nlizzard.horizonhub.entity.query.UserInfoQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.EmailCodeMapper;
import com.nlizzard.horizonhub.mappers.UserInfoMapper;
import com.nlizzard.horizonhub.service.EmailCodeService;
import com.nlizzard.horizonhub.utils.SysCacheUtils;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description:邮箱验证码ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("emailCodeService")
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private WebConfig webConfig;

    @Resource
    private SysCacheUtils sysCacheUtils;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<EmailCode> findListByParam(EmailCodeQuery query) {
        return this.emailCodeMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(EmailCodeQuery query) {
        return this.emailCodeMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<EmailCode> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(EmailCode bean) {
        return this.emailCodeMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.emailCodeMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.emailCodeMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据EmailAndCode查询
     */
    @Override
    public EmailCode getEmailCodeByEmailAndCode(String email, String code) {
        return this.emailCodeMapper.selectByEmailAndCode(email, code);
    }

    /**
     * 根据EmailAndCode更新
     */
    @Override
    public void updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code) {
        this.emailCodeMapper.updateByEmailAndCode(bean, email, code);
    }

    /**
     * 根据EmailAndCode删除
     */
    @Override
    public void deleteEmailCodeByEmailAndCode(String email, String code) {
        this.emailCodeMapper.deleteByEmailAndCode(email, code);
    }

    /**
     * 发送邮件验证码(重载方法)
     *
     * @param toEmail
     * @param code
     */
    private void sendEmailCode(String toEmail, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            //邮件发件人
            helper.setFrom(webConfig.getSendUserName());
            //邮件收件人
            helper.setTo(toEmail);
            SysSetting4EmailDto emailDto = sysCacheUtils.getSysSetting().getEmailSetting();
            //邮件主题
            helper.setSubject(emailDto.getEmailTitle());
            //邮件内容
            helper.setText(emailDto.getEmailContent().replace("${code}", code), true);
            //邮件发送时间
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("邮件发送失败", e);
            throw new BusinessException("邮件发送失败");
        }
    }

    /**
     * 发送邮箱验证码
     *
     * @param toEmail 邮箱地址
     * @param type    0-发送注册验证码，1-邮箱验证码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String toEmail, Integer type) {
        //如果是注册，校验邮箱是否已存在
        if (type == 0) {
            UserInfo userInfo = userInfoMapper.selectByEmail(toEmail);
            if (null != userInfo) {
                throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "邮箱已经存在");
            }
        }
        // 生成随机验证码
        String code = RandomUtil.randomString(Constants.EMAIL_CODE_LENGTH);
        // 是否使用邮箱发送邮件
        if (webConfig.getIsSendEmailCode() != null && webConfig.getIsSendEmailCode()) {
            // 调用重载方法发送验证码
            sendEmailCode(toEmail, code);
        }
        // 打印在控制台
        logger.info("发送邮箱验证码成功，邮箱：{}，验证码：{}", toEmail, code);
        // 使旧的验证码全部失效
        emailCodeMapper.disableEmailCode(toEmail);
        // 创建新的验证码记录
        EmailCode emailCode = new EmailCode();
        emailCode.setCode(code);
        emailCode.setEmail(toEmail);
        emailCode.setStatus(0);
        emailCode.setCreateTime(new Date());
        emailCodeMapper.insert(emailCode);
    }

    /**
     * 校验邮箱验证码
     *
     * @param email
     * @param code
     */
    @Override
    public void checkCode(String email, String code) {
        EmailCode emailCode = emailCodeMapper.selectByEmailAndCode(email, code);
        if (null == emailCode) {
            throw new BusinessException("邮箱验证码不正确");
        }
        if (emailCode.getStatus() == 1 || System.currentTimeMillis() - emailCode.getCreateTime().getTime() > Constants.EMAIL_CODE_EXPIRED_MINUTE * 1000 * 60) {
            throw new BusinessException("邮箱验证码已失效");
        }
        emailCodeMapper.disableEmailCode(email);
    }
}