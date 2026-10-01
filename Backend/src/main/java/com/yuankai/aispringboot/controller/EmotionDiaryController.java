package com.yuankai.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuankai.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import com.yuankai.aispringboot.DTO.query.EmotionDiaryQueryDTO;
import com.yuankai.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import com.yuankai.aispringboot.annotation.OperationLog;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.service.EmotionDiaryService;
import com.yuankai.aispringboot.util.GetUserInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {
    @Autowired
    private EmotionDiaryService emotionDiaryService;

    // 创建或更新情绪日志
    @OperationLog("创建/更新情绪日记")
    @PostMapping
    public Result<EmotionDiaryResponseDTO> createOrUpdateEmotionDiary(@Valid @RequestBody  EmotionDiaryCreateDTO createOrUpdateDTO) {
        // 获取当前用户
        Long userId = GetUserInfo.getUserId();

        EmotionDiaryResponseDTO emotionDiary = emotionDiaryService.createOrUpdateEmotionDiary(userId, createOrUpdateDTO);
        return Result.success(emotionDiary);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/page")
    public Result<Page<EmotionDiaryResponseDTO>> getEmotionDiaryByPage(@Valid EmotionDiaryQueryDTO queryDTO) {
        return Result.success(emotionDiaryService.getEmotionDiaryByPage(queryDTO));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog("删除情绪日记")
    @DeleteMapping("/admin/{id}")
    public Result<?> deleteEmotionDiary(@Min(value = 1, message = "ID不合法") @PathVariable Long id) {
        Long userId = GetUserInfo.getUserId();
        emotionDiaryService.deleteEmotionDiary(id,userId);
        return Result.success();
    }


}
