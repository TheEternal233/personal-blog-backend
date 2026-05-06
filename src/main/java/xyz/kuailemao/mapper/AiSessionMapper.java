package xyz.kuailemao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.kuailemao.domain.entity.AiSession;

import java.util.List;


@Mapper
public interface AiSessionMapper extends BaseMapper<AiSession> {
}
