package xyz.kuailemao.quartz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.constants.SQLConst;
import xyz.kuailemao.domain.entity.Article;
import xyz.kuailemao.mapper.ArticleMapper;
import xyz.kuailemao.service.ArticleService;
import xyz.kuailemao.utils.RedisCache;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kuailemao
 * <p>
 * 创建时间：2024/1/1 22:25
 * 刷新缓存任务 / 5分钟刷新一次
 */
@Slf4j
public class RefreshTheCache extends QuartzJobBean {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private ArticleService articleService;

    @Resource
    private RedisCache redisCache;

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) {
        log.info("-------------------------------开始同步文章浏览量到数据库-------------------------------");
        int successCount = 0;
        int skipCount = 0;
        try {
            // 只查询公开文章，避免更新私密/草稿文章
            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
            List<Article> updateList = new ArrayList<>();
            for (Article article : articles) {
                Long id = article.getId();
                Object cacheObject = redisCache.getCacheObject(RedisConst.ARTICLE_VISIT_COUNT + id);
                if (cacheObject == null) {
                    skipCount++;
                    continue;
                }
                long visitCount = Long.parseLong(cacheObject.toString());
                Article updateArticle = new Article();
                updateArticle.setId(id);
                updateArticle.setVisitCount(visitCount);
                updateList.add(updateArticle);
                successCount++;
            }
            // 批量更新，一次 SQL 完成所有文章的浏览量同步
            if (!updateList.isEmpty()) {
                articleService.updateBatchById(updateList);
            }
            log.info("-------------------------------同步文章浏览量完成，成功{}条，跳过{}条-------------------------------", successCount, skipCount);
        } catch (Exception e) {
            log.error("同步文章浏览量失败", e);
        }
    }
}