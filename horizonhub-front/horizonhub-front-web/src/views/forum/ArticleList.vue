<template>
  <div
    class="container-body article-list-body"
    :style="{ width: proxy.globalInfo.bodyWidth + 'px' }"
  >
    <div class="home-hero">
      <div class="hero-content">
        <div class="hero-text">
          <p class="hero-kicker">HorizonHub 社区</p>
          <h1>开放分享，自由讨论，连接真实观点</h1>
          <p class="hero-desc">
            HorizonHub 是一个开放论坛，欢迎你分享经验、提出问题、参与讨论，
            在多元观点中找到有价值的答案。
          </p>
          <div class="hero-tags">
            <span>开放</span>
            <span>共建</span>
            <span>高质量交流</span>
          </div>
        </div>
      </div>
      <div class="hero-glow"></div>
    </div>
    <!--二级板块信息-->
    <div class="sub-board" v-if="pBoardId">
      <span :class="['board-item', boardId == 0 ? 'active' : '']">
        <router-link :to="`/forum/${pBoardId}`">全部</router-link></span
      >
      <span
        v-for="item in subBoardList"
        :key="item.boardId"
        :class="['board-item', item.boardId == boardId ? 'active' : '']"
      >
        <router-link :to="`/forum/${item.pBoardId}/${item.boardId}`">{{
          item.boardName
        }}</router-link>
      </span>
    </div>
    <div class="article-panel">
      <div class="top-tab">
        <div
          :class="['tab', orderType == 0 ? 'active' : '']"
          @click="changeOrderType(0)"
          tabindex="0"
          role="button"
          @keyup.enter="changeOrderType(0)"
        >
          热榜
        </div>
        <div
          :class="['tab', orderType == 1 ? 'active' : '']"
          @click="changeOrderType(1)"
          tabindex="0"
          role="button"
          @keyup.enter="changeOrderType(1)"
        >
          发布时间
        </div>
        <div
          :class="['tab', orderType == 2 ? 'active' : '']"
          @click="changeOrderType(2)"
          tabindex="0"
          role="button"
          @keyup.enter="changeOrderType(2)"
        >
          最新
        </div>
      </div>
      <div class="article-list">
        <DataList
          :loading="loading"
          :dataSource="articleListInfo"
          @loadData="loadArticle"
          noDataMsg="没有发现帖子，赶紧发布一个吧"
        >
          <template #default="{ data }">
            <ArticleListItem
              :data="data"
              :showComment="showComment"
            ></ArticleListItem>
          </template>
        </DataList>
      </div>
    </div>
  </div>
</template>

<script setup>
import ArticleListItem from "./ArticleListItem.vue";
import { ref, reactive, getCurrentInstance, onMounted, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useStore } from "vuex";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const store = useStore();

const api = {
  loadArticle: "/forum/loadArticle",
};

const changeOrderType = (type) => {
  orderType.value = type;
  loadArticle();
};

//文章列表
//一级板块
const pBoardId = ref(0);
//二级板块
const boardId = ref(0);
const orderType = ref(0);
const loading = ref(false);
const articleListInfo = ref({});
const loadArticle = async () => {
  loading.value = true;
  let params = {
    pageNo: articleListInfo.value.pageNo,
    pBoardId: pBoardId.value,
    boardId: boardId.value,
    orderType: orderType.value,
  };
  let result = await proxy.Request({
    url: api.loadArticle,
    params: params,
    showLoading: false,
  });
  loading.value = false;
  if (!result) {
    return;
  }
  articleListInfo.value = result.data;
};

//二级板块
const subBoardList = ref([]);
const setSubBoard = () => {
  subBoardList.value = store.getters.getSubBoardList(pBoardId.value);
};
//监听路由变化
watch(
  () => route.params,
  (newVal, oldVal) => {
    if (
      Object.keys(newVal).length != 0 &&
      !newVal.pBoardId &&
      !newVal.boardId
    ) {
      return;
    }
    pBoardId.value = newVal.pBoardId;
    boardId.value = newVal.boardId || 0;
    setSubBoard();
    loadArticle();
    store.commit("setActivePboardId", newVal.pBoardId);
    store.commit("setActiveBoardId", newVal.boardId);
  },
  { immediate: true }
);

