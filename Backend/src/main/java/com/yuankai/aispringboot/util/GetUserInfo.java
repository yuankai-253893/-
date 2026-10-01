package com.yuankai.aispringboot.util;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.yuankai.aispringboot.config.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从当前请求中获取登录用户信息。
 *
 * 为什么不再每次都验签：
 * JWT 验签是一次 HMAC-SHA256 计算。原实现每个 getter 都要重新解析一遍 token，
 * 而一个 Controller 方法常常同时调用 getUserId() + getUserType()，
 * 等于一次请求验签 2-3 次，属于纯粹的重复计算。
 *
 * JwtAuthenticationFilter 验签通过后，把结果缓存到请求属性
 * {@link JwtAuthenticationFilter#JWT_USER_ATTR}，直接读取 —— 一次请求只验一次签。
 * 请求属性随请求销毁，不存在 ThreadLocal 泄漏问题（Tomcat 线程复用串号风险）。
 *
 * 兜底：非 Web 环境（如单元测试）拿不到请求上下文时，才真正走一次验签，行为保持不变。
 */
public class GetUserInfo {

    // 获取用户ID
    public static Long getUserId() {
        return getCurrentUser().getUserId();
    }

    // 获取用户名（token中的claim名为username）
    public static String getUserName() {
        return getCurrentUser().getUsername();
    }

    // 获取用户角色类型（token中的claim名为roleType，与JwtTokenUtil.generateToken保持一致）
    public static Integer getUserType() {
        return getCurrentUser().getRoleType();
    }

    /**
     * 一次性取回 userId / username / roleType。
     * 一次请求里同时需要多个字段时推荐用它，取值逻辑只走一遍。
     */
    public static JwtTokenUtil.TokenVerificationResult getCurrentUser() {
        // 1、优先读过滤器已验签的缓存结果（Web 请求场景，零验签开销）
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object cached = request.getAttribute(JwtAuthenticationFilter.JWT_USER_ATTR);
            if (cached instanceof JwtTokenUtil.TokenVerificationResult result) {
                return result;
            }
        }

        // 2、兜底：没有请求上下文（单元测试等）时才真正验签
        JwtTokenUtil.TokenVerificationResult result =
                JwtTokenUtil.validateToken(JwtTokenUtil.getCurrentToken());
        if (result == null) {
            throw new JWTVerificationException("Token 无效或已过期");
        }
        return result;
    }
}
