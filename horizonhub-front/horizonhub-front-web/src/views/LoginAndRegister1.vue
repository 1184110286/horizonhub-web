<template>
  <div>
    <Dialog
      :show="dialogConfig.show"
      :title="dialogConfig.title"
      :buttons="dialogConfig.buttons"
      width="460px"
      :showCancel="false"
      @close="closeDialog"
    >
      <el-form
        class="login-register"
        :model="formData"
        :rules="rules"
        ref="formDataRef"
      >
        <div class="auth-intro">
          <p class="auth-kicker">HorizonHub 开放论坛</p>
          <h3 v-if="opType == 1">欢迎回来</h3>
          <h3 v-else-if="opType == 0">创建你的社区账号</h3>
          <h3 v-else>安全重置密码</h3>
          <p class="auth-desc">开放分享、友善交流，从这里开始。</p>
        </div>
        <div class="auth-switch">
          <button
            type="button"
            :class="['switch-btn', opType == 1 ? 'active' : '']"
            @click="showPanel(1)"
          >
            登录
          </button>
          <button
            type="button"
            :class="['switch-btn', opType == 0 ? 'active' : '']"
            @click="showPanel(0)"
          >
            注册
          </button>
          <button
            type="button"
            :class="['switch-btn', opType == 2 ? 'active' : '']"
            @click="showPanel(2)"
          >
            重置
          </button>
        </div>
        <!--input输入-->
        <el-form-item prop="email">
          <el-input
            size="large"
            clearable
            placeholder="请输入邮箱"
            v-model="formData.email"
            maxLength="150"
          >
            <template #prefix>
              <span class="iconfont icon-account"></span>
            </template>
          </el-input>
        </el-form-item>
        <!--登录密码-->
        <el-form-item prop="password" v-if="opType == 1">
          <el-input
            :type="passwordEyeType.passwordEyeOpen ? 'text' : 'password'"
            size="large"
            placeholder="请输入密码"
            v-model="formData.password"
          >
            <template #prefix>
              <span class="iconfont icon-password"></span>
            </template>
            <template #suffix>
              <span
                @click="eyeChange('passwordEyeOpen')"
                :class="[
                  'iconfont',
                  passwordEyeType.passwordEyeOpen
                    ? 'icon-eye'
                    : 'icon-close-eye',
                ]"
              ></span>
            </template>
          </el-input>
        </el-form-item>
        <!--注册-->
        <div v-if="opType == 0 || opType == 2">
          <el-form-item prop="emailCode">
            <div class="send-emali-panel">
              <el-input
                size="large"
                placeholder="请输入邮箱验证码"
                v-model="formData.emailCode"
              >
                <template #prefix>
                  <span class="iconfont icon-checkcode"></span>
                </template>
              </el-input>
              <el-button
                class="send-mail-btn"
                type="primary"
                size="large"
                @click="getEmailCode"
                >获取验证码</el-button
              >
            </div>
            <!-- <el-popover placement="left" :width="500" trigger="click">
              <div>
                <p>1、在垃圾箱中查找邮箱验证码</p>
                <p>2、在邮箱中头像->设置->反垃圾->白名单->设置邮件地址白名单</p>
                <p>
                  3、将邮箱【laoluo@wuhancoder.com】添加到白名单不知道怎么设置？
                </p>
              </div>
              <template #reference>
                <span class="a-link" :style="{ 'font-size': '14px' }"
                  >未收到邮箱验证码？</span
                >
              </template>
            </el-popover> -->
          </el-form-item>
          <el-form-item prop="nickName" v-if="opType == 0">
            <el-input
              size="large"
              clearable
              placeholder="请输入昵称"
              v-model="formData.nickName"
              maxLength="20"
            >
              <template #prefix>
                <span class="iconfont icon-account"></span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="registerPassword">
            <el-input
              :type="
                passwordEyeType.registerPasswordEyeOpen ? 'text' : 'password'
              "
              size="large"
              placeholder="请输入密码"
              v-model="formData.registerPassword"
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
              <template #suffix>
                <span
                  @click="eyeChange('registerPasswordEyeOpen')"
                  :class="[
                    'iconfont',
                    passwordEyeType.registerPasswordEyeOpen
                      ? 'icon-eye'
                      : 'icon-close-eye',
                  ]"
                ></span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="reRegisterPassword">
            <el-input
              :type="
                passwordEyeType.reRegisterPasswordEyeOpen ? 'text' : 'password'
              "
              size="large"
              placeholder="请再次输入密码"
              v-model="formData.reRegisterPassword"
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
              <template #suffix>
                <span
                  @click="eyeChange('reRegisterPasswordEyeOpen')"
                  :class="[
                    'iconfont',
                    passwordEyeType.reRegisterPasswordEyeOpen
                      ? 'icon-eye'
                      : 'icon-close-eye',
                  ]"
                ></span>
              </template>
            </el-input>
          </el-form-item>
        </div>
        <el-form-item prop="checkCode">
          <div class="check-code-panel">
            <el-input
              size="large"
              placeholder="请输入验证码"
              v-model="formData.checkCode"
              @keyup.enter="doSubmit"
            >
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
            <img
              :src="checkCodeUrl"
              class="check-code"
              @click="changeCheckCode(0)"
            />
          </div>
        </el-form-item>
        <el-form-item v-if="opType == 1">
          <div class="rememberme-panel">
            <el-checkbox v-model="formData.rememberMe">记住我</el-checkbox>
          </div>
          <div class="no-account">
            <a href="javascript:void(0)" class="a-link" @click="showPanel(2)"
              >忘记密码？</a
            >
            <a href="javascript:void(0)" class="a-link" @click="showPanel(0)"
              >没有账号？</a
            >
          </div>
        </el-form-item>
        <el-form-item v-if="opType == 0">
          <a href="javascript:void(0)" class="a-link" @click="showPanel(1)"
            >已有账号?</a
          >
        </el-form-item>
        <el-form-item v-if="opType == 2">
          <a href="javascript:void(0)" class="a-link" @click="showPanel(1)"
            >去登录?</a
          >
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="op-btn" @click="doSubmit">
            <span v-if="opType == 0">注册</span>
            <span v-if="opType == 1">登录</span>
            <span v-if="opType == 2">重置密码</span>
          </el-button>
        </el-form-item>
      </el-form>
    </Dialog>
    <!--发送邮箱验证码-->
    <Dialog
      :show="dialogConfig4SendMailCode.show"
      :title="dialogConfig4SendMailCode.title"
      :buttons="dialogConfig4SendMailCode.buttons"
      width="500px"
      :showCancel="false"
      @close="dialogConfig4SendMailCode.show = false"
    >
      <el-form
        :model="formData4SendMailCode"
        :rules="rules"
        ref="formData4SendMailCodeRef"
        label-width="80px"
      >
        <el-form-item label="邮箱">
          {{ formData.email }}
        </el-form-item>
        <el-form-item label="验证码" prop="checkCode">
          <div class="check-code-panel">
            <el-input
              size="large"
              placeholder="请输入验证码"
              v-model="formData4SendMailCode.checkCode"
            >
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
            <img
              :src="checkCodeUrl4SendMailCode"
              class="check-code"
              @click="changeCheckCode(1)"
            />
          </div>
        </el-form-item>
      </el-form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useStore } from "vuex";
