package com.springrag.conversation.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.springrag.config.MemoryProperties;
import com.springrag.conversation.dto.CachedMessage;
import com.springrag.conversation.service.RecentMessageStore;

@Service
public class RedisRecentMessageStore implements RecentMessageStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRecentMessageStore.class);

    private final RedisTemplate<String, CachedMessage> redisTemplate;
    private final MemoryProperties memoryProperties;

    public RedisRecentMessageStore(RedisTemplate<String, CachedMessage> redisTemplate,
                                  MemoryProperties memoryProperties) {

        this.redisTemplate = redisTemplate;
        this.memoryProperties = memoryProperties;
    }

    private String buildKey(UUID conversationId) {

        return "conversation:" + conversationId + ":memory";
    }

    @Override
    public void addMessage(UUID conversationId, CachedMessage message) {

        String key = buildKey(conversationId);

        redisTemplate.opsForList().rightPush(key, message);

        int maxMessages = memoryProperties.getMaxMessages();
        Long size = redisTemplate.opsForList().size(key);

        if (size != null && size > maxMessages) {

            redisTemplate.opsForList().trim(key, size - maxMessages, size - 1);
        }

        redisTemplate.expire(key, memoryProperties.getTtl());

        log.debug("Cached message in Redis. key={}, role={}", key, message.getRole());
    }

    @Override
    public List<CachedMessage> getRecentMessages(UUID conversationId) {

        String key = buildKey(conversationId);

        List<CachedMessage> values = redisTemplate.opsForList().range(key, 0, -1);

        if (values == null || values.isEmpty()) {

            log.debug("Redis cache miss. key={}", key);

            return Collections.emptyList();
        }

        log.debug("Redis cache hit. key={}, messageCount={}", key, values.size());

        return values;
    }

    @Override
    public void clearMemory(UUID conversationId) {

        String key = buildKey(conversationId);

        redisTemplate.delete(key);

        log.info("Redis memory cleared. key={}", key);
    }
}