package com.yuankai.aispringboot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 标注在需要记录操作日志的 Controller 方法上，由 OperationLogAspect 切面自动记录：
 * 操作人、操作描述、接口地址、请求参数（脱敏）、耗时、成功/失败。
 *
 * 用法：
 * <pre>
 *     @OperationLog("新增知识文章")
 *     @PostMapping("/article")
 *     public Result<...> addArticle(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作描述，如"用户登录"、"新增知识文章" */
    String value();
}
