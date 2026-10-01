package com.yuankai.aispringboot.config;


import cn.hutool.core.text.AntPathMatcher;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.util.JwtAuthticationFilter;
import com.yuankai.aispringboot.util.ResponseUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static final String[] PUBLIC_PATH = {
            "/",
            "/api/user/login",
            "/api/user/add",
            "/files/**",
    };

    public static Boolean isPublicPath(String requestUrl) {
        for (String path : PUBLIC_PATH) {
            if (antPathMatcher.match(path, requestUrl)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public JwtAuthticationFilter jwtAuthticationFilter() {
        return new JwtAuthticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护（API服务通常不需要）
                .csrf(AbstractHttpConfigurer::disable)  //固定写法
                // 禁用匿名认证：否则无 token 的请求会被 Spring Security 视为"已认证的匿名主体"，
                // 拒绝时走 accessDeniedHandler 返回 A0301"访问未授权"；禁用后正确走
                // authenticationEntryPoint 返回 401"暂未登录或token已经过期"
                .anonymous(AbstractHttpConfigurer::disable)
                // 配置会话管理为无状态（JWT需要）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //配置请求的授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开的路径，无需登录即可访问
                        .requestMatchers(PUBLIC_PATH).permitAll()
                        // 其他请求都需要认证
                        .anyRequest().authenticated()
                )
                // 鉴权失败的统一出口：不配这两个处理器，@PreAuthorize 拒绝请求时抛出的
                // AccessDeniedException 会走 Spring Security 默认逻辑返回 403 空白页，
                // 前端拿不到 {code,msg,data}。这里复用 ResponseUtil，返回体与手写判断时完全一致。
                .exceptionHandling(ex -> ex
                        // 未认证（匿名访问受保护资源）
                        .authenticationEntryPoint((request, response, authException) ->
                                ResponseUtil.writeError(response, ResultCode.UNAUTHORIZED))
                        // 已认证但权限不足（@PreAuthorize 校验失败）
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED))
                )
                // 添加JWT认证过滤器（过滤器方法，过滤器类型）
                .addFilterBefore(jwtAuthticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
