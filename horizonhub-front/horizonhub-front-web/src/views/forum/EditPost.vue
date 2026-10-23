<template>
  <div class="edit-post">
    <el-form
      :model="formData"
      :rules="rules"
      ref="formDataRef"
      class="post-panel"
      label-width="60px"
    >
      <div class="post-editor">
        <el-card :body-style="{ padding: '5px' }">
          <template #header>
            <div class="post-editor-title">
              <span>正文</span>
              <div class="change-editor-type">
                <span class="iconfont icon-change" @click="changeEditor">
                  切换为{{
                    editorType == 0 ? "markdown编辑器" : "富文本编辑器"
                  }}
                </span>
              </div>
            </div>
          </template>
          <!--input输入-->
          <el-form-item prop="content" label-width="0">
            <EditorHtml
              :height="htmlEditorHeight"
              v-if="editorType == 0"
              v-model="formData.content"
            ></EditorHtml>
            <EditorMarkdown
              :height="markdownHeight"
              v-if="editorType == 1"
              v-model="formData.markdownContent"
              @htmlContent="setHtmlContent"
            >
            </EditorMarkdown>
          </el-form-item>
        </el-card>
      </div>
      <div class="post-setting">
        <el-card :body-style="{ padding: '5px' }">
          <template #header>
            <span>设置</span>
          </template>
          <div class="setting-inner">
            <!--input输入-->
            <el-form-item label="标题" prop="title">
              <el-input
                clearable
                :maxlength="150"
                placeholder="提示信息"
                v-model="formData.title"
              ></el-input>
            </el-form-item>
            <el-form-item label="板块" prop="boardIds">
              <el-cascader
                placeholder="请选择板块"
                :options="boardList"
                :props="boardProps"
                clearable
                v-model="formData.boardIds"
                :style="{ width: '100%' }"
              />
            </el-form-item>
            <el-form-item label="封面" prop="cover">
              <CoverUpload v-model="formData.cover"></CoverUpload>
            </el-form-item>
            <!--textarea输入-->
            <el-form-item label="摘要" prop="summary">
              <el-input
                clearable
                placeholder="提示信息"
                type="textarea"
                :rows="5"
                :maxlength="200"
                resize="none"
                show-word-limit
                v-model="formData.summary"
              ></el-input>
            </el-form-item>
            <el-form-item label="附件" prop="cover">
              <AttachmentSelector
                v-model="formData.attachment"
              ></AttachmentSelector>
              当前仅支持上传zip,rar格式压缩包
            </el-form-item>
            <!--input输入-->
            <el-form-item
              label="积分"
              prop="integral"
              v-if="formData.attachment"
            >
              <el-input
                clearable
                placeholder="请输入积分"
                v-model="formData.integral"
              ></el-input>
              <span class="tips">附件下载积分，0表示无需积分下载</span>
            </el-form-item>
            <!--input输入-->
            <el-form-item label="" prop="">
              <el-button
                type="primary"
                class="save-btn"
                :style="{ width: '100%' }"
                @click="postHandler"
                >保存</el-button
              >
            </el-form-item>
          </div>
        </el-card>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, watch, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessageBox } from "element-plus";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const markdownHeight = window.innerHeight - 80 - 60;
const htmlEditorHeight = window.innerHeight - 80 - 140;

const api = {
  loadBoard: "/forum/loadBoard4Post",
  postArticle: "/forum/postArticle",
  articleDetail4Update: "/forum/articleDetail4Update",
  updateArticle: "/forum/updateArticle",
};

const articleId = ref(null);
const getArticleDetail = () => {
  nextTick(async () => {
    formDataRef.value.resetFields();
    if (articleId.value) {
      //修改
      let result = await proxy.Request({
        url: api.articleDetail4Update,
        params: {
          articleId: articleId.value,
        },
        showError: false,
        errorCallback: (response) => {
          ElMessageBox.alert(response.info, "错误", {
            "show-close": false,
            callback: (action) => {
              router.go(-1);
            },
          });
        },
      });
      if (!result) {
        return;
      }
      let articleInfo = result.data.forumArticle;
      //设置编辑器
      editorType.value = articleInfo.editorType;
      //设置板块信息
      articleInfo.boardIds = [];
      articleInfo.boardIds.push(articleInfo.pBoardId);
      if (articleInfo.boardId != null && articleInfo.boardId != 0) {
        articleInfo.boardIds.push(articleInfo.boardId);
      }
      //设置封面信息
      if (articleInfo.cover) {
        articleInfo.cover = { imageUrl: articleInfo.cover };
      }
      //设置附件
      if (result.data.attachment) {
        articleInfo.attachment = {
          name: result.data.attachment.fileName,
        };
        articleInfo.integral = result.data.attachment.integral;
      }
      formData.value = articleInfo;
    } else {
      formData.value = {};
      editorType.value = proxy.VueCookies.get("editorType") || 0;
    }
  });
};
//设置markdown编辑器的富文本信息
const setHtmlContent = (htmlContent) => {
  formData.value.content = htmlContent;
};

watch(
  () => route,
  (newVal, oldVal) => {
    if (
      newVal.path.indexOf("/editPost") != -1 ||
      newVal.path.indexOf("/newPost") != -1
    ) {
      articleId.value = newVal.params.articleId;
      getArticleDetail();
    }
  },
  { immediate: true, deep: true }
);

const formData = ref({});
const formDataRef = ref();
const rules = {
  title: [{ required: true, message: "请输入标题" }],
  boardIds: [{ required: true, message: "请选择板块" }],
  content: [{ required: true, message: "请输入正文" }],
  integral: [
    { required: true, message: "请输入下载所需积分" },
    { validator: proxy.Verify.number, message: "积分只能是数字" },
  ],
};

