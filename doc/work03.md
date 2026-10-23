# work03

## 一、第三天工作概述

在前两天完成项目基础工程初始化、数据库库表设计以及 `horizonhub-common` 公共分层搭建之后，第三天的工作开始进一步向“可运行的核心业务能力”推进。

这一天的工作重点主要集中在两个方向：

1. 完成账号体系中的基础接口能力，包括图片验证码生成、邮箱验证码发送、账号注册等接口，以及对应的 Service 层与 Mapper 层实现。
2. 完成系统设置从数据库读取到内存的初始化机制，建立系统设置 DTO、枚举映射、缓存工具以及系统启动自动加载流程，为后续业务代码统一读取系统配置提供基础支撑。

从这一阶段开始，项目不再只是停留在“有库表、有分层”的基础状态，而是已经逐步具备了真实业务流程的雏形。尤其是在注册链路和系统配置运行时读取这两个方向上，已经形成了相对完整的闭环。

---

## 二、账号相关接口开发

当前账号相关接口位于：`horizonhub-web/src/main/java/com/nlizzard/horizonhub/controller/AccountController.java`

该控制器继承了公共层中的 `BaseController`，并通过注入 `EmailCodeService` 与 `UserInfoService`，完成注册前置校验与账号注册流程的组织。

目前已经完成了以下三个接口：

1. `checkCode`：生成图片验证码
2. `sendEmailCode`：发送邮箱验证码
3. `register`：注册账号

### 2.1 图片验证码接口 `checkCode`

接口路径：`/checkCode`

该接口用于生成图片验证码，并直接将验证码图片输出到响应流中。当前实现中通过 `CreateImageCode` 动态生成验证码图片，尺寸为
`130 x 38`，验证码长度为 `5` 位，同时带有干扰元素，用于增强基础防刷能力。

该接口的核心逻辑包括：

- 设置响应头，禁止浏览器缓存验证码图片。
- 设置响应内容类型为 `image/jpeg`。
- 根据 `type` 参数决定验证码写入到哪一个 Session Key 中。
- 通过 `response.getOutputStream()` 将验证码图片直接输出。

当前 `type` 的设计含义如下：

- `0` 或 `null`：用于登录/注册场景的图片验证码，写入 `Constants.CHECK_CODE_KEY`
- `1`：用于发送邮箱验证码场景的图片验证码，写入 `Constants.CHECK_CODE_KEY_EMAIL`

这部分实现的意义在于，将“注册校验验证码”和“发送邮箱验证码前的图片校验”做了区分，避免不同业务场景共用同一份 Session
数据，降低串用风险。

### 2.2 发送邮箱验证码接口 `sendEmailCode`

接口路径：`/sendEmailCode`

该接口用于在用户输入邮箱后，向目标邮箱发送邮箱验证码。它并不是直接无条件发送，而是先校验用户提交的图片验证码是否正确，只有通过图片验证码校验后，才会继续执行邮箱验证码发送流程。

接口接收的主要参数包括：

- `email`：目标邮箱地址
- `checkCode`：图片验证码内容
- `type`：验证码类型，当前约定 `0` 为注册验证码，`1` 为找回密码等其它邮箱验证码场景

该接口的处理流程如下：

1. 从 Session 中读取 `Constants.CHECK_CODE_KEY_EMAIL` 对应的图片验证码。
2. 调用 `equalsIgnoreCase` 进行不区分大小写校验。
3. 如果验证码不正确，则抛出 `BusinessException("图片验证码不正确")`。
4. 如果验证码正确，则调用 `emailCodeService.sendEmailCode(email, type)` 发送邮箱验证码。
5. 在 `finally` 代码块中移除 Session 中的图片验证码，保证该验证码只使用一次。

这里的“一次性移除”设计非常重要。它能够避免验证码重复复用，提高校验安全性，也符合注册前置校验的基本要求。

### 2.3 注册接口 `register`

接口路径：`/register`

