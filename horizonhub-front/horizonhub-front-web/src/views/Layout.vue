<template>
  <div>
    <div class="header" :class="{ 'header-hidden': !showHeader }">
      <div
        class="header-content"
        :style="{ width: proxy.globalInfo.bodyWidth + 'px' }"
      >
        <!--logo-->
        <router-link to="/" class="logo">
          <span
            v-for="item in logoInfo"
            :style="{ color: item.color }"
            :key="item"
            >{{ item.letter }}</span
          >
        </router-link>
        <!--板块信息-->
        <nav class="menu-panel" aria-label="主导航">
          <router-link
            :class="[
              'menu-item home',
              activePboardId == undefined ? 'active' : '',
            ]"
            to="/"
            >首页</router-link
          >
          <template v-for="board in boardList" :key="board.boardId">
            <el-popover
              placement="bottom-start"
              :width="300"
              trigger="hover"
              v-if="board.children && board.children.length > 0"
            >
              <template #reference>
                <button
                  type="button"
                  :class="[
                    'menu-item',
                    board.boardId == activePboardId ? 'active' : '',
                  ]"
                  @click="boardClickHandler(board)"
                >
                  {{ board.boardName }}
                </button>
              </template>
              <div class="sub-board-list">
                <button
                  type="button"
                  :class="[
                    'sub-board',
                    subBoard.boardId == activeBoardId ? 'active' : '',
                  ]"
                  v-for="subBoard in board.children"
                  @click="subBoardClickHandler(subBoard)"
                  :key="subBoard.boardId"
                >
                  {{ subBoard.boardName }}
                </button>
              </div>
            </el-popover>
            <button
              type="button"
              :class="[
                'menu-item',
                board.boardId == activePboardId ? 'active' : '',
              ]"
              v-else
              @click="boardClickHandler(board)"
            >
              {{ board.boardName }}
            </button>
          </template>
        </nav>
        <!--登录，注册 用户信息-->
        <div class="user-info-panel">
          <div class="search-entry">
            <el-input
              v-model.trim="headerKeyword"
              placeholder="搜索帖子、板块或关键词"
              clearable
              @keyup.enter="goSearch"
            >
              <template #suffix>
                <span
                  class="iconfont icon-search search-icon"
                  @click="goSearch"
                ></span>
              </template>
            </el-input>
          </div>
          <div class="op-btn">
            <el-button type="primary" class="new-post-btn" @click="newPost">
              发帖<span class="iconfont icon-add"></span>
            </el-button>
          </div>

          <!--显示消息分类-->
          <template v-if="userInfo.userId">
            <div class="message-info">
              <el-dropdown>
                <el-badge
                  :value="messageCountInfo.total"
                  class="item"
                  :hidden="
                    messageCountInfo.total == null ||
                    messageCountInfo.total == 0
                  "
                >
                  <div class="iconfont icon-message"></div>
                </el-badge>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      @click="gotoMessage('reply')"
                      class="message-item"
                    >
                      <span class="text">回复我的</span>
                      <span
                        class="count-tag"
                        v-if="messageCountInfo.reply > 0"
                        >{{
                          messageCountInfo.reply > 99
                            ? "99+"
                            : messageCountInfo.reply
                        }}</span
                      >
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="gotoMessage('likePost')"
                      class="message-item"
                    >
                      <span class="text">赞了我的文章</span>
                      <span
                        class="count-tag"
                        v-if="messageCountInfo.likePost > 0"
                        >{{
                          messageCountInfo.likePost > 99
                            ? "99+"
                            : messageCountInfo.likePost
                        }}</span
                      >
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="gotoMessage('downloadAttachment')"
                      class="message-item"
                    >
                      <span class="text">下载了我的附件</span>
                      <span
                        class="count-tag"
                        v-if="messageCountInfo.downloadAttachment > 0"
                        >{{
                          messageCountInfo.downloadAttachment > 99
                            ? "99+"
                            : messageCountInfo.downloadAttachment
                        }}</span
                      >
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="gotoMessage('likeComment')"
                      class="message-item"
                    >
                      <span class="text">赞了我的评论</span>
                      <span
                        class="count-tag"
                        v-if="messageCountInfo.likeComment > 0"
                        >{{
                          messageCountInfo.likeComment > 99
                            ? "99+"
                            : messageCountInfo.likeComment
                        }}</span
                      >
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="gotoMessage('sys')"
                      class="message-item"
                    >
                      <span class="text">系统消息</span>
                      <span class="count-tag" v-if="messageCountInfo.sys > 0">{{
                        messageCountInfo.sys > 99 ? "99+" : messageCountInfo.sys
                      }}</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <!-- 用户信息（头像） -->
            <div class="user-info">
              <el-dropdown>
                <avatar
                  :userId="userInfo.userId"
                  :width="50"
                  :addLink="false"
                ></avatar>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="gotoUcenter(userInfo.userId)"
                      >我的主页</el-dropdown-item
                    >
                    <el-dropdown-item @click="logout">退出</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <div class="auth-links" v-else>
            <button
              type="button"
              class="ghost-link"
              @click="loginAndRegister(1)"
            >
              登录
            </button>
            <button
              type="button"
              class="ghost-link"
              @click="loginAndRegister(0)"
            >
              注册
            </button>
          </div>
        </div>
      </div>
    </div>
    <!-- 主体内容 -->
    <div class="body-content">
      <router-view />
    </div>
    <div class="footer" v-if="showFooter">
      <div
        class="footer-content"
        :style="{ width: proxy.globalInfo.bodyWidth + 'px' }"
      >
        <div class="footer-top-card">
          <div class="footer-brand">
            <div class="logo-letter">
              <span
                v-for="item in logoInfo"
                :style="{ color: item.color }"
                :key="item.letter"
                >{{ item.letter }}</span
              >
            </div>
            <p class="brand-desc">一个开放、包容、持续生长的论坛社区</p>
            <div class="brand-badges">
              <span>开放交流</span>
              <span>知识共享</span>
              <span>理性讨论</span>
            </div>
          </div>
          <div class="footer-section">
            <h4 class="section-title">快速入口</h4>
            <nav class="footer-nav">
              <router-link to="/">首页</router-link>
              <router-link to="/search">搜索</router-link>
              <router-link to="/newPost">发布</router-link>
            </nav>
          </div>
          <div class="footer-section">
            <h4 class="section-title">关于 HorizonHub</h4>
            <p class="section-desc">
              HorizonHub
              是一个开放论坛，任何人都可以在这里分享知识、经验与观点。我们鼓励理性讨论与互助交流。
            </p>
            <ul class="value-list">
              <li>开放注册与自由表达</li>
              <li>友善互助与内容共建</li>
              <li>鼓励原创与高质量讨论</li>
            </ul>
          </div>
        </div>
        <div class="footer-bottom">
          <p class="copyright">© 2026 HorizonHub · Open Forum Community</p>
        </div>
      </div>
    </div>
    <!--登录 注册-->
    <LoginAndRegister ref="loginRegisterRef"></LoginAndRegister>
    <!--回到顶部-->
    <el-backtop :right="50" :bottom="100"></el-backtop>
    <!--AI 助手悬浮入口-->
    <AiAssistant></AiAssistant>
  </div>
