<template>
  <div class="editor-html">
    <Toolbar
      style="border-bottom: 1px solid #ccc"
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      :mode="mode"
    />
    <Editor
      :style="{ height: height + 'px', 'overflow-y': 'hidden' }"
      :model-value="modelValue"
      :defaultConfig="editorConfig"
      :mode="mode"
      @onCreated="handleCreated"
      @onChange="onChange"
    />
  </div>
</template>

<script setup>
import "@wangeditor/editor/dist/css/style.css"; // 引入 css
import { onBeforeUnmount, ref, shallowRef } from "vue";
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";
import { getCurrentInstance } from "vue";
import { useStore } from "vuex";
const store = useStore();
const { proxy } = getCurrentInstance();

const props = defineProps({
  modelValue: {
    type: String,
    default: "",
  },
  height: {
    type: Number,
    default: 500,
  },
});

const mode = ref("default");
const editorRef = shallowRef();

const toolbarConfig = {
  excludeKeys: [
    "uploadVideo", // 排除菜单组，写菜单组 key 的值即可
  ],
};

const editorConfig = {
  placeholder: "请输入内容...",
  excludeKeys: ["uploadVideo"],
  // 菜单栏配置
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 3 * 1024 * 1024,
      server: "/api/file/uploadImage",
      fieldName: "file",
      customInsert(responseData, insertFn) {
        //正常请求
        if (responseData.code == 200) {
          insertFn(
            proxy.globalInfo.imageUrl + responseData.data.fileName,
            "",
            ""
          );
          return;
        } else if (responseData.code == 901) {
          //登录超时
          store.commit("showLogin", true);
          store.commit("updateLoginUserInfo", null);
          return;
        }
        proxy.Message.error(responseData.info);
      },
    },
  },
};

const emit = defineEmits();
const onChange = (editor) => {
  emit("update:modelValue", editor.getHtml());
};

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor == null) return;
  editor.destroy();
});

const handleCreated = (editor) => {
  editorRef.value = editor; // 记录 editor 实例，重要！
};
</script>

<style lang="scss">
.editor-html {
  border: 1px solid #d6e5f1;
  border-radius: 10px;
  background: #fff;
  overflow: visible;
  position: relative;
  z-index: 20;

  :deep(.w-e-toolbar),
  :deep(.w-e-bar),
  :deep(.w-e-bar-item),
  :deep(.w-e-bar-item-group) {
    overflow: visible;
  }

  :deep(.w-e-select-list),
  :deep(.w-e-drop-panel),
  :deep(.w-e-bar-item-menus-container) {
    min-width: 136px;
    z-index: 30;
  }

  :deep(.w-e-select-list ul li) {
    padding-right: 14px;
    white-space: nowrap;
  }
}
</style>