import md5 from "js-md5";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const store = useStore();
const api = {
  checkCode: "/api/checkCode",
  sendMailCode: "/sendEmailCode",
  register: "/register",
  login: "/login",
  resetPwd: "/resetPwd",
};

// 0:注册 1:登录 2:重置密码
const opType = ref();
const showPanel = (type) => {
  opType.value = type;
  resetForm();
};
defineExpose({ showPanel });

//验证码
const checkCodeUrl = ref(api.checkCode);
const checkCodeUrl4SendMailCode = ref(api.checkCode);
const changeCheckCode = (type) => {
  if (type == 0) {
    checkCodeUrl.value =
      api.checkCode + "?type=" + type + "&time=" + new Date().getTime();
  } else {
    checkCodeUrl4SendMailCode.value =
      api.checkCode + "?type=" + type + "&time=" + new Date().getTime();
  }
};

//密码显示隐藏操作
const passwordEyeType = reactive({
  passwordEyeOpen: false,
  registerPasswordEyeOpen: false,
  reRegisterPasswordEyeOpen: false,
});
const eyeChange = (type) => {
  passwordEyeType[type] = !passwordEyeType[type];
};

//发送邮箱验证码弹窗
const formData4SendMailCode = ref({});
const formData4SendMailCodeRef = ref();
const dialogConfig4SendMailCode = reactive({
  show: false,
  title: "发送邮箱验证码",
  buttons: [
    {
      type: "primary",
      text: "发送验证码",
      click: () => {
        sendEmailCode();
      },
    },
  ],
});
//获取邮箱验证码
const getEmailCode = () => {
  formDataRef.value.validateField("email", (valid) => {
    if (!valid) {
      return;
    }
    dialogConfig4SendMailCode.show = true;

    nextTick(() => {
      changeCheckCode(1);
      formData4SendMailCodeRef.value.resetFields();
      formData4SendMailCode.value = {
        email: formData.value.email,
      };
    });
  });
};
//发送邮件
const sendEmailCode = () => {
  formData4SendMailCodeRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    const params = Object.assign({}, formData4SendMailCode.value);
    params.type = opType.value == 0 ? 0 : 1;
    let result = await proxy.Request({
      url: api.sendMailCode,
      params: params,
      errorCallback: () => {
        changeCheckCode(1);
      },
    });
    if (!result) {
      return;
    }
    proxy.Message.success("验证码发送成功，请登录邮箱查看");
    dialogConfig4SendMailCode.show = false;
  });
};

