<div align="center">

# 🌅 HorizonHub Frontend

**现代化的论坛社区前端解决方案**

[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D?style=flat-square&logo=vue.js)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-4.x-646CFF?style=flat-square&logo=vite)](https://vitejs.dev/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.x-409EFF?style=flat-square)](https://element-plus.org/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

[功能特性](#-功能特性) •
[快速开始](#-快速开始) •
[项目结构](#-项目结构) •
[技术栈](#-技术栈) •
[后端仓库](https://github.com/nlizzard/horizonhub)

</div>

---

## 📖 项目简介

HorizonHub 是一个功能完善的现代化论坛社区系统，本仓库为其前端实现，采用 Vue 3 + Vite 构建，包含**用户端**和**管理端**两个独立应用。

- 🖥️ **用户端 (Web)** - 面向普通用户的论坛前台，支持文章浏览、发布、评论、搜索等功能
- 🔧 **管理端 (Admin)** - 面向管理员的后台系统，支持内容管理、用户管理、系统设置等功能

## ✨ 功能特性

### 用户端 (Web)

| 模块 | 功能 |
|------|------|
| 📝 **文章系统** | 文章列表、详情浏览、发布/编辑文章、Markdown/富文本编辑器 |
| 💬 **评论系统** | 多级评论、图片评论、楼中楼回复 |
| 📂 **板块分类** | 多级板块分类、板块筛选 |
| 👤 **用户中心** | 个人主页、资料编辑、积分记录、消息通知 |
| 🔍 **搜索功能** | 全站内容搜索 |
| 📎 **附件管理** | 附件上传、下载 |

### 管理端 (Admin)

| 模块 | 功能 |
|------|------|
| 📋 **内容管理** | 帖子审核、评论管理、板块配置 |
| 👥 **用户管理** | 用户列表、用户状态管理、站内消息 |
| ⚙️ **系统设置** | 站点配置、系统参数设置 |

## 🚀 快速开始

### 环境要求

- **Node.js** >= 16.0.0
- **pnpm** >= 7.0.0 (推荐) 或 npm >= 8.0.0

### 克隆仓库

```bash
git clone https://github.com/nlizzard/horizonhub-front.git
cd horizonhub-front
```

### 安装依赖

```bash
# 用户端
cd horizonhub-front-web
pnpm install

# 管理端
cd ../horizonhub-front-admin
pnpm install
```

### 配置环境变量

根据需要在各子项目中创建 `.env.local` 文件配置后端 API 地址等环境变量。

### 启动开发服务器

```bash
# 用户端 (默认端口: 5173)
cd horizonhub-front-web
pnpm dev

# 管理端 (默认端口: 5173)
cd horizonhub-front-admin
pnpm dev
```

### 构建生产版本

```bash
# 用户端
cd horizonhub-front-web
pnpm build

# 管理端
cd horizonhub-front-admin
pnpm build
```

构建产物将输出到各子项目的 `dist/` 目录。

## 📁 项目结构

```
horizonhub-front/
├── horizonhub-front-web/        # 用户端前台
│   ├── public/                  # 静态资源
│   ├── src/
│   │   ├── assets/              # 样式、图片等资源
│   │   ├── components/          # 公共组件
│   │   │   ├── Avatar.vue       # 头像组件
│   │   │   ├── Cover.vue        # 封面组件
│   │   │   ├── Dialog.vue       # 弹窗组件
│   │   │   ├── DataList.vue     # 数据列表组件
│   │   │   ├── EditorHtml.vue   # 富文本编辑器
│   │   │   ├── EditorMarkdown.vue # Markdown 编辑器
│   │   │   └── ...
│   │   ├── router/              # 路由配置
│   │   ├── store/               # Vuex 状态管理
│   │   ├── utils/               # 工具函数
│   │   ├── views/               # 页面组件
│   │   │   ├── forum/           # 论坛相关页面
│   │   │   ├── ucenter/         # 用户中心页面
│   │   │   └── ...
│   │   ├── App.vue              # 根组件
│   │   └── main.js              # 入口文件
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── horizonhub-front-admin/      # 管理后台
│   ├── public/
│   ├── src/
│   │   ├── assets/
│   │   ├── components/          # 公共组件
│   │   │   ├── Table.vue        # 表格组件
│   │   │   ├── Dialog.vue       # 弹窗组件
│   │   │   └── ...
│   │   ├── router/              # 路由配置
│   │   ├── utils/               # 工具函数
│   │   ├── views/               # 页面组件
│   │   │   ├── forum/           # 内容管理
│   │   │   ├── user/            # 用户管理
│   │   │   ├── settings/        # 系统设置
│   │   │   └── ...
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── .gitignore
└── README.md
```

## 🛠️ 技术栈

### 核心框架

| 技术 | 版本 | 说明 |
|------|------|------|
| [Vue.js](https://vuejs.org/) | 3.x | 渐进式 JavaScript 框架 |
| [Vite](https://vitejs.dev/) | 4.x | 下一代前端构建工具 |
| [Vue Router](https://router.vuejs.org/) | 4.x | 官方路由管理器 |
| [Vuex](https://vuex.vuejs.org/) | 4.x | 状态管理模式 |

### UI 与样式

| 技术 | 说明 |
|------|------|
| [Element Plus](https://element-plus.org/) | Vue 3 组件库 |
| [Sass](https://sass-lang.com/) | CSS 预处理器 |

### 编辑器

| 技术 | 说明 |
|------|------|
| [v-md-editor](https://github.com/code-farmer-i/vue-markdown-editor) | Markdown 编辑器 |
| [wangEditor](https://www.wangeditor.com/) | 富文本编辑器 |
| [highlight.js](https://highlightjs.org/) | 代码语法高亮 |

### 工具库

| 技术 | 说明 |
|------|------|
| [Axios](https://axios-http.com/) | HTTP 客户端 |
| [js-md5](https://github.com/emn178/js-md5) | MD5 加密 |
| [vue-cookies](https://github.com/cmp-cc/vue-cookies) | Cookie 管理 |

## 🔗 相关链接

- 📦 **后端仓库**: [https://github.com/nlizzard/horizonhub](https://github.com/nlizzard/horizonhub)

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

## 📄 许可证

本项目基于 [MIT](LICENSE) 许可证开源。

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star ⭐ 支持一下！**

</div>
