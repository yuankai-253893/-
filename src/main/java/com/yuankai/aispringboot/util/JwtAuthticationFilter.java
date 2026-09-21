package com.yuankai.aispringboot.util;

import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.config.SecurityConfig;
import com.yuankai.aispringboot.entity.User;
import com.yuankai.aispringboot.mapper.UserMapper;
import com.yuankai.aispringboot.service.RedisTokenBlacklist;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class JwtAuthticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthticationFilter.class);

    @Resource
    private RedisTokenBlacklist redisTokenBlacklist;

    @Resource
    private UserMapper userMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        // 检查是否为公开路径
        return SecurityConfig.isPublicPath(requestUrl);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 获取请求的URI和方法
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();
        log.debug("Request URI: {}, Method: {}", requestUri, requestMethod);

        // 1、提取token
        String token = JwtTokenUtil.extractTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            // 2、验证token并获取用户信息
            JwtTokenUtil.TokenVerificationResult validationResult = JwtTokenUtil.validateToken(token);

            if (validationResult != null && validationResult.isValid()) {
                // 3、检查 Token 是否在黑名单中
                if (redisTokenBlacklist.isBlacklisted(token)) {
                    clearSecurityContext();
                    ResponseUtil.writeError(response, ResultCode.TOKEN_BLOCKED);
                    return;
                }

                // 4、校验用户状态：用户被禁用或已删除时，旧 token 立即失效
                User user = userMapper.selectById(validationResult.getUserId());
                if (user == null || !user.isActive()) {
                    clearSecurityContext();
                    ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
                    return;
                }

                log.debug("JWT验证通过, 用户: {}", validationResult.getUsername());

                // 5、创建Spring Security认证对象（用户信息已校验，直接使用token携带的角色）
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + validationResult.getRoleType())
                );

                // 创建UsernamePasswordAuthenticationToken对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        validationResult.getUsername(),
                        null,
                        authorities     // 权限列表
                );

                // 设置认证信息到Spring Security上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 将Token存储到请求属性中
                request.setAttribute("jwtToken", token);
            }else {
                clearSecurityContext();
                ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
            }
        }else {
            // 清理上下文
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }


    // 清理Spring Security上下文
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

}
