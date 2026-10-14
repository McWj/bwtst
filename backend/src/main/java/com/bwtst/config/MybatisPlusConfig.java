package com.bwtst.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.bwtst.common.TenantContext;
import com.bwtst.common.TenantSchemaMigrator;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

// add by codebuddy 20260817
// add by codebuddy 20260923: 接入 MyBatis-Plus 多租户拦截器，自动按当前租户读写 tenant_id
@Configuration
public class MybatisPlusConfig {

    /** 不参与租户隔离的表：租户表自身必须跨租户可读 */
    private static final Set<String> TENANT_IGNORE_TABLES = Set.of("sys_tenant");

    @Value("${bwtst.tenant.default-id:1}")
    private long defaultTenantId;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantSchemaMigrator tenantSchemaMigrator) {
        // 兜底租户：定时任务、启动初始化等无登录上下文的链路
        TenantContext.setDefaultTenantId(defaultTenantId);
        // 关键：必须在拦截器生效前补齐 tenant_id 列，否则首次升级时早于迁移的查询会报 Unknown column
        tenantSchemaMigrator.migrate();

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户拦截器必须早于分页拦截器添加，否则分页统计的 SQL 不会被改写
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.getTenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return tableName == null || TENANT_IGNORE_TABLES.contains(tableName.toLowerCase());
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