该接口用于完成用户注册，是当前账号体系中最核心的一个接口。与发送邮箱验证码接口类似，注册接口在真正进入业务逻辑之前，也会先对图片验证码进行校验。

接口接收的主要参数包括：

- `email`：注册邮箱
- `nickName`：用户昵称
- `password`：登录密码
- `checkCode`：图片验证码
- `emailCode`：邮箱验证码

接口处理流程如下：

1. 从 Session 中读取 `Constants.CHECK_CODE_KEY` 对应的图片验证码。
2. 校验前端提交的图片验证码是否正确。
3. 如果不正确，则抛出业务异常。
4. 如果正确，则调用 `userInfoService.register(email, nickName, password, emailCode)` 执行注册逻辑。
5. 在 `finally` 中移除 Session 内保存的图片验证码，防止重复提交时复用旧验证码。

这一层控制器的职责划分比较清晰：

- Controller 负责参数接收、图片验证码校验、调用业务服务。
- Service 负责邮箱唯一性校验、邮箱验证码校验、用户落库、积分初始化、欢迎消息写入等业务处理。

这样的结构便于后续继续扩展登录、找回密码、重置密码等账号能力。

---

## 三、注册与验证码业务链路实现

第三天不仅完成了 `AccountController` 中的接口入口，还补齐了接口背后的 Service 层和 Mapper 层实现，使注册流程真正具备了可执行的业务闭环。

### 3.1 邮箱验证码业务实现 `EmailCodeServiceImpl`

实现类路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/service/impl/EmailCodeServiceImpl.java`

该类负责邮箱验证码的生成、发送、入库、失效和校验，是账号注册链路中的关键基础服务。

#### （1）发送邮箱验证码逻辑

对外暴露的方法为：`sendEmailCode(String toEmail, Integer type)`

核心处理流程如下：

1. 当 `type == 0` 时，先检查该邮箱是否已经在 `user_info` 表中存在。
2. 如果邮箱已存在，则直接抛出异常，避免重复注册。
3. 通过 `RandomStringUtils.random(Constants.EMAIL_CODE_LENGTH, true, true)` 生成随机验证码。
4. 如果当前配置允许真实发送邮件，则继续调用内部重载方法执行邮件发送。
5. 无论是否真实发信，都会在日志中打印邮箱和验证码，便于本地联调。
6. 调用 `emailCodeMapper.disableEmailCode(toEmail)` 将该邮箱历史未使用验证码全部置为失效。
7. 组装新的 `EmailCode` 对象并写入 `email_code` 表。

从实现上看，这里已经完成了验证码“先失效旧记录，再生成新记录”的处理方式，能够保证同一邮箱在同一时刻只有最新验证码有效。

#### （2）邮件发送内容与系统设置联动

在真正发送邮件时，业务代码会从系统设置缓存中读取邮件模板配置：

- `SysCacheUtils.getSysSetting().getEmailSetting().getEmailTitle()`
- `SysCacheUtils.getSysSetting().getEmailSetting().getEmailContent()`

这说明邮件标题和邮件正文模板已经不再直接硬编码在业务逻辑里，而是转为依赖系统设置表中的 `email`
配置项。这样后续如果需要修改邮件文案，只需要修改数据库配置或后台配置功能，而不需要重新改代码。

#### （3）邮箱验证码校验逻辑

校验方法为：`checkCode(String email, String code)`

校验过程主要包括：

1. 根据邮箱和验证码从 `email_code` 表中查询记录。
2. 如果查不到记录，则说明验证码错误，直接抛出异常。
3. 如果状态已经为已使用，或者超出 `Constants.EMAIL_CODE_EXPIRED_MINUTE` 指定的有效时间，则判定为已失效。
4. 校验通过后，将该邮箱的验证码统一失效，避免验证码再次被使用。

该实现已经覆盖了验证码校验中的两个关键问题：

- 验证码是否正确
- 验证码是否过期或已使用

这为后续注册、找回密码等涉及邮箱校验的场景提供了统一基础能力。

### 3.2 邮箱验证码 Mapper 实现

相关文件如下：

- `horizonhub-common/src/main/java/com/nlizzard/horizonhub/mappers/EmailCodeMapper.java`
- `horizonhub-common/src/main/resources/com/nlizzard/horizonhub/mappers/EmailCodeMapper.xml`

当前 Mapper 层除了继承公共 `BaseMapper<T, P>` 的通用增删改查能力外，还补充了邮箱验证码场景专用方法：

- `selectByEmailAndCode`：按邮箱和验证码精确查询
- `updateByEmailAndCode`：按联合主键更新
- `deleteByEmailAndCode`：按联合主键删除
- `disableEmailCode`：将某个邮箱下未使用的验证码批量置为已失效

其中 `disableEmailCode` 的 SQL 为：将指定邮箱下 `status = 0` 的记录统一更新为 `status = 1`。这正是注册与验证码链路中“验证码单次生效”的核心保证之一。

### 3.3 用户注册业务实现 `UserInfoServiceImpl`

实现类路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/service/impl/UserInfoServiceImpl.java`

