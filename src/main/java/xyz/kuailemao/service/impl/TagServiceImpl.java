package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.kuailemao.constants.FunctionConst;
import xyz.kuailemao.domain.dto.SearchTagDTO;
import xyz.kuailemao.domain.dto.TagDTO;
import xyz.kuailemao.domain.entity.ArticleTag;
import xyz.kuailemao.domain.entity.Tag;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.TagVO;
import xyz.kuailemao.mapper.ArticleTagMapper;
import xyz.kuailemao.mapper.TagMapper;
import xyz.kuailemao.service.TagService;
import xyz.kuailemao.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * (Tag)表服务实现类
 *
 * @author kuailemao
 * @since 2023-10-15 02:29:14
 */
@Service("tagService")
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private ArticleTagMapper articleTagMapper;

    @Resource
    private TagMapper tagMapper;

    @Override
    public List<TagVO> listAllTag() {
        List<Tag> tags = this.query().list();
        if (tags.isEmpty()) return List.of();

        // 批量查询所有标签的文章数
        Map<Long, Long> articleCountMap = getArticleCountMap(tags);

        return tags.stream().map(tag -> tag.asViewObject(TagVO.class,
                item -> item.setArticleCount(articleCountMap.getOrDefault(tag.getId(), 0L)))).toList();
    }

    @Override
    public ResponseResult<Void> addTag(TagDTO tagDTO) {
        if (this.save(tagDTO.asViewObject(Tag.class))) return ResponseResult.success();
        return ResponseResult.failure();
    }

    @Override
    public List<TagVO> searchTag(SearchTagDTO searchTagDTO) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(searchTagDTO.getTagName()), Tag::getTagName, searchTagDTO.getTagName());
        if (StringUtils.isNotNull(searchTagDTO.getStartTime()) && StringUtils.isNotNull(searchTagDTO.getEndTime()))
            queryWrapper.between(Tag::getCreateTime, searchTagDTO.getStartTime(), searchTagDTO.getEndTime());

        List<Tag> tags = tagMapper.selectList(queryWrapper);
        if (tags.isEmpty()) return List.of();

        // 批量查询所有标签的文章数
        Map<Long, Long> articleCountMap = getArticleCountMap(tags);

        return tags.stream()
                .map(tag -> tag.asViewObject(TagVO.class,
                        item -> item.setArticleCount(articleCountMap.getOrDefault(tag.getId(), 0L))))
                .toList();
    }

    // 批量查询标签的文章数
    private Map<Long, Long> getArticleCountMap(List<Tag> tags) {
        List<Long> tagIds = tags.stream().map(Tag::getId).toList();
        Map<Long, Long> articleCountMap = new HashMap<>();
        if (!tagIds.isEmpty()) {
            articleCountMap = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getTagId, tagIds))
                    .stream()
                    .collect(Collectors.groupingBy(ArticleTag::getTagId, Collectors.counting()));
        }
        return articleCountMap;
    }

    @Override
    public TagVO getTagById(Long id) {
        return tagMapper.selectById(id).asViewObject(TagVO.class);
    }

    @Transactional
    @Override
    public ResponseResult<Void> addOrUpdateTag(TagDTO tagDTO) {
        if (this.saveOrUpdate(tagDTO.asViewObject(Tag.class))) return ResponseResult.success();
        return ResponseResult.failure();
    }

    @Transactional
    @Override
    public ResponseResult<Void> deleteTagByIds(List<Long> ids) {
        // 是否有剩下文章
        Long count = articleTagMapper.selectCount(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getTagId, ids));
        if (count > 0) return ResponseResult.failure(FunctionConst.TAG_EXIST_ARTICLE);
        // 执行删除
        if (this.removeByIds(ids)) return ResponseResult.success();
        return ResponseResult.failure();
    }
}