//登录，注册 弹出配置
const dialogConfig = reactive({
  show: false,
  title: "标题",
});

const checkRePassword = (rule, value, callback) => {
  if (value !== formData.value.registerPassword) {
    callback(new Error(rule.message));
  } else {
    callback();
  }
};
const formData = ref({});
const formDataRef = ref();
const rules = {
  email: [
    { required: true, message: "请输入邮箱" },
    { validator: proxy.Verify.email, message: "请输入正确的邮箱" },
  ],
  password: [{ required: true, message: "请输入密码" }],
  emailCode: [{ required: true, message: "请输入邮箱验证码" }],
  nickName: [{ required: true, message: "请输入昵称" }],
  registerPassword: [
    { required: true, message: "请输入密码" },
    {
      validator: proxy.Verify.password,
      message: "密码只能是数字，字母，特殊字符 8-18位",
    },
  ],
  reRegisterPassword: [
    { required: true, message: "请再次输入密码" },
    {
      validator: checkRePassword,
      message: "两次输入的密码不一致",
    },
  ],
  checkCode: [{ required: true, message: "请输入图片验证码" }],
};

//重置表单
const resetForm = () => {
  dialogConfig.show = true;
  if (opType.value == 0) {
    dialogConfig.title = "注册";
  } else if (opType.value == 1) {
    dialogConfig.title = "登录";
  } else if (opType.value == 2) {
    dialogConfig.title = "重置密码";
  }
  nextTick(() => {
    changeCheckCode(0);
    formDataRef.value.resetFields();
    formData.value = {};

    //登录
    if (opType.value == 1) {
      const cookieLoginInfo = proxy.VueCookies.get("loginInfo");
      if (cookieLoginInfo) {
        formData.value = cookieLoginInfo;
      }
    }
  });
};

// 登录、注册、重置密码  提交表单
const doSubmit = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {};
    Object.assign(params, formData.value);
    //注册
    if (opType.value == 0 || opType.value == 2) {
      params.password = params.registerPassword;
      delete params.registerPassword;
      delete params.reRegisterPassword;
    }
    //登录
    if (opType.value == 1) {
      let cookieLoginInfo = proxy.VueCookies.get("loginInfo");
      let cookiePassword =
        cookieLoginInfo == null ? null : cookieLoginInfo.password;
      if (params.password !== cookiePassword) {
        params.password = md5(params.password);
      }
    }
    let url = null;
    if (opType.value == 0) {
      url = api.register;
    } else if (opType.value == 1) {
      url = api.login;
    } else if (opType.value == 2) {
      url = api.resetPwd;
    }
    let result = await proxy.Request({
      url: url,
      params: params,
      errorCallback: () => {
        changeCheckCode(0);
      },
    });
    if (!result) {
      return;
    }
    //注册返回
    if (opType.value == 0) {
      proxy.Message.success("注册成功,请登录");
      showPanel(1);
    } else if (opType.value == 1) {
      //登录
      if (params.rememberMe) {
        const loginInfo = {
          email: params.email,
          password: params.password,
          rememberMe: params.rememberMe,
        };
        proxy.VueCookies.set("loginInfo", loginInfo, "7d");
      } else {
        proxy.VueCookies.remove("loginInfo");
      }
      dialogConfig.show = false;
      proxy.Message.success("登录成功");
      store.commit("updateLoginUserInfo", result.data);
    } else if (opType.value == 2) {
      //重置密码
      proxy.Message.success("重置密码成功,请登录");
      showPanel(1);
    }
  });
};

const closeDialog = () => {
  dialogConfig.show = false;
  store.commit("showLogin", false);
};
</script>

<style lang="scss">
:deep(.cust-dialog .el-dialog) {
  border-radius: 20px;
  border: 1px solid #d2e8f6;
  box-shadow: 0 20px 44px rgba(12, 84, 130, 0.2);
  overflow: hidden;
}

