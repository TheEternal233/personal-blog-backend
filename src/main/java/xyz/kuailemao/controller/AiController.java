package xyz.kuailemao.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.entity.AiMessage;
import xyz.kuailemao.domain.entity.AiSession;
import xyz.kuailemao.domain.response.Result;
import xyz.kuailemao.service.AiService;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiService aiService;

    // 创建新会话
    @PostMapping("/sessions")
    public Result createSession(@RequestBody AiSession session) {
        return Result.success(aiService.createSession(session));
    }

    // 获取会话列表
    @GetMapping("/sessions")
    public Result getSessions() {
        return Result.success(aiService.getSessionList());
    }

    // 获取会话详情
    @GetMapping("/sessions/{id}")
    public Result getSession(@PathVariable Long id) {
        return Result.success(aiService.getSessionById(id));
    }

    // 更新会话标题
    @PutMapping("/sessions/{id}")
    public Result updateSessionTitle(@PathVariable Long id, @RequestBody AiSession session) {
        aiService.updateSessionTitle(id, session.getTitle());
        return Result.success();
    }

    // 删除会话
    @DeleteMapping("/sessions/{id}")
    public Result deleteSession(@PathVariable Long id) {
        aiService.deleteSession(id);
        return Result.success();
    }

    // 发送消息（AI 回复 + 记忆 + 函数调用）
    @PostMapping("/chat")
    public Result sendMessage(@RequestBody AiMessage message) {
        log.info("接收到的对象: {}", message);
        log.info("sessionId: {}, content: {}", message.getSessionId(), message.getContent());
        String reply = aiService.chatWithMemory(message.getSessionId(), message.getContent());
        return Result.success(reply);
    }

    // 获取会话消息历史
    @GetMapping("/sessions/{id}/messages")
    public Result getSessionMessages(@PathVariable Long id) {
        List<AiMessage> messages = aiService.getMessagesBySessionId(id);
        return Result.success(messages);
    }
}