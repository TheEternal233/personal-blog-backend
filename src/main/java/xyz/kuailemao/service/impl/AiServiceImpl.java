package xyz.kuailemao.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import xyz.kuailemao.domain.entity.AiMessage;
import xyz.kuailemao.domain.entity.AiSession;
import xyz.kuailemao.mapper.AiMessageMapper;
import xyz.kuailemao.mapper.AiSessionMapper;
import xyz.kuailemao.mapper.UserMapper;
import xyz.kuailemao.service.AiService;
import xyz.kuailemao.utils.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Resource(name = "zhipuAiChatClient")
    private ChatClient zhipuAiChatClient;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private UserMapper userMapper;
    @Resource
    private AiSessionMapper aiSessionMapper;

    @Resource
    private AiMessageMapper aiMessageMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ===================== redis常量 =====================
    private static final String REDIS_CHAT_PREFIX = "ai:chat:";
    private static final long REDIS_EXPIRE = 3600 *24*7;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    // ===================== 会话管理 =====================
    @Override
    public AiSession createSession(AiSession session) {

        // 从 SecurityContext 获取当前用户 ID
        Long userId = SecurityUtils.getUserId();
        session.setUserId(userId);
        session.setPreview("嗨，你好呀！有什么可以帮到你的吗？");
        session.setTitle(""); // 初始标题为空，首次聊天时生成
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        aiSessionMapper.insert(session);
        return session;
    }

    @Override
    public List<AiSession> getSessionList() {
        Long userId = SecurityUtils.getUserId();
        return aiSessionMapper.selectList(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getUserId, userId)
        );
    }

    @Override
    public AiSession getSessionById(Long id) {
        Long userId = SecurityUtils.getUserId();
        return aiSessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId)
        );
    }

    @Override
    public void updateSessionTitle(Long id, String title) {
        UpdateWrapper<AiSession> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("title", title)
                .set("updated_at", LocalDateTime.now());
        aiSessionMapper.update(null, wrapper);
    }

    @Override
    public void deleteSession(Long id) {
        Long userId = SecurityUtils.getUserId();
        // 校验是否属于当前用户
        AiSession session = aiSessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId)
        );
        if (session == null) {
            throw new RuntimeException("无权操作此会话");
        }
        // 删db
        chatMemory.clear(id.toString());
        aiMessageMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getSessionId, id));
        aiSessionMapper.deleteById(id);
        // 删redis
        redisTemplate.delete(REDIS_CHAT_PREFIX + id);
    }

    // ===================== 带记忆聊天=====================
    @Override
    public String chatWithMemory(Long sessionId, String userMessage) {

        AiSession existSession = aiSessionMapper.selectById(sessionId);
        //不存在则自动创建
        if (existSession == null) {
            // 会话不存在，自动创建
            AiSession newSession = new AiSession();
            newSession.setId(sessionId);
            newSession.setUserId(SecurityUtils.getUserId());
            newSession.setTitle(""); // 初始标题为空，首次聊天时生成
            newSession.setPreview("这是AI对话预览");
            newSession.setCreatedAt(LocalDateTime.now());
            newSession.setUpdatedAt(LocalDateTime.now());
            aiSessionMapper.insert(newSession);
            existSession = newSession;
        }

        saveMessage(sessionId, "user", userMessage);

        String reply = zhipuAiChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(
                        "chat_memory_conversation_id",
                        sessionId.toString()
                ))
                .call()
                .content();

        saveMessage(sessionId, "assistant", reply);
        
        // 如果是首次聊天（标题为空），生成标题
        if (existSession.getTitle() == null || existSession.getTitle().isEmpty()) {
            generateTitleFromFirstMessage(sessionId, userMessage, reply);
        }
        
        return reply;
    }

    @Override
    public List<AiMessage> getMessagesBySessionId(Long sessionId) {
        String key = REDIS_CHAT_PREFIX + sessionId;

        // 1. 先读 redis（每条消息是独立的元素）
        List<Object> redisMessages = redisTemplate.opsForList().range(key, 0, -1);
        if (CollUtil.isNotEmpty(redisMessages)) {
            try {
                return redisMessages.stream()
                        .map(obj -> JSON.parseObject(obj.toString(), AiMessage.class))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Redis 消息解析异常", e);
                // 解析失败就清空缓存，走数据库
                redisTemplate.delete(key);
            }
        }

        // 2. redis 没有，读数据库
        List<AiMessage> dbMessages = aiMessageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getSessionId, sessionId)
                        .orderByAsc(AiMessage::getCreatedAt)
        );

        // 3. 回写 redis（逐条写入，与 saveMessage 格式一致）
        if (CollUtil.isNotEmpty(dbMessages)) {
            for (AiMessage msg : dbMessages) {
                redisTemplate.opsForList().rightPush(key, msg);
            }
            redisTemplate.expire(key, REDIS_EXPIRE, TimeUnit.SECONDS);
        }

        return dbMessages;
    }

    // ===================== 私有方法 =====================
    private void saveMessage(Long sessionId, String role, String content) {
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        log.info(">>> AiMessage 对象: {}", msg);
        // 先存入mysql（获取自增ID后再缓存）
        aiMessageMapper.insert(msg);
        // 再存入redis
        redisTemplate.opsForList().rightPush(REDIS_CHAT_PREFIX + sessionId, msg);
        redisTemplate.expire(REDIS_CHAT_PREFIX+sessionId,REDIS_EXPIRE, TimeUnit.SECONDS);
    }


    /**
     * 根据用户首条消息和AI回复生成会话标题
     * @param sessionId 会话ID
     * @param userMessage 用户首条消息
     * @param aiReply AI回复
     */
    private void generateTitleFromFirstMessage(Long sessionId, String userMessage, String aiReply) {
        try {
            // 使用AI生成简短标题
            String prompt = String.format(
                "请根据以下用户问题和AI回复，生成一个简短的会话标题（不超过15个字）。\n" +
                "用户问题：%s\n" +
                "AI回复：%s\n" +
                "只需返回标题，不要其他内容。",
                userMessage.length() > 100 ? userMessage.substring(0, 100) + "..." : userMessage,
                aiReply.length() > 100 ? aiReply.substring(0, 100) + "..." : aiReply
            );
            
            String generatedTitle = zhipuAiChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            // 清理标题：去除多余空白字符
            if (generatedTitle != null) {
                generatedTitle = generatedTitle.trim();
                // 限制标题长度，超过15个字符则截断并添加省略号
                if (generatedTitle.length() > 15) {
                    generatedTitle = generatedTitle.substring(0, 15) + "...";
                }
            }
            
            // 如果生成失败，使用默认标题
            if (generatedTitle == null || generatedTitle.isEmpty()) {
                generatedTitle = "AI对话";
            }
            
            // 更新会话标题
            updateSessionTitle(sessionId, generatedTitle);
            log.info("会话 {} 标题已生成: {}", sessionId, generatedTitle);
        } catch (Exception e) {
            log.error("生成会话标题失败，使用默认标题", e);
            updateSessionTitle(sessionId, "AI对话");
        }
    }
}