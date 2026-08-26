package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.constants.SQLConst;
import xyz.kuailemao.domain.dto.ArticleDTO;
import xyz.kuailemao.domain.dto.SearchArticleDTO;
import xyz.kuailemao.domain.entity.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.*;
import xyz.kuailemao.enums.*;
import xyz.kuailemao.exceptions.FileUploadException;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.*;
import xyz.kuailemao.utils.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * (Article)表服务实现类
 *
 * @author kuailemao
 * @since 2023-10-15 02:29:13
 */
@Slf4j
@Service("articleService")
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ArticleTagMapper articleTagMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private FavoriteService favoriteService;

    @Resource
    private LikeService likeService;

    @Resource
    private CommentService commentService;

    @Resource
    private RedisCache redisCache;

    @Resource
    private FileUploadUtils fileUploadUtils;

    @Resource
    private UserMapper userMapper;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private CommentMapper commentMapper;


    @Override
    public PageVO<List<ArticleVO>> listAllArticle(Integer pageNum, Integer pageSize) {
        // 文章
        Page<Article> page = new Page<>(pageNum, pageSize);
        this.page(page, new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE).orderByDesc(Article::getCreateTime));
        List<Article> list = page.getRecords();
        if (list.isEmpty()) return new PageVO<>(List.of(), 0L);

        // 文章分类
        Map<Long, String> categoryMap = categoryMapper.selectBatchIds(list.stream().map(Article::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

        // 标签关系查询：使用 LambdaQueryWrapper 按 article_id 查询，而非 selectBatchIds（selectBatchIds 按主键查询）
        List<Long> articleIds = list.stream().map(Article::getId).toList();
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        Set<Long> tagIds = articleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagMap = tagIds.isEmpty() ? Map.of() :
                tagMapper.selectBatchIds(tagIds).stream().collect(Collectors.toMap(Tag::getId, Tag::getTagName));

        // 批量获取 Redis 计数（只获取一次，避免每条文章都获取整个 Hash）
        Map<String, Long> favoriteCountMap = convertToLongMap(redisCache.getCacheMap(RedisConst.ARTICLE_FAVORITE_COUNT));
        Map<String, Long> likeCountMap = convertToLongMap(redisCache.getCacheMap(RedisConst.ARTICLE_LIKE_COUNT));
        Map<String, Long> commentCountMap = convertToLongMap(redisCache.getCacheMap(RedisConst.ARTICLE_COMMENT_COUNT));

        List<ArticleVO> articleVOS = list.stream().map(article -> {
            ArticleVO articleVO = article.asViewObject(ArticleVO.class);
            articleVO.setCategoryName(categoryMap.get(article.getCategoryId()));
            articleVO.setTags(articleTags.stream()
                    .filter(at -> Objects.equals(at.getArticleId(), article.getId()))
                    .map(at -> tagMap.get(at.getTagId()))
                    .filter(Objects::nonNull)
                    .toList());
            String idStr = articleVO.getId().toString();
            articleVO.setFavoriteCount(favoriteCountMap.getOrDefault(idStr, 0L));
            articleVO.setLikeCount(likeCountMap.getOrDefault(idStr, 0L));
            articleVO.setCommentCount(commentCountMap.getOrDefault(idStr, 0L));
            return articleVO;
        }).toList();

        return new PageVO<>(articleVOS, page.getTotal());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> convertToLongMap(Map<String, Object> cacheMap) {
        Map<String, Long> result = new HashMap<>();
        if (cacheMap == null) return result;
        for (Map.Entry<String, Object> entry : cacheMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Long) {
                result.put(entry.getKey(), (Long) value);
            } else if (value instanceof Integer) {
                result.put(entry.getKey(), ((Integer) value).longValue());
            } else if (value instanceof Number) {
                result.put(entry.getKey(), ((Number) value).longValue());
            }
        }
        return result;
    }

    @Override
    public List<RecommendArticleVO> listRecommendArticle() {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsTop, SQLConst.RECOMMEND_ARTICLE).and(i -> i.eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        List<Article> articles = articleMapper.selectList(wrapper);
        return articles.stream().map(article -> article.asViewObject(RecommendArticleVO.class)).toList();
    }

    @Override
    public List<RandomArticleVO> listRandomArticle() {
        // 先查所有符合条件的文章 ID（只查 ID 列，走索引，极快）
        List<Long> allIds = articleMapper.selectArticleIdsByStatus(SQLConst.PUBLIC_ARTICLE);
        if (allIds.isEmpty()) return List.of();

        // Java 层洗牌，取前 N 个，避免 ORDER BY RAND() 全表扫描
        Collections.shuffle(allIds);
        List<Long> randomIds = allIds.subList(0, Math.min(SQLConst.RANDOM_ARTICLE_COUNT, allIds.size()));

        List<Article> randomArticles = articleMapper.selectBatchIds(randomIds);
        return randomArticles.stream()
                .map(article -> article.asViewObject(RandomArticleVO.class))
                .toList();
    }

    @Override
    public ArticleDetailVO getArticleDetail(Integer id) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE)
                .eq(Article::getId, id));

        if (StringUtils.isNull(article)) return null;

        // 文章分类
        Category category = categoryMapper.selectById(article.getCategoryId());
        // 文章标签关系（已修正）
        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getArticleId, article.getId()));
        // 标签
        List<Tag> tags = tagMapper.selectBatchIds(articleTags.stream().map(ArticleTag::getTagId).toList());

        // 上下篇文章
        LambdaQueryWrapper<Article> preAndNextWrapper = new LambdaQueryWrapper<>();
        preAndNextWrapper.eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE);
        // 上一篇
        preAndNextWrapper.lt(Article::getId, id);
        Article preArticle = articleMapper.selectOne(preAndNextWrapper
                .orderByDesc(Article::getId)
                .last(SQLConst.LIMIT_ONE_SQL));
        // 下一篇
        preAndNextWrapper.clear();
        preAndNextWrapper.eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE);
        preAndNextWrapper.gt(Article::getId, id);
        Article nextArticle = articleMapper.selectOne(preAndNextWrapper
                .orderByAsc(Article::getId)
                .last(SQLConst.LIMIT_ONE_SQL));

        // ====================== 优化点：从Redis获取数量，不查数据库（已修正） ======================
        String articleIdStr = id.toString();
        Long commentCount = Optional.ofNullable(redisCache.getCacheMapValue(RedisConst.ARTICLE_COMMENT_COUNT, articleIdStr))
                .map(obj -> obj instanceof Long ? (Long) obj : Long.valueOf(obj.toString()))
                .orElse(0L);
        Long likeCount = Optional.ofNullable(redisCache.getCacheMapValue(RedisConst.ARTICLE_LIKE_COUNT, articleIdStr))
                .map(obj -> obj instanceof Long ? (Long) obj : Long.valueOf(obj.toString()))
                .orElse(0L);
        Long favoriteCount = Optional.ofNullable(redisCache.getCacheMapValue(RedisConst.ARTICLE_FAVORITE_COUNT, articleIdStr))
                .map(obj -> obj instanceof Long ? (Long) obj : Long.valueOf(obj.toString()))
                .orElse(0L);

        return article.asViewObject(ArticleDetailVO.class, vo -> {
            vo.setCategoryName(category != null ? category.getCategoryName() : "未分类");
            vo.setCategoryId(category != null ? category.getId() : 0);
            vo.setTags(tags.stream().map(tag -> tag.asViewObject(TagVO.class)).toList());

            // 直接使用缓存数据
            vo.setCommentCount(commentCount);
            vo.setLikeCount(likeCount);
            vo.setFavoriteCount(favoriteCount);

            vo.setPreArticleId(preArticle == null ? 0 : preArticle.getId());
            vo.setPreArticleTitle(preArticle == null ? "" : preArticle.getArticleTitle());
            vo.setNextArticleId(nextArticle == null ? 0 : nextArticle.getId());
            vo.setNextArticleTitle(nextArticle == null ? "" : nextArticle.getArticleTitle());
        });
    }

    @Override
    public List<RelatedArticleVO> relatedArticleList(Integer categoryId, Integer articleId) {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE)
                        .and(i -> i.eq(Article::getCategoryId, categoryId))
                        .ne(Article::getId, articleId)
                        .last("LIMIT " + SQLConst.RELATED_ARTICLE_COUNT)
        );
        return articles.stream().map(article -> article.asViewObject(RelatedArticleVO.class)).toList();
    }

    @Override
    public List<TimeLineVO> listTimeLine() {
        List<Article> list = this.query()
                .eq("status", SQLConst.PUBLIC_ARTICLE)
                .orderByDesc("create_time")
                .last("LIMIT 500")
                .list();
        return list.stream().map(article -> article.asViewObject(TimeLineVO.class)).toList();
    }

    @Override
    public List<CategoryArticleVO> listCategoryArticle(Integer type, Long typeId) {
        List<Article> articles;
        if (type == 1)
            articles = articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getCategoryId, typeId));
        else if (type == 2) {
            List<Long> articleIds = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, typeId)).stream().map(ArticleTag::getArticleId).toList();
            if (!articleIds.isEmpty()) articles = articleMapper.selectBatchIds(articleIds);
            else articles = List.of();
        } else articles = List.of();

        if (Objects.isNull(articles) || articles.isEmpty()) return null;
        List<ArticleTag> articleTags = articleTagMapper.selectBatchIds(articles.stream().map(Article::getId).toList());
        List<Tag> tags = tagMapper.selectBatchIds(articleTags.stream().map(ArticleTag::getTagId).toList());

        return articles.stream().map(article -> article.asViewObject(CategoryArticleVO.class, item -> {
            item.setCategoryId(article.getCategoryId());
            item.setTags(tags.stream().filter(tag -> articleTags.stream().anyMatch(articleTag -> Objects.equals(articleTag.getArticleId(), article.getId()) && Objects.equals(articleTag.getTagId(), tag.getId()))).map(tag -> tag.asViewObject(TagVO.class)).toList());
        })).toList();
    }

    @Override
    public void addVisitCount(Long id) {
        // 访问量去重
        HttpServletRequest request = SecurityUtils.getCurrentHttpRequest();
        String KEY = RedisConst.ARTICLE_VISIT_COUNT_LIMIT + id + ":" + IpUtils.getIpAddr(request);
        if (redisCache.getCacheObject(KEY) == null) {
            redisCache.setCacheObject(KEY, 1, RedisConst.ARTICLE_VISIT_COUNT_INTERVAL, TimeUnit.SECONDS);
            // 修复：直接自增，不存在会自动从 1 开始
            redisCache.increment(RedisConst.ARTICLE_VISIT_COUNT + id, 1L);
        }
    }

    @Override
    public ResponseResult<String> uploadArticleCover(MultipartFile articleCover) {
        try {
            String articleCoverUrl = null;
            try {
                articleCoverUrl = fileUploadUtils.upload(UploadEnum.ARTICLE_COVER, articleCover);
            } catch (FileUploadException e) {
                return ResponseResult.failure(e.getMessage());
            }
            if (StringUtils.isNotNull(articleCoverUrl))
                return ResponseResult.success(articleCoverUrl);
            else
                return ResponseResult.failure("上传格式错误");
        } catch (Exception e) {
            log.error("文章封面上传失败", e);
            return ResponseResult.failure();
        }
    }

    @Resource
    private ArticleTagService articleTagService;

    @Transactional
    @Override
    public ResponseResult<Void> publish(ArticleDTO articleDTO) {
        Article article = articleDTO.asViewObject(Article.class, v -> v.setUserId(SecurityUtils.getUserId()));
        if (this.saveOrUpdate(article)) {
            // 查询已有标签关系
            List<Long> existingTagIds = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>()
                            .eq(ArticleTag::getArticleId, article.getId()))
                    .stream().map(ArticleTag::getTagId).toList();
            List<Long> newTagIds = articleDTO.getTagId();

            // 只删除被移除的标签
            List<Long> toDelete = existingTagIds.stream()
                    .filter(tagId -> !newTagIds.contains(tagId)).toList();
            if (!toDelete.isEmpty()) {
                articleTagMapper.delete(
                        new LambdaQueryWrapper<ArticleTag>()
                                .eq(ArticleTag::getArticleId, article.getId())
                                .in(ArticleTag::getTagId, toDelete));
            }

            // 只新增新标签
            List<Long> toAdd = newTagIds.stream()
                    .filter(tagId -> !existingTagIds.contains(tagId)).toList();
            if (!toAdd.isEmpty()) {
                List<ArticleTag> articleTags = toAdd.stream()
                        .map(tagId -> ArticleTag.builder().articleId(article.getId()).tagId(tagId).build()).toList();
                articleTagService.saveBatch(articleTags);
            }
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Value("${minio.bucketName}")
    private String bucketName;

    @Override
    public ResponseResult<Void> deleteArticleCover(String articleCoverUrl) {
        try {
            // 提取图片名称
            int index = articleCoverUrl.indexOf(bucketName);
            if (index == -1) {
                log.error("URL不包含bucketName: {}", articleCoverUrl);
                return ResponseResult.failure("封面URL格式错误");
            }
            String articleCoverName = articleCoverUrl.substring(index + bucketName.length());
            fileUploadUtils.deleteFiles(List.of(articleCoverName));
            return ResponseResult.success();
        } catch (Exception e) {
            log.error("删除文章封面失败", e);
            return ResponseResult.failure();
        }
    }

    @Override
    public ResponseResult<String> uploadArticleImage(MultipartFile articleImage) {
        try {
            String url = fileUploadUtils.upload(UploadEnum.ARTICLE_IMAGE, articleImage);
            if (StringUtils.isNotNull(url))
                return ResponseResult.success(url);
            else
                return ResponseResult.failure("上传格式错误");
        } catch (Exception e) {
            log.error("文章图片上传失败", e);
        }
        return ResponseResult.failure("文章图片上传失败");
    }

    @Override
    public List<ArticleListVO> listArticle() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .orderByDesc(Article::getCreateTime)
        );
        if (articles.isEmpty()) return List.of();

        // 批量查分类
        Map<Long, String> categoryMap = categoryMapper.selectBatchIds(
                articles.stream().map(Article::getCategoryId).distinct().toList()
        ).stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

        // 批量查用户
        Map<Long, String> userMap = userMapper.selectBatchIds(
                articles.stream().map(Article::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));

        // 批量查文章标签关系
        List<ArticleTag> allArticleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>()
                        .in(ArticleTag::getArticleId,
                                articles.stream().map(Article::getId).toList())
        );

        // 批量查标签
        Map<Long, String> tagMap = tagMapper.selectBatchIds(
                allArticleTags.stream().map(ArticleTag::getTagId).distinct().toList()
        ).stream().collect(Collectors.toMap(Tag::getId, Tag::getTagName));

        // 按文章ID分组标签关系
        Map<Long, List<Long>> articleTagMap = allArticleTags.stream()
                .collect(Collectors.groupingBy(
                        ArticleTag::getArticleId,
                        Collectors.mapping(ArticleTag::getTagId, Collectors.toList())
                ));

        return articles.stream().map(article -> {
            ArticleListVO vo = article.asViewObject(ArticleListVO.class);
            vo.setCategoryName(categoryMap.get(article.getCategoryId()));
            vo.setUserName(userMap.get(article.getUserId()));

            List<Long> tagIds = articleTagMap.getOrDefault(article.getId(), List.of());
            vo.setTagsName(tagIds.stream().map(tagMap::get).filter(Objects::nonNull).toList());

            return vo;
        }).toList();
    }

    @Override
    public List<ArticleListVO> searchArticle(SearchArticleDTO searchArticleDTO) {
        // 1. 构建查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotNull(searchArticleDTO.getArticleTitle()),
                        Article::getArticleTitle, searchArticleDTO.getArticleTitle())
                .eq(StringUtils.isNotNull(searchArticleDTO.getCategoryId()),
                        Article::getCategoryId, searchArticleDTO.getCategoryId())
                .eq(StringUtils.isNotNull(searchArticleDTO.getStatus()),
                        Article::getStatus, searchArticleDTO.getStatus())
                .eq(StringUtils.isNotNull(searchArticleDTO.getIsTop()),
                        Article::getIsTop, searchArticleDTO.getIsTop());

        // 2. 查文章
        List<Article> articles = articleMapper.selectList(wrapper);
        if (articles.isEmpty()) {
            return List.of();
        }

        // 3. 批量查分类（1条SQL）
        Map<Long, String> categoryMap = categoryMapper.selectBatchIds(
                articles.stream().map(Article::getCategoryId).distinct().toList()
        ).stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

        // 4. 批量查用户（1条SQL）
        Map<Long, String> userMap = userMapper.selectBatchIds(
                articles.stream().map(Article::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));

        // 5. 批量查文章标签关系（1条SQL）
        List<ArticleTag> allArticleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>()
                        .in(ArticleTag::getArticleId,
                                articles.stream().map(Article::getId).toList())
        );

        // 6. 批量查标签（1条SQL）
        Map<Long, String> tagMap = tagMapper.selectBatchIds(
                allArticleTags.stream().map(ArticleTag::getTagId).distinct().toList()
        ).stream().collect(Collectors.toMap(Tag::getId, Tag::getTagName));

        // 7. 按文章ID分组标签关系
        Map<Long, List<Long>> articleTagMap = allArticleTags.stream()
                .collect(Collectors.groupingBy(
                        ArticleTag::getArticleId,
                        Collectors.mapping(ArticleTag::getTagId, Collectors.toList())
                ));

        // 8. 组装结果
        return articles.stream().map(article -> {
            ArticleListVO vo = article.asViewObject(ArticleListVO.class);
            vo.setCategoryName(categoryMap.get(article.getCategoryId()));
            vo.setUserName(userMap.get(article.getUserId()));

            List<Long> tagIds = articleTagMap.getOrDefault(article.getId(), List.of());
            vo.setTagsName(tagIds.stream()
                    .map(tagMap::get)
                    .filter(Objects::nonNull)
                    .toList());

            return vo;
        }).toList();
    }

    @Override
    public ResponseResult<Void> updateStatus(Long id, Integer status) {
        if (this.update(new LambdaUpdateWrapper<Article>().eq(Article::getId, id).set(Article::getStatus, status))) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Override
    public ResponseResult<Void> updateIsTop(Long id, Integer isTop) {
        if (this.update(new LambdaUpdateWrapper<Article>().eq(Article::getId, id).set(Article::getIsTop, isTop))) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Override
    public ArticleDTO getArticleDTO(Long id) {
        ArticleDTO articleDTO = articleMapper.selectById(id).asViewObject(ArticleDTO.class);
        if (StringUtils.isNotNull(articleDTO)) {
            // 查询文章标签
            List<Long> tagIds = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleDTO.getId())).stream().map(ArticleTag::getTagId).toList();
            articleDTO.setTagId(tagMapper.selectBatchIds(tagIds).stream().map(Tag::getId).toList());
            return articleDTO;
        }
        return null;
    }

    @Transactional
    @Override
    public ResponseResult<Void> deleteArticle(List<Long> ids) {
        if (this.removeByIds(ids)) {
            // 删除标签关系
            articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, ids));
            // 删除点赞、收藏、评论
            likeMapper.delete(new LambdaQueryWrapper<Like>().eq(Like::getType, LikeEnum.LIKE_TYPE_ARTICLE.getType()).and(a -> a.in(Like::getTypeId, ids)));
            favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getType, FavoriteEnum.FAVORITE_TYPE_ARTICLE.getType()).and(a -> a.in(Favorite::getTypeId, ids)));
            commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getType, CommentEnum.COMMENT_TYPE_ARTICLE.getType()).and(a -> a.in(Comment::getTypeId, ids)));

            // ===================== 新增：清理缓存 =====================
            ids.forEach(id -> {
                redisCache.deleteObject(RedisConst.ARTICLE_VISIT_COUNT + id);
                redisCache.delCacheMapValue(RedisConst.ARTICLE_LIKE_COUNT, id.toString());
                redisCache.delCacheMapValue(RedisConst.ARTICLE_FAVORITE_COUNT, id.toString());
                redisCache.delCacheMapValue(RedisConst.ARTICLE_COMMENT_COUNT, id.toString());
            });

            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }
    @Override
    public List<InitSearchTitleVO> initSearchByTitle() {
        //找到公开的文章
        List<Article> articles = this.list(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        //文章为空，返回空列表
        if (articles.isEmpty()) {
            return List.of();
        }
        // 优化：只查询当前文章用到的分类
        Map<Long, String> categoryMap = categoryMapper.selectBatchIds(
                articles.stream().map(Article::getCategoryId).distinct().toList()
        ).stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

        return articles.stream()
                .map(article ->
                        article.asViewObject(InitSearchTitleVO.class,
                                item -> item.setCategoryName(categoryMap.get(article.getCategoryId()))
                        )
                )
                .toList();
    }

    @Override
    public List<HotArticleVO> listHotArticle() {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE).orderByDesc(Article::getVisitCount).last("LIMIT 5"));
        if (!articles.isEmpty()) {
            return articles.stream().map(article -> article.asViewObject(HotArticleVO.class)).toList();
        }
        return List.of();
    }

    @Override
    public List<SearchArticleByContentVO> searchArticleByContent(String keyword) {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>().like(Article::getArticleContent, keyword).eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));
        if (!articles.isEmpty()) {
            List<SearchArticleByContentVO> listVos = articles.stream().map(article -> article.asViewObject(SearchArticleByContentVO.class, vo -> {
                vo.setCategoryName(categoryMap.get(article.getCategoryId()));
            })).toList();
            int index = -1;
            for (SearchArticleByContentVO articleVo : listVos) {
                String content = articleVo.getArticleContent();
                index = content.toLowerCase().indexOf(keyword.toLowerCase());
                if (index != -1) {
                    int end = Math.min(content.length(), index + keyword.length() + 20);
                    String highlighted = keyword + content.substring(index + keyword.length(), end);
                    articleVo.setArticleContent(stripMarkdown(highlighted));
                }
            }
            if (index != -1) {
                return listVos;
            }
        }
        return List.of();
    }

    /**
     * 去掉markdown格式
     *
     * @param markdown markdown
     * @return txt
     */
    private String stripMarkdown(String markdown) {
        return markdown.replaceAll("(?m)^\\s*#.*$", "") // 去掉标题
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // 去掉加粗
                .replaceAll("\\*(.*?)\\*", "$1") // 去掉斜体
                .replaceAll("`([^`]*)`", "$1") // 去掉行内代码
                .replaceAll("~~(.*?)~~", "$1") // 去掉删除线
                .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1") // 去掉链接
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "") // 去掉图片
                .replaceAll(">\\s?", "") // 去掉引用
                .replaceAll("(?m)^\\s*[-*+]\\s+", "") // 去掉无序列表
                .replaceAll("(?m)^\\s*\\d+\\.\\s+", "") // 去掉有序列表
                .replaceAll("\\n", " "); // 去掉换行符
    }
}