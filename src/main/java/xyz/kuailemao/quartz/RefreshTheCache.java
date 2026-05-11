package xyz.kuailemao.quartz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.domain.entity.Article;
import xyz.kuailemao.mapper.ArticleMapper;
import xyz.kuailemao.utils.RedisCache;

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
    private RedisCache redisCache;
    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) {
        log.info("-------------------------------开始同步文章浏览量到数据库-------------------------------");
        int successCount = 0;
        int failCount = 0;
        try {
            // 获取所有文章id
            List<Long> articleIds = articleMapper.selectList(null).stream().map(Article::getId).toList();
            // 通过id从redis中获取缓存的访问量
            for(Long id : articleIds){
                try {
                    Object cacheObject = redisCache.getCacheObject(RedisConst.ARTICLE_VISIT_COUNT + id);
                    if(cacheObject==null) continue;
                    long visitCount = Long.parseLong(cacheObject.toString());

                    articleMapper.update(null,
                            new LambdaUpdateWrapper<Article>()
                                    .eq(Article::getId, id)
                                    .set(Article::getVisitCount, visitCount)
                    );
                    successCount++;
                }catch (Exception e){
                    failCount++;
                    log.error("同步文章[id={}]浏览量失败", id, e);
                }
            }
            log.info("-------------------------------同步文章浏览量完成，成功{}条，失败{}条-------------------------------", successCount, failCount);
        } catch (Exception e) {
            log.error("同步文章浏览量失败",e);
        }
    }
}