</template>

<script setup>
import LoginAndRegister from "./LoginAndRegister.vue";
import {
  ref,
  reactive,
  getCurrentInstance,
  onMounted,
  onBeforeUnmount,
  watch,
} from "vue";
import { useRouter, useRoute } from "vue-router";
import { useStore } from "vuex";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const store = useStore();
const headerKeyword = ref("");

const api = {
  getUserInfo: "/getUserInfo",
  loadBoard: "/board/loadBoard",
  loadMessageCount: "/ucenter/getMessageCount",
  logout: "/logout",
  getSysSetting: "/getSysSetting",
};

// 优雅的渐变色配色方案
const logoInfo = ref([
  { letter: "H", color: "#6366f1" }, // Indigo
  { letter: "o", color: "#8b5cf6" }, // Violet
  { letter: "r", color: "#a855f7" }, // Purple
  { letter: "i", color: "#d946ef" }, // Fuchsia
  { letter: "z", color: "#ec4899" }, // Pink
  { letter: "o", color: "#f43f5e" }, // Rose
  { letter: "n", color: "#ef4444" }, // Red
  { letter: "H", color: "#06b6d4" }, // Cyan
  { letter: "u", color: "#0ea5e9" }, // Sky
  { letter: "b", color: "#3b82f6" }, // Blue
]);

