package com.nlizzard.horizonhub.service.impl;

import cn.hutool.core.util.IdUtil;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.WebConfig;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.enums.*;
import com.nlizzard.horizonhub.entity.pojo.*;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.ForumCommentQuery;
import com.nlizzard.horizonhub.entity.query.UserInfoQuery;
import com.nlizzard.horizonhub.entity.query.UserIntegralRecordQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.ForumArticleMapper;
import com.nlizzard.horizonhub.mappers.ForumCommentMapper;
import com.nlizzard.horizonhub.mappers.UserInfoMapper;
import com.nlizzard.horizonhub.mappers.UserIntegralRecordMapper;
import com.nlizzard.horizonhub.service.EmailCodeService;
import com.nlizzard.horizonhub.service.UserInfoService;
import com.nlizzard.horizonhub.service.UserMessageService;
import com.nlizzard.horizonhub.utils.FileUtils;
import com.nlizzard.horizonhub.utils.JsonUtils;
import com.nlizzard.horizonhub.utils.OKHttpUtils;
import com.nlizzard.horizonhub.utils.PasswordUtils;
import com.nlizzard.horizonhub.utils.SysCacheUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:用户信息ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    public static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private PasswordUtils passwordUtils;

    @Resource
    private UserIntegralRecordMapper<UserIntegralRecord, UserIntegralRecordQuery> userIntegralRecordMapper;

    @Resource
    private UserMessageService userMessageService;

    @Resource
    private WebConfig webConfig;

    @Resource
    private FileUtils fileUtils;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private ForumCommentMapper<ForumComment, ForumCommentQuery> forumCommentMapper;

    @Resource
    private SysCacheUtils sysCacheUtils;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserInfo> findListByParam(UserInfoQuery query) {
        return this.userInfoMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(UserInfoQuery query) {
        return this.userInfoMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserInfo bean) {
        return this.userInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据UserId查询
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        return this.userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据UserId更新
     */
    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * 根据UserId删除
     */
    @Override
    public Integer deleteUserInfoByUserId(String userId) {
        return this.userInfoMapper.deleteByUserId(userId);
    }

    /**
     * 根据Email查询
     */
    @Override
    public UserInfo getUserInfoByEmail(String email) {
        return this.userInfoMapper.selectByEmail(email);
    }

    /**
     * 根据Email更新
     */
    @Override
    public Integer updateUserInfoByEmail(UserInfo bean, String email) {
        return this.userInfoMapper.updateByEmail(bean, email);
    }

    /**
     * 根据Email删除
     */
    @Override
    public Integer deleteUserInfoByEmail(String email) {
        return this.userInfoMapper.deleteByEmail(email);
    }

    /**
     * 根据NickName查询
     */
    @Override
    public UserInfo getUserInfoByNickName(String nickName) {
        return this.userInfoMapper.selectByNickName(nickName);
    }

    /**
     * 根据NickName更新
     */
    @Override
    public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
        return this.userInfoMapper.updateByNickName(bean, nickName);
    }

    /**
     * 根据NickName删除
     */
    @Override
    public Integer deleteUserInfoByNickName(String nickName) {
        return this.userInfoMapper.deleteByNickName(nickName);
    }

    /**
     * 注册账号接口
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password, String emailCode) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null != userInfo) {
            throw new BusinessException("邮箱账号已经存在");
        }
        UserInfo nickNameUser = this.userInfoMapper.selectByNickName(nickName);
        if (null != nickNameUser) {
            throw new BusinessException("昵称已经存在");
        }

        //校验邮箱验证码，如果不正确或者过期会抛出异常，成功则失效验证码
        emailCodeService.checkCode(email, emailCode);

        // 雪花算法生成用户 ID
        String userId = IdUtil.getSnowflakeNextIdStr();
        userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setNickName(nickName);
        userInfo.setEmail(email);
        userInfo.setPassword(passwordUtils.encode(password));
        userInfo.setJoinTime(new Date());
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setTotalIntegral(0);
        userInfo.setCurrentIntegral(0);
        this.userInfoMapper.insert(userInfo);

        // 注册时赠送积分
        updateUserIntegral(userId, UserIntegralOperTypeEnum.REGISTER, UserIntegralChangeTypeEnum.ADD.getChangeType(), Constants.REGISTER_GIFT_INTEGRAL);


        // 记录系统消息
        UserMessage userMessage = new UserMessage();
        userMessage.setReceivedUserId(userId);
        userMessage.setMessageType(MessageTypeEnum.SYS.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        // 系统设置中读取注册欢迎信息
        userMessage.setMessageContent(sysCacheUtils.getSysSetting().getRegisterSetting().getRegisterWelcomeInfo());
        userMessageService.add(userMessage);

    }

    /**
     * 更新用户积分
     *
     * @param userId       用户ID
     * @param operTypeEnum 操作类型枚举
     * @param changeType   增加或减少类型，1-增加，-1-减少
     * @param integral     变更的积分数值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserIntegral(String userId, UserIntegralOperTypeEnum operTypeEnum, Integer changeType, Integer integral) {
        integral = changeType * integral;
        if (integral == 0) {
            return;
        }

        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        // 如果是减少积分，且当前积分不足以扣除，则将变更的积分数值调整为当前积分的相反数，确保用户积分不会变成负数
        if (UserIntegralChangeTypeEnum.REDUCE.getChangeType().equals(changeType) && userInfo.getCurrentIntegral() + integral < 0) {
            integral = changeType * userInfo.getCurrentIntegral();
        }

        // 新增积分变更记录
        UserIntegralRecord record = new UserIntegralRecord();
        record.setUserId(userId);
        record.setOperType(operTypeEnum.getOperType());
        record.setCreateTime(new Date());
        record.setIntegral(integral);
        userIntegralRecordMapper.insert(record);

        // 更新用户积分
        Integer count = this.userInfoMapper.updateIntegral(userId, integral);
        if (count == 0) {
            throw new BusinessException("更新用户积分失败");
        }
    }

    /**
     * 登录接口
     *
     * @param email     邮箱
     * @param password  密码
     * @param ipAddress 登录 IP 地址
     */
    @Override
    public SessionWebUserDto login(String email, String password, String ipAddress) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo || !passwordUtils.matches(password, userInfo.getPassword())) {
            throw new BusinessException("账号或者密码错误");
        }
        // 惰性升级：旧版无盐 MD5 校验通过后，重新加密为 BCrypt
        if (passwordUtils.isLegacyMd5(userInfo.getPassword())) {
            UserInfo upgrade = new UserInfo();
            upgrade.setPassword(passwordUtils.encode(password));
            this.userInfoMapper.updateByUserId(upgrade, userInfo.getUserId());
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已禁用");
        }
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        updateInfo.setLastLoginIp(ipAddress);
        // 根据登录 IP 地址获取所在地信息，更新最后登录所在地字段
        Map<String, String> addressInfo = getIpAddress(ipAddress);
        String province = addressInfo.get("pro");
        province = StringUtils.isBlank(province) ? Constants.IP_PROVINCE_DEFAULT : province;
        updateInfo.setLastLoginIpAddress(province);
        // 更新用户信息（最后登录时间，最后登录ip，最后登录省份）
        this.userInfoMapper.updateByUserId(updateInfo, userInfo.getUserId());
        // 去除掉敏感字段，将用户信息转换成 SessionWebUserDto，存入 session 中
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setNickName(userInfo.getNickName());
        sessionWebUserDto.setProvince(province);
        sessionWebUserDto.setUserId(userInfo.getUserId());
        // 判断是否管理员，管理员邮箱在系统设置中配置，多个用逗号分隔
        sessionWebUserDto.setAdmin(!StringUtils.isBlank(webConfig.getAdminEmails()) && ArrayUtils.contains(webConfig.getAdminEmails().split(","), userInfo.getEmail()));
        return sessionWebUserDto;
    }

    // 根据 IP 地址获取所在地信息
    public Map<String, String> getIpAddress(String ip) {
        Map<String, String> addressInfo = new HashMap<>();
        try {
            String url = webConfig.getIpAddressUrl() + ip;
            String responseJson = OKHttpUtils.getRequest(url);
            if (responseJson == null || StringUtils.isBlank(responseJson)) {
                return addressInfo;
            }
            addressInfo = JsonUtils.json2Object(responseJson, Map.class);
            return addressInfo;
        } catch (Exception e) {
            logger.error("获取ip所在地失败");
        }
        return addressInfo;
    }

    /**
     * 重置密码接口
     *
     * @param email     邮箱
     * @param password  新密码
     * @param emailCode 邮箱验证码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(String email, String password, String emailCode) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo) {
            throw new BusinessException("邮箱账号不存在");
        }
        //校验邮箱验证码
        emailCodeService.checkCode(email, emailCode);

        UserInfo updateInfo = new UserInfo();
        updateInfo.setPassword(passwordUtils.encode(password));
        this.userInfoMapper.updateByEmail(updateInfo, email);
    }

    /**
     * 更新用户信息接口
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserInfo userInfo, MultipartFile avatar) {
        userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());
        if (avatar != null) {
            fileUtils.uploadFile2Local(avatar, FileUploadTypeEnum.AVATAR, userInfo.getUserId());
        }
    }

    /**
     * 更新用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Integer status, String userId) {
        UserStatusEnum userStatusEnum = UserStatusEnum.getUserStatusEnumByStatus(status);
        if (userStatusEnum == null) {
            throw new BusinessException("状态参数错误");
        }

        if (UserStatusEnum.DISABLE.getStatus().equals(status)) {
            // 删除用户全部文章和评论
            forumArticleMapper.updateStatusBatchByUserId(status, userId);
            forumCommentMapper.updateStatusBatchByUserId(status, userId);

            //同时需要更新文章的评论数
            ForumCommentQuery forumCommentQuery = new ForumCommentQuery();
            forumCommentQuery.setStatus(1);
            forumCommentQuery.setPCommentId(0);
            forumCommentQuery.setUserId(userId);
            List<ForumComment> forumComments = forumCommentMapper.selectList(forumCommentQuery);
            if (!forumComments.isEmpty()) {
                // 获得文章ID列表
                List<String> idList = forumComments.stream().map(ForumComment::getArticleId).toList();
                // 更新文章评论数
                forumArticleMapper.resetCommentCount(idList);
            }
        }
        userInfoMapper.updateUserStatusById(status, userId);
    }

    /**
     * 发送消息
     *
     * @param userId   接收消息的用户ID
     * @param message  消息内容
     * @param integral 积分
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(String userId, String message, Integer integral) {
        UserMessage userMessage = new UserMessage();
        userMessage.setReceivedUserId(userId);
        userMessage.setMessageType(MessageTypeEnum.SYS.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessage.setMessageContent(message);
        userMessageService.add(userMessage);

        UserIntegralChangeTypeEnum changeTypeEnum = UserIntegralChangeTypeEnum.ADD;
        if (integral != null && integral != 0) {
            //扣减积分传入正数
            if (integral < 0) {
                integral = integral * -1;
                changeTypeEnum = UserIntegralChangeTypeEnum.REDUCE;
            }
            updateUserIntegral(userId, UserIntegralOperTypeEnum.ADMIN, changeTypeEnum.getChangeType(), integral);
        }
    }
}