<template>
  <div
    class="article-item"
    @click="goToArticle"
    v-memo="[
      data.articleId,
      data.title,
      data.summary,
      data.readCount,
      data.goodCount,
      data.commentCount,
      data.cover,
      data.topType,
      data.status,
    ]"
  >
    <div class="article-item-inner">
      <div class="article-body">
        <div class="user-info" @click.stop>
          <Avatar :userId="data.userId" :width="30"></Avatar>
          <router-link :to="'/user/' + data.userId" class="link-info">{{
            data.nickName
          }}</router-link>
          <el-divider direction="vertical"></el-divider>
          <div class="post-time">{{ data.postTime }}</div>
          <div class="address">&nbsp;·&nbsp;{{ data.userIpAddress }}</div>
          <el-divider direction="vertical"></el-divider>
          <router-link :to="`/forum/${data.pBoardId}`" class="link-info">{{
            data.pBoardName
          }}</router-link>
          <template v-if="data.boardName">
            <span>&nbsp;/&nbsp;</span>
            <router-link
              :to="`/forum/${data.pBoardId}/${data.boardId}`"
              class="link-info"
              >{{ data.boardName }}</router-link
            >
          </template>
        </div>
        <div class="title">
          <span v-if="data.topType == 1" class="top">置顶</span>
          <span v-if="data.status == 0" class="tag tag-no-audit">待审核</span>
          <span v-if="htmlTitle" v-html="data.title"></span>
          <span v-else>{{ data.title }}</span>
        </div>
        <div class="summary">{{ data.summary }}</div>
        <div class="article-info" @click.stop>
          <span class="iconfont icon-eye-solid">
            {{ data.readCount == 0 ? "阅读" : data.readCount }}
          </span>
          <span class="iconfont icon-good">
            {{ data.goodCount == 0 ? "点赞" : data.goodCount }}
          </span>
          <span class="iconfont icon-comment" v-if="showComment">
            {{ data.commentCount == 0 ? "评论" : data.commentCount }}
          </span>
          <span
            class="iconfont icon-edit edit-btn"
            v-if="showEdit"
            @click="editArticle(data.articleId)"
            >编辑</span
          >
        </div>
      </div>
      <div class="cover-wrap" v-if="data.cover">
        <Cover :cover="data.cover" :width="172" :height="128"></Cover>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
const router = useRouter();
const props = defineProps({
  data: {
    type: Object,
  },
  showComment: {
    type: Boolean,
  },
  showEdit: {
    type: Boolean,
  },
  htmlTitle: {
    type: Boolean,
    default: false,
  },
});

// 点击卡片跳转到文章详情
const goToArticle = () => {
  router.push(`/post/${props.data.articleId}`);
};

const editArticle = (articleId) => {
  router.push(`/editPost/${articleId}`);
};
</script>

<style lang="scss" scoped>
.article-item {
  padding: 10px 14px 6px;
  content-visibility: auto;
  contain-intrinsic-size: 220px;
  .article-item-inner {
    border: 1px solid #e4edf6;
    background: linear-gradient(180deg, #ffffff 0%, #fcfeff 100%);
    border-radius: 16px;
    padding: 24px;
    display: flex;
    gap: 16px;
    transition: transform 0.22s ease, box-shadow 0.22s ease,
      border-color 0.22s ease, background-color 0.22s ease;
    cursor: pointer;

    .article-body {
      flex: 1;
      min-width: 0;
      .user-info {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 2px;
        font-size: 14px;
        color: #4e5969;
        .link-info {
          margin-left: 5px;
          color: #2f4b66;
          text-decoration: none;
          transition: color 0.2s ease;
        }
        .link-info:hover {
          color: #0f8fd9;
        }
        .post-time {
          font-size: 13px;
          color: #8396a8;
        }
        .address {
          color: #8396a8;
        }
      }
      .title {
        font-weight: 700;
        color: #11263a;
        font-size: 20px;
        line-height: 1.45;
        margin: 10px 0 8px;
        display: block;
        transition: color 0.2s ease;

        .top {
          font-size: 12px;
          border-radius: 999px;
          border: 1px solid #9dcfe9;
          color: #0f6f9f;
          background: #e3f4ff;
          padding: 1px 8px;
          margin-right: 8px;
        }
      }

      .summary {
        font-size: 14px;
        line-height: 1.65;
        color: #4b5563;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
      .article-info {
        margin-top: 12px;
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px 16px;
        font-size: 13px;
        .iconfont {
          color: #6e8499;
          margin-right: 0;
          font-size: 14px;
          border-radius: 999px;
          padding: 4px 8px;
          background: #f1f8ff;
        }
        .iconfont:before {
          padding-right: 3px;
        }
        .edit-btn {
          color: #0f8fd9;
          cursor: pointer;
        }
        .edit-btn:hover {
          background: #e6f5ff;
        }
      }
    }

    .cover-wrap {
      flex: 0 0 172px;
    }

    :deep(.cover),
    :deep(img) {
      border-radius: 8px;
    }
  }
}
.article-item:hover {
  background: transparent;
}

.article-item:hover .article-item-inner {
  background: #f9fcff;
  border-color: #b8d8ee;
  box-shadow: 0 10px 24px rgba(18, 86, 133, 0.12);
  transform: translateY(-2px);
}

.article-item:hover .article-item-inner .article-body .title {
  color: #0f8fd9;
}

@media (max-width: 768px) {
  .article-item {
    padding: 8px 10px 4px;

    .article-item-inner {
      border-radius: 12px;
      padding: 16px;

      .cover-wrap {
        flex-basis: 124px;
      }

      .article-body {
        .title {
          font-size: 18px;
        }
      }
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .article-item .article-item-inner,
  .article-item .article-item-inner .article-body .title,
  .article-item .article-item-inner .article-body .user-info .link-info {
    transition: none;
  }

  .article-item:hover .article-item-inner {
    transform: none;
  }
}
</style>