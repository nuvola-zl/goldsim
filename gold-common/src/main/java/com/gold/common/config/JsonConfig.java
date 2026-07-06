package com.gold.common.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ全局JSON序列化配置
 * 所有微服务引入common后自动生效，统一使用JSON传输消息，替代JDK二进制序列化
 */
@Configuration
public class JsonConfig {

    /**
     * 消息转换器：Jackson JSON序列化
     * 发送消息自动转JSON字符串，消费时自动反序列化为实体
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    /**
     * 关键：配置监听器容器工厂，使用 JSON 反序列化
     * 这样 @RabbitListener 接收消息时才会用 Jackson 反序列化
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}