package com.springrag.conversation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import com.springrag.conversation.dto.CachedMessage;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CachedMessage> memoryRedisTemplate(RedisConnectionFactory factory) {

        RedisTemplate<String, CachedMessage> template = new RedisTemplate<>();

        template.setConnectionFactory(factory);

        JacksonJsonRedisSerializer<CachedMessage> serializer = new JacksonJsonRedisSerializer<>(CachedMessage.class);

        template.setDefaultSerializer(serializer);

        return template;
    }
}