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

@Builder
@Data
@TableName("knowledge_category")
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCategory {
    // 分类ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 父分类ID
    @TableField("parent_id")
    private Long parentId;

    // 分类名称
    @TableField("category_name")
    private String categoryName;

    // 分类代码
    @TableField("category_code")
    private String categoryCode;

    // 分类描述
    private String description;

    // 排序
    @TableField("sort_order")
    private Integer sortOrder;

    // 状态 0:禁用 1:正常
    @Builder.Default
    private Integer status = 1;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
