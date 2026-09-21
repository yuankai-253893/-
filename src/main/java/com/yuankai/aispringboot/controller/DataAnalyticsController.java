package com.yuankai.aispringboot.controller;

import com.yuankai.aispringboot.DTO.response.DataAnalyticsResponseDTO;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.enumclass.UserType;
import com.yuankai.aispringboot.service.DataAnalyticsService;
import com.yuankai.aispringboot.util.GetUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/data-analytics/overview")
public class DataAnalyticsController {
    @Autowired
    private DataAnalyticsService dataAnalyticsService;

    @GetMapping
    public Result<DataAnalyticsResponseDTO> getDataAnalytics() {
        Integer roleType = GetUserInfo.getUserType();
        if (!roleType.equals(UserType.ADMIN.getCode()))
            return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);

        DataAnalyticsResponseDTO result = dataAnalyticsService.getDataAnalytics();

        log.info("用户{}获取综合数据分析", GetUserInfo.getUserId());
        return Result.success(result);
    }
}
