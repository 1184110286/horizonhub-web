package com.nlizzard.horizonhub.ai.memory;

/**
 * 对话记忆的扁平存储结构。
 * <p>
 * 不直接 JSON 化 Spring AI 的 {@code Message} 对象（含枚举、元数据，反序列化脆弱），
 * 而是落成 {@code type + content} 的稳定结构，读取时按消息类型还原为对应 Message 实现。
 * 这样记忆格式不受 Spring AI 内部 Message 演进影响。
 */
public class StoredMessage {

    /** 消息类型，取 {@code MessageType.name()}：USER / ASSISTANT / SYSTEM / TOOL */
    private String type;

    /** 消息文本 */
    private String content;

    public StoredMessage() {
    }

    public StoredMessage(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