//提交信息
const postHandler = () => {
  // 表单校验
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {};
    Object.assign(params, formData.value);
    //设置板块ID
    if (params.boardIds.length == 1) {
      params.pBoardId = params.boardIds[0];
    } else if (params.boardIds.length == 2) {
      params.pBoardId = params.boardIds[0];
      params.boardId = params.boardIds[1];
    }
    delete params.boardIds;
    //设置编辑器类型
    params.editorType = editorType.value;
    //获取内容
    const contentText = params.content.replace(/<(?!img).*?>/g, "");
    if (contentText == "") {
      proxy.message.warning("正文不能为空");
      return;
    }
    if (params.attachment != null) {
      params.attachmentType == 1;
    } else {
      params.attachmentType = 0;
    }

    //封面
    if (!(params.cover instanceof File)) {
      delete params.cover;
    }
    //附件不是文件乐行，值设置为空
    if (!(params.attachment instanceof File)) {
      delete params.attachment;
    }

    // 有文章id表明是更新，没有表明是发表
    let result = await proxy.Request({
      url: params.articleId ? api.updateArticle : api.postArticle,
      params: params,
    });
    if (!result) {
      return;
    }
    proxy.Message.success("保存成功");
    router.push(`/post/${result.data}`);
  });
};

//板块信息
const boardProps = {
  multiple: false,
  checkStrictly: true,
  value: "boardId",
  label: "boardName",
};
const boardList = ref([]);
const loadBardList = async () => {
  let result = await proxy.Request({
    url: api.loadBoard,
  });
  if (!result) {
    return;
  }
  boardList.value = result.data;
};
loadBardList();

//编辑器类型 0：富文本  1:markdown
const editorType = ref(null);
const changeEditor = () => {
  proxy.Confirm("切换编辑器会清空正在编辑的内容，确定要切换吗？", () => {
    editorType.value = editorType.value == 0 ? 1 : 0;
    formData.value.content = "";
    formData.value.markdownContent = "";
    proxy.VueCookies.set("editorType", editorType.value, -1);
  });
};
</script>

<style lang="scss">
.edit-post {
  padding: 14px 14px 24px;
  max-width: 1400px;
  margin: 0 auto;
  background: linear-gradient(180deg, #f8fcff 0%, #f3f8fd 100%);
  border: 1px solid #e3eef7;
  border-radius: 16px;

  .post-panel {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 450px;
    align-items: start;
    gap: 15px;
    overflow: visible;

    :deep(.el-card) {
      border-radius: 12px;
      border: 1px solid #eef2f6;
      box-shadow: 0 8px 24px rgba(20, 74, 117, 0.05);
      transition: box-shadow 0.3s ease;
      overflow: visible;

      &:hover {
        box-shadow: 0 12px 32px rgba(20, 74, 117, 0.08);
      }
    }

    .el-card__header {
      padding: 15px 20px;
      background: #fcfdfe;
      border-bottom: 1px solid #f0f4f8;
      font-weight: 600;
      color: #1a365d;
      border-top-left-radius: 12px;
      border-top-right-radius: 12px;
    }

    :deep(.el-card__body) {
      overflow: visible;
    }

    .post-editor {
      flex: 1;
      position: relative;
      z-index: 10;
      .post-editor-title {
        display: flex;
        justify-content: space-between;
        align-items: center;

        span {
          font-size: 16px;
        }

        .change-editor-type {
          .iconfont {
            cursor: pointer;
            color: #0ea5e9;
            font-size: 14px;
            font-weight: normal;
            background: #f0f9ff;
            padding: 5px 12px;
            border-radius: 999px;
            transition: all 0.2s ease;

            &:hover {
              background: #e0f2fe;
              color: #0284c7;
              transform: translateY(-1px);
            }
          }
        }
      }
    }

    .post-setting {
      width: 100%;

      .setting-inner {
        max-height: calc(100vh - 140px);
        overflow-y: auto;
        padding: 5px 10px;

        &::-webkit-scrollbar {
          width: 5px;
        }
        &::-webkit-scrollbar-thumb {
          background: #cbd5e1;
          border-radius: 5px;
        }

        .el-form-item {
          align-items: flex-start;
          margin-bottom: 22px;

          :deep(.el-form-item__label) {
            font-weight: 500;
            color: #334155;
          }
        }

        .save-btn {
          margin-top: 10px;
          height: 40px;
          font-size: 15px;
          font-weight: 600;
          letter-spacing: 1px;
          border: 1px solid #0891b2;
          background: linear-gradient(120deg, #08a3c9 0%, #0b8fb5 100%);
          border-radius: 8px;
          color: #fff;
          box-shadow: 0 6px 14px rgba(8, 145, 178, 0.24);
          transition: transform 0.2s ease, box-shadow 0.2s ease,
            background 0.2s ease;

          &:hover {
            transform: translateY(-1px);
            background: linear-gradient(120deg, #0994b9 0%, #0f7b97 100%);
            box-shadow: 0 8px 18px rgba(8, 145, 178, 0.3);
          }

          &:active {
            transform: translateY(0);
            box-shadow: 0 4px 10px rgba(8, 145, 178, 0.22);
          }
        }
      }
      .tips {
        color: #94a3b8;
        font-size: 13px;
        margin-top: 4px;
        display: inline-block;
      }
    }
  }
}

@media (max-width: 1200px) {
  .edit-post {
    .post-panel {
      grid-template-columns: 1fr;
    }
  }
}
</style>