用户注册业务通过 `register(String email, String nickName, String password, String emailCode)` 实现，并使用事务控制保证多个写操作的一致性。

注册流程主要包括以下步骤：

#### （1）注册前唯一性校验

- 先根据邮箱查询 `user_info` 表，校验邮箱是否已存在。
- 再根据昵称查询 `user_info` 表，校验昵称是否已被占用。

这一步保证了数据库层之外，业务层也能提前给出明确提示。

#### （2）邮箱验证码校验

调用 `emailCodeService.checkCode(email, emailCode)` 对邮箱验证码进行统一校验。

这里没有在注册方法里重复实现验证码校验逻辑，而是复用 `EmailCodeService` 中的能力，说明当前代码已经开始形成较好的职责复用关系。

#### （3）创建用户基础信息

用户信息写入前，系统完成了以下处理：

- 使用 Hutool 的雪花算法生成用户 ID
- 使用 `SecureUtil.md5(password)` 对密码进行 MD5 加密
- 设置注册时间 `joinTime`
- 设置账户状态为启用状态
- 初始化总积分与当前积分为 `0`

最终插入 `user_info` 表，完成账号创建。

#### （4）注册赠送积分

注册成功后，系统会调用：

`updateUserIntegral(userId, UserIntegralOperTypeEnum.REGISTER, UserIntegralChangeTypeEnum.ADD.getChangeType(), 5)`

这表示当前系统已经把“注册赠送积分”纳入注册链路，并同步写入积分记录表与用户积分字段。说明用户体系与积分体系已经开始产生联动。

#### （5）写入系统欢迎消息

在注册成功后，系统还会构造一条 `UserMessage` 系统消息写入消息表：

- 接收人为当前新注册用户
- 消息类型为系统消息
- 状态为未读
- 消息内容取自系统设置中的注册欢迎语

欢迎语来源为：

`SysCacheUtils.getSysSetting().getRegisterSetting().getRegisterWelcomeInfo()`

这一步说明系统设置缓存不仅应用在邮件模板中，也已经参与到注册后的业务消息生成过程中。

### 3.4 用户信息 Mapper 支撑

相关文件如下：

- `horizonhub-common/src/main/java/com/nlizzard/horizonhub/mappers/UserInfoMapper.java`
- `horizonhub-common/src/main/resources/com/nlizzard/horizonhub/mappers/UserInfoMapper.xml`

当前 `UserInfoMapper` 除了基础 CRUD 之外，还补充了以下业务能力：

- `selectByUserId`
- `selectByEmail`
- `selectByNickName`
- `updateIntegral`

其中：

- `selectByEmail` 支撑注册时的邮箱唯一性判断
- `selectByNickName` 支撑昵称唯一性判断
- `updateIntegral` 支撑注册后积分变更

这表明用户基础表已经不仅仅是简单的账号信息表，也开始承接账户状态、积分余额等用户中心基础能力。

