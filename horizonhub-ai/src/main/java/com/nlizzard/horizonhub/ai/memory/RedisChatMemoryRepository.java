package com.nlizzard.horizonhub.ai.memory;

import com.nlizzard.horizonhub.utils.JsonUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的对话记忆仓库：把每个会话的消息历史持久化到 Redis，实现跨请求的多轮上下文。
 * <p>
 * 选型说明：Spring AI 1.0 GA 仅内置 InMemory / JDBC 两种 {@link ChatMemoryRepository}，没有官方
 * Redis 实现。本类自定义实现接口，复用项目已有的 {@link StringRedisTemplate} + {@link JsonUtils}
 * （Hutool）序列化方案，与 {@code TokenService} 的 Redis 用法保持一致。
 * <ul>
 *   <li>key 约定：{@code horizonHub:ai:memory:{conversationId}}，复用项目 key 命名风格；</li>
 *   <li>滑动过期：每次 {@link #saveAll} 都重置 TTL，活跃会话保持记忆、长期不活跃的自动清理；</li>
 *   <li>消息以扁平 {@link StoredMessage}（type + content）落库，读取时按 {@link MessageType} 还原，
 *       避免直接序列化 Message 对象（含枚举/元数据，反序列化脆弱）。</li>
 * </ul>
 * 注意：{@code saveAll} 语义是「替换该会话全部消息」，故每次写入都覆盖整条 JSON。
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisChatMemoryRepository.class);

    private static final String KEY_PREFIX = "horizonHub:ai:memory:";

    /** 会话记忆在 Redis 中的存活时间：7 天，过期自动清理，避免无限堆积 */
    private static final Duration TTL = Duration.ofDays(7);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> findConversationIds() {
        // 用 SCAN（非 KEYS）遍历会话 key，避免在大数据量下阻塞 Redis
        Set<String> keys;
        try {
            keys = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> result = new HashSet<>();
                // 用 keyCommands().scan 而非 connection.scan（后者在 Spring Data Redis 3.4+ 已过时）
                Cursor<byte[]> cursor = connection.keyCommands().scan(
                        ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(100).build());
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
                cursor.close();
                return result;
            });
        } catch (DataAccessException e) {
            logger.warn("扫描会话 ID 失败：{}", e.getMessage());
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                ids.add(key.substring(KEY_PREFIX.length()));
            }
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        } catch (DataAccessException e) {
            logger.warn("读取会话记忆失败：{}", e.getMessage());
            return List.of();
        }
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<StoredMessage> stored = JsonUtils.jsonArray2List(json, StoredMessage.class);
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>(stored.size());
        for (StoredMessage s : stored) {
            Message msg = toMessage(s);
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (conversationId == null) {
            return;
        }
        String key = KEY_PREFIX + conversationId;
        if (messages == null || messages.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }
        List<StoredMessage> stored = new ArrayList<>(messages.size());
        for (Message m : messages) {
            String text = m.getText();
            if (text == null || text.isEmpty()) {
                continue;
            }
            stored.add(new StoredMessage(m.getMessageType().name(), text));
        }
        if (stored.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }
        String json = JsonUtils.object2Json(stored);
        // 每次写入都带 TTL：滑动过期，活跃会话续期，不活跃的自动回收
        stringRedisTemplate.opsForValue().set(key, json, TTL.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (conversationId != null) {
            stringRedisTemplate.delete(KEY_PREFIX + conversationId);
        }
    }

    /**
     * 把扁平的 {@link StoredMessage} 还原为 Spring AI 的 {@link Message} 实现。
     * 类型异常（如历史脏数据）时返回 null，由调用方跳过。
     */
    private Message toMessage(StoredMessage s) {
        if (s == null || s.getType() == null) {
            return null;
        }
        MessageType type;
        try {
            type = MessageType.valueOf(s.getType());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String content = s.getContent() == null ? "" : s.getContent();
        return switch (type) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            // 工具响应消息属调用过程中的瞬态信息，记忆里简化回落为 user 文本即可
            case TOOL -> new UserMessage(content);
        };
    }
}
