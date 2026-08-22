package xyz.kuailemao.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.constants.SQLConst;
import xyz.kuailemao.domain.entity.*;
import xyz.kuailemao.domain.vo.ArticleVO;
import xyz.kuailemao.enums.CommentEnum;
import xyz.kuailemao.enums.FavoriteEnum;
import xyz.kuailemao.enums.LikeEnum;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.RedisService;
import xyz.kuailemao.utils.RedisCache;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kuailemao
 * <p>
 * 创建时间：2023/10/22 15:18
 */
@Slf4j
@Service
public class RedisServiceImpl implements RedisService {

    @Resource
    private RedisCache redisCache;

    @Resource
    private ArticleMapper articleMapper;

    @Override
    public void articleCountClear() {
        log.info("--------执行清除redis文章相关数量缓存--------");
        boolean isDel = redisCache.deleteObject(RedisConst.ARTICLE_FAVORITE_COUNT);
        isDel = isDel && redisCache.deleteObject(RedisConst.ARTICLE_LIKE_COUNT);
        isDel = isDel && redisCache.deleteObject(RedisConst.ARTICLE_COMMENT_COUNT);
        if (isDel) log.info("--------清除redis文章相关数量缓存成功--------");
        else log.info("--------清除redis文章相关数量缓存失败--------");
    }

    @Override
    public void articleVisitCount() {
        try {
            articleMapper.selectList(null).forEach(article -> redisCache.setCacheObject(RedisConst.ARTICLE_VISIT_COUNT + article.getId(), article.getVisitCount()));
            log.info("--------执行redis文章访问量缓存成功--------");
        } catch (Exception e) {
            log.error("--------执行redis文章访问量缓存失败", e);
        }
    }

    @Override
    public void clearLimitCache() {
        log.info("--------执行清除redis限流缓存--------:");
        try {
            Collection<String> keys = redisCache.keys("limit*");
            if (redisCache.deleteObject(keys) > 0) log.info("--------清除redis限流缓存成功--------");
            else log.info("--------没有redis限流缓存，无法清除--------");
        } catch (Exception e) {
            log.error("--------执行清除redis限流缓存失败", e);
        }

    }

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private BlackListMapper blackListMapper;

    @Override
    public void initCount() {
        log.info("--------开始执行缓存文章点赞数量，评论数量，收藏数量--------");
        // 获取所有文章ID
        List<Long> articleIds = articleMapper.selectList(null).stream().map(Article::getId).toList();
        if (articleIds.isEmpty()) {
            redisCache.setCacheMap(RedisConst.ARTICLE_FAVORITE_COUNT, Map.of());
            redisCache.setCacheMap(RedisConst.ARTICLE_LIKE_COUNT, Map.of());
            redisCache.setCacheMap(RedisConst.ARTICLE_COMMENT_COUNT, Map.of());
            return;
        }

        // 批量查询文章收藏量：GROUP BY type_id，一次查询
        Map<String, Long> favoriteCount = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getType, FavoriteEnum.FAVORITE_TYPE_ARTICLE.getType())
                        .in(Favorite::getTypeId, articleIds)
        ).stream().collect(Collectors.groupingBy(f -> f.getTypeId().toString(), Collectors.counting()));

        // 批量查询文章点赞量：GROUP BY type_id，一次查询
        Map<String, Long> likeCount = likeMapper.selectList(
                new LambdaQueryWrapper<Like>()
                        .eq(Like::getType, LikeEnum.LIKE_TYPE_ARTICLE.getType())
                        .in(Like::getTypeId, articleIds)
        ).stream().collect(Collectors.groupingBy(l -> l.getTypeId().toString(), Collectors.counting()));

        // 批量查询文章评论量：GROUP BY type_id，一次查询
        Map<String, Long> commentCount = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getType, CommentEnum.COMMENT_TYPE_ARTICLE.getType())
                        .eq(Comment::getIsCheck, SQLConst.COMMENT_IS_CHECK)
                        .in(Comment::getTypeId, articleIds)
        ).stream().collect(Collectors.groupingBy(c -> c.getTypeId().toString(), Collectors.counting()));

        // 补充没有数据的文章，默认值为 0
        for (Long articleId : articleIds) {
            String key = articleId.toString();
            favoriteCount.putIfAbsent(key, 0L);
            likeCount.putIfAbsent(key, 0L);
            commentCount.putIfAbsent(key, 0L);
        }

        redisCache.setCacheMap(RedisConst.ARTICLE_FAVORITE_COUNT, favoriteCount);
        redisCache.setCacheMap(RedisConst.ARTICLE_LIKE_COUNT, likeCount);
        redisCache.setCacheMap(RedisConst.ARTICLE_COMMENT_COUNT, commentCount);
        log.info("--------成功执行缓存文章点赞数量，评论数量，收藏数量--------");
    }

    @Override
    public void initBlackList() {
        // 清除黑名单缓存
        redisCache.deleteObject(RedisConst.BLACK_LIST_UID_KEY);
        redisCache.deleteObject(RedisConst.BLACK_LIST_IP_KEY);

        // 将所有黑名单id初始化到redis中
        log.info("--------开始执行初始化黑名单缓存--------");
        List<BlackList> blackLists = blackListMapper.selectList(null);
        if (!blackLists.isEmpty()) {
            blackLists.forEach(blackList -> {
                if (blackList.getUserId() != null) {
                    redisCache.setCacheMapValue(RedisConst.BLACK_LIST_UID_KEY, blackList.getUserId().toString(), blackList.getExpiresTime());
                } else {
                    redisCache.setCacheMapValue(RedisConst.BLACK_LIST_IP_KEY, blackList.getIpInfo().getCreateIp(), blackList.getExpiresTime());
                }
            });
            log.info("--------成功执行初始化黑名单缓存--------");
        } else {
            log.info("--------没有黑名单信息，无法初始化--------");
        }
    }
}