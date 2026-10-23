<template>
  <div class="layout-body">
    <el-container>
      <el-aside class="aside" :style="{ width: asideWidth + 'px' }">
        <div class="logo">
          <span v-if="!menuCollapse">HorizonHub管理后台</span>
        </div>
        <div class="menu-panel">
          <el-menu
            :default-openeds="defaultOpeneds"
            :collapse-transition="false"
            :collapse="menuCollapse"
            class="el-menu-vertical-demo"
            default-active="2"
            background-color="#ffffff"
            text-color="#606266"
            active-text-color="#ffffff"
            router
            :defaultActive="defaultActive"
          >
            <template v-for="item in menuList" :key="item.path">
              <el-sub-menu
                :index="item.path"
                v-if="item.children"
                :key="`sub-${item.path}`"
              >
                <template #title>
                  <i :class="['iconfont', item.icon]"></i>
                  <span class="menu-name">{{ item.menuName }}</span>
                </template>
                <el-menu-item
                  :index="subItem.path"
                  v-for="subItem in item.children"
                  :key="subItem.path"
                >
                  <i
                    v-if="subItem.icon"
                    :class="['iconfont', subItem.icon, 'sub-icon']"
                  ></i>
                  <span class="menu-name">{{
                    subItem.menuName
                  }}</span></el-menu-item
                >
              </el-sub-menu>
              <el-menu-item :index="item.path" v-else :key="`item-${item.path}`">
                <i :class="['iconfont', item.icon]"></i>
                <template #title>
                  <span class="menu-name">{{ item.menuName }}</span>
                </template>
              </el-menu-item>
            </template>
          </el-menu>
        </div>
      </el-aside>
      <el-container>
        <el-header class="header">
          <div
            :class="[
              'op-menu',
              'iconfont',
              menuCollapse ? 'icon-expand' : 'icon-collapse',
            ]"
            @click="opMenu"
          ></div>
          <div class="menu-bread">
            <el-breadcrumb>
              <template v-for="item in menuBreadCrumbList" :key="item.path">
                <el-breadcrumb-item v-if="item.name">
                  {{ item.name }}
                </el-breadcrumb-item>
              </template>
            </el-breadcrumb>
          </div>
        </el-header>
        <el-main class="main-content">
          <div class="tag-content">
            <el-tabs
              type="border-card"
              v-model="defaultActive"
              @tab-change="tabClick"
              @edit="editTab"
            >
              <el-tab-pane
                :name="item.path"
                :label="item.menuName"
                :closable="tabList.length > 1"
                v-for="item in tabList"
                :key="item.path"
              ></el-tab-pane>
            </el-tabs>
          </div>
          <div class="content-body">
            <router-view />
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from "vue-router";
import { ref, watch } from "vue";
const router = useRouter();
const route = useRoute();
//默认选中
const defaultActive = ref();
//aside宽度
const asideWidth = ref(220);
//默认展开的菜单
const defaultOpeneds = ref([]);
//系统菜单
const menuList = [
  {
    menuName: "内容管理",
    icon: "icon-article",
    path: "/forum",
    children: [
      {
        menuName: "帖子管理",
        icon: "icon-article",
        path: "/forum/article",
      },
      {
        menuName: "评论管理",
        icon: "icon-comment",
        path: "/forum/comment",
      },
      {
        menuName: "板块管理",
        icon: "icon-empty",
        path: "/forum/board",
      },
    ],
  },
  {
    menuName: "用户管理",
    icon: "icon-user",
    path: "/user",
    children: [
      {
        menuName: "用户列表",
        icon: "icon-user",
        path: "/user/list",
      },
    ],
  },
  {
    menuName: "设置",
    icon: "icon-settings",
    path: "/settings",
    children: [
      {
        menuName: "系统设置",
        icon: "icon-settings",
        path: "/settings/sys",
      },
    ],
  },
];
//菜单转换为map
const menuMap = {};
const init = () => {
  menuList.forEach((item) => {
    defaultOpeneds.value.push(item.path);
    item.children.forEach((subItem) => {
      menuMap[subItem.path] = subItem;
    });
  });
};

init();
//收起关闭菜单
const menuCollapse = ref(false);
const opMenu = () => {
  menuCollapse.value = !menuCollapse.value;
  if (menuCollapse.value) {
    asideWidth.value = 63;
  } else {
    asideWidth.value = 250;
  }
};
//菜单面包屑
const menuBreadCrumbList = ref([]);

