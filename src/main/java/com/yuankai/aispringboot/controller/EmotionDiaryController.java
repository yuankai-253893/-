package com.yuankai.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuankai.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import com.yuankai.aispringboot.DTO.query.EmotionDiaryQueryDTO;
import com.yuankai.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.enumclass.UserType;
import com.yuankai.aispringboot.service.EmotionDiaryService;
import com.yuankai.aispringboot.util.GetUserInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {
    @Autowired
    private EmotionDiaryService emotionDiaryService;

    // 创建或更新情绪日志
    @PostMapping
    public Result<EmotionDiaryResponseDTO> createOrUpdateEmotionDiary(@Valid @RequestBody  EmotionDiaryCreateDTO createOrUpdateDTO) {
        // 获取当前用户
        Long userId = GetUserInfo.getUserId();

        EmotionDiaryResponseDTO emotionDiary = emotionDiaryService.createOrUpdateEmotionDiary(userId, createOrUpdateDTO);
        return Result.success(emotionDiary);
    }

    @GetMapping("/admin/page")
    public Result<Page<EmotionDiaryResponseDTO>> getEmotionDiaryByPage(@Valid EmotionDiaryQueryDTO queryDTO) {
        // 仅管理员能够访问操作
        Integer roleType = GetUserInfo.getUserType();
        if (!UserType.ADMIN.getCode().equals(roleType))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        return Result.success(emotionDiaryService.getEmotionDiaryByPage(queryDTO));
    }

    @DeleteMapping("/admin/{id}")
    public Result<?> deleteEmotionDiary(@Min(value = 1, message = "ID不合法") @PathVariable Long id) {
        Long userId = GetUserInfo.getUserId();
        Integer roleType = GetUserInfo.getUserType();
        if (!UserType.ADMIN.getCode().equals(roleType))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        emotionDiaryService.deleteEmotionDiary(id,userId);
        return Result.success();
    }


}
