package com.evcs.order.config;

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
        
        // 使用Jackson序列化器作为value的序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
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
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new ChannelTopic(INVALIDATE_TOPIC));
        log.info("Redis message listener container configured for topic: {}", INVALIDATE_TOPIC);
        return container;
    }
    
    /**
     * 配置消息监听适配器
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(BillingPlanCacheInvalidationListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }
    
    /**
     * 配置Topic
     */
    @Bean
    public ChannelTopic billingPlanUpdateTopic() {
        return new ChannelTopic(INVALIDATE_TOPIC);
    }
}
