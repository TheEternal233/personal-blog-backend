package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.kuailemao.constants.FunctionConst;
import xyz.kuailemao.domain.dto.CategoryDTO;
import xyz.kuailemao.domain.dto.SearchCategoryDTO;
import xyz.kuailemao.domain.entity.Article;
import xyz.kuailemao.domain.entity.Category;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.CategoryVO;
import xyz.kuailemao.mapper.ArticleMapper;
import xyz.kuailemao.mapper.CategoryMapper;
import xyz.kuailemao.service.CategoryService;
import xyz.kuailemao.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * (Category)表服务实现类
 *
 * @author kuailemao
 * @since 2023-10-15 02:29:14
 */
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listAllCategory() {
        List<Category> categories = this.query().list();
        if (categories.isEmpty()) return List.of();

        // 批量查询所有分类的文章数
        Map<Long, Long> articleCountMap = getCategoryArticleCountMap(categories);

        return categories.stream().map(category -> category.asViewObject(CategoryVO.class, item -> {
            item.setArticleCount(articleCountMap.getOrDefault(category.getId(), 0L));
        })).toList();
    }

    @Override
    public ResponseResult<Void> addCategory(CategoryDTO categoryDTO) {
        categoryDTO.setId(null);
        if (this.save(categoryDTO.asViewObject(Category.class))) return ResponseResult.success();
        return ResponseResult.failure();
    }

    @Override
    public List<CategoryVO> searchCategory(SearchCategoryDTO searchCategoryDTO) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(searchCategoryDTO.getCategoryName()), Category::getCategoryName, searchCategoryDTO.getCategoryName());
        if (StringUtils.isNotNull(searchCategoryDTO.getStartTime()) && StringUtils.isNotNull(searchCategoryDTO.getEndTime()))
            queryWrapper.between(Category::getCreateTime, searchCategoryDTO.getStartTime(), searchCategoryDTO.getEndTime());

        List<Category> categories = categoryMapper.selectList(queryWrapper);
        if (categories.isEmpty()) return List.of();

        // 批量查询所有分类的文章数
        Map<Long, Long> articleCountMap = getCategoryArticleCountMap(categories);

        return categories.stream()
                .map(category -> category.asViewObject(CategoryVO.class,
                        item -> item.setArticleCount(articleCountMap.getOrDefault(category.getId(), 0L))))
                .toList();
    }

    // 批量查询分类的文章数
    private Map<Long, Long> getCategoryArticleCountMap(List<Category> categories) {
        List<Long> categoryIds = categories.stream().map(Category::getId).toList();
        Map<Long, Long> articleCountMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            articleCountMap = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>().in(Article::getCategoryId, categoryIds))
                    .stream()
                    .collect(Collectors.groupingBy(Article::getCategoryId, Collectors.counting()));
        }
        return articleCountMap;
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        return categoryMapper.selectById(id).asViewObject(CategoryVO.class);
    }

    @Transactional
    @Override
    public ResponseResult<Void> addOrUpdateCategory(CategoryDTO categoryDTO) {
        if (this.saveOrUpdate(categoryDTO.asViewObject(Category.class))) return ResponseResult.success();
        return ResponseResult.failure();
    }

    @Transactional
    @Override
    public ResponseResult<Void> deleteCategoryByIds(List<Long> ids) {
        // 是否有剩下文章
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<Article>().in(Article::getCategoryId, ids));
        if (count > 0) return ResponseResult.failure(FunctionConst.CATEGORY_EXIST_ARTICLE);
        // 执行删除
        if (this.removeByIds(ids)) return ResponseResult.success();
        return ResponseResult.failure();
    }
}