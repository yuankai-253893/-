package com.yuankai.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_article")
@Builder        // 实体转成数据
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle{
    // 文章ID（UUID）
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    // 文章分类ID
    @TableField("category_id")
    private Long categoryId;

    // 文章标题
    private String title;

    // 文章摘要
    private String summary;

    // 文章内容
    private String content;

    // 封面图片
    @TableField("cover_image")
    private String cover;

    // 标签
    private String tags;

    // 作者ID
    @TableField("author_id")
    private Long authorId;

    // 阅读次数
    @TableField("read_count")
    private Integer readCount;

    // 文章状态
    private Integer status;

    // 发布时间
    @TableField("published_at")
    private LocalDateTime publishAt;


    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
