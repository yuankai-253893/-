package com.yuankai.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuankai.aispringboot.DTO.command.ArticleCreateDTO;
import com.yuankai.aispringboot.DTO.command.ArticleStatusUpdateDTO;
import com.yuankai.aispringboot.DTO.command.ArticleUpdateDTO;
import com.yuankai.aispringboot.DTO.query.ArticleListQueryDTO;
import com.yuankai.aispringboot.DTO.query.ArticlePageQueryDTO;
import com.yuankai.aispringboot.DTO.response.ArticleResponseDTO;
import com.yuankai.aispringboot.DTO.response.ArticleSimpleResponseDTO;
import com.yuankai.aispringboot.DTO.response.CategoryResponseDTO;
import com.yuankai.aispringboot.annotation.OperationLog;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.enumclass.UserType;
import com.yuankai.aispringboot.util.GetUserInfo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.yuankai.aispringboot.service.KnowledgeCategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeCategoryController {
    @Autowired
    private KnowledgeCategoryService knowledgeCategoryService;

    // 查询知识文章分类
    @GetMapping("/category/tree")
    public Result<List<CategoryResponseDTO>> getCategoryTree() {
        Long userId = GetUserInfo.getUserId();
        log.info("用户{}查询知识文章分类树", userId);

        List<CategoryResponseDTO> categoryTree = knowledgeCategoryService.getCategoryTree();

        return Result.success(categoryTree);
    }

    // 查询知识文章列表（管理端，路径加 admin 前缀避免与用户端冲突）
    @GetMapping("/admin/article/page")
    public Result<Page<ArticleSimpleResponseDTO>> getAdminArticleByPage(@Valid ArticleListQueryDTO queryDTO){
        // 仅管理员能够访问操作
        Integer roleType = GetUserInfo.getUserType();
        if (!UserType.ADMIN.getCode().equals(roleType))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        Page<ArticleSimpleResponseDTO> result = knowledgeCategoryService.getArticleByPage(queryDTO);
        log.info("管理员{}查询知识文章列表", GetUserInfo.getUserId());
        return Result.success(result);
    }

    // 查询知识文章列表（用户端：推荐阅读，按阅读量等排序）
    @GetMapping("/article/page")
    public Result<Page<ArticleSimpleResponseDTO>> getUserArticleByPage(@Valid ArticlePageQueryDTO queryDTO) {
        Page<ArticleSimpleResponseDTO> result = knowledgeCategoryService.getArticleByPage(queryDTO);

        log.info("用户{}查询知识文章列表", GetUserInfo.getUserId());
        return Result.success(result);
    }

    // 创建知识文章
    @OperationLog("新增知识文章")
    @PostMapping("/article")
    public Result<ArticleResponseDTO> createArticle(@Valid @RequestBody ArticleCreateDTO createDTO) {
        // 仅管理员能够访问操作
        Integer roleType = GetUserInfo.getUserType();
        if (!UserType.ADMIN.getCode().equals(roleType))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        Long userId = GetUserInfo.getUserId();
        ArticleResponseDTO result = knowledgeCategoryService.createArticle(createDTO, userId);

        log.info("管理员{}创建文章：{}", userId, result.getId());
        return Result.success(result);
    }

    // 查询知识文章详情
    @GetMapping("/article/{id}")
    public Result<ArticleResponseDTO> getArticleById(@PathVariable String id) {
        ArticleResponseDTO result = knowledgeCategoryService.getArticleById(id, GetUserInfo.getUserType());

        log.info("用户{}查询文章：{}", GetUserInfo.getUserId(), id);
        return Result.success(result);
    }

    // 更新知识文章
    @OperationLog("更新知识文章")
    @PutMapping("/admin/article/{id}")
    public Result<ArticleResponseDTO> updateArticle(@PathVariable String id, @Valid @RequestBody ArticleUpdateDTO updateDTO) {
        Integer roleType = GetUserInfo.getUserType();
        if (!UserType.ADMIN.getCode().equals(roleType))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        ArticleResponseDTO result = knowledgeCategoryService.updateArticle(id, updateDTO);
        log.info("管理员{}更新文章：{}", GetUserInfo.getUserId(), id);
        return Result.success(result);
    }

    // 更新知识文章状态
    @OperationLog("更新文章状态")
    @PutMapping("/admin/article/{id}/status")
    public Result<?> updateArticleStatus(@PathVariable String id, @Valid @RequestBody ArticleStatusUpdateDTO updateDTO) {
        Integer roleType = GetUserInfo.getUserType();
        if (!UserType.ADMIN.getCode().equals(roleType))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        knowledgeCategoryService.updateArticleStatus(id, updateDTO);

        log.info("管理员{}更新文章{}状态为{}", GetUserInfo.getUserId(), id, updateDTO.getStatus());
        return Result.success();
    }

    // 删除知识文章
    @OperationLog("删除知识文章")
    @DeleteMapping("/admin/article/{id}/delete")
    public Result<?> deleteArticle(@PathVariable String id) {
        Integer roleType = GetUserInfo.getUserType();
        if (!UserType.ADMIN.getCode().equals(roleType))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        knowledgeCategoryService.deleteArticle(id);
        log.info("管理员{}删除文章：{}", GetUserInfo.getUserId(), id);
        return Result.success();
    }

}
