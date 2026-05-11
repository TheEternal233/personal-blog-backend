package xyz.kuailemao.config.rabbit;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息转换器配置（解决反序列化安全错误）
 */
@Configuration
public class RabbitMessageConverterConfig {

    /**
     * 使用 JSON 序列化消息，彻底解决反序列化报错问题
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}