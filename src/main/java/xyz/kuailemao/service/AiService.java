package xyz.kuailemao.service;

import xyz.kuailemao.domain.entity.AiMessage;
import xyz.kuailemao.domain.entity.AiSession;
import java.util.List;

public interface AiService {

    // 创建会话
    AiSession createSession(AiSession session);

    // 获取会话列表
    List<AiSession> getSessionList();

    // 获取会话详情
    AiSession getSessionById(Long id);

    // 更新会话标题
    void updateSessionTitle(Long id, String title);

    // 删除会话
    void deleteSession(Long id);

    // 带记忆聊天
    String chatWithMemory(Long sessionId, String userMessage);

    // 获取消息历史
    List<AiMessage> getMessagesBySessionId(Long sessionId);
}