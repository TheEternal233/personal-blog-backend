package xyz.kuailemao.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
import java.time.LocalDateTime;
import java.util.List;

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

    // ===================== 会话管理 =====================
    @Override
    public AiSession createSession(AiSession session) {

        // 从 SecurityContext 获取当前用户 ID
        Long userId = getCurrentUserId();
        session.setUserId(userId);
        session.setPreview("新对话");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        aiSessionMapper.insert(session);
        return session;
    }

    @Override
    public List<AiSession> getSessionList() {
        Long userId = getCurrentUserId();
        return aiSessionMapper.selectList(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getUserId, userId)
        );
    }

    @Override
    public AiSession getSessionById(Long id) {
        Long userId = getCurrentUserId();
        return aiSessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId)
        );
    }

    @Override
    public void updateSessionTitle(Long id, String title) {
        AiSession session = new AiSession();
        session.setId(id);
        session.setTitle(title);
        aiSessionMapper.updateById(session);
    }

    @Override
    public void deleteSession(Long id) {
        Long userId = getCurrentUserId();
        // 校验是否属于当前用户
        AiSession session = aiSessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId)
        );
        if (session == null) {
            throw new RuntimeException("无权操作此会话");
        }

        chatMemory.clear(id.toString());
        aiMessageMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getSessionId, id));
        aiSessionMapper.deleteById(id);
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
            newSession.setUserId(getCurrentUserId());
            newSession.setTitle("AI对话");
            newSession.setPreview("这是AI对话预览");
            newSession.setCreatedAt(LocalDateTime.now());
            newSession.setUpdatedAt(LocalDateTime.now());
            aiSessionMapper.insert(newSession);
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
        return reply;
    }

    @Override
    public List<AiMessage> getMessagesBySessionId(Long sessionId) {
        return aiMessageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getSessionId, sessionId)
                        .orderByAsc(AiMessage::getCreatedAt)
        );
    }

    // ===================== 私有方法 =====================
    private void saveMessage(Long sessionId, String role, String content) {
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        log.info(">>> AiMessage 对象: {}", msg);
        aiMessageMapper.insert(msg);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            // 根据用户名去 UserMapper 里查用户ID（需要注入 UserMapper）
            return userMapper.selectIdByUsername(username);
        }
        // 未登录或获取失败时，返回默认用户ID（测试用）
        return 1L;
    }
}