---

## 四、系统设置从数据库加载到内存

除了账号注册链路之外，第三天另一项非常关键的工作，是完成系统设置的运行时加载机制。

这项工作的目标是：在系统启动后，将数据库中的系统设置读取出来，解析成 Java 对象后放入内存缓存，供业务层直接读取，避免每次都查询数据库。

### 4.1 系统设置表设计基础

数据库脚本位于：`sql/horizonhub.sql`

当前 `sys_setting` 表结构如下：

| 字段             | 类型             | 说明            |
|----------------|----------------|---------------|
| `code`         | `varchar(10)`  | 配置编码，主键       |
| `json_content` | `varchar(500)` | 配置内容，JSON 字符串 |

当前已初始化的系统配置项包括：

- `audit`：审核设置
- `comment`：评论设置
- `email`：邮件设置
- `like`：点赞设置
- `post`：发帖设置
- `register`：注册设置

其中与第三天工作直接相关的初始化值包括：

- `email`：包含邮箱标题和邮箱内容模板
- `register`：包含注册欢迎语

这为“邮箱验证码发送”和“注册成功系统消息”两个场景提供了配置来源。

### 4.2 系统设置聚合对象 `SysSettingDto`

文件路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/entity/dto/SysSettingDto.java`

`SysSettingDto` 是系统设置在内存中的聚合载体，当前已经包含以下属性：

- `auditSetting`
- `commentSetting`
- `postSetting`
- `likeSetting`
- `emailSetting`
- `registerSetting`

也就是说，系统启动完成后，会把数据库中多条配置记录最终组装成一个完整的 `SysSettingDto`
对象，并统一放入缓存中。业务代码后续只需要获取这一份对象，就可以读取各个系统配置分组。

### 4.3 各类系统设置 DTO

在 `horizonhub-common/src/main/java/com/nlizzard/horizonhub/entity/dto/` 目录下，当前已经定义了多种系统设置 DTO，例如：

- `SysSetting4AuditDto`
- `SysSetting4CommentDto`
- `SysSetting4PostDto`
- `SysSetting4LikeDto`
- `SysSetting4EmailDto`
- `SysSetting4RegisterDto`

其中与当前工作最直接相关的两个 DTO 如下。

#### （1）`SysSetting4EmailDto`

该 DTO 用于承接邮件相关配置，当前包含：

- `emailTitle`：邮件标题
- `emailContent`：邮件内容模板

在邮箱验证码发送时，业务代码会从该 DTO 中读取主题与正文内容，从而实现邮件文案配置化。

#### （2）`SysSetting4RegisterDto`

该 DTO 用于承接注册相关配置，当前包含：

- `registerWelcomeInfo`：注册欢迎语

在用户注册成功后，系统会从该 DTO 中读取欢迎语，并写入 `user_message` 表，作为用户收到的第一条系统消息。

### 4.4 系统设置枚举映射 `SysSettingCodeEnum`

文件路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/entity/enums/SysSettingCodeEnum.java`

为了让数据库中的 `code` 和 Java 中的 DTO 类型建立对应关系，当前项目定义了 `SysSettingCodeEnum`。

该枚举为每一种系统设置维护了以下信息：

- `code`：数据库中的配置编码
- `classZ`：该配置对应的 DTO 全限定类名
- `propName`：在 `SysSettingDto` 中对应的属性名
- `desc`：配置说明

例如：

- `EMAIL` 对应 `code = email`，映射到 `SysSetting4EmailDto`，写入 `emailSetting`
- `REGISTER` 对应 `code = register`，映射到 `SysSetting4RegisterDto`，写入 `registerSetting`

有了这层枚举映射之后，系统设置加载逻辑就不需要写大量硬编码判断，而是可以通过**统一反射机制**完成配置装配。

### 4.5 系统设置加载实现 `SysSettingServiceImpl`

