package com.yuankai.aispringboot.service.convert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuankai.aispringboot.DTO.command.ArticleCreateDTO;
import com.yuankai.aispringboot.DTO.response.ArticleResponseDTO;
import com.yuankai.aispringboot.DTO.response.ArticleSimpleResponseDTO;
import com.yuankai.aispringboot.DTO.response.CategoryResponseDTO;
import com.yuankai.aispringboot.entity.KnowledgeArticle;
import com.yuankai.aispringboot.entity.KnowledgeCategory;
import com.yuankai.aispringboot.entity.User;
import com.yuankai.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class KnowledgeCategoryConvert {
    @Autowired
    private UserMapper userMapper;

    // 知识文章转simple返回DTO，用于分页查询文章
    public ArticleSimpleResponseDTO convertToSimpleResponseDTO(KnowledgeArticle knowledgeArticle) {
        return ArticleSimpleResponseDTO.builder()
                .id(knowledgeArticle.getId())
                .categoryId(knowledgeArticle.getCategoryId())
                .title(knowledgeArticle.getTitle())
                .summary(knowledgeArticle.getSummary())
                .tags(knowledgeArticle.getTags())
                .authorName(getAuthorNameById(knowledgeArticle.getAuthorId()))
                .readCount(knowledgeArticle.getReadCount())
                .status(knowledgeArticle.getStatus())
                .publishAt(knowledgeArticle.getPublishAt())
                .createdAt(knowledgeArticle.getCreatedAt())
                .updatedAt(knowledgeArticle.getUpdatedAt())
                .build();
    }

    // 知识文章分类表实体转DTO
    public CategoryResponseDTO knowledgeCategoryToDTO(KnowledgeCategory knowledgeCategory) {
        return CategoryResponseDTO.builder()
                .id(knowledgeCategory.getId())
                .parentId(knowledgeCategory.getParentId())
                .categoryName(knowledgeCategory.getCategoryName())
                .categoryCode(knowledgeCategory.getCategoryCode())
                .description(knowledgeCategory.getDescription())
                .sortOrder(knowledgeCategory.getSortOrder())
                .status(knowledgeCategory.getStatus())
                .createdAt(knowledgeCategory.getCreatedAt())
                .updatedAt(knowledgeCategory.getUpdatedAt())
                .build();
    }

    // 文章创建DTO转实体
    public KnowledgeArticle convertToEntity(ArticleCreateDTO articleDTO, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return KnowledgeArticle.builder()
                .id(articleDTO.getId())
                .title(articleDTO.getTitle())
                .categoryId(articleDTO.getCategoryId())
                .summary(articleDTO.getSummary())
                .content(articleDTO.getContent())
                .cover(articleDTO.getCoverImage())
                .tags(articleDTO.getTags())
                .authorId(userId)
                .readCount(0)
                .status(1)
                .publishAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // 知识文章转返回DTO
    public ArticleResponseDTO convertToResponseDTO(KnowledgeArticle knowledgeArticle) {
        return ArticleResponseDTO.builder()
                .id(knowledgeArticle.getId())
                .categoryId(knowledgeArticle.getCategoryId())
                .title(knowledgeArticle.getTitle())
                .summary(knowledgeArticle.getSummary())
                .readCount(knowledgeArticle.getReadCount())
                .content(knowledgeArticle.getContent())
                .cover(knowledgeArticle.getCover())
                .tags(knowledgeArticle.getTags())
                .authorId(knowledgeArticle.getAuthorId())
                .status(knowledgeArticle.getStatus())
                .publishAt(knowledgeArticle.getPublishAt())
                .createdAt(knowledgeArticle.getCreatedAt())
                .updatedAt(knowledgeArticle.getUpdatedAt())
                .build();
    }

    // 根据作者ID查询作者名称（返回DTO用）
    public String getAuthorNameById(Long authorId) {
        if (authorId == null) {
            return null;
        }
        User user = userMapper.selectById(authorId);
        if (user == null) {
            return null;
        }
        return user.getUsername();
    }

    // 根据作者名称查询作者ID（创建DTO用）
    public Long getUserIdByName(String authorName) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, authorName);
        User user = userMapper.selectOne(queryWrapper);
        // 作者名不存在时返回 null，调用方会跳过该条件，避免 NPE
        if (user == null) {
            return null;
        }
        return user.getId();
    }

}