//监听 板块数据变化
watch(
  () => store.state.boardList,
  (newVal, oldVal) => {
    setSubBoard();
  },
  { immediate: true }
);

const showComment = ref(false);
watch(
  () => store.state.sysSetting,
  (newVal, oldVal) => {
    if (newVal) {
      showComment.value = newVal.commentOpen;
    }
  },
  { immediate: true }
);
</script>

<style lang="scss">
@import url("https://fonts.googleapis.com/css2?family=Outfit:wght@500;600;700&family=Noto+Sans+SC:wght@400;500;600&display=swap");

.article-list-body {
  font-family: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
  padding: 16px 0 22px;

  .home-hero {
    position: relative;
    overflow: hidden;
    border-radius: 22px;
    padding: 24px 24px;
    margin-bottom: 14px;
    border: 1px solid #dce8f7;
    background: linear-gradient(135deg, #f8fcff 0%, #eef6ff 40%, #f6fbff 100%);
    box-shadow: 0 12px 28px rgba(21, 77, 128, 0.08);

    .hero-content {
      position: relative;
      z-index: 2;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;
    }

    .hero-text {
      max-width: 680px;

      .hero-kicker {
        margin: 0 0 8px;
        color: #1769aa;
        letter-spacing: 0.08em;
        font-size: 12px;
        font-weight: 600;
      }

      h1 {
        margin: 0;
        font-family: "Outfit", "Noto Sans SC", sans-serif;
        color: #0d2438;
        font-size: clamp(24px, 3vw, 34px);
        font-weight: 700;
        letter-spacing: 0.01em;
        line-height: 1.24;
      }

      .hero-desc {
        margin: 10px 0 0;
        color: #2f5879;
        font-size: 14px;
        line-height: 1.7;
      }

      .hero-tags {
        margin-top: 14px;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        span {
          display: inline-flex;
          align-items: center;
          border-radius: 999px;
          padding: 5px 11px;
          font-size: 12px;
          color: #145b7f;
          background: #d8f4fb;
          border: 1px solid #99d9ea;
        }
      }
    }

    .hero-glow {
      position: absolute;
      width: 260px;
      height: 260px;
      border-radius: 50%;
      right: -65px;
      top: -85px;
      background: radial-gradient(
        circle,
        rgba(22, 172, 214, 0.2) 0%,
        rgba(22, 172, 214, 0) 68%
      );
    }
  }

  .sub-board {
    padding: 6px 0px 14px 0px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    .board-item {
      background: rgba(255, 255, 255, 0.8);
      border-radius: 999px;
      border: 1px solid #dbe5ef;
      padding: 4px 12px;
      color: #61788d;
      cursor: pointer;
      font-size: 14px;
      transition: all 0.2s ease;
      a {
        text-decoration: none;
        color: #61788d;
      }
    }
    .board-item:hover {
      border-color: #9ec7e4;
      background: #fff;
      transform: translateY(-1px);
    }
    .active {
      background: #0f8fd9;
      border-color: #0f8fd9;
      a {
        color: #fff;
      }
    }
  }
  .article-panel {
    background: rgba(255, 255, 255, 0.86);
    border-radius: 20px;
    border: 1px solid #dce8f7;
    box-shadow: 0 12px 30px rgba(24, 69, 107, 0.08);
    .top-tab {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 14px 18px;
      font-size: 15px;
      border-bottom: 1px solid #e2ecf5;
      .tab {
        cursor: pointer;
        padding: 8px 2px;
        border-radius: 0;
        border-bottom: 2px solid transparent;
        color: #4e657b;
        transition: all 0.22s ease;
      }
      .tab:hover {
        background: transparent;
        color: #1a5f93;
      }
      .tab:focus-visible {
        outline: 2px solid #1a7fc5;
        outline-offset: 2px;
      }
      .active {
        color: #0f8fd9;
        border-bottom-color: #0f8fd9;
        font-weight: 600;
      }
    }
    .article-list {
      padding: 6px 0;
    }
  }

  @media (max-width: 768px) {
    padding-top: 10px;

    .home-hero {
      border-radius: 16px;
      padding: 18px 16px;
    }

    .article-panel {
      border-radius: 14px;
      .top-tab {
        padding: 12px 12px;
      }
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .sub-board .board-item,
    .article-panel .top-tab .tab {
      transition: none;
    }
  }
}
</style>