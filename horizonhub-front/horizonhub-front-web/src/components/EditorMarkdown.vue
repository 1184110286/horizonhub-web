<template>
  <v-md-editor
    class="editor-markdown"
    :model-value="modelValue"
    :height="height + 'px'"
    :disabled-menus="[]"
    :include-level="[1, 2, 3, 4, 5, 6]"
    @upload-image="uploadImageHandler"
    @change="change"
    @fullscreen-change="handleFullscreenChange"
  >
  </v-md-editor>
</template>

<script setup>
import VMdEditor from "@kangc/v-md-editor";
import "@kangc/v-md-editor/lib/style/base-editor.css";
import githubTheme from "@kangc/v-md-editor/lib/theme/github.js";
import "@kangc/v-md-editor/lib/theme/style/github.css";
// highlightjs
import hljs from "highlight.js";

import { getCurrentInstance, onBeforeUnmount } from "vue";
const { proxy } = getCurrentInstance();

VMdEditor.use(githubTheme, {
  Hljs: hljs,
});

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

const emit = defineEmits();
const change = (markdownContent, htmlContent) => {
  emit("update:modelValue", markdownContent);
  emit("htmlContent", htmlContent);
};

const handleFullscreenChange = (isFullscreen) => {
  if (isFullscreen) {
    document.body.classList.add("md-editor-fullscreen");
  } else {
    document.body.classList.remove("md-editor-fullscreen");
  }
};

onBeforeUnmount(() => {
  document.body.classList.remove("md-editor-fullscreen");
});

const uploadImageHandler = async (event, insertImage, files) => {
  let result = await proxy.Request({
    url: "file/uploadImage",
    params: {
      file: files[0],
    },
  });
  if (!result) {
    return;
  }
  const url = proxy.globalInfo.imageUrl + result.data.fileName;
  insertImage({
    url: url,
    desc: "图片",
  });
};
</script>

<style lang="scss">
.editor-markdown {
  border: 1px solid #d6e5f1;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(20, 74, 117, 0.08);

  :deep(.v-md-editor__toolbar) {
    border-bottom: 1px solid #dbe9f3;
    background: #f8fcff;
  }

  :deep(.v-md-editor__toolbar-item) {
    color: #4a6478;
  }

  :deep(.v-md-editor__toolbar-item:hover) {
    background: #eaf5fc;
    color: #0b7f9f;
  }

  :deep(.v-md-editor__menu) {
    border: 1px solid #d6e7f3;
    box-shadow: 0 10px 24px rgba(26, 86, 122, 0.16);
  }

  :deep(.v-md-editor__menu--panel) {
    max-height: 260px;
  }

  :deep(.v-md-editor__menu--list .v-md-editor__menu-item) {
    padding: 0 18px;
  }

  :deep(.v-md-editor--fullscreen) {
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 2200;
    border-radius: 0;
    border: none;
    box-shadow: none;
    background: #ffffff;
    overflow: hidden;
  }

  :deep(.v-md-editor--fullscreen .v-md-editor__toolbar) {
    padding: 7px 10px;
    border-bottom: 1px solid #d6e8f3;
    background: linear-gradient(180deg, #f8fcff 0%, #eff8fd 100%);
  }

  :deep(.v-md-editor--fullscreen .v-md-editor__toolbar-item) {
    color: #466278;
    opacity: 1;
  }

  :deep(
      .v-md-editor--fullscreen
        .v-md-editor__toolbar-item:not(
          .v-md-editor__toolbar-item--disabled
        ):hover
    ) {
    background: #e6f4fb;
    color: #0b7f9f;
  }

  :deep(.v-md-editor--fullscreen .v-md-editor__main) {
    height: calc(100% - 49px);
    background: #fafdff;
  }

  :deep(
      .v-md-editor--fullscreen.v-md-editor--editable
        .v-md-editor__editor-wrapper
    ) {
    border-right: 1px solid #d6e8f3;
    background: #ffffff;
  }

  :deep(
      .v-md-editor--fullscreen.v-md-editor--editable
        .v-md-editor__preview-wrapper
    ) {
    background: #f9fcff;
  }
}

@media (max-width: 768px) {
  .editor-markdown {
    :deep(.v-md-editor--fullscreen) {
      border: none;
      border-radius: 0;
      box-shadow: none;
    }

    :deep(.v-md-editor--fullscreen .v-md-editor__main) {
      height: calc(100% - 48px);
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .editor-markdown {
    :deep(.v-md-editor__toolbar-item) {
      transition: none;
    }
  }
}
</style>