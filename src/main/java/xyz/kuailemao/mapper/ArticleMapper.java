package xyz.kuailemao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.kuailemao.domain.entity.Article;

import java.util.List;


/**
 * (Article)表数据库访问层
 *
 * @author TheEternal
 * @since 2026/3/31
 */
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT id FROM t_article WHERE status = #{status} and is_deleted = 0")
    List<Long> selectArticleIdsByStatus(@Param("status") Integer status);
}