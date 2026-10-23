<template>
  <!-- 悬浮 AI 助手：右下角气泡按钮 + 对话面板 -->
  <div class="ai-assistant">
    <!-- 对话面板 -->
    <transition name="ai-panel">
      <div v-if="visible" class="ai-panel">
        <!-- 头部 -->
        <div class="ai-header">
          <div class="ai-title-wrap">
            <div class="ai-avatar">
              <span class="ai-avatar-icon">H</span>
              <span class="ai-pulse"></span>
            </div>
            <div class="ai-title-text">
              <div class="ai-title">小H 助手</div>
              <div class="ai-subtitle">
                <span class="ai-dot" :class="{ on: online }"></span>
                {{ online ? "在线 · 随时为你服务" : "正在连接…" }}
              </div>
            </div>
          </div>
          <div class="ai-head-actions">
            <button class="ai-head-btn" type="button" @click="newChat" title="新对话" aria-label="新对话">
              <span class="iconfont icon-add"></span>
            </button>
            <button class="ai-head-btn" type="button" @click="visible = false" aria-label="关闭">
              <span class="iconfont icon-close"></span>
            </button>
          </div>
        </div>

        <!-- 消息区 -->
        <div class="ai-messages" ref="messagesRef">
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="ai-msg"
            :class="msg.role"
          >
            <div v-if="msg.role === 'assistant'" class="ai-msg-avatar">H</div>
            <div class="ai-msg-bubble">
              <span v-if="msg.role === 'assistant' && msg.content === '' && msg.loading" class="ai-typing">
                <i></i><i></i><i></i>
              </span>
              <span v-else v-html="renderMd(msg.content)"></span>
            </div>
          </div>
        </div>

        <!-- 未登录提示 -->
        <div v-if="!userInfo.userId" class="ai-guest-hint">
          <span class="iconfont icon-add"></span>
          <span>游客模式可用，<a href="javascript:void(0)" @click="goLogin">登录</a>体验更佳</span>
        </div>

        <!-- 输入区 -->
        <div class="ai-input-area">
          <textarea
            ref="inputRef"
            v-model.trim="inputText"
            class="ai-input"
            rows="1"
            placeholder="问问论坛的帖子、板块…（Enter 发送，Shift+Enter 换行）"
            @keydown.enter.exact.prevent="send"
            @input="autoGrow"
          ></textarea>
          <button
            class="ai-send"
            type="button"
            :disabled="!inputText || streaming"
            @click="send"
          >
            <span v-if="!streaming" class="iconfont icon-search"></span>
            <span v-else class="ai-send-loading"></span>
          </button>
        </div>

        <!-- 快捷问题 -->
        <div v-if="messages.length <= 1" class="ai-quick">
          <button v-for="q in quickQuestions" :key="q" type="button" @click="askQuick(q)">{{ q }}</button>
        </div>
      </div>
    </transition>

    <!-- 悬浮按钮 -->
    <button
      class="ai-fab"
      type="button"
      :class="{ hidden: visible }"
      @click="visible = true"
      aria-label="打开 AI 助手"
    >
      <span class="ai-fab-icon">H</span>
      <span class="ai-fab-ring"></span>
      <span class="ai-fab-tip">问问我？</span>
    </button>
  </div>
</template>

<script setup>
import { ref, nextTick, getCurrentInstance, watch, onMounted } from "vue";
import { useStore } from "vuex";

const store = useStore();
const { proxy } = getCurrentInstance();

const visible = ref(false);
const inputText = ref("");
const streaming = ref(false);
const online = ref(false);
const messagesRef = ref();
const inputRef = ref();

// 用户登录态（从 store 取，Layout 已维护）
const userInfo = ref({});
watch(
  () => store.state.loginUserInfo,
  (v) => {
    userInfo.value = v || {};
  },
  { immediate: true }
);

const goLogin = () => {
  store.commit("showLogin", true);
};

// 消息列表
const messages = ref([
  {
    role: "assistant",
    content: "你好呀，我是 HorizonHub 的小H 🌊\n可以帮你找帖子、了解板块。有什么想问的？",
    loading: false,
  },
]);

// 会话 ID：持久化在 localStorage，使 AI 能记住跨轮上下文（多轮记忆）
const CONV_KEY = "ai_conversation_id";
const getConversationId = () => {
  let id = localStorage.getItem(CONV_KEY);
  if (!id) {
    id = "ai-" + Date.now() + "-" + Math.random().toString(36).slice(2, 10);
    localStorage.setItem(CONV_KEY, id);
  }
  return id;
};
// 开始新对话：重置会话 ID，清空本地消息历史
const newChat = () => {
  localStorage.removeItem(CONV_KEY);
  messages.value = [
    { role: "assistant", content: "已开始新对话，有什么想问的？", loading: false },
  ];
  inputText.value = "";
  nextTick(() => inputRef.value && inputRef.value.focus());
};

const quickQuestions = [
  "有什么热门帖子？",
  "介绍一下论坛的板块",
  "怎么发帖？",
];

