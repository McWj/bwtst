package com.bwtst.config;

import com.bwtst.common.JwtUtil;
import com.bwtst.common.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

// add by codebuddy 20260923: 认证过滤器——校验 Authorization 令牌，解析出租户与用户身份写入 TenantContext
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 只拦截后端业务接口，静态资源与前端路由放行
        if (!uri.startsWith("/api/")) {
            return true;
        }
        // 跨域预检放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        // 认证接口放行（业务登录 /login 与平台管理端登录 /platform-login）
        return uri.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            token = header.substring(BEARER_PREFIX.length()).trim();
        }
        Map<String, Object> claims = jwtUtil.parse(token);
        if (claims == null) {
            writeUnauthorized(request, response, "未登录或登录已过期");
            return;
        }
        try {
            Long tenantId = claims.get("tid") instanceof Number n ? n.longValue() : null;
            Integer userId = claims.get("uid") instanceof Number n ? n.intValue() : null;
            String username = claims.get("sub") == null ? null : String.valueOf(claims.get("sub"));
            TenantContext.set(tenantId, userId, username);
            // add by codebuddy 20260923: 平台超级管理员标志
            TenantContext.setPlatformAdmin(claims.get("pa") instanceof Number n && n.intValue() == 1);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        // 401 由过滤器直接返回，绕过了 MVC 的 CORS 处理，这里补上跨域头
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
