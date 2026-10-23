<template>
  <div class="login-body">
    <div class="login-wrapper">
      <div class="info-panel">
        <h1 class="logo-title">HorizonHub</h1>
        <p class="desc">
          新一代高质量交互论坛系统。为开发者和创作者提供沉浸式的交流体验与内容创作空间。
        </p>
        <div class="feature-list">
          <div class="feature-item"><span>✔️</span> 内容实时同步分享</div>
          <div class="feature-item"><span>✔️</span> 纯净的社区交流环境</div>
          <div class="feature-item"><span>✔️</span> 高效直观的后台管理</div>
        </div>
      </div>
      <div class="login-panel">
        <div class="login-title">管理员登录</div>
        <el-form :model="formData" :rules="rules" ref="formDataRef">
          <el-form-item prop="account">
            <el-input
              placeholder="请输入账号"
              v-model="formData.account"
              size="large"
            >
              <template #prefix>
                <span class="iconfont icon-account"></span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              type="password"
              placeholder="请输入密码"
              v-model="formData.password"
              size="large"
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="checkCode">
            <div class="check-code-panel">
              <el-input
                placeholder="请输入验证码"
                v-model="formData.checkCode"
                class="input-panel"
                size="large"
                @keyup.enter.native="login"
              >
                <template #prefix>
                  <span class="iconfont icon-checkcode"></span>
                </template>
              </el-input>
              <img
                :src="checkCodeUrl"
                class="check-code"
                @click="changeCheckCode"
              />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :style="{ width: '100%' }" @click="login"
              >登录</el-button
            >
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, reactive, ref } from "vue";
import { useRouter } from "vue-router";

const { proxy } = getCurrentInstance();
const router = useRouter();
const api = {
  checkCode: "api/checkCode",
  login: "/login",
};

const checkCodeUrl = ref(api.checkCode);
const changeCheckCode = () => {
  checkCodeUrl.value = api.checkCode + "?" + new Date().getTime();
};

//表单相关
const formDataRef = ref(null);
const formData = reactive({});

const rules = {
  account: [
    {
      required: true,
      message: "请输入用户名",
    },
  ],
  password: [
    {
      required: true,
      message: "请输入密码",
    },
  ],
  checkCode: [
    {
      required: true,
      message: "请输入验证码",
    },
  ],
};

const login = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    // 直接发送明文口令，服务端比对（必须走 HTTPS）
    let params = Object.assign({}, formData);
    let result = await proxy.Request({
      url: api.login,
      params: params,
      errorCallback: () => {
        changeCheckCode();
      },
    });
    if (!result) {
      return;
    }
    proxy.VueCookies.set("userInfo", result.data, 0);
    proxy.Message.success("登录成功");
    router.push("/");
  });
};
</script>

<style lang="scss">
@keyframes fadeInScale {
  0% {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes fadeInUp {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-body {
  width: 100%;
  height: 100vh;
  background-size: cover;
  background-position: center;
  background-image: url(../assets/login-bg.jpg);
  display: flex;
  align-items: center;
  justify-content: center;

  .login-wrapper {
    display: flex;
    width: 800px;
    background: rgba(255, 255, 255, 0.3);
    backdrop-filter: blur(15px);
    -webkit-backdrop-filter: blur(15px);
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.4);
    box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.2);
    overflow: hidden;
    /* 入场动画 */
    animation: fadeInScale 0.8s ease-out forwards;

    .info-panel {
      flex: 1;
      padding: 40px;
      background: linear-gradient(
        135deg,
        rgba(0, 0, 0, 0.3) 0%,
        rgba(0, 0, 0, 0.1) 100%
      );
      color: #fff;
      display: flex;
      flex-direction: column;
      justify-content: center;
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
      border-right: 1px solid rgba(255, 255, 255, 0.15);

      .logo-title {
        font-size: 38px;
        margin-bottom: 20px;
        font-weight: 800;
        letter-spacing: 1px;
        opacity: 0;
        animation: fadeInUp 0.6s ease-out 0.2s forwards;
      }
      .desc {
        font-size: 15px;
        line-height: 1.8;
        margin-bottom: 30px;
        opacity: 0.9;
        opacity: 0;
        animation: fadeInUp 0.6s ease-out 0.4s forwards;
      }
      .feature-list {
        .feature-item {
          display: flex;
          align-items: center;
          margin-bottom: 12px;
          font-size: 15px;
          opacity: 0;
          animation: fadeInUp 0.6s ease-out forwards;
          transition: transform 0.3s ease;

          &:hover {
            transform: translateX(10px);
          }

          &:nth-child(1) {
            animation-delay: 0.6s;
          }
          &:nth-child(2) {
            animation-delay: 0.7s;
          }
          &:nth-child(3) {
            animation-delay: 0.8s;
          }

          span {
            margin-right: 8px;
            font-size: 16px;
            color: #67c23a;
          }
        }
      }
    }

    .login-panel {
      width: 400px;
      padding: 40px 30px;
      flex-shrink: 0;

      .login-title {
        font-size: 28px;
        font-weight: bold;
        text-align: center;
        margin-bottom: 30px;
        color: #333;
        text-shadow: 0 1px 2px rgba(255, 255, 255, 0.5);
        opacity: 0;
        animation: fadeInUp 0.6s ease-out 0.3s forwards;
      }

      .el-form {
        .el-form-item {
          opacity: 0;
          animation: fadeInUp 0.6s ease-out forwards;

          &:nth-child(1) {
            animation-delay: 0.5s;
          }
          &:nth-child(2) {
            animation-delay: 0.6s;
          }
          &:nth-child(3) {
            animation-delay: 0.7s;
          }
          &:nth-child(4) {
            animation-delay: 0.8s;
          }
        }
      }

      .check-code-panel {
        width: 100%;
        display: flex;
        align-items: center;
        .input-panel {
          flex: 1;
          margin-right: 15px;
        }
        .check-code {
          cursor: pointer;
          border-radius: 4px;
          height: 40px;
          transition: transform 0.3s ease, box-shadow 0.3s ease;
          &:hover {
            transform: scale(1.05);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
          }
        }
      }

      .el-button {
        height: 40px;
        font-size: 16px;
        border-radius: 6px;
        transition: all 0.3s ease;
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
        }
      }
    }
  }
}
</style>