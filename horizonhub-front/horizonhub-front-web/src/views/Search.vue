<template>
  <div
    class="container-body search-body"
    :style="{ width: proxy.globalInfo.bodyWidth + 'px' }"
  >
    <div class="search-header" v-if="keyword">
      <span class="search-keyword">"{{ keyword }}"</span> 的搜索结果
    </div>
    <div class="article-list">
      <DataList
        :loading="loading"
        :dataSource="articleListInfo"
        @loadData="loadData"
        noDataMsg="没有发现帖子，赶紧发布一个吧"
      >
        <template #default="{ data }">
          <ArticleListItem
            :data="data"
            :showComment="showComment"
            :htmlTitle="true"
          ></ArticleListItem>
        </template>
      </DataList>
    </div>
  </div>
</template>

<script setup>
import ArticleListItem from "@/views/forum/ArticleListItem.vue";
import { ref, getCurrentInstance, watch, computed } from "vue";
import { useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
import { useStore } from "vuex";
const route = useRoute();
const store = useStore();

const api = {
  loadArticle: "/forum/search",
};

// 从URL获取搜索关键词
const keyword = computed(() => {
  return (route.query.keyword || "").toString().trim();
});

const loading = ref(false);
const articleListInfo = ref({});

const search = async (pageNo = 1) => {
  if (!keyword.value || keyword.value.length < 3) {
    return;
  }
  loading.value = true;
  let params = {
    pageNo: pageNo,
    keyword: keyword.value,
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
  let list = result.data.list;
  list.forEach((element) => {
    element.title = element.title.replace(
      params.keyword,
      "<span style='color:red'>" + params.keyword + "</span>"
    );
  });
  articleListInfo.value = result.data;
};

// 分页加载
const loadData = () => {
  search(articleListInfo.value.pageNo);
};

const showComment = ref(false);
watch(
  () => store.state.sysSetting,
  (newVal) => {
    if (newVal) {
      showComment.value = newVal.commentOpen;
    }
  },
  { immediate: true, deep: true }
);

// 监听URL关键词变化，自动触发搜索
watch(
  () => route.query.keyword,
  (newVal) => {
    const kw = (newVal || "").toString().trim();
    if (kw && kw.length >= 3) {
      articleListInfo.value = {}; // 清空之前的结果
      search();
    }
  },
  { immediate: true }
);
</script>

<style lang="scss">
.search-body {
  background: #fff;
  padding: 10px;
  min-height: calc(100vh - 210px);
  .search-header {
    padding: 15px 10px;
    font-size: 16px;
    color: #666;
    border-bottom: 1px solid #eee;
    margin-bottom: 10px;
    .search-keyword {
      color: #409eff;
      font-weight: 500;
    }
  }
}
</style>