const showHeader = ref(true);
let scrollRafId = 0;
let lastScrollTop = 0;
let scrollHandler = null;

//获取滚动条的高度
const getScrollTop = () => {
  let scrollTop =
    document.documentElement.scrollTop ||
    window.pageYOffset ||
    document.body.scrollTop;
  return scrollTop;
};

const initScroll = () => {
  lastScrollTop = getScrollTop();
  scrollHandler = () => {
    if (scrollRafId) {
      return;
    }
    scrollRafId = window.requestAnimationFrame(() => {
      const currentScrollTop = getScrollTop();
      const scrollingDown = currentScrollTop > lastScrollTop;
      const shouldHideHeader = scrollingDown && currentScrollTop > 100;
      if (showHeader.value === shouldHideHeader) {
        showHeader.value = !shouldHideHeader;
      }
      lastScrollTop = currentScrollTop;
      scrollRafId = 0;
    });
  };
  window.addEventListener("scroll", scrollHandler, { passive: true });
};

//登录注册
const loginRegisterRef = ref();
const loginAndRegister = (type) => {
  loginRegisterRef.value.showPanel(type);
};
onMounted(() => {
  initScroll();
  getUserInfo();
  loadSysSetting();
});

onBeforeUnmount(() => {
  if (scrollHandler) {
    window.removeEventListener("scroll", scrollHandler);
  }
  if (scrollRafId) {
    window.cancelAnimationFrame(scrollRafId);
    scrollRafId = 0;
  }
});

//获取用户信息
const getUserInfo = async () => {
  let result = await proxy.Request({
    url: api.getUserInfo,
  });
  if (!result) {
    return;
  }
  store.commit("updateLoginUserInfo", result.data);
};

//获取板块信息
const boardList = ref([]);
const loadBoard = async () => {
  let result = await proxy.Request({
    url: api.loadBoard,
  });
  if (!result) {
    return;
  }
  boardList.value = result.data;
  store.commit("saveBoardList", result.data);
};
loadBoard();

//监听 登录用户信息
const userInfo = ref({});
watch(
  () => store.state.loginUserInfo,
  (newVal, oldVal) => {
    if (newVal != undefined && newVal != null) {
      userInfo.value = newVal;
    } else {
      userInfo.value = {};
    }
  },
  { immediate: true }
);
//监听是否展示登录框
watch(
  () => store.state.showLogin,
  (newVal, oldVal) => {
    if (newVal) {
      loginAndRegister(1);
    }
  },
  { immediate: true }
);

//板块点击
const boardClickHandler = (board) => {
  router.push(`/forum/${board.boardId}`);
};

//二级板块
const subBoardClickHandler = (subBoard) => {
  router.push(`/forum/${subBoard.pBoardId}/${subBoard.boardId}`);
};

//当前选中的板块
const activePboardId = ref(0);
watch(
  () => store.state.activePboardId,
  (newVal, oldVal) => {
    if (newVal !== 0) {
      activePboardId.value = newVal;
    }
  },
  { immediate: true }
);

const activeBoardId = ref(0);
watch(
  () => store.state.activeBoardId,
  (newVal, oldVal) => {
    activeBoardId.value = newVal;
  },
  { immediate: true }
);

//发帖
const newPost = () => {
  if (!store.getters.getLoginUserInfo) {
    loginAndRegister(1);
  } else {
    router.push("/newPost");
  }
};

const gotoUcenter = (userId) => {
  router.push(`/user/${userId}`);
};

//消息相关
const gotoMessage = (type) => {
  router.push(`/user/message/${type}`);
};

const messageCountInfo = ref({});
const loadMessageCount = async () => {
  let result = await proxy.Request({
    url: api.loadMessageCount,
  });
  if (!result) {
    return;
  }
  messageCountInfo.value = result.data;
  store.commit("updateMessageCountInfo", result.data);
};

watch(
  () => store.state.messageCountInfo,
  (newVal, oldVal) => {
    messageCountInfo.value = newVal || {};
  },
  { immediate: true }
);

watch(
  () => store.state.loginUserInfo,
  (newVal, oldVal) => {
    if (newVal) {
      loadMessageCount();
    }
  },
  { immediate: true }
);

//退出
const logout = () => {
  proxy.Confirm("确定要退出吗?", async () => {
    let result = await proxy.Request({
      url: api.logout,
    });
    if (!result) {
      return;
    }
    store.commit("updateLoginUserInfo", null);
  });
};

//获取系统配置
const loadSysSetting = async () => {
  let result = await proxy.Request({
    url: api.getSysSetting,
  });
  if (!result) {
    return;
  }
  store.commit("saveSysSetting", result.data);
};

const goSearch = () => {
  const keyword = (headerKeyword.value || "").trim();
  if (!keyword) {
    proxy.Message.warning("请输入关键字");
    return;
  }
  if (keyword.length < 3) {
    proxy.Message.warning("关键字太少，至少三个字");
    return;
  }
  router.push({
    path: "/search",
    query: {
      keyword,
    },
  });
};

//是否展示底部
const showFooter = ref(true);
watch(
  () => route.path,
  (newVal, oldVal) => {
    if (newVal.indexOf("newPost") != -1 || newVal.indexOf("editPost") != -1) {
      showFooter.value = false;
    } else {
      showFooter.value = true;
    }
  },
  { immediate: true }
);
</script>

<style lang="scss">
@import url("https://fonts.googleapis.com/css2?family=Poppins:wght@500;600;700&family=Noto+Sans+SC:wght@400;500;700&display=swap");