// 简易 markdown 渲染（加粗、列表、换行），避免引依赖
const renderMd = (text) => {
  if (!text) return "";
  let html = text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
  html = html.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  html = html.replace(/^- (.+)$/gm, "• $1");
  html = html.replace(/\n/g, "<br/>");
  return html;
};

const autoGrow = () => {
  nextTick(() => {
    const el = inputRef.value;
    if (el) {
      el.style.height = "auto";
      el.style.height = Math.min(el.scrollHeight, 100) + "px";
    }
  });
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
};

const askQuick = (q) => {
  inputText.value = q;
  send();
};

const send = () => {
  const text = inputText.value;
  if (!text || streaming.value) return;

  messages.value.push({ role: "user", content: text });
  inputText.value = "";
  autoGrow();
  scrollToBottom();

  // 占位一条 assistant 消息，流式填充
  const reply = { role: "assistant", content: "", loading: true };
  messages.value.push(reply);
  scrollToBottom();

  streaming.value = true;
  streamChat(text, reply);
};

// SSE 流式调用
const streamChat = (message, reply) => {
  const url = `/api/ai/chat?message=${encodeURIComponent(message)}&conversationId=${encodeURIComponent(getConversationId())}`;
  const source = new EventSource(url);

  source.onopen = () => {
    online.value = true;
  };

  source.onmessage = (e) => {
    reply.loading = false;
    // 错误标记
    if (e.data && e.data.startsWith("[ERROR]")) {
      reply.content = e.data.replace("[ERROR]", "").trim();
      source.close();
      streaming.value = false;
      scrollToBottom();
      return;
    }
    reply.content += e.data;
    scrollToBottom();
  };

  source.onerror = () => {
    source.close();
    streaming.value = false;
    online.value = false;
    if (reply.loading) {
      reply.loading = false;
      reply.content = "连接中断，请稍后重试。如果持续失败，可能是 AI 助手未配置。";
      scrollToBottom();
    }
  };

  // 没有专门的 done 事件，靠服务端关闭连接（SseEmitter.complete）
  // onerror 会触发（EventSource 在服务端关闭后会重连，这里 close 掉避免重连）
  // 用一个标志区分"正常结束"和"出错"：onmessage 收到 [DONE] 或连接自然结束
};

onMounted(() => {
  // 首次打开时聚焦输入框
  watch(visible, (v) => {
    if (v) {
      nextTick(() => inputRef.value && inputRef.value.focus());
    }
  });
});
</script>

<style lang="scss" scoped>
.ai-assistant {
  --ai-cyan: #0891b2;
  --ai-cyan-light: #67e8f9;
  --ai-blue: #3b82f6;
  --ai-violet: #8b5cf6;
  --ai-bg: rgba(255, 255, 255, 0.78);
  --ai-border: rgba(143, 193, 230, 0.5);
  --ai-text: #2a455d;
  --ai-text-soft: #5f7d96;
  z-index: 2000;
}

/* ===== 悬浮按钮 ===== */
.ai-fab {
  position: fixed;
  right: 28px;
  bottom: 90px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, var(--ai-cyan) 0%, var(--ai-blue) 100%);
  box-shadow: 0 10px 28px rgba(8, 145, 178, 0.42),
    0 2px 8px rgba(59, 130, 246, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.3s ease, opacity 0.3s ease;
  z-index: 2000;
  animation: ai-float 3s ease-in-out infinite;

  &.hidden {
    opacity: 0;
    pointer-events: none;
    transform: scale(0.5) translateY(20px);
  }

  &:hover {
    transform: translateY(-4px) scale(1.06);
    box-shadow: 0 16px 36px rgba(8, 145, 178, 0.52);
    .ai-fab-tip {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

.ai-fab-icon {
  color: #fff;
  font-size: 26px;
  font-weight: 800;
  font-family: "Poppins", sans-serif;
  z-index: 2;
}

.ai-fab-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid var(--ai-cyan-light);
  opacity: 0;
  animation: ai-pulse-ring 2.4s ease-out infinite;
}

.ai-fab-tip {
  position: absolute;
  right: 72px;
  top: 50%;
  transform: translate(8px, -50%);
  background: var(--ai-text);
  color: #fff;
  font-size: 12px;
  padding: 5px 11px;
  border-radius: 12px;
  white-space: nowrap;
  opacity: 0;
  transition: all 0.3s ease;
  pointer-events: none;
  &::after {
    content: "";
    position: absolute;
    right: -4px;
    top: 50%;
    transform: translateY(-50%);
    border: 5px solid transparent;
    border-left-color: var(--ai-text);
  }
}

@keyframes ai-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
@keyframes ai-pulse-ring {
  0% { opacity: 0.7; transform: scale(0.9); }
  100% { opacity: 0; transform: scale(1.4); }
}

/* ===== 对话面板 ===== */
.ai-panel {
  position: fixed;
  right: 28px;
  bottom: 90px;
  width: 380px;
  height: 540px;
  background: var(--ai-bg);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--ai-border);
  border-radius: 22px;
  box-shadow: 0 24px 60px rgba(15, 97, 127, 0.22),
    0 4px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 2000;
  font-family: "Noto Sans SC", "PingFang SC", sans-serif;
}

.ai-panel-enter-active, .ai-panel-leave-active {
  transition: all 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.ai-panel-enter-from, .ai-panel-leave-to {
  opacity: 0;
  transform: translateY(24px) scale(0.92);
  transform-origin: bottom right;
}

/* 头部 */
.ai-header {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(8, 145, 178, 0.92), rgba(59, 130, 246, 0.92));
  color: #fff;
}

.ai-title-wrap {
  display: flex;
  align-items: center;
  gap: 11px;
}

.ai-avatar {
  position: relative;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 1.5px solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-family: "Poppins", sans-serif;
  font-size: 18px;
}

.ai-avatar-icon { position: relative; z-index: 2; }

.ai-pulse {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.5);
  animation: ai-pulse-ring 2s ease-out infinite;
}

