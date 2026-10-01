package com.yuankai.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 由 AOP 切面 {@code OperationLogAspect} 自动写入，记录"谁、何时、调了哪个接口、耗时、结果"
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 ID（公开接口如登录/注册为 null） */
    @TableField("user_id")
    private Long userId;

    /** 操作用户名（公开接口从请求参数中提取） */
    private String username;

    /** 操作描述，如"新增知识文章" */
    private String operation;

    /** HTTP 方法：POST / PUT / DELETE */
    private String method;

    /** 请求地址 */
    @TableField("request_url")
    private String requestUrl;

    /** 请求参数（已脱敏、超长截断） */
    private String params;

    /** 耗时（毫秒） */
    @TableField("cost_time")
    private Long costTime;

    /** 执行结果：1 成功，0 失败 */
    private Integer status;

    /** 异常信息（失败时记录，截断 500 字符） */
    @TableField("error_msg")
    private String errorMsg;

    /** 创建时间：数据库默认 CURRENT_TIMESTAMP */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
