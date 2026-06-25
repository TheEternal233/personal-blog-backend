package xyz.kuailemao.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import xyz.kuailemao.utils.AiFunctionUtil;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiConfig {

    // 注入你的工具类
    private final AiFunctionUtil aiFunctionUtil;

    @Value("${spring.ai.openai.api-key}")
    private String siliconApiKey;

    @Value("${spring.ai.openai.base-url}")
    private String siliconEndpoint;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String siliconEmbeddingModel;
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
         * 硅基OpenAI + RAG + 工具调用
         */
        @Primary
        @Bean("aiChatClient")
        public ChatClient aiChatClient(
                OpenAiChatModel model,
                ChatMemory chatMemory,
                VectorStore vectorStore
        ) {

            return ChatClient.builder(model)
                    .defaultSystem("""
                        
                        你是热心、简洁、可爱的智能助手 TheEternal。
                        
                        【使用知识规则】
                        1. 日常聊天、问候、常识、创意问题 → 直接回答，不要使用知识库。
                        2. 只有与【博客系统、功能使用、平台规则、网站说明】相关的问题 → 才使用知识库。
                        3. 时间、励志语录、密码、幸运数字、字数统计 → 必须调用工具。
                        4. 不知道就说不知道，绝不编造。
                        5. 知识库只是参考，不是强制答案。
                        6. 全程使用中文回答。
                        """)
                    .defaultAdvisors(
                            new MessageChatMemoryAdvisor(chatMemory),
                            new QuestionAnswerAdvisor(vectorStore)
                    )
                    // 注册你保留的所有工具
                    .defaultFunctions(
                            "getCurrentTime",
                            "randomQuote",
                            "generatePassword",
                            "luckyNumber",
                            "wordCount"
                    )
                    .build();
        }

        /**
         * 向量模型 - 手动创建避免Spring AI自动配置发送额外参数
         */
        @Primary
        @Bean
        public EmbeddingModel embeddingModel() {
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(siliconEndpoint)
                    .apiKey(siliconApiKey)
                    .build();
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(siliconEmbeddingModel)
                    .build();
            return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
        }

        /**
         * 文本分块
         */
        @Bean
        public TokenTextSplitter textSplitter() {
            return new TokenTextSplitter();
        }
    }
}