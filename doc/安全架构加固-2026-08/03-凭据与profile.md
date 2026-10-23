# 03 敏感凭据外置化与 profile 分离

> 涉及提交：`f791169` chore: 敏感配置外置化并拆分 dev/prod profile

## 原来的实现

两端 `application.yml` 把所有敏感值**明文写死**，且**不区分 dev / prod**：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/horizonhub?...
    username: root
    password: 123456          # 明文
    druid:
      stat-view-servlet:      # Druid 监控控制台
        enabled: true         # 默认开启，暴露 /druid/*
        login-username: nlizzard
        login-password: nlizzard8023   # 弱口令、与开发者同名
  data:
    redis:
      host: 192.168.123.2
      password:               # 空，无密码

admin:
  adminName: admin
  password: 123456            # 后台登录口令明文
```

`isDev.open` 一个布尔值在 dev / prod 之间切换。两个 yml 都被 git 跟踪（`.gitignore` 未排除），这些口令已进入 git 历史。

## 存在的问题

- **明文敏感信息入库**：数据库密码、Druid 控制台账密、后台口令、Redis（无密码）全部随代码进 git，泄漏即被直接利用。
- **无 profile 分离**：dev / prod 共用一份配置，靠布尔切换，无法做到「生产配置最小化、开发配置宽松」的隔离。
- **Druid 监控台默认开启**：`/druid/*` 暴露 SQL、连接池、Session 等敏感信息，且口令可猜。
- 引入 AI 后：AI 代理若调用本系统，等于把数据库和后台暴露在这些已知弱口令上。

## 改进后的实现

### 1. 敏感项用 `${ENV:默认值}` 占位符外置

基础 `application.yml` 改为：

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}      # 默认 dev，本地开箱即用
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:horizonhub}?...
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}            # 默认值仅用于本地 dev
    druid:
      stat-view-servlet:
        enabled: ${DRUID_MONITOR_ENABLED:false}  # 默认关闭
        login-username: ${DRUID_MONITOR_USERNAME:nlizzard}
        login-password: ${DRUID_MONITOR_PASSWORD:nlizzard8023}
  data:
    redis:
      host: ${REDIS_HOST:192.168.123.2}
      password: ${REDIS_PASSWORD:}
admin:
  adminName: ${ADMIN_NAME:admin}
  password: ${ADMIN_PASSWORD:123456}
projectFolder: ${PROJECT_FOLDER:E:/project/horizonhub}
```

### 2. 新增严格的 `application-prod.yml`（fail-fast）

生产覆盖文件**不给敏感项默认值**——缺失对应环境变量时，Spring 占位符解析失败、应用直接启动失败（fail-fast），杜绝误用 dev 默认弱口令：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}?...
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}            # 无默认值，缺失即启动失败
    druid:
      stat-view-servlet:
        enabled: false                  # 生产关闭 Druid 控制台
  data:
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}
  servlet:
    session:
      cookie: { http-only: true, secure: true, same-site: strict }   # Cookie 安全加固
```

### 3. `.gitignore` 排除本地覆盖

```gitignore
# 本地个人配置覆盖（含真实口令，切勿提交）
**/application-local.yml
**/application-local.yaml
```

## 如何工作（改造后）

- **本地开发**：默认 `dev` profile，占位符用 `${ENV:默认值}` 的默认值，无需配置环境变量即可启动。
- **生产部署**：以 `SPRING_PROFILES_ACTIVE=prod` 启动，加载 `application-prod.yml` 覆盖；必须注入 `DB_USERNAME` / `DB_PASSWORD` / `REDIS_PASSWORD` / `ADMIN_PASSWORD` 等环境变量，否则启动失败。
- **真实口令不入库**：用 `application-local.yml`（已被 ignore）承载本地真实口令，或直接用环境变量。
- **Druid 默认关闭**：仅本地通过 `DRUID_MONITOR_ENABLED=true` 按需开启。

## 验证 / 注意事项

- ⚠️ **本轮只改了配置文件，未清理 git 历史**：旧口令（`123456` / `nlizzard8023` 等）仍存在于历史提交里。若仓库已公开或曾泄漏，需要：① 轮换所有真实口令；② 用 `git filter-repo` 或 BFG 清理历史。
- 生产启动命令示例（Linux）：`SPRING_PROFILES_ACTIVE=prod DB_PASSWORD=xxx ... java -jar app.jar`。
- Druid 控制台一旦开启，务必配强口令 + `allow` IP 白名单（本轮未做白名单，仅默认关闭）。
