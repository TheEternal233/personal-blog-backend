package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kuailemao.domain.entity.AiSession;

import java.util.List;

public interface AiSessionService extends IService<AiSession> {

    List<AiSession> getSessionsByUserId(Long userId);

    AiSession createSession(Long userId,String title);

    AiSession updateSessionTitle(Long id,Long userId,String title);

    void deleteSession(Long id,Long userId);
}
