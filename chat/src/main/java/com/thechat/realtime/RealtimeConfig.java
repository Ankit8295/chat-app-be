package com.thechat.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RealtimeConfig {

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    ChannelTopic realtimeChannelTopic(RealtimeProperties realtimeProperties) {
        return new ChannelTopic(realtimeProperties.channel());
    }

    @Bean
    RedisMessageListenerContainer realtimeMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RealtimeSubscriber realtimeSubscriber,
            ChannelTopic realtimeChannelTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(realtimeSubscriber, realtimeChannelTopic);
        return container;
    }
}
