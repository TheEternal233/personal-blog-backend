package xyz.kuailemao.config;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Primary;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@Configuration
public class AiConfig {


    @Configuration
    public class CommonConfiguration {

        /**
         * 聊天记忆
         */
        @Bean
        public ChatMemory chatMemory() {
            return new InMemoryChatMemory();
        }

        /**
         * 智谱AI ChatClient（已修复，无 VectorStore）
         */
        @Primary
        @Bean("zhipuAiChatClient")
        public ChatClient zhipuAiChatClient(
                ZhiPuAiChatModel model,
                ChatMemory chatMemory,
                Function<TimeRequest, String> getCurrentTime
        ) {
            return ChatClient.builder(model)
                    .defaultSystem("""
                        你的身份：一个热心、可爱的智能助手，名字叫TheEternal，必须严格以这个身份回答所有问题。
                        语言规则（强制遵守，违反则无法完成任务）：
                        1. 所有回答必须使用纯正、流畅的中文，禁止使用任何英文单词、英文句子、英文标点。
                        2. 即使用户用英文提问，也必须用中文回复，绝对不允许夹杂任何英文内容。
                        3. 回答简洁明了，避免冗余，优先使用口语化中文。
                        4. 【绝对铁律】只要用户问时间、日期、年份、月份、星期、几点、几号，
                           必须100%调用getCurrentTime工具获取真实时间！
                        """)
                    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                    .defaultFunctions("getCurrentTime")
                    .build();
        }

        /**
         * 获取当前时间函数
         */
        @Bean
        @Description("获取当前系统时间，回答用户时间相关问题")
        public Function<TimeRequest, String> getCurrentTime() {
            return request -> {

                ZoneId zoneId = ZoneId.of("Asia/Shanghai");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);
                return "当前时间：" + ZonedDateTime.now(zoneId).format(formatter);
            };
        }

        @JsonClassDescription("时间查询请求参数")
        public static class TimeRequest {
        }

    }
}