.ai-title-text { line-height: 1.3; }
.ai-title { font-size: 15px; font-weight: 700; letter-spacing: 0.3px; }
.ai-subtitle {
  font-size: 11px;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
}

.ai-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fca5a5;
  &.on {
    background: #86efac;
    box-shadow: 0 0 6px #86efac;
  }
}

.ai-head-actions {
  display: flex;
  gap: 8px;
}

.ai-head-btn {
  appearance: none;
  border: none;
  background: rgba(255, 255, 255, 0.18);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  .iconfont { font-size: 12px; }
  &:hover { background: rgba(255, 255, 255, 0.35); }
}

/* 消息区 */
.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scroll-behavior: smooth;
  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb {
    background: rgba(143, 193, 230, 0.5);
    border-radius: 3px;
  }
}

.ai-msg {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  animation: ai-msg-in 0.3s ease;
}
.ai-msg.user { flex-direction: row-reverse; }

@keyframes ai-msg-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-msg-avatar {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--ai-cyan), var(--ai-blue));
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  font-family: "Poppins", sans-serif;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-msg-bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13.5px;
  line-height: 1.65;
  word-break: break-word;
}

.ai-msg.assistant .ai-msg-bubble {
  background: #fff;
  color: var(--ai-text);
  border: 1px solid var(--ai-border);
  border-bottom-left-radius: 4px;
}

.ai-msg.user .ai-msg-bubble {
  background: linear-gradient(135deg, var(--ai-cyan), var(--ai-blue));
  color: #fff;
  border-bottom-right-radius: 4px;
}

/* 打字指示器 */
.ai-typing {
  display: inline-flex;
  gap: 4px;
  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--ai-cyan);
    animation: ai-bounce 1.2s infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}
@keyframes ai-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-5px); opacity: 1; }
}

/* 游客提示 */
.ai-guest-hint {
  padding: 7px 16px;
  font-size: 11.5px;
  color: var(--ai-text-soft);
  background: rgba(103, 232, 249, 0.12);
  display: flex;
  align-items: center;
  gap: 6px;
  a { color: var(--ai-cyan); font-weight: 600; }
}

/* 输入区 */
.ai-input-area {
  padding: 10px 12px 12px;
  display: flex;
  gap: 8px;
  align-items: flex-end;
  border-top: 1px solid var(--ai-border);
  background: rgba(247, 251, 255, 0.6);
}

.ai-input {
  flex: 1;
  resize: none;
  border: 1px solid var(--ai-border);
  border-radius: 16px;
  padding: 9px 13px;
  font-size: 13.5px;
  line-height: 1.5;
  max-height: 100px;
  outline: none;
  font-family: inherit;
  background: #fff;
  color: var(--ai-text);
  transition: border-color 0.2s, box-shadow 0.2s;
  &::placeholder { color: #9bb3c7; }
  &:focus {
    border-color: var(--ai-cyan);
    box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.12);
  }
}

.ai-send {
  appearance: none;
  border: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--ai-cyan), var(--ai-blue));
  color: #fff;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, opacity 0.2s;
  .iconfont { font-size: 15px; }
  &:hover:not(:disabled) { transform: scale(1.08); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.ai-send-loading {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: ai-spin 0.7s linear infinite;
}
@keyframes ai-spin { to { transform: rotate(360deg); } }

/* 快捷问题 */
.ai-quick {
  padding: 0 14px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  background: rgba(247, 251, 255, 0.6);
  button {
    appearance: none;
    border: 1px solid var(--ai-border);
    background: #fff;
    color: var(--ai-text-soft);
    padding: 5px 11px;
    border-radius: 13px;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
    &:hover {
      color: var(--ai-cyan);
      border-color: var(--ai-cyan-light);
      background: rgba(8, 145, 178, 0.06);
    }
  }
}

/* 响应式 */
@media (max-width: 600px) {
  .ai-panel {
    right: 0;
    left: 0;
    bottom: 0;
    width: 100%;
    height: 80vh;
    border-radius: 22px 22px 0 0;
  }
  .ai-fab { right: 16px; bottom: 80px; }
}
</style>
