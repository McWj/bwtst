package com.bwtst.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// add by codebuddy 20260923: SaaS 多租户结构迁移。
// 必须在 MyBatis-Plus 租户拦截器启用之前执行，否则任何早于迁移的查询都会因缺少 tenant_id 列而报错。
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    /** 迁移只执行一次 */
    private volatile boolean migrated = false;

    /** 需要租户隔离的业务表（sys_tenant 自身不参与隔离） */
    private static final String[] TENANT_TABLES = {
            "delivery_shipment", "delivery_shipment_detail",
            "distribution", "distribution_detail", "distribution_center",
            "distribution_return", "distribution_return_detail",
            "goods", "goods_shop",
            "ptemplate", "ptemplate_goods", "ptemplate_shop",
            "purchase", "purchase_detail",
            "purchase_inbound", "purchase_inbound_detail",
            "purchase_return", "purchase_return_detail",
            "requisition", "requisition_detail",
            "shop", "supplier", "supplier_price",
            "sys_config", "sys_user", "sys_user_role", "sys_user_scope",
            "warehouse_return", "warehouse_return_detail"
    };

    /** 原全局唯一索引 → 租户内唯一索引（表, 旧索引名, 新索引名, 索引列） */
    private static final String[][] TENANT_UNIQUE_KEYS = {
            {"distribution", "distribution_index", "uk_tenant_distribution_no", "tenant_id, distribution_no"},
            {"distribution_detail", "requisition_detail_index", "uk_tenant_distribution_detail",
                    "tenant_id, distribution_id, seq"},
            {"distribution_return", "distribution_return_index", "uk_tenant_distribution_return_no",
                    "tenant_id, distribution_return_no"},
            {"distribution_return_detail", "distribution_return_detail_index", "uk_tenant_distribution_return_detail",
                    "tenant_id, distribution_return_id, seq"},
            {"goods", "goods_item_no", "uk_tenant_goods_item_no", "tenant_id, item_no"},
            {"goods_shop", "goods_shop_item", "uk_tenant_goods_shop_item", "tenant_id, shop_id, item_id"},
            {"ptemplate", "ptemplate_no_index", "uk_tenant_ptemplate_no", "tenant_id, ptemplate_no"},
            {"ptemplate_goods", "ptemplate_goods_index", "uk_tenant_ptemplate_goods", "tenant_id, ptemplate_id, seq"},
            {"ptemplate_shop", "ptemplate_shop_index", "uk_tenant_ptemplate_shop", "tenant_id, ptemplate_id, shop_id"},
            {"purchase", "requisition_index", "uk_tenant_purchase_no", "tenant_id, purchase_no"},
            {"purchase_detail", "purchase_detail_index", "uk_tenant_purchase_detail", "tenant_id, purchase_id, seq"},
            {"purchase_return", "purchase_return_index", "uk_tenant_purchase_return_no",
                    "tenant_id, purchase_return_no"},
            {"purchase_return_detail", "purchase_detail_index", "uk_tenant_purchase_return_detail",
                    "tenant_id, purchase_return_id, seq"},
            {"requisition", "requisition_index", "uk_tenant_requisition_no", "tenant_id, requisition_no, site_id"},
            {"requisition_detail", "requisition_detail_index", "uk_tenant_requisition_detail",
                    "tenant_id, requisition_id, seq"},
            {"shop", "shop_no_index", "uk_tenant_shop_no", "tenant_id, shop_no"},
            {"supplier", "uk_supplier_code", "uk_tenant_supplier_code", "tenant_id, supplier_code"},
            {"sys_user", "username", "uk_tenant_username", "tenant_id, username"}
    };

    /**
     * 幂等迁移：建租户表 → 预置默认租户 → 业务表补 tenant_id → 全局唯一键改租户内唯一。
     * 存量数据统一归入默认租户（id=1）。失败不阻断启动。
     */
    public synchronized void migrate() {
        if (migrated) {
            return;
        }
        try {
            createTable("sys_tenant",
                    "CREATE TABLE IF NOT EXISTS sys_tenant (" +
                            " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            " tenant_code VARCHAR(64) NOT NULL," +
                            " tenant_name VARCHAR(200) NOT NULL," +
                            " status VARCHAR(1) NOT NULL DEFAULT '1'," +
                            " erp_url VARCHAR(255) NULL," +
                            " erp_digi_key VARCHAR(128) NULL," +
                            " erp_prod VARCHAR(64) NULL," +
                            " erp_acct VARCHAR(64) NULL," +
                            " erp_ent_id VARCHAR(32) NULL," +
                            " create_datetime DATETIME NULL," +
                            " UNIQUE KEY uk_tenant_code (tenant_code)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            Long tenantCnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_tenant", Long.class);
            if (tenantCnt == null || tenantCnt == 0) {
                jdbcTemplate.update(
                        "INSERT INTO sys_tenant (id, tenant_code, tenant_name, status, create_datetime)"
                                + " VALUES (1, ?, ?, '1', NOW())",
                        "bwcs", "广东横琴源麦商秋供应链管理有限公司");
                log.info("已预置默认租户 bwcs(id=1)，存量数据统一归入该租户");
            }

            for (String t : TENANT_TABLES) {
                addColumnIfMissing(t, "tenant_id",
                        "ALTER TABLE " + t + " ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1");
                addIndexIfMissing(t, "idx_" + t + "_tenant_id",
                        "CREATE INDEX idx_" + t + "_tenant_id ON " + t + " (tenant_id)");
            }

            for (String[] uk : TENANT_UNIQUE_KEYS) {
                migrateUniqueKey(uk[0], uk[1], uk[2], uk[3]);
            }

            // add by codebuddy 20260923: 密码哈希由 MD5 升级为 PBKDF2(约 84 字符)，需扩展存储列宽度
            addColumnLengthIfTooShort("sys_user", "password", 200,
                    "ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(200) NOT NULL");

            // add by codebuddy 20260923: 平台超级管理员标志列
            addColumnIfMissing("sys_user", "is_platform_admin",
                    "ALTER TABLE sys_user ADD COLUMN is_platform_admin VARCHAR(1) NOT NULL DEFAULT '0'");
            // 系统首个超管默认为默认租户的 admin，保证升级后仍有人能管理租户
            Long paCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE is_platform_admin = '1'", Long.class);
            if (paCnt == null || paCnt == 0) {
                int set = jdbcTemplate.update(
                        "UPDATE sys_user SET is_platform_admin = '1' WHERE tenant_id = 1 AND username = 'admin'");
                if (set > 0) {
                    log.info("已将默认租户的 admin 设置为平台超级管理员");
                }
            }

            migrated = true;
            log.info("SaaS 多租户结构迁移完成，已覆盖 {} 张业务表", TENANT_TABLES.length);
        } catch (Exception e) {
            log.warn("SaaS 多租户结构迁移失败(不影响启动，但隔离可能不完整): {}", e.getMessage());
        }
    }

    /** add by codebuddy 20260923: 列长度不足时扩展（幂等） */
    private void addColumnLengthIfTooShort(String table, String column, int minLength, String ddl) {
        Integer len = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (len != null && len < minLength) {
            jdbcTemplate.execute(ddl);
            log.info("已扩展 {}.{} 列长度 {} -> {}", table, column, len, minLength);
        }
    }

    /** 幂等迁移唯一索引——先删旧全局唯一索引，再按租户维度重建 */
    private void migrateUniqueKey(String table, String oldIndex, String newIndex, String columns) {
        if (indexExists(table, newIndex)) {
            return;
        }
        try {
            if (!oldIndex.equals(newIndex) && indexExists(table, oldIndex)) {
                // 一条语句内完成删除与重建，避免中途失败导致唯一约束丢失
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP INDEX " + oldIndex
                        + ", ADD UNIQUE KEY " + newIndex + " (" + columns + ")");
            } else {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD UNIQUE KEY " + newIndex + " (" + columns + ")");
            }
        } catch (Exception e) {
            log.warn("租户内唯一索引迁移失败 {}.{}: {}", table, newIndex, e.getMessage());
        }
    }

    private void createTable(String table, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndexIfMissing(String table, String index, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class, table, index);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private boolean indexExists(String table, String index) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class, table, index);
        return count != null && count > 0;
    }
}