实现类路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/service/impl/SysSettingServiceImpl.java`

该类中的 `initSysSettingToCache()` 方法，是系统设置读取到内存的核心实现。

整体处理流程如下：

1. 创建一个空的 `SysSettingDto` 聚合对象。
2. 查询 `sys_setting` 表中的全部配置记录。
3. 遍历每一条配置记录，读取其中的 `json_content`。
4. 根据 `code` 到 `SysSettingCodeEnum` 中找到对应配置项。
5. 通过枚举中维护的 DTO 类名，使用 `JsonUtils.json2Object` 将 JSON 内容反序列化成具体 DTO。
6. 根据枚举中的属性名，使用反射将 DTO 写入到 `SysSettingDto` 对应字段。
7. 最终调用 `SysCacheUtils.setSysSettingMap(sysSettingDto)` 写入内存缓存。

该实现的优点比较明显：

- 配置项之间解耦，不需要为每一种配置单独写一套读取逻辑。
- 新增系统设置时，只需要新增 DTO、枚举映射和数据库配置，即可接入整体机制。
- 业务代码读取配置时，只需要访问缓存工具类，不需要频繁查询数据库。

### 4.6 系统设置缓存工具 `SysCacheUtils`

文件路径：`horizonhub-common/src/main/java/com/nlizzard/horizonhub/utils/SysCacheUtils.java`

该工具类使用线程安全的 `ConcurrentHashMap` 存储系统设置对象，并对外暴露两个核心方法：

- `getSysSetting()`：获取当前缓存中的系统设置聚合对象
- `setSysSettingMap(SysSettingDto sysSettingDto)`：写入系统设置缓存

缓存的键使用 `Constants.SYS_SETTING_KEY` 统一管理，这样在项目其它位置读取系统配置时，不需要关心缓存细节，只需要直接调用工具类获取即可。

目前这份缓存已经被至少两个业务场景直接使用：

1. 邮箱验证码发送时读取邮件标题和正文模板
2. 用户注册成功后读取注册欢迎语

这说明系统设置缓存已经不是孤立存在，而是确实参与到了业务流程中。

---

## 五、系统启动自动加载系统设置

为了让系统设置在应用启动后立即可用，当前项目还补充了启动初始化逻辑。

相关文件位于：`horizonhub-web/src/main/java/com/nlizzard/horizonhub/InitRun.java`

该类实现了 `ApplicationRunner` 接口，并标注为 `@Component`，因此在 Spring Boot 应用启动完成后会自动执行 `run()` 方法。

当前 `run()` 方法中完成的核心动作只有一项：

- 调用 `sysSettingService.initSysSettingToCache()`，将数据库中的系统设置预加载到内存中

这个设计的意义非常明确：

1. 保证系统启动后，邮件模板、注册欢迎语等配置已经可直接读取。
2. 避免第一次调用业务接口时才懒加载配置，减少运行时分支复杂度。
3. 为后续更多依赖系统配置的业务场景打下基础。

从当前代码结构看，`InitRun` 已经承担起“系统启动预热”的职责，未来如果还需要在启动时加载板块缓存、敏感词库、权限配置等内容，也可以继续在这一层扩展。

---

## 六、与本次工作相关的数据库与业务联动

从现有实现来看，第三天的工作已经打通了多张表之间的业务协作关系。

### 6.1 `email_code` 表的作用

在发送邮箱验证码时，系统会将验证码落库到 `email_code` 表，并对同一邮箱旧验证码做失效处理。注册时再通过该表校验邮箱验证码是否正确、是否过期、是否已使用。

这张表已经真正参与到了账号注册闭环中，不再只是静态表结构。

### 6.2 `user_info` 表的作用

注册成功后，用户基础信息会写入 `user_info` 表，内容包括用户 ID、昵称、邮箱、密码、注册时间、状态以及积分初始化信息。

这意味着当前项目已经开始具备最基本的账号持久化能力。

### 6.3 `user_integral_record` 表的作用

用户注册成功后，系统会赠送初始积分，同时记录积分流水。由此可见，积分体系已经与注册行为完成了初步打通。

### 6.4 `user_message` 表的作用

新用户注册成功后，系统会向 `user_message` 表写入一条系统欢迎消息。该消息的内容来自系统设置中的注册欢迎语配置。

这说明消息体系、系统设置体系、注册体系三者之间已经形成联动。

---

## 七、结合仓库现状的其它代码变动总结

结合当前仓库代码结构以及现有 git 提交记录，除了第三天重点推进的账号接口与系统设置缓存能力之外，同一阶段还伴随着一批与本轮工作高度相关的基础代码完善。

### 7.1 公共基础控制层与异常处理能力继续完善

在 `horizonhub-common` 中，已经存在：

- `BaseController`
- `GlobalExceptionHandlerController`
- `ResponseVO`

这说明当前接口开发并不是零散堆砌的，而是在统一返回结构和统一异常处理机制之上进行的。`AccountController`
能够直接复用这些基础能力，有利于接口风格保持一致。

### 7.2 通用 Mapper、Query、Service 分层已经开始真正承接业务

从 git 记录可以看到，同阶段完成了大批基础类的新增，包括：

- 各业务表对应的 POJO
- 各业务查询对象 `Query`
- 各业务 `Mapper` 与 `Mapper.xml`
- 各业务 `Service` 与 `ServiceImpl`

这说明第二天完成的“基础分层搭建”，到了第三天已经开始进入“支撑真实业务”的阶段。当前注册流程、积分流程、系统消息流程、系统配置流程都已经落在这些公共分层之上运行。

### 7.3 系统配置不再停留在表结构层面，而是正式进入运行期使用阶段

在第二天的工作中，`sys_setting` 更多体现为数据库设计与 DTO 准备；而到了当前阶段，系统配置已经真正参与到业务运行：

- 邮件标题和邮件内容从缓存读取
- 注册欢迎语从缓存读取
- 系统启动时自动完成配置预热

这意味着项目已经从“准备系统配置”推进到“消费系统配置”。这是一个很关键的阶段变化。

### 7.4 `horizonhub-web` 模块开始承接真实前台业务入口

此前 `horizonhub-web` 主要还处于工程初始化状态，而当前已经新增并使用 `AccountController`、`InitRun`
等入口类，开始承担前台账号相关接口和启动初始化职责。

这也符合项目整体模块规划：

- `horizonhub-web` 负责前台业务接口
- `horizonhub-common` 提供共享业务能力和底层支撑

模块边界已经开始在实际代码中体现出来，而不仅仅停留在工程结构规划上。

---

## 八、阶段小结

第三天的工作可以理解为：在完成基础工程与公共分层之后，正式落地了“注册前置能力”和“系统配置运行时能力”两条重要主线。

这一阶段取得的核心成果包括：

1. 完成图片验证码接口，实现基础的人机校验能力。
2. 完成邮箱验证码发送接口，形成发送前图片校验、验证码生成、旧验证码失效、新验证码入库的完整链路。
3. 完成注册接口，实现邮箱唯一性校验、昵称唯一性校验、邮箱验证码校验、用户写库、积分初始化、欢迎消息写入等业务流程。
4. 完成系统设置 DTO、枚举映射、缓存工具和启动加载机制，使数据库中的系统配置能够在系统启动时自动进入内存。
5. 实现邮件模板与注册欢迎语的配置化读取，增强后续系统可维护性和可扩展性。

经过这一天的推进，`horizonhub` 已经不再只是一个完成了工程初始化和数据库设计的项目，而是开始具备论坛系统中最基础、最关键的一部分真实业务能力。

从当前基础来看，后续可以继续围绕以下方向推进：

- 登录与找回密码流程
- 用户登录态管理
- 板块与帖子前台接口
- 评论、点赞、消息提醒等业务链路
- 后台系统设置维护能力

这也意味着项目已经从“准备阶段”进入“业务能力持续落地阶段”。

