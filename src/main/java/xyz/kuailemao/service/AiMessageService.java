package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kuailemao.domain.entity.AiMessage;

import java.util.List;
import java.util.Map;

public interface AiMessageService extends IService<AiMessage> {
    List<AiMessage> getMessagesBySessionId(Long sessionId);
    Map<String, Object> sendMessage(Long sessionId, Long userId, String message);
}
