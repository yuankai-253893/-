package com.yuankai.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuankai.aispringboot.DTO.command.ArticleCreateDTO;
import com.yuankai.aispringboot.DTO.command.ArticleStatusUpdateDTO;
import com.yuankai.aispringboot.DTO.command.ArticleUpdateDTO;
import com.yuankai.aispringboot.DTO.query.ArticleListQueryDTO;
import com.yuankai.aispringboot.DTO.query.ArticlePageQueryDTO;
import com.yuankai.aispringboot.DTO.response.ArticleResponseDTO;
import com.yuankai.aispringboot.DTO.response.ArticleSimpleResponseDTO;
import com.yuankai.aispringboot.DTO.response.CategoryResponseDTO;
import com.yuankai.aispringboot.entity.KnowledgeArticle;
import com.yuankai.aispringboot.enumclass.UserType;
import com.yuankai.aispringboot.entity.KnowledgeCategory;
import com.yuankai.aispringboot.exception.BusinessException;
import com.yuankai.aispringboot.mapper.KnowledgeCategoryMapper;
import com.yuankai.aispringboot.mapper.KnowledgeArticleMapper;
import com.yuankai.aispringboot.service.convert.KnowledgeCategoryConvert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class KnowledgeCategoryService {
    // 分类树缓存：静态数据读多写少，全量缓存命中率最高
    private static final String CATEGORY_TREE_KEY = "knowledge:category:tree";  // 分类树缓存键
    private static final long CATEGORY_TREE_TTL_HOURS = 1;                      // 缓存有效期 1 小时

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 注入 Spring Boot 自动配置的 ObjectMapper（Jackson 3，已注册 JavaTimeModule，支持 LocalDateTime 序列化）
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Autowired
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Autowired
    private KnowledgeCategoryConvert knowledgeCategoryConvert;

    public List<CategoryResponseDTO> getCategoryTree() {
        // 1. 缓存优先：命中直接返回，避免每次全表查询（Cache Aside 读路径）
        try {
            String cached = redisTemplate.opsForValue().get(CATEGORY_TREE_KEY);
            if (StrUtil.isNotBlank(cached)) {
                log.info("分类树缓存命中");
                return objectMapper.readValue(cached, new TypeReference<List<CategoryResponseDTO>>() {});
            }
        } catch (Exception e) {
            // 缓存反序列化失败不阻塞主流程，回退查库
            log.warn("分类树缓存读取失败，回退数据库查询", e);
        }

        // 2. 缓存未命中 → 查库并挂树
        List<CategoryResponseDTO> tree = buildCategoryTreeFromDb();

        // 3. 回填缓存（TTL 兜底）
        try {
            redisTemplate.opsForValue().set(CATEGORY_TREE_KEY, objectMapper.writeValueAsString(tree),
                    CATEGORY_TREE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("分类树缓存写入失败", e);
        }
        return tree;
    }

    // 从数据库构建分类树
    private List<CategoryResponseDTO> buildCategoryTreeFromDb() {
        // 查询所有启用状态(status = 1)的分类，并按 sort_order 升序排列
        LambdaQueryWrapper<KnowledgeCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeCategory::getStatus, 1)
                .orderByAsc(KnowledgeCategory::getSortOrder);
        List<KnowledgeCategory> categories = knowledgeCategoryMapper.selectList(queryWrapper);

        // 把所有分类转成 DTO，并放进 Map（key=分类id），方便后面按 id 找父分类
        Map<Long, CategoryResponseDTO> dtoMap = new HashMap<>();
        for (KnowledgeCategory category : categories) {
            CategoryResponseDTO dto = knowledgeCategoryConvert.knowledgeCategoryToDTO(category);
            dtoMap.put(dto.getId(), dto);
        }

        // 遍历所有 DTO，挂到父分类的 children 下；parent_id=0 的作为顶级分类
        List<CategoryResponseDTO> tree = new ArrayList<>();
        for (CategoryResponseDTO dto : dtoMap.values()) {
            // 找到父分类
            CategoryResponseDTO parent = dtoMap.get(dto.getParentId());
            if (parent == null) {
                // 父分类不存在（parentId=0 或父分类已被禁用）→ 作为顶级分类
                tree.add(dto);
            } else {
                // 有父分类 → 挂到父分类的 children 下
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(dto);
            }
        }

        return tree;
    }

    // 主动失效：分类发生增删改后调用，删除缓存让下次查询重建（Cache Aside 写路径）
    public void clearCategoryTreeCache() {
        redisTemplate.delete(CATEGORY_TREE_KEY);
        log.info("分类树缓存已清除");
    }

    // 管理员端分页查询文章
    public Page<ArticleSimpleResponseDTO> getArticleByPage(ArticleListQueryDTO queryDTO) {
        // 构建分页对象
        Page<KnowledgeArticle> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getSize());
        // 构建查询条件：根据分类ID，文章标题查询文章
        LambdaQueryWrapper<KnowledgeArticle> queryWrapper = new LambdaQueryWrapper<>();
        // 条件式查询：参数有值才拼接条件，避免 category_id = null / title LIKE null 查不到数据
        queryWrapper.eq(queryDTO.getCategoryId() != null, KnowledgeArticle::getCategoryId, queryDTO.getCategoryId())
                .like(StrUtil.isNotBlank(queryDTO.getTitle()), KnowledgeArticle::getTitle, queryDTO.getTitle());

        // 如果文章状态存在，则还要使用文章状态查询
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(KnowledgeArticle::getStatus, queryDTO.getStatus());
        }

        // 如果作者名字存在，则还要使用作者名字查询
        if (StrUtil.isNotBlank(queryDTO.getAuthorName())) {
            // 通过user表将string类型的作者名字转换为long类型的作者id
            Long authorId = knowledgeCategoryConvert.getUserIdByName(queryDTO.getAuthorName());
            // 作者名不存在时不追加条件（而不是拼 author_id = null）
            queryWrapper.eq(authorId != null, KnowledgeArticle::getAuthorId, authorId);
        }

        // 按发布时间倒序排列（新发布的文章在前）
        queryWrapper.orderByDesc(KnowledgeArticle::getPublishAt);
        Page<KnowledgeArticle> articlePage = knowledgeArticleMapper.selectPage(page, queryWrapper);

        Page<ArticleSimpleResponseDTO> responsePage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        responsePage.setRecords(articlePage.getRecords().stream().map(knowledgeCategoryConvert::convertToSimpleResponseDTO).toList());
        return responsePage;
    }

    // 用户端分页查询文章
    public Page<ArticleSimpleResponseDTO> getArticleByPage(ArticlePageQueryDTO queryDTO) {
        Page<KnowledgeArticle> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getSize());
        LambdaQueryWrapper<KnowledgeArticle> queryWrapper = new LambdaQueryWrapper<>();

        // 查询已发布文章（status=1），按阅读次数降序排列
        queryWrapper.eq(KnowledgeArticle::getStatus, 1).orderByDesc(KnowledgeArticle::getReadCount);
        Page<KnowledgeArticle> articlePage = knowledgeArticleMapper.selectPage(page, queryWrapper);
        Page<ArticleSimpleResponseDTO> responsePage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        responsePage.setRecords(articlePage.getRecords().stream().map(knowledgeCategoryConvert::convertToSimpleResponseDTO).toList());
        return responsePage;
    }

    // 管理员端创建文章
    public ArticleResponseDTO createArticle(ArticleCreateDTO articleDTO, Long userId) {
        log.info("用户{}创建文章", userId);
        // 校验文章ID不重复
        if (articleDTO.getId() != null && knowledgeArticleMapper.selectById(articleDTO.getId()) != null) {
            throw new BusinessException("该文章ID已存在");
        }
        // 校验文章分类存在
        if (knowledgeCategoryMapper.selectById(articleDTO.getCategoryId()) == null) {
            throw new BusinessException("文章分类不存在");
        }
        // 校验文章存在
        LambdaQueryWrapper<KnowledgeArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeArticle::getTitle, articleDTO.getTitle())
                .eq(KnowledgeArticle::getCategoryId, articleDTO.getCategoryId());
        if (knowledgeArticleMapper.exists(queryWrapper))
            throw new BusinessException("该文章已存在");

        // 创建文章
        KnowledgeArticle knowledgeArticle = knowledgeCategoryConvert.convertToEntity(articleDTO, userId);

        // 输入数据库
        knowledgeArticleMapper.insert(knowledgeArticle);

        return knowledgeCategoryConvert.convertToResponseDTO(knowledgeArticle);
    }

    // 根据ID获取文章（roleType：当前用户角色，用于未发布文章的权限控制）
    public ArticleResponseDTO getArticleById(String id, Integer roleType) {
        // 判断文章是否存在
        KnowledgeArticle knowledgeArticle = knowledgeArticleMapper.selectById(id);
        if (knowledgeArticle == null) {
            throw new BusinessException("该文章不存在");
        }

        // 未发布(status=0)的文章仅管理员可查看；普通用户即使知道id也不能看，防止越权
        if (Integer.valueOf(0).equals(knowledgeArticle.getStatus()) && !UserType.ADMIN.getCode().equals(roleType)) {
            throw new BusinessException("该文章不存在或未发布");
        }

        // 阅读量+1：用SQL原子自增，避免并发时丢失更新；COALESCE 处理 read_count 为 NULL 的情况
        LambdaUpdateWrapper<KnowledgeArticle> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowledgeArticle::getId, id)
                .setSql("read_count = COALESCE(read_count, 0) + 1");
        knowledgeArticleMapper.update(null, updateWrapper);

        // 重新查询获取最新数据（包含自增后的阅读量）
        knowledgeArticle = knowledgeArticleMapper.selectById(id);
        return knowledgeCategoryConvert.convertToResponseDTO(knowledgeArticle);
    }

    // 更新知识文章
    public ArticleResponseDTO updateArticle(String id,ArticleUpdateDTO articleDTO) {
        if (!id.equals(articleDTO.getId()))
            throw new BusinessException("传入文章ID不匹配");

        // 校验文章分类存在
        if (knowledgeCategoryMapper.selectById(articleDTO.getCategoryId()) == null) {
            throw new BusinessException("文章分类不存在");
        }

        KnowledgeArticle knowledgeArticle = knowledgeArticleMapper.selectById(id);
        if (knowledgeArticle == null) {
            throw new BusinessException("该文章不存在");
        }

        // 检查该分类id是否存在于知识文章分类表
        if (knowledgeCategoryMapper.selectById(articleDTO.getCategoryId()) == null) {
            throw new BusinessException("文章分类不存在");
        }

        // 更新文章信息
        knowledgeArticle.setTitle(articleDTO.getTitle());
        knowledgeArticle.setContent(articleDTO.getContent());
        knowledgeArticle.setCover(articleDTO.getCoverImage());
        knowledgeArticle.setCategoryId(articleDTO.getCategoryId());
        knowledgeArticle.setSummary(articleDTO.getSummary());
        knowledgeArticle.setTags(articleDTO.getTags());
        knowledgeArticle.setUpdatedAt(LocalDateTime.now());

        // 将新的文章信息更新到数据库
        knowledgeArticleMapper.updateById(knowledgeArticle);

        ArticleResponseDTO result = knowledgeCategoryConvert.convertToResponseDTO(knowledgeArticle);

        return result;
    }

    // 更新文章状态
    public void updateArticleStatus(String id, ArticleStatusUpdateDTO updateDTO) {
        KnowledgeArticle knowledgeArticle = knowledgeArticleMapper.selectById(id);
        if (knowledgeArticle == null) {
            throw new BusinessException("该文章不存在");
        }

        if (updateDTO.getStatus() == null
                || (!Integer.valueOf(0).equals(updateDTO.getStatus()) && !Integer.valueOf(1).equals(updateDTO.getStatus()))) {
            throw new BusinessException("输入状态值错误");
        }

        knowledgeArticle.setStatus(updateDTO.getStatus());
        knowledgeArticleMapper.updateById(knowledgeArticle);
    }

    // 删除文章
    public void deleteArticle(String id) {
        KnowledgeArticle knowledgeArticle = knowledgeArticleMapper.selectById(id);
        if (knowledgeArticle == null) {
            throw new BusinessException("该文章不存在");
        }
        knowledgeArticleMapper.deleteById(id);
    }

}
