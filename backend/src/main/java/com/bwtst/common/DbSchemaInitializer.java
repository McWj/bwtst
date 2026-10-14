package com.bwtst.common;

import com.bwtst.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// add by codebuddy 20260817: 启动时幂等创建新增的表结构并预置配送中心数据，失败不影响主流程
@Slf4j
@Component
@RequiredArgsConstructor
public class DbSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    // add by codebuddy 20260923: 多租户结构迁移组件（同包，无需 import）
    private final TenantSchemaMigrator tenantSchemaMigrator;

    @Override
    public void run(String... args) {
        try {
            createTable("distribution_center",
                    "CREATE TABLE IF NOT EXISTS distribution_center (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " dc_no VARCHAR(50) NOT NULL," +
                            " dc_name VARCHAR(200) NOT NULL" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            createTable("delivery_shipment",
                    "CREATE TABLE IF NOT EXISTS delivery_shipment (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " shipment_no VARCHAR(50)," +
                            " shipment_date DATE," +
                            " volume DECIMAL(18,4)," +
                            " weight DECIMAL(18,4)," +
                            " erp_no VARCHAR(50)," +
                            // add by codebuddy 20260817: ERP配送单号（发货前必填）
                            " erp_shipment_no VARCHAR(50)," +
                            // add by codebuddy 20260817: 发货状态：空=未发货，Y=已发货待上传，SC=已上传
                            " status VARCHAR(10)," +
                            " delivery_center_id INT," +
                            " creator VARCHAR(50)," +
                            " create_datetime DATETIME" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // add by codebuddy 20260817: 配送发货单表增加 ERP配送单号、发货状态列
            addColumnIfMissing("delivery_shipment", "erp_shipment_no",
                    "ALTER TABLE delivery_shipment ADD COLUMN erp_shipment_no VARCHAR(50) NULL");
            addColumnIfMissing("delivery_shipment", "status",
                    "ALTER TABLE delivery_shipment ADD COLUMN status VARCHAR(10) NULL");
            // add by codebuddy 20260818: 配送发货单表增加收货门店id列
            addColumnIfMissing("delivery_shipment", "site_id",
                    "ALTER TABLE delivery_shipment ADD COLUMN site_id INT NULL");

            createTable("delivery_shipment_detail",
                    "CREATE TABLE IF NOT EXISTS delivery_shipment_detail (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " shipment_id VARCHAR(50)," +
                            " seq INT," +
                            " order_seq INT," +
                            " item_id INT," +
                            " qty DECIMAL(18,4)," +
                            " lot_no VARCHAR(50)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // add by codebuddy 20260818: 配送中心采购入库单
            createTable("purchase_inbound",
                    "CREATE TABLE IF NOT EXISTS purchase_inbound (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " purchase_no VARCHAR(50)," +
                            " purchase_date DATE," +
                            " supplier_code VARCHAR(50)," +
                            " delivery_center_id INT," +
                            " erp_no VARCHAR(50)," +
                            " status VARCHAR(10)," +
                            " creator VARCHAR(50)," +
                            " create_datetime DATETIME" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            createTable("purchase_inbound_detail",
                    "CREATE TABLE IF NOT EXISTS purchase_inbound_detail (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " purchase_id VARCHAR(50)," +
                            " seq INT," +
                            " item_id INT," +
                            " unit VARCHAR(20)," +
                            " qty DECIMAL(18,4)," +
                            " price DECIMAL(18,4)," +
                            " amount DECIMAL(18,4)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // add by codebuddy 20260818: 配送中心采购仓退单
            createTable("warehouse_return",
                    "CREATE TABLE IF NOT EXISTS warehouse_return (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " return_no VARCHAR(50)," +
                            " return_date DATE," +
                            " supplier_code VARCHAR(50)," +
                            " delivery_center_id INT," +
                            " erp_no VARCHAR(50)," +
                            " status VARCHAR(10)," +
                            " creator VARCHAR(50)," +
                            " create_datetime DATETIME" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            createTable("warehouse_return_detail",
                    "CREATE TABLE IF NOT EXISTS warehouse_return_detail (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " return_id VARCHAR(50)," +
                            " seq INT," +
                            " item_id INT," +
                            " unit VARCHAR(20)," +
                            " qty DECIMAL(18,4)," +
                            " price DECIMAL(18,4)," +
                            " amount DECIMAL(18,4)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            addColumnIfMissing("goods_shop", "delivery_center_id",
                    "ALTER TABLE goods_shop ADD COLUMN delivery_center_id INT NULL");

            // add by codebuddy 20260817: 门店商品表增加主供应商id列（关联 supplier.id）
            addColumnIfMissing("goods_shop", "supplier_id",
                    "ALTER TABLE goods_shop ADD COLUMN supplier_id INT NULL");

            // add by codebuddy 20260817: 要货单表增加 ERP 单号字段，支持保存多张单（|分隔），故长度取 200
            addColumnIfMissing("requisition", "erp_no",
                    "ALTER TABLE requisition ADD COLUMN erp_no VARCHAR(200) NULL");

            // add by codebuddy 20260817: 要货单/配送收货单/配送退货单主表增加配送中心id字段
            addColumnIfMissing("requisition", "delivery_center_id",
                    "ALTER TABLE requisition ADD COLUMN delivery_center_id INT NULL");
            addColumnIfMissing("distribution", "delivery_center_id",
                    "ALTER TABLE distribution ADD COLUMN delivery_center_id INT NULL");
            addColumnIfMissing("distribution_return", "delivery_center_id",
                    "ALTER TABLE distribution_return ADD COLUMN delivery_center_id INT NULL");
            // add by codebuddy 20260817: 配送退货单主表增加来源配送收货单字段
            addColumnIfMissing("distribution_return", "source_distribution_id",
                    "ALTER TABLE distribution_return ADD COLUMN source_distribution_id INT NULL");
            addColumnIfMissing("distribution_return", "source_distribution_no",
                    "ALTER TABLE distribution_return ADD COLUMN source_distribution_no VARCHAR(64) NULL");

            // add by codebuddy 20260817: 系统配置表，用于首页配置定时任务间隔等
            createTable("sys_config",
                    "CREATE TABLE IF NOT EXISTS sys_config (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " config_key VARCHAR(100) NOT NULL," +
                            " config_value VARCHAR(500)," +
                            " remark VARCHAR(200)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // add by codebuddy 20260817: 预置要货单ERP上传定时任务间隔（分钟），默认 3 分钟
            // add by codebuddy 20260923: 预置只针对默认租户，避免全局判断导致新租户缺少自己的配置
            Long cfgCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_config WHERE config_key = ? AND tenant_id = 1",
                    Long.class, SysConfigService.KEY_UPLOAD_INTERVAL_MINUTES);
            if (cfgCnt == null || cfgCnt == 0) {
                jdbcTemplate.update(
                        "INSERT INTO sys_config (config_key, config_value, remark, tenant_id) VALUES (?, ?, ?, 1)",
                        SysConfigService.KEY_UPLOAD_INTERVAL_MINUTES, "3", "要货单ERP上传定时任务间隔（分钟）");
            }

            // add by codebuddy 20260818: 预置定时任务开关，默认停用，避免用户手动关闭后刷新页面又被自动开启
            Long enCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_config WHERE config_key = ? AND tenant_id = 1",
                    Long.class, SysConfigService.KEY_UPLOAD_ENABLED);
            if (enCnt == null || enCnt == 0) {
                jdbcTemplate.update(
                        "INSERT INTO sys_config (config_key, config_value, remark, tenant_id) VALUES (?, ?, ?, 1)",
                        SysConfigService.KEY_UPLOAD_ENABLED, "false", "要货单ERP上传定时任务开关（true-启用/false-停用）");
            }

            // add by codebuddy 20260817: 门店商品(shop_id, item_id)联合索引，保障价格批量更新性能
            addIndexIfMissing("goods_shop", "idx_goods_shop_shop_item",
                    "CREATE INDEX idx_goods_shop_shop_item ON goods_shop (shop_id, item_id)");

            // add by codebuddy 20260818: 给 goods_shop/ptemplate_shop 加归属字段索引，避免按配送中心/门店过滤时全表扫描
            addIndexIfMissing("goods_shop", "idx_goods_shop_delivery_center",
                    "CREATE INDEX idx_goods_shop_delivery_center ON goods_shop (delivery_center_id)");
            addIndexIfMissing("goods_shop", "idx_goods_shop_shop_id",
                    "CREATE INDEX idx_goods_shop_shop_id ON goods_shop (shop_id)");
            addIndexIfMissing("ptemplate_shop", "idx_ptemplate_shop_shop_id",
                    "CREATE INDEX idx_ptemplate_shop_shop_id ON ptemplate_shop (shop_id)");

            // add by codebuddy 20260818: 数据总览只统计单据主表，按单据自带归属id过滤，为各单据表归属字段加索引
            addIndexIfMissing("requisition", "idx_requisition_site_id",
                    "CREATE INDEX idx_requisition_site_id ON requisition (site_id)");
            addIndexIfMissing("distribution", "idx_distribution_site_id",
                    "CREATE INDEX idx_distribution_site_id ON distribution (site_id)");
            addIndexIfMissing("purchase", "idx_purchase_site_id",
                    "CREATE INDEX idx_purchase_site_id ON purchase (site_id)");
            addIndexIfMissing("distribution_return", "idx_distribution_return_site_id",
                    "CREATE INDEX idx_distribution_return_site_id ON distribution_return (site_id)");
            addIndexIfMissing("purchase_return", "idx_purchase_return_site_id",
                    "CREATE INDEX idx_purchase_return_site_id ON purchase_return (site_id)");
            addIndexIfMissing("delivery_shipment", "idx_delivery_shipment_dc_id",
                    "CREATE INDEX idx_delivery_shipment_dc_id ON delivery_shipment (delivery_center_id)");
            addIndexIfMissing("purchase_inbound", "idx_purchase_inbound_dc_id",
                    "CREATE INDEX idx_purchase_inbound_dc_id ON purchase_inbound (delivery_center_id)");
            addIndexIfMissing("warehouse_return", "idx_warehouse_return_dc_id",
                    "CREATE INDEX idx_warehouse_return_dc_id ON warehouse_return (delivery_center_id)");

            // add by codebuddy 20260818: 为单据主表增加 ERP 上传失败报错信息字段
            // add by codebuddy 20260818: 为单据主表增加 ERP 上传失败报错信息字段
            String[] billTables = {"requisition", "distribution", "purchase", "distribution_return",
                    "purchase_return", "delivery_shipment", "purchase_inbound", "warehouse_return"};
            for (String t : billTables) {
                addColumnIfMissing(t, "error_message",
                        "ALTER TABLE " + t + " ADD COLUMN error_message TEXT");
            }
            // add by codebuddy 20260818: 单据主表增加最近一次调用 ERP 接口时间(传输时间)与终止上传标记(abort_upload='Y' 表示终止自动上传)
            for (String t : billTables) {
                addColumnIfMissing(t, "last_transmit_at",
                        "ALTER TABLE " + t + " ADD COLUMN last_transmit_at DATETIME NULL");
                addColumnIfMissing(t, "abort_upload",
                        "ALTER TABLE " + t + " ADD COLUMN abort_upload VARCHAR(10) NULL");
            }

            // 预置配送中心数据
            Long cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM distribution_center WHERE dc_no = ? AND tenant_id = 1",
                    Long.class, "BWCS");
            if (cnt == null || cnt == 0) {
                jdbcTemplate.update(
                        "INSERT INTO distribution_center (dc_no, dc_name, tenant_id) VALUES (?, ?, 1)",
                        "BWCS", "广东横琴源麦商秋供应链管理有限公司");
            }

            // add by codebuddy 20260817: 门店商品表配送中心统一更新为 BWCS（幂等）
            Integer dcId = jdbcTemplate.queryForObject(
                    "SELECT id FROM distribution_center WHERE dc_no = ? AND tenant_id = 1", Integer.class, "BWCS");
            if (dcId != null) {
                // add by codebuddy 20260923: 必须限定默认租户，否则会污染其它租户的门店商品归属
                int updated = jdbcTemplate.update(
                        "UPDATE goods_shop SET delivery_center_id = ? "
                                + "WHERE tenant_id = 1 AND (delivery_center_id IS NULL OR delivery_center_id <> ?)",
                        dcId, dcId);
                log.info("门店商品表配送中心统一更新为 BWCS(dcId={})，共更新 {} 条", dcId, updated);
            }

            // add by codebuddy 20260819: 配送退货单上传 ERP（return.create）所需字段
            addColumnIfMissing("distribution_return", "site_no",
                    "ALTER TABLE distribution_return ADD COLUMN site_no VARCHAR(64) NULL");
            addColumnIfMissing("distribution_return_detail", "item_barcode",
                    "ALTER TABLE distribution_return_detail ADD COLUMN item_barcode VARCHAR(64) NULL");
            addColumnIfMissing("distribution_return_detail", "item_batch_no",
                    "ALTER TABLE distribution_return_detail ADD COLUMN item_batch_no VARCHAR(64) NULL");

            // add by codebuddy 20260819: 供应商资料扩展字段
            addColumnIfMissing("supplier", "contact",
                    "ALTER TABLE supplier ADD COLUMN contact VARCHAR(64) NULL");
            addColumnIfMissing("supplier", "phone",
                    "ALTER TABLE supplier ADD COLUMN phone VARCHAR(64) NULL");
            addColumnIfMissing("supplier", "address",
                    "ALTER TABLE supplier ADD COLUMN address VARCHAR(255) NULL");
            addColumnIfMissing("supplier", "status",
                    "ALTER TABLE supplier ADD COLUMN status VARCHAR(1) NULL DEFAULT '1'");
            addColumnIfMissing("supplier", "remark",
                    "ALTER TABLE supplier ADD COLUMN remark VARCHAR(255) NULL");

            // add by codebuddy 20260819: 供应商价格表
            createTableIfMissing("supplier_price",
                    "CREATE TABLE supplier_price (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "supplier_id INT NOT NULL, " +
                            "item_id INT NOT NULL, " +
                            "price DECIMAL(18,4) DEFAULT 0, " +
                            "status VARCHAR(1) NULL DEFAULT '1', " +
                            "remark VARCHAR(255) NULL, " +
                            "INDEX idx_sp_supplier (supplier_id), " +
                            "INDEX idx_sp_item (item_id)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // add by codebuddy 20260819: 系统用户模块
            createTable("sys_user",
                    "CREATE TABLE IF NOT EXISTS sys_user (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " username VARCHAR(64) NOT NULL UNIQUE," +
                            // add by codebuddy 20260923: PBKDF2 哈希约 84 字符，需 200 宽度
                            " password VARCHAR(200) NOT NULL," +
                            " real_name VARCHAR(64)," +
                            " status VARCHAR(1) NULL DEFAULT '1'," +
                            // add by codebuddy 20260923: 平台超级管理员标志
                            " is_platform_admin VARCHAR(1) NOT NULL DEFAULT '0'" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            createTable("sys_user_role",
                    "CREATE TABLE IF NOT EXISTS sys_user_role (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " user_id INT NOT NULL," +
                            " role VARCHAR(20) NOT NULL," +
                            " INDEX idx_ur_user (user_id)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            createTable("sys_user_scope",
                    "CREATE TABLE IF NOT EXISTS sys_user_scope (" +
                            " id INT AUTO_INCREMENT PRIMARY KEY," +
                            " user_id INT NOT NULL," +
                            " org_type VARCHAR(20) NOT NULL," +
                            " org_id INT NOT NULL," +
                            " is_default VARCHAR(1) NULL DEFAULT '0'," +
                            " INDEX idx_us_user (user_id)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // add by codebuddy 20260819: 预置默认管理员 admin/123456（总部角色）
            Long adminCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE username = ? AND tenant_id = 1", Long.class, "admin");
            if (adminCnt == null || adminCnt == 0) {
                // add by codebuddy 20260923: 默认租户的管理员同时作为平台超级管理员
                jdbcTemplate.update(
                        "INSERT INTO sys_user (username, password, real_name, status, tenant_id, is_platform_admin)"
                                + " VALUES (?, ?, ?, ?, 1, '1')",
                        "admin", com.bwtst.common.Md5Util.md5("123456"), "系统管理员", "1");
                Long adminId = jdbcTemplate.queryForObject(
                        "SELECT id FROM sys_user WHERE username = ? AND tenant_id = 1", Long.class, "admin");
                jdbcTemplate.update(
                        "INSERT INTO sys_user_role (user_id, role, tenant_id) VALUES (?, ?, 1)", adminId, "hq");
            }

            // add by codebuddy 20260923: 多租户结构已在租户拦截器启用前由 TenantSchemaMigrator 完成，此处兜底确认
            tenantSchemaMigrator.migrate();

            log.info("DbSchemaInitializer 建表/数据预置完成");
        } catch (Exception e) {
            log.warn("DbSchemaInitializer 执行失败(不影响主流程): {}", e.getMessage());
        }
    }

    private void createTable(String table, String ddl) {
        jdbcTemplate.execute(ddl);
    }

    // add by codebuddy 20260819: 幂等建表（表不存在才执行）
    private void createTableIfMissing(String table, String ddl) {
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
}
