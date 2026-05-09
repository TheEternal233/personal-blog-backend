package xyz.kuailemao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.kuailemao.domain.entity.Comment;

import java.util.List;
import java.util.Map;


/**
 * (Comment)表数据库访问层
 *
 * @author kuailemao
 * @since 2023-10-19 15:44:56
 */
public interface CommentMapper extends BaseMapper<Comment> {

}