:deep(.cust-dialog .el-dialog__header) {
  margin: 0;
  padding: 14px 18px;
  background: linear-gradient(120deg, #ebf9ff 0%, #e0f4fc 100%);
  border-bottom: 1px solid #d6e9f6;
}

:deep(.cust-dialog .el-dialog__title) {
  color: #164e63;
  font-weight: 700;
}

:deep(.cust-dialog .dialog-body) {
  border: none;
  background: linear-gradient(180deg, #fafdff 0%, #f4fbff 100%);
  padding: 18px;
}

.login-register {
  padding: 4px 0 2px;

  .auth-intro {
    margin-bottom: 12px;

    .auth-kicker {
      margin: 0;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.08em;
      color: #0f7490;
    }

    h3 {
      margin: 6px 0 4px;
      font-size: 24px;
      line-height: 1.25;
      color: #0f2f46;
      font-weight: 700;
    }

    .auth-desc {
      margin: 0;
      color: #426177;
      font-size: 13px;
    }
  }

  .auth-switch {
    margin-bottom: 14px;
    padding: 4px;
    border-radius: 12px;
    background: #e8f7fd;
    border: 1px solid #c9e5f3;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 4px;

    .switch-btn {
      appearance: none;
      border: none;
      background: transparent;
      border-radius: 8px;
      height: 34px;
      font-size: 13px;
      color: #44687f;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .switch-btn:hover {
      color: #0f7490;
      background: #f3fbff;
    }

    .switch-btn.active {
      background: #ffffff;
      color: #0d6787;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(14, 116, 144, 0.18);
    }
  }

  .el-form-item {
    margin-bottom: 16px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 4px 14px;
    box-shadow: 0 0 0 1px #d6e7f3 inset;
    transition: all 0.22s ease;
    background: #f8fcff;

    &:hover {
      box-shadow: 0 0 0 1px #bfd9ea inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 2px rgba(8, 145, 178, 0.18), 0 0 0 1px #0891b2 inset;
      background: #fff;
    }
  }

  :deep(.el-input__prefix) {
    .iconfont {
      font-size: 18px;
      color: #7b95a8;
    }
  }

  :deep(.el-input__suffix) {
    .iconfont {
      font-size: 16px;
      color: #7b95a8;
      cursor: pointer;
      transition: color 0.2s ease;

      &:hover {
        color: #0891b2;
      }
    }
  }

  .send-emali-panel {
    display: flex;
    width: 100%;
    justify-content: space-between;
    gap: 10px;

    .send-mail-btn {
      border-radius: 12px;
      border: none;
      background: linear-gradient(135deg, #0891b2 0%, #06b6d4 100%);
      box-shadow: 0 6px 14px rgba(8, 145, 178, 0.28);
      font-weight: 600;

      &:hover {
        background: linear-gradient(135deg, #0e7490 0%, #0891b2 100%);
      }
    }
  }

  .rememberme-panel {
    width: 100%;

    :deep(.el-checkbox__label) {
      color: #546e81;
      font-size: 14px;
    }

    :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
      background-color: #0891b2;
      border-color: #0891b2;
    }
  }

  .no-account {
    width: 100%;
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
  }

  .a-link {
    color: #0f7490;
    font-size: 14px;
    text-decoration: none;
    transition: color 0.2s ease;

    &:hover {
      color: #0b5e76;
      text-decoration: underline;
    }
  }

  .op-btn {
    width: 100%;
    height: 44px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 600;
    border: none;
    background: linear-gradient(135deg, #0891b2 0%, #06b6d4 100%);
    box-shadow: 0 8px 20px rgba(8, 145, 178, 0.34);
    transition: all 0.22s ease;

    &:hover {
      background: linear-gradient(135deg, #0e7490 0%, #0891b2 100%);
      box-shadow: 0 10px 24px rgba(8, 145, 178, 0.42);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

.check-code-panel {
  display: flex;
  width: 100%;
  gap: 10px;

  .el-input {
    flex: 1;
  }

  .check-code {
    height: 40px;
    border-radius: 10px;
    cursor: pointer;
    border: 1px solid #cfe3f1;
    transition: border-color 0.2s ease;

    &:hover {
      border-color: #0891b2;
    }
  }
}

@media (max-width: 768px) {
  .login-register .auth-intro h3 {
    font-size: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-register .auth-switch .switch-btn,
  .login-register :deep(.el-input__wrapper),
  .login-register :deep(.el-input__suffix) .iconfont,
  .login-register .send-emali-panel .send-mail-btn,
  .login-register .op-btn,
  .check-code-panel .check-code {
    transition: none;
  }
}
</style>