//tab操作
const tabList = ref([]);
const tabClick = (e) => {
  router.push(e);
};
const editTab = (targetKey, action) => {
  if (action !== "remove") {
    return;
  }
  let curlPath = defaultActive.value;
  let tabs = tabList.value;
  if (targetKey == defaultActive.value) {
    tabs.forEach((tab, index) => {
      if (tab.path === targetKey) {
        //如果删除的不是当前选中的tag，那么选中下一个或者前一个
        let nextTab = tabs[index + 1] || tabs[index - 1];
        if (nextTab) {
          curlPath = nextTab.path;
        }
      }
    });
  }
  tabList.value = tabs.filter((tab) => tab.path !== targetKey);
  if (curlPath !== defaultActive.value) {
    router.push(curlPath);
  }
};

watch(
  () => route,
  (newVal, oldVal) => {
    defaultActive.value = route.path;
    menuBreadCrumbList.value = route.matched;
    let currentMenu = tabList.value.find((item) => {
      return item.path == defaultActive.value;
    });
    if (!currentMenu) {
      tabList.value.push(menuMap[defaultActive.value]);
    }
  },
  { immediate: true, deep: true }
);
</script>

<style lang="scss">
//菜单收起样式
.el-popper {
  border: none !important;
  .el-menu-item.is-active {
    background: var(--el-color-primary) !important;
    color: #fff !important;
  }
  .el-menu-item:hover {
    color: var(--el-color-primary);
  }
  .el-menu--popup {
    padding: 0px;
  }
}
.layout-body {
  .aside {
    background: #ffffff;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
    transition: width 0.3s ease;
    z-index: 10;

    .logo {
      display: flex;
      height: 60px;
      color: #333;
      background: #ffffff;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      font-weight: bold;
      border-bottom: 1px solid #f0f0f0;
      letter-spacing: 1px;
    }

    .menu-panel {
      height: calc(100vh - 60px);
      overflow-y: auto;

      .menu-name {
        padding-left: 10px;
        font-weight: 500;
      }

      //去除边框
      .el-menu {
        border-right: none;
        background: transparent;
      }
      //每个菜单的颜色
      .el-menu-item {
        background: #ffffff;
        color: #606266;
        margin: 4px 8px;
        border-radius: 8px;
        height: 48px;
        line-height: 48px;
        transition: all 0.3s ease;

        .sub-icon {
          font-size: 14px;
          margin-right: 2px;
          opacity: 0.7;
        }
      }
      //选中的颜色
      .el-menu-item.is-active {
        color: #fff;
        background: var(--el-color-primary);
        box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
      }
      //鼠标移上去的颜色
      .el-menu-item:hover:not(.is-active) {
        background: #f5f7fa;
        color: var(--el-color-primary);
      }

      .el-sub-menu__title {
        height: 50px;
        line-height: 50px;
        color: #333;
        font-weight: bold;
        margin: 4px 8px;
        border-radius: 8px;
        &:hover {
          background: #f5f7fa;
          color: var(--el-color-primary);
        }
      }
    }
  }
  .header {
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    height: 60px;
    line-height: 60px;
    padding: 0px 20px !important;
    display: flex;
    align-items: center;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

    .op-menu {
      font-size: 22px;
      cursor: pointer;
      color: #606266;
      transition: color 0.3s;
      &:hover {
        color: var(--el-color-primary);
      }
    }
    .menu-bread {
      margin-left: 20px;

      .el-breadcrumb__inner {
        font-weight: normal;
      }
    }
  }
  .main-content {
    padding: 0px;
    background: #f4f6f8;

    .tag-content {
      padding: 10px 10px 0 10px;
      .el-tabs--border-card {
        border: none;
        background: transparent;
        box-shadow: none;

        .el-tabs__header {
          background-color: transparent;
          border-bottom: none;
        }

        .el-tabs__item {
          background: #fff;
          border: 1px solid #e4e7ed;
          border-radius: 4px 4px 0 0;
          margin-right: 5px;
          height: 36px;
          line-height: 36px;
          color: #606266;
          transition: all 0.3s;

          &.is-active {
            background: #fff;
            border-bottom-color: #fff;
            color: var(--el-color-primary);
          }

          &:hover {
            color: var(--el-color-primary);
          }
        }
      }
      .el-tabs__content {
        display: none;
      }
    }
    .content-body {
      overflow: hidden;
      padding: 15px;
      margin: 0 10px 10px 10px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);
      min-height: calc(100vh - 130px);
    }
  }
}
</style>