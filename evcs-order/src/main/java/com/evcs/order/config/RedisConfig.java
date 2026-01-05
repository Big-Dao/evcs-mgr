package com.evcs.order.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.evcs.order.listener.BillingPlanCacheInvalidationListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 配置Redis消息监听器和RedisTemplate
 * 
 * @author EVCS Team
 * @since M4 - Week 4 Performance Optimization
 */
@Slf4j
@Configuration
@Profile("!test")  // 测试环境排除此配置
public class RedisConfig {
    
    private static final String INVALIDATE_TOPIC = "billing:plan:update";
    
    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用String序列化器作为key的序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // 配置ObjectMapper以支持Java 8日期时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启用默认类型信息，以便反序列化时能恢复具体类型
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        
        // 使用Jackson序列化器作为value的序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        log.info("RedisTemplate configured successfully");
        return template;
    }
    
    /**
     * 配置Redis消息监听器容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(java.util.Objects.requireNonNull(connectionFactory, "connectionFactory must not be null"));
        container.addMessageListener(
            java.util.Objects.requireNonNull(listenerAdapter, "listenerAdapter must not be null"), 
            new ChannelTopic(INVALIDATE_TOPIC)
        );
        log.info("Redis message listener container configured for topic: {}", INVALIDATE_TOPIC);
        return container;
    }
    
    /**
     * 配置消息监听适配器
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(BillingPlanCacheInvalidationListener listener) {
        return new MessageListenerAdapter(
            java.util.Objects.requireNonNull(listener, "listener must not be null"), 
            "onMessage"
        );
    }
    
    /**
     * 配置Topic
     */
    @Bean
    public ChannelTopic billingPlanUpdateTopic() {
        return new ChannelTopic(INVALIDATE_TOPIC);
    }
}
