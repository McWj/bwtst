package com.bwtst.common;

// add by codebuddy 20260923: SaaS 多租户上下文——保存当前请求的租户与登录用户身份（ThreadLocal）
public final class TenantContext {

    /** 默认租户 id：无请求上下文时兜底使用（定时任务、启动初始化、未登录的兼容调用） */
    private static volatile long defaultTenantId = 1L;

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    // add by codebuddy 20260923: 平台超级管理员标志（可跨租户管理租户）
    private static final ThreadLocal<Boolean> PLATFORM_ADMIN = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setDefaultTenantId(long id) {
        defaultTenantId = id;
    }

    public static long getDefaultTenantId() {
        return defaultTenantId;
    }

    public static void set(Long tenantId, Integer userId, String username) {
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    /**
     * 当前租户 id。
     * 无上下文时返回默认租户，保证定时任务/启动初始化等非请求链路仍可正常读写数据（过渡期兼容）。
     */
    public static long getTenantId() {
        Long id = TENANT_ID.get();
        return id == null ? defaultTenantId : id;
    }

    public static Integer getUserId() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    /** 是否处于已登录的请求上下文 */
    public static boolean isAuthenticated() {
        return USER_ID.get() != null;
    }

    public static void setPlatformAdmin(boolean flag) {
        PLATFORM_ADMIN.set(flag);
    }

    /** 是否平台超级管理员（可跨租户管理租户） */
    public static boolean isPlatformAdmin() {
        return Boolean.TRUE.equals(PLATFORM_ADMIN.get());
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        USERNAME.remove();
        PLATFORM_ADMIN.remove();
    }
}
