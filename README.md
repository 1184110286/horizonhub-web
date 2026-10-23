# HorizonHub - 前后端分离论坛项目

[![License](https://img.shields.io/badge/License-MIT%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.12-green.svg)](https://spring.io/projects/spring-boot)

HorizonHub 是一个基于 Java 21 和 Spring Boot 3 的论坛系统，包含用户侧接口、管理侧接口、公共业务模块，以及一个基于 Spring AI 的 **AI 助手**。前端工程位于 `horizonhub-front` 目录下（web 管理台 + 用户台）。

## 项目说明

- 当前仓库：`https://github.com/nlizzard/horizonhub`
- 前端工程已合并到当前仓库的 `horizonhub-front` 目录下，未来不再单独维护前端仓库
- `horizonhub-web` 和 `horizonhub-admin` 是后端服务模块（用户侧 / 管理侧），`horizonhub-ai` 是 AI 助手模块（作为 library 被用户侧引入）

## 技术栈

| 分类 | 技术 | 说明 |
| --- | --- | --- |
| 语言与构建 | Java 21、Maven | 多模块 Maven 工程 |
| 核心框架 | Spring Boot 3.5.12、Spring MVC、Spring AOP | 接口开发、拦截、启动管理 |
| 数据访问 | MyBatis 3.0.5、MySQL、Druid 1.2.27 | Mapper + XML，数据库连接池 |
| 缓存 | Spring Data Redis | 系统配置缓存、操作频率控制、**Token 登录态** |
| 认证 | HttpSession + **JWT/Redis 有状态 Token（双轨）** | 前端走 Session，AI / 第三方走 Token；密码用 BCrypt |
| AI 能力 | **Spring AI 1.0.9**（OpenAI 兼容端点） | 悬浮 AI 助手，检索增强（RAG）+ SSE 流式对话 |
| 通信与工具 | OkHttp 4.12.0、Jackson 2.18.6、Hutool 5.8.43、Apache Commons | HTTP 调用、JSON、常用工具 |
| 安全与校验 | spring-security-crypto、jjwt、jsoup、spring-boot-starter-validation | 密码 BCrypt、JWT 签名、富文本 XSS 净化、JSR-303 参数校验 |
| 邮件与日志 | Spring Boot Mail、Logback 1.5.25 | 邮箱验证码、日志输出 |
| 前端 | Vue 3、Element Plus、Vue Router、Vuex、Vite | 用户台 + 管理台 |

## 功能点

### 用户侧 API（`horizonhub-web`）

- 图形验证码、邮箱验证码、注册、登录、退出、找回密码
- 板块树加载与发帖板块选择
- 文章列表、文章详情、关键字搜索
- 发帖与编辑，支持富文本和 Markdown 两种编辑模式
- 封面图、正文图片、附件上传与下载
- 文章点赞、评论点赞
- 评论列表、回复评论、评论图片上传
- 用户资料修改、头像上传
- 我的文章、积分流水、消息中心
- 附件积分下载与下载通知

### 管理侧 API（`horizonhub-admin`）

- 管理员验证码登录
- 用户列表查询、启用/禁用、发送站内消息、积分发放
- 文章列表、删除、审核、置顶、调整所属板块
- 评论列表、评论审核、评论删除
- 板块树管理、板块新增/编辑/删除、排序调整
- 系统设置读取与保存
- 管理侧文件访问接口

### AI 助手（`horizonhub-ai`）

- 前端右下角悬浮客服「小H」，点击进入对话面板
- 基于论坛真实数据回答问题（检索增强：每轮预取板块树 + 热门/相关帖拼入 prompt）
- 帮用户寻找帖子、介绍板块、引导发帖
- SSE 流式输出（逐字打字效果）
- 自动检测登录态，游客可用、登录体验更佳
- 可对接任意 OpenAI 兼容大模型（DeepSeek / 通义千问 / Kimi 等）

### 公共能力（`horizonhub-common`）

- 实体、DTO、VO、Query、Mapper、Service 的公共实现
- `@GlobalInterceptor` + `@VerifyParam` 参数校验与统一拦截（JSR-303 并存）
- 双轨登录态：基于 Session（前端）与基于 Token（AI / 第三方）
- Redis 缓存系统配置，并在服务启动时预热
- 基于 Redis Lua 的操作频率限制
- 用户积分、消息通知、审核状态流转
- 本地文件存储与图片/附件访问封装
- 富文本正文服务端 XSS 净化

## 工程结构

```text
horizonhub/
├── horizonhub-common/    # 公共模块：entity、service、mapper、utils、config、切面、全局异常
├── horizonhub-web/       # 用户侧后端 API（引入 horizonhub-ai，端点随 web 启动加载）
├── horizonhub-admin/     # 管理侧后端 API
├── horizonhub-ai/        # AI 助手模块（library：Spring AI 对话 + 检索增强 + SSE）
├── horizonhub-front/     # 前端工程（front-web 用户台 + front-admin 管理台）
├── sql/                  # 数据库脚本
├── doc/                  # 过程文档 / 阶段记录 / 安全架构加固记录
└── pom.xml               # Maven 父工程
```

## 运行环境

- JDK 21+
- Maven 3.9+
- MySQL 8.x
- Redis 6.x 或以上
- （可选）任意 OpenAI 兼容大模型的 API Key，用于启用 AI 助手

## 快速开始（本地开发）

### 1. 克隆仓库

```bash
git clone https://github.com/nlizzard/horizonhub.git
cd horizonhub
```

### 2. 创建数据库并导入脚本

应用配置默认连接的数据库名是 `horizonhub`，请先创建数据库，再导入 [sql/horizonhub.sql](sql/horizonhub.sql)。

```sql
CREATE DATABASE horizonhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p horizonhub < sql/horizonhub.sql
```

### 3. 配置（二选一）

本地开发默认使用 `dev` profile，开箱即用——`application.yml` 内所有敏感项都带 `${ENV:默认值}` 的默认值，无需配置环境变量即可启动。

如果你想覆盖默认值，推荐用本地覆盖文件（已被 `.gitignore` 忽略，不会提交）：

- `horizonhub-web/src/main/resources/application-local.yml`
- `horizonhub-admin/src/main/resources/application-local.yml`

示例（仅覆盖你想改的项）：

```yaml
spring:
  datasource:
    password: 你的数据库密码
  data:
    redis:
      host: 127.0.0.1
  ai:
    openai:
      api-key: sk-你的DeepSeek-key   # 注入后启用 AI 助手
projectFolder: D:/your/path/horizonhub
```

### 4. 构建项目

```bash
mvn clean install
```

### 5. 启动服务

```bash
# 用户侧 API（含 AI 助手端点）
mvn -pl horizonhub-web -am spring-boot:run

# 管理侧 API
mvn -pl horizonhub-admin -am spring-boot:run
```

### 6. 启动前端（可选）

```bash
cd horizonhub-front/horizonhub-front-web
npm install
npm run dev      # 用户台，默认 http://localhost:5173
```

```bash
cd horizonhub-front/horizonhub-front-admin
npm install
npm run dev      # 管理台
```

### 7. 默认访问地址

- 用户侧 API：`http://localhost:8080/api`
- 管理侧 API：`http://localhost:8081/api`
- 用户台前端：`http://localhost:5173`

## 必要配置项

### 环境变量 / 配置项速查

| 配置项 | 说明 | dev 默认值 | prod 是否必填 |
| --- | --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库地址、端口、库名 | localhost / 3306 / horizonhub | 是（仅 host/name） |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号密码 | root / 123456 | 是 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | 192.168.123.2 / 6379 / 空 | 是（含密码） |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 发件邮箱 | xxxx@qq.com / xxxx | 密码必填 |
| `ADMIN_EMAILS` | 前台管理员邮箱（逗号分隔） | test111@qq.com | 是 |
| `ADMIN_NAME` / `ADMIN_PASSWORD` | 后台登录账号密码（仅 admin） | admin / 123456 | 是（用强口令） |
| `PROJECT_FOLDER` | 文件根目录 | E:/project/horizonhub | 是 |
| `TOKEN_SECRET` | Token 签名密钥（≥32 字节） | 内置 dev 密钥 | 是（强随机值） |
| `AI_API_KEY` | AI 助手大模型密钥 | 空（不启用） | 是（启用 AI 必填） |
| `AI_BASE_URL` / `AI_MODEL` | AI 端点 / 模型名 | DeepSeek / deepseek-chat | base-url+model 必填 |
| `SEND_MAIL_OPEN` | 是否真实发邮件 | false | — |
| `DRUID_MONITOR_ENABLED` | Druid 监控台 | false（关闭） | false（关闭） |

> dev 默认值仅供本地开箱即用；**生产环境绝不使用这些弱口令**。

### Redis

- 系统启动时将系统设置写入 Redis
- 用户发帖、评论、点赞、上传等频率控制依赖 Redis
- **Token 登录态存于 Redis**（key `horizonHub:token:{tokenId}`），可主动吊销

### 邮件验证码

- `send.mail.open=false` 时，邮箱验证码不真正发出，而是写入日志（适合本地开发）
- `send.mail.open=true` 时，需补全 `spring.mail.*` 配置

### 文件目录

项目通过 `projectFolder` 指定文件根目录，运行时会在该目录下使用这些路径：

- `file/avatar`：头像
- `file/images`：文章图片、评论图片、封面图
- `file/attachment`：文章附件
- `file/temp`：临时上传文件

### 管理员账号

- **前台管理员**：邮箱在 `ADMIN_EMAILS` 中配置的用户，登录后拥有管理员权限
- **后台登录**：账号密码来自 `ADMIN_NAME` / `ADMIN_PASSWORD`（dev 默认 `admin` / `123456`）

### AI 助手

- **未注入 `AI_API_KEY` 时**，AI 助手不可用（前端对话会返回「AI 助手未配置」提示），**不影响系统其余功能**
- 注入后，对话基于真实论坛数据（检索增强），流式输出
- 默认指向 DeepSeek，可换任意 OpenAI 兼容端点（通义 / Kimi / OpenAI 等）

## 部署方式（生产环境）

生产环境通过 `prod` profile 启动，**所有敏感项必须以环境变量注入**——`application-prod.yml` 对敏感项不给默认值，缺失即启动失败（fail-fast），杜绝误用 dev 弱口令。

### 1. 打包

```bash
mvn clean package -DskipTests
# 产物：
#   horizonhub-web/target/horizonhub-web-1.0.jar
#   horizonhub-admin/target/horizonhub-admin-1.0.jar
```

### 2. 准备环境变量

必填项（缺失会启动失败）：

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=...         DB_NAME=horizonhub     DB_USERNAME=...   DB_PASSWORD=...
export REDIS_HOST=...      REDIS_PASSWORD=...
export MAIL_PASSWORD=...   ADMIN_EMAILS=...
export ADMIN_NAME=...      ADMIN_PASSWORD=...     # 仅 admin 服务
export PROJECT_FOLDER=/data/horizonhub
export TOKEN_SECRET=随机生成至少32字节的密钥
export AI_BASE_URL=https://api.deepseek.com   AI_MODEL=deepseek-chat   AI_API_KEY=sk-...
```

> 生成 Token 密钥：`openssl rand -base64 48`

### 3. 启动

```bash
java -jar horizonhub-web-1.0.jar
java -jar horizonhub-admin-1.0.jar
```

生产环境的额外安全加固（已在 `application-prod.yml` 生效）：

- Druid 监控控制台关闭
- Session Cookie：`http-only` / `secure` / `same-site=strict`
- 日志级别 WARN
- 邮件真实发送（`send.mail.open=true`）

### 4. 前端部署

```bash
cd horizonhub-front/horizonhub-front-web
npm run build      # 产物在 dist/，由 Nginx 等托管，反向代理 /api 到后端
```

## 配置说明（认证）

- **双轨认证**：
  - 前端（web / admin）走 **Session（Cookie）**，无需改动
  - **AI / 第三方 / 移动端**走 **Token**：`POST /api/account/tokenLogin`（账号密码）拿 token，后续请求带 `Authorization: Bearer <token>`；`POST /api/account/tokenLogout` 吊销
  - Token = JWT 外壳（载 tokenId + HMAC 签名）+ Redis 登录态，支持主动吊销 / 强制下线
  - 拦截器优先认 Token、回落 Session
- 密码采用 **BCrypt** 加盐存储，兼容旧版 MD5（登录时自动惰性升级）
- 写操作接口均为 POST，规避 GET-CSRF
- 富文本正文经 jsoup 白名单净化，防存储型 XSS

## 开发说明

- 当前仓库已内置前端页面（`horizonhub-front`），无需另找前端仓库
- 用户侧与管理侧服务都默认挂载在 `/api` 上下文路径下
- `doc/` 目录保存阶段性开发文档与安全架构加固记录（`doc/安全架构加固-2026-08/`）
- 仓库当前未提供完整自动化测试用例，联调时建议优先验证登录、发帖、评论、审核、附件下载、AI 助手等核心流程

## 文档

- [安全架构加固记录](doc/安全架构加固-2026-08/README.md)：9 项安全/架构改造 + Token 演进 + AI 助手的详细前后对比文档

## 贡献

欢迎通过 Issue 或 Pull Request 参与改进。

```bash
git checkout -b feature/your-feature
git commit -m "feat: your feature"
git push origin feature/your-feature
```

## 许可证

本项目采用 [MIT License](LICENSE)。
