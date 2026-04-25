package xyz.kuailemao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.kuailemao.domain.entity.AiMessage;

import java.util.List;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
    //List<AiMessage> selectBySessionId(Long sessionId);
}