.header {
  top: 0px;
  width: 100%;
  position: fixed;
  z-index: 1000;
  transition: transform 0.2s ease, opacity 0.2s ease;
  will-change: transform;
  background: linear-gradient(
    180deg,
    rgba(243, 250, 255, 0.96),
    rgba(243, 250, 255, 0)
  );
  .header-content {
    margin: 0px auto;
    align-items: center;
    margin-top: 10px;
    height: 64px;
    padding: 0 16px;
    border-radius: 18px;
    border: 1px solid rgba(185, 213, 236, 0.85);
    box-shadow: 0 10px 30px rgba(20, 74, 117, 0.13);
    background: rgba(255, 255, 255, 0.96);
    display: flex;
    align-items: center;
    font-family: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
    .logo {
      position: relative;
      display: block;
      text-decoration: none;
      margin-right: 16px;

      span {
        font-size: 35px;
        font-family: "Poppins", sans-serif;
        font-weight: 700;
        display: inline-block;
        transition: transform 0.3s ease, filter 0.3s ease;
      }

      &:hover span {
        animation: logo-wave-anim 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      }

      @for $i from 1 through 10 {
        &:hover span:nth-child(#{$i}) {
          animation-delay: #{($i - 1) * 0.04}s;
        }
      }
    }
    .menu-panel {
      display: flex;
      align-items: center;
      gap: 10px;
      flex: 1;
      .menu-item {
        appearance: none;
        border: none;
        background: transparent;
        margin-left: 0;
        border-radius: 999px;
        padding: 8px 12px;
        color: #38526a;
        font-size: 15px;
        transition: background-color 0.2s ease, color 0.2s ease,
          transform 0.2s ease;
        cursor: pointer;
      }
      .menu-item:hover {
        background: #ebf6ff;
        color: #1676b8;
        transform: translateY(-1px);
      }
      .menu-item:focus-visible {
        outline: 2px solid #1f87cb;
        outline-offset: 2px;
      }
      .home {
        text-decoration: none;
        color: #244766;
      }
      .active {
        color: #0f8fd9;
        background: #e9f6ff;
        font-weight: 600;
      }
    }
    .user-info-panel {
      width: 540px;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 10px;

      .search-entry {
        width: 260px;

        :deep(.el-input__wrapper) {
          border-radius: 999px;
          background: #f7fbff;
          box-shadow: 0 0 0 1px #d2e4f3 inset;
        }

        :deep(.el-input__inner) {
          color: #2a455d;
        }

        .search-icon {
          color: #5f7d96;
          font-size: 16px;
          cursor: pointer;
          transition: color 0.2s ease;
        }

        .search-icon:hover {
          color: #0891b2;
        }
      }

      .op-btn {
        .new-post-btn.el-button--primary {
          border: 1px solid #0891b2;
          background: linear-gradient(120deg, #08a3c9 0%, #0b8fb5 100%);
          border-radius: 999px;
          height: 36px;
          padding: 0 18px;
          font-weight: 600;
          letter-spacing: 0.5px;
          color: #ffffff;
          box-shadow: 0 6px 14px rgba(8, 145, 178, 0.26);
          transition: transform 0.22s ease, box-shadow 0.22s ease,
            background 0.22s ease;
        }

        .new-post-btn .iconfont {
          margin-left: 6px;
          font-size: 13px;
          transition: transform 0.22s ease;
        }

        .new-post-btn.el-button--primary:hover {
          transform: translateY(-1px);
          background: linear-gradient(120deg, #0994b9 0%, #0f7b97 100%);
          box-shadow: 0 8px 18px rgba(8, 145, 178, 0.32);
        }

        .new-post-btn:hover .iconfont {
          transform: translateX(1px);
        }

        .new-post-btn.el-button--primary:active {
          transform: translateY(0);
          box-shadow: 0 4px 10px rgba(8, 145, 178, 0.24);
        }

        .new-post-btn.el-button--primary:focus-visible {
          outline: 2px solid #67e8f9;
          outline-offset: 2px;
        }
      }

      .auth-links {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-left: 2px;

        .ghost-link {
          appearance: none;
          border: 1px solid #b9d3e8;
          border-radius: 999px;
          padding: 6px 13px;
          background: rgba(255, 255, 255, 0.8);
          color: #335770;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .ghost-link:hover {
          color: #0f8fd9;
          border-color: #8fc1e6;
          background: #f2faff;
        }
      }

      .message-info {
        margin-left: 5px;
        margin-right: 10px;
        cursor: pointer;
        .icon-message {
          font-size: 25px;
          color: #70879d;
        }
      }
    }
  }
}

.header-hidden {
  transform: translateY(-110%);
  opacity: 0;
  pointer-events: none;
}

body.md-editor-fullscreen {
  .header {
    transform: translateY(-110%);
    opacity: 0;
    pointer-events: none;
  }
}

.sub-board-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  .sub-board {
    appearance: none;
    border: 1px solid #d4e7f7;
    background: #f5fbff;
    border-radius: 20px;
    color: #4d6a84;
    margin-top: 8px;
    padding: 3px 12px;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .sub-board:hover {
    color: #0f8fd9;
    border-color: #a5cbe7;
    transform: translateY(-1px);
  }
  .active {
    background: #0f8fd9;
    border-color: #0f8fd9;
    color: #fff;
  }
  .active:hover {
    color: #fff;
  }
}

.body-content {
  margin-top: 78px;
  position: relative;
  min-height: calc(100vh - 210px);
}

.message-item {
  display: flex;
  justify-content: space-around;
  .text {
    flex: 1;
  }
  .count-tag {
    height: 15px;
    line-height: 15px;
    min-width: 20px;
    display: inline-block;
    background: #f56c6c;
    border-radius: 10px;
    font-size: 13px;
    text-align: center;
    color: #fff;
    margin-left: 10px;
  }
}

.footer {
  background: linear-gradient(180deg, #f1fbff 0%, #e7f4fc 100%);
  border-top: 1px solid #cfe3f1;
  margin-top: 36px;
  .footer-content {
    margin: 0 auto;
    padding: 26px 0 20px;
    font-family: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;

    .footer-top-card {
      background: linear-gradient(
        140deg,
        rgba(255, 255, 255, 0.94),
        rgba(238, 250, 255, 0.9)
      );
      border: 1px solid #d2e8f6;
      box-shadow: 0 14px 34px rgba(20, 90, 136, 0.12);
      border-radius: 20px;
      padding: 30px 28px;
      display: grid;
      grid-template-columns: 1.2fr 0.8fr 1fr;
      gap: 34px;
      margin-bottom: 20px;
    }

    .footer-brand {
      .logo-letter {
        font-size: 28px;
        font-family: "Poppins", sans-serif;
        font-weight: 700;
        margin-bottom: 12px;
        span {
          display: inline-block;
        }
      }
      .brand-desc {
        color: #41607a;
        font-size: 14px;
        line-height: 1.7;
        margin-bottom: 16px;
      }

      .brand-badges {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        span {
          display: inline-flex;
          align-items: center;
          border-radius: 999px;
          padding: 5px 11px;
          font-size: 12px;
          color: #0f617f;
          background: #d6f4fb;
          border: 1px solid #9fd7e8;
        }
      }
    }

    .footer-section {
      .section-title {
        font-size: 16px;
        font-weight: 600;
        color: #184866;
        margin-bottom: 16px;
        position: relative;
        padding-bottom: 10px;
        &::after {
          content: "";
          position: absolute;
          left: 0;
          bottom: 0;
          width: 34px;
          height: 2px;
          background: linear-gradient(90deg, #0891b2, #22d3ee);
          border-radius: 2px;
        }
      }
      .section-desc {
        font-size: 14px;
        color: #43657f;
        line-height: 1.7;
      }
      .footer-nav {
        display: flex;
        flex-direction: column;
        gap: 10px;
        a {
          font-size: 14px;
          color: #45657d;
          text-decoration: none;
          transition: color 0.2s ease, transform 0.2s ease;
          &:hover {
            color: #0891b2;
            transform: translateX(2px);
          }
        }
      }

      .value-list {
        margin: 12px 0 0;
        padding-left: 18px;
        color: #45657d;
        font-size: 13px;
        line-height: 1.8;
      }
    }

    .footer-bottom {
      padding-top: 8px;
      text-align: center;
      .copyright {
        font-size: 13px;
        color: #67859b;
      }
    }
  }
}

@media (max-width: 768px) {
  .footer .footer-content {
    padding: 18px 12px 16px;
    .footer-top-card {
      grid-template-columns: 1fr;
      gap: 20px;
      padding: 20px 16px;
    }
  }
}

@media (max-width: 1100px) {
  .header .header-content {
    border-radius: 0;
    margin-top: 0;
    border-left: none;
    border-right: none;
  }

  .header .header-content .user-info-panel {
    width: 420px;
    .search-entry {
      width: 200px;
    }
  }
}

@media (max-width: 860px) {
  .header .header-content .user-info-panel .search-entry {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .header {
    transition: none;
  }
  .header .header-content .logo span {
    transition: none;
    animation: none;
  }
  .header .header-content .menu-panel .menu-item,
  .sub-board-list .sub-board,
  .header .header-content .user-info-panel .op-btn :deep(.el-button--primary),
  .header .header-content .user-info-panel .auth-links .ghost-link,
  .header .header-content .user-info-panel .search-entry .search-icon {
    transition: none;
  }
}

@keyframes logo-wave-anim {
  0% {
    transform: translateY(0) scale(1);
    filter: brightness(1);
  }
  30% {
    transform: translateY(-4px) scale(1.1);
    filter: brightness(1.15);
    text-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
  }
  100% {
    transform: translateY(0) scale(1);
    filter: brightness(1);
    text-shadow: none;
  }
}
</style>