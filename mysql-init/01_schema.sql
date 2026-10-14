-- bwtst 数据库表结构导出
-- 生成时间: Fri Aug 21 13:25:43 CST 2026

CREATE DATABASE IF NOT EXISTS `bwtst` DEFAULT CHARACTER SET utf8mb4;
USE `bwtst`;

-- 表 delivery_shipment
CREATE TABLE `delivery_shipment` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `shipment_no` VARCHAR(50),
  `shipment_date` DATE,
  `volume` DECIMAL(18,4),
  `weight` DECIMAL(18,4),
  `erp_no` VARCHAR(50),
  `delivery_center_id` INT(10),
  `creator` VARCHAR(50),
  `create_datetime` DATETIME,
  `erp_shipment_no` VARCHAR(50),
  `status` VARCHAR(10),
  `site_id` INT(10),
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  PRIMARY KEY (`id`),
  KEY `idx_delivery_shipment_dc_id` (`delivery_center_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 delivery_shipment_detail
CREATE TABLE `delivery_shipment_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `shipment_id` VARCHAR(50),
  `seq` INT(10),
  `order_seq` INT(10),
  `item_id` INT(10),
  `qty` DECIMAL(18,4),
  `lot_no` VARCHAR(50),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 distribution
CREATE TABLE `distribution` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `distribution_no` VARCHAR(40) COMMENT '配送收货单号',
  `distribution_date` DATE NOT NULL COMMENT '配送收货日期',
  `erp_no` VARCHAR(40) COMMENT 'erp单号',
  `site_id` INT(10) NOT NULL COMMENT '收货门店id',
  `creator` VARCHAR(40) COMMENT '创建人',
  `create_datetime` DATETIME COMMENT '创建时间',
  `status` VARCHAR(2) COMMENT '状态：空-未确认 Y-已确认 TJ-已提交',
  `delivery_center_id` INT(10),
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  `site_no` VARCHAR(40) COMMENT '门店编号(上传ERP用)',
  `delivery_shipment_no` VARCHAR(40) COMMENT '对应配送发货单号',
  `delivery_date` DATE COMMENT '配送日期(上传ERP用)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `distribution_index` (`distribution_no`),
  KEY `idx_distribution_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送收货主表';

-- 表 distribution_center
CREATE TABLE `distribution_center` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `dc_no` VARCHAR(50) NOT NULL,
  `dc_name` VARCHAR(200) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 distribution_detail
CREATE TABLE `distribution_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `distribution_id` VARCHAR(40) COMMENT '配送单id',
  `seq` INT(10) NOT NULL COMMENT '项次',
  `item_id` INT(10) NOT NULL COMMENT '商品id',
  `requisition_qty` DECIMAL(20,3) COMMENT '要货数量',
  `qty` DECIMAL(20,3) COMMENT '收货数量',
  `unit` VARCHAR(10) COMMENT '单位',
  `price` DECIMAL(20,6) NOT NULL COMMENT '单价',
  `amount` DECIMAL(20,2) COMMENT '金额',
  `tax_rate` DECIMAL(12,4) COMMENT '税率',
  `tax_amount` DECIMAL(20,2) COMMENT '税额',
  `batch_no` VARCHAR(40) COMMENT '批次号',
  `barcode` VARCHAR(40) COMMENT '商品条码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `requisition_detail_index` (`distribution_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送收货单明细表';

-- 表 distribution_return
CREATE TABLE `distribution_return` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `distribution_return_no` VARCHAR(40) COMMENT '配送退货单号',
  `distribution_return_date` DATE NOT NULL COMMENT '配送退货日期',
  `erp_no` VARCHAR(40) COMMENT 'erp单号',
  `site_id` INT(10) NOT NULL COMMENT '收货门店id',
  `creator` VARCHAR(40) COMMENT '创建人',
  `create_datetime` DATETIME COMMENT '创建时间',
  `status` VARCHAR(2) COMMENT '状态：空-未确认 Y-已确认 TJ-已提交',
  `delivery_center_id` INT(10),
  `source_distribution_id` INT(10),
  `source_distribution_no` VARCHAR(64),
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  `site_no` VARCHAR(64),
  PRIMARY KEY (`id`),
  UNIQUE KEY `distribution_return_index` (`distribution_return_no`),
  KEY `idx_distribution_return_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送退货主表';

-- 表 distribution_return_detail
CREATE TABLE `distribution_return_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `distribution_return_id` VARCHAR(40) COMMENT '配送退货单id',
  `seq` INT(10) NOT NULL COMMENT '项次',
  `item_id` INT(10) NOT NULL COMMENT '商品id',
  `receipt_qty` DECIMAL(20,3) COMMENT '收数量',
  `qty` DECIMAL(20,3) COMMENT '退数量',
  `unit` VARCHAR(10) COMMENT '单位',
  `price` DECIMAL(20,6) NOT NULL COMMENT '单价',
  `amount` DECIMAL(20,2) COMMENT '金额',
  `item_barcode` VARCHAR(64),
  `item_batch_no` VARCHAR(64),
  PRIMARY KEY (`id`),
  UNIQUE KEY `distribution_return_detail_index` (`distribution_return_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送退货单明细表';

-- 表 goods
CREATE TABLE `goods` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `item_no` VARCHAR(80) NOT NULL COMMENT '商品编号',
  `item_name` VARCHAR(255) COMMENT '商品名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `goods_item_no` (`item_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品表';

-- 表 goods_shop
CREATE TABLE `goods_shop` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `shop_id` INT(10) NOT NULL COMMENT '门店id',
  `item_id` INT(10) NOT NULL COMMENT '商品id',
  `delivery_type` TINYINT COMMENT '配送方式; 1.统采直送，2.统采配送',
  `price` DECIMAL(20,6) COMMENT '价格',
  `delivery_center_id` INT(10),
  `supplier_id` INT(10) COMMENT '主供应商id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `goods_shop_item` (`shop_id`,`item_id`),
  KEY `idx_goods_shop_delivery_center` (`delivery_center_id`),
  KEY `idx_goods_shop_shop_id` (`shop_id`),
  KEY `idx_goods_shop_shop_item` (`shop_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门店商品表';

-- 表 ptemplate
CREATE TABLE `ptemplate` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `ptemplate_no` VARCHAR(40) COMMENT '模板编号',
  `ptemplate_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ptemplate_no_index` (`ptemplate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='要货模板主表';

-- 表 ptemplate_goods
CREATE TABLE `ptemplate_goods` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `ptemplate_id` INT(10) NOT NULL COMMENT '模板id',
  `seq` INT(10) NOT NULL COMMENT '项次',
  `item_id` INT(10) NOT NULL COMMENT '商品id',
  `unit` VARCHAR(20) COMMENT '单位',
  `qty` DECIMAL(20,3) COMMENT '数量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ptemplate_goods_index` (`ptemplate_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='要货模板商品明细';

-- 表 ptemplate_shop
CREATE TABLE `ptemplate_shop` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `ptemplate_id` INT(10) NOT NULL COMMENT '模板id',
  `seq` INT(10) NOT NULL COMMENT '项次',
  `shop_id` INT(10) NOT NULL COMMENT '门店id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ptemplate_shop_index` (`ptemplate_id`,`shop_id`),
  KEY `idx_ptemplate_shop_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='要货模板门店明细';

-- 表 purchase
CREATE TABLE `purchase` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `purchase_no` VARCHAR(40) COMMENT '采购收货单号',
  `purchase_date` DATE NOT NULL COMMENT '采购收货日期',
  `erp_no` VARCHAR(40) COMMENT 'erp单号',
  `site_id` INT(10) NOT NULL COMMENT '收货门店id',
  `creator` VARCHAR(40) COMMENT '创建人',
  `create_datetime` DATETIME COMMENT '创建时间',
  `status` VARCHAR(2) COMMENT '状态：空-未确认 Y-已确认 TJ-已提交',
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  PRIMARY KEY (`id`),
  UNIQUE KEY `requisition_index` (`purchase_no`),
  KEY `idx_purchase_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购收货主表';

-- 表 purchase_detail
CREATE TABLE `purchase_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `purchase_id` VARCHAR(40) COMMENT '采购收货单id',
  `seq` INT(10) NOT NULL COMMENT '项次',
  `item_id` INT(10) NOT NULL COMMENT '商品id',
  `requisition_qty` DECIMAL(20,3) COMMENT '要货数量',
  `qty` DECIMAL(20,3) COMMENT '收货数量',
  `unit` VARCHAR(10) COMMENT '单位',
  `price` DECIMAL(20,6) NOT NULL COMMENT '单价',
  `amount` DECIMAL(20,2) COMMENT '金额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `purchase_detail_index` (`purchase_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购收货单明细表';

-- 表 purchase_inbound
CREATE TABLE `purchase_inbound` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `purchase_no` VARCHAR(50),
  `purchase_date` DATE,
  `supplier_code` VARCHAR(50),
  `delivery_center_id` INT(10),
  `erp_no` VARCHAR(50),
  `status` VARCHAR(10),
  `creator` VARCHAR(50),
  `create_datetime` DATETIME,
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  PRIMARY KEY (`id`),
  KEY `idx_purchase_inbound_dc_id` (`delivery_center_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 purchase_inbound_detail
CREATE TABLE `purchase_inbound_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `purchase_id` VARCHAR(50),
  `seq` INT(10),
  `item_id` INT(10),
  `unit` VARCHAR(20),
  `qty` DECIMAL(18,4),
  `price` DECIMAL(18,4),
  `amount` DECIMAL(18,4),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 purchase_return
CREATE TABLE `purchase_return` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `purchase_return_no` VARCHAR(40) COMMENT '采购退货单号',
  `purchase_return_date` DATE NOT NULL COMMENT '采购退货日期',
  `erp_no` VARCHAR(40) COMMENT 'erp单号',
  `site_id` INT(10) NOT NULL COMMENT '收货门店id',
  `creator` VARCHAR(40) COMMENT '创建人',
  `create_datetime` DATETIME COMMENT '创建时间',
  `status` VARCHAR(2) COMMENT '状态：空-未确认 Y-已确认 TJ-已提交',
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  PRIMARY KEY (`id`),
  UNIQUE KEY `purchase_return_index` (`purchase_return_no`),
  KEY `idx_purchase_return_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购退货主表';

-- 表 purchase_return_detail
CREATE TABLE `purchase_return_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `purchase_return_id` VARCHAR(40) COMMENT '采购退货单id',
  `seq` INT(10) NOT NULL COMMENT '项次',
  `item_id` INT(10) NOT NULL COMMENT '商品id',
  `receipt_qty` DECIMAL(20,3) COMMENT '收数量',
  `qty` DECIMAL(20,3) COMMENT '退数量',
  `unit` VARCHAR(10) COMMENT '单位',
  `price` DECIMAL(20,6) NOT NULL COMMENT '单价',
  `amount` DECIMAL(20,2) COMMENT '金额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `purchase_detail_index` (`purchase_return_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购退货单明细表';

-- 表 requisition
CREATE TABLE `requisition` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `requisition_no` VARCHAR(40) COMMENT '要货单号',
  `requisition_date` DATE NOT NULL COMMENT '要货日期',
  `site_id` INT(10) NOT NULL COMMENT '门店id',
  `template_no` INT(10) COMMENT '要货模板id',
  `demand_date` DATE NOT NULL COMMENT '需求日期',
  `creator` VARCHAR(40) COMMENT '创建人',
  `create_datetime` DATETIME COMMENT '创建时间',
  `status` VARCHAR(2) COMMENT '状态：空-未确认 Y-已确认 TJ-已提交',
  `is_add_po` VARCHAR(1) COMMENT '是否追加要货：Y是，N否',
  `is_urgent_order` VARCHAR(1) COMMENT '是否紧急要货：Y是，N否',
  `distribution_fee` DECIMAL(20,2) COMMENT '预估运费',
  `weight` DECIMAL(20,2) COMMENT '预估重量',
  `weight_unit` VARCHAR(40) COMMENT '创建人',
  `volume` DECIMAL(20,2) COMMENT '预估体积',
  `volume_unit` VARCHAR(40) COMMENT '体积单位',
  `erp_no` VARCHAR(200),
  `delivery_center_id` INT(10),
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  PRIMARY KEY (`id`),
  UNIQUE KEY `requisition_index` (`requisition_no`,`site_id`),
  KEY `idx_requisition_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='要货单主表';

-- 表 requisition_detail
CREATE TABLE `requisition_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `requisition_id` VARCHAR(40) COMMENT '要货单id',
  `seq` INT(10) NOT NULL COMMENT '项次',
  `item_id` INT(10) NOT NULL COMMENT '商品id',
  `qty` DECIMAL(20,3) COMMENT '要货数量',
  `unit` VARCHAR(10) COMMENT '单位',
  `price` DECIMAL(20,6) NOT NULL COMMENT '单价',
  `amount` DECIMAL(20,2) COMMENT '金额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `requisition_detail_index` (`requisition_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='要货单明细表';

-- 表 shop
CREATE TABLE `shop` (
  `id` INT(10) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `shop_no` VARCHAR(80) NOT NULL COMMENT '门店编号',
  `shop_name` VARCHAR(255) COMMENT '门店名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `shop_no_index` (`shop_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门店表';

-- 表 supplier
CREATE TABLE `supplier` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `supplier_code` VARCHAR(32) NOT NULL COMMENT '供应商编号',
  `supplier_name` VARCHAR(128) NOT NULL COMMENT '供应商名称',
  `contact` VARCHAR(64),
  `phone` VARCHAR(64),
  `address` VARCHAR(255),
  `status` VARCHAR(1) DEFAULT 1,
  `remark` VARCHAR(255),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商表';

-- 表 supplier_price
CREATE TABLE `supplier_price` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `supplier_id` INT(10) NOT NULL,
  `item_id` INT(10) NOT NULL,
  `price` DECIMAL(18,4) DEFAULT 0.0000,
  `status` VARCHAR(1) DEFAULT 1,
  `remark` VARCHAR(255),
  PRIMARY KEY (`id`),
  KEY `idx_sp_item` (`item_id`),
  KEY `idx_sp_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 sys_config
CREATE TABLE `sys_config` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(100) NOT NULL,
  `config_value` VARCHAR(500),
  `remark` VARCHAR(200),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 sys_user
CREATE TABLE `sys_user` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(200) NOT NULL,
  `real_name` VARCHAR(64),
  `status` VARCHAR(1) DEFAULT 1,
  `is_platform_admin` VARCHAR(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 sys_user_role
CREATE TABLE `sys_user_role` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `user_id` INT(10) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ur_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 sys_user_scope
CREATE TABLE `sys_user_scope` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `user_id` INT(10) NOT NULL,
  `org_type` VARCHAR(20) NOT NULL,
  `org_id` INT(10) NOT NULL,
  `is_default` VARCHAR(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_us_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 warehouse_return
CREATE TABLE `warehouse_return` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `return_no` VARCHAR(50),
  `return_date` DATE,
  `supplier_code` VARCHAR(50),
  `delivery_center_id` INT(10),
  `erp_no` VARCHAR(50),
  `status` VARCHAR(10),
  `creator` VARCHAR(50),
  `create_datetime` DATETIME,
  `error_message` TEXT,
  `last_transmit_at` DATETIME,
  `abort_upload` VARCHAR(10),
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_return_dc_id` (`delivery_center_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- 表 warehouse_return_detail
CREATE TABLE `warehouse_return_detail` (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `return_id` VARCHAR(50),
  `seq` INT(10),
  `item_id` INT(10),
  `unit` VARCHAR(20),
  `qty` DECIMAL(18,4),
  `price` DECIMAL(18,4),
  `amount` DECIMAL(18,4),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='';

-- ============================================================
-- add by codebuddy 20260923: SaaS 多租户改造（仅用于全新数据库的一次性初始化）
-- 说明：已运行的环境由后端 DbSchemaInitializer 在启动时自动幂等补齐，请勿在已升级的库上重复执行本段。
-- ============================================================

-- 1. 租户（企业）表：每个租户一套独立 ERP 账套凭证
CREATE TABLE IF NOT EXISTS `sys_tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '企业编码（登录时使用）',
  `tenant_name` VARCHAR(200) NOT NULL COMMENT '企业名称',
  `status` VARCHAR(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用 0-停用',
  `erp_url` VARCHAR(255) DEFAULT NULL COMMENT 'ERP 接口地址',
  `erp_digi_key` VARCHAR(128) DEFAULT NULL COMMENT 'ERP 数字签名密钥',
  `erp_prod` VARCHAR(64) DEFAULT NULL COMMENT 'ERP 产品码',
  `erp_acct` VARCHAR(64) DEFAULT NULL COMMENT 'ERP 账号',
  `erp_ent_id` VARCHAR(32) DEFAULT NULL COMMENT 'ERP 企业账套 ID',
  `create_datetime` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SaaS 租户（企业）';

-- 2. 预置默认租户（存量数据统一归入 id=1）
INSERT INTO `sys_tenant` (`id`, `tenant_code`, `tenant_name`, `status`, `create_datetime`)
SELECT 1, 'bwcs', '广东横琴源麦商秋供应链管理有限公司', '1', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_tenant` WHERE `id` = 1);

-- 3. 所有业务表补 tenant_id 列与索引
ALTER TABLE `delivery_shipment`           ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_delivery_shipment_tenant_id` (`tenant_id`);
ALTER TABLE `delivery_shipment_detail`    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_delivery_shipment_detail_tenant_id` (`tenant_id`);
ALTER TABLE `distribution`                ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_distribution_tenant_id` (`tenant_id`);
ALTER TABLE `distribution_detail`         ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_distribution_detail_tenant_id` (`tenant_id`);
ALTER TABLE `distribution_center`         ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_distribution_center_tenant_id` (`tenant_id`);
ALTER TABLE `distribution_return`         ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_distribution_return_tenant_id` (`tenant_id`);
ALTER TABLE `distribution_return_detail`  ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_distribution_return_detail_tenant_id` (`tenant_id`);
ALTER TABLE `goods`                       ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_goods_tenant_id` (`tenant_id`);
ALTER TABLE `goods_shop`                  ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_goods_shop_tenant_id` (`tenant_id`);
ALTER TABLE `ptemplate`                   ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_ptemplate_tenant_id` (`tenant_id`);
ALTER TABLE `ptemplate_goods`             ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_ptemplate_goods_tenant_id` (`tenant_id`);
ALTER TABLE `ptemplate_shop`              ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_ptemplate_shop_tenant_id` (`tenant_id`);
ALTER TABLE `purchase`                    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_purchase_tenant_id` (`tenant_id`);
ALTER TABLE `purchase_detail`             ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_purchase_detail_tenant_id` (`tenant_id`);
ALTER TABLE `purchase_inbound`            ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_purchase_inbound_tenant_id` (`tenant_id`);
ALTER TABLE `purchase_inbound_detail`     ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_purchase_inbound_detail_tenant_id` (`tenant_id`);
ALTER TABLE `purchase_return`             ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_purchase_return_tenant_id` (`tenant_id`);
ALTER TABLE `purchase_return_detail`      ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_purchase_return_detail_tenant_id` (`tenant_id`);
ALTER TABLE `requisition`                 ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_requisition_tenant_id` (`tenant_id`);
ALTER TABLE `requisition_detail`          ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_requisition_detail_tenant_id` (`tenant_id`);
ALTER TABLE `shop`                        ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_shop_tenant_id` (`tenant_id`);
ALTER TABLE `supplier`                    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_supplier_tenant_id` (`tenant_id`);
ALTER TABLE `supplier_price`              ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_supplier_price_tenant_id` (`tenant_id`);
ALTER TABLE `sys_config`                  ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_sys_config_tenant_id` (`tenant_id`);
ALTER TABLE `sys_user`                    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_sys_user_tenant_id` (`tenant_id`);
ALTER TABLE `sys_user_role`               ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_sys_user_role_tenant_id` (`tenant_id`);
ALTER TABLE `sys_user_scope`              ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_sys_user_scope_tenant_id` (`tenant_id`);
ALTER TABLE `warehouse_return`            ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_warehouse_return_tenant_id` (`tenant_id`);
ALTER TABLE `warehouse_return_detail`     ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1, ADD KEY `idx_warehouse_return_detail_tenant_id` (`tenant_id`);

-- 4. 全局唯一键 → 租户内唯一（不同租户可使用相同业务编码）
ALTER TABLE `distribution`                DROP INDEX `distribution_index`, ADD UNIQUE KEY `uk_tenant_distribution_no` (`tenant_id`,`distribution_no`);
ALTER TABLE `distribution_detail`         DROP INDEX `requisition_detail_index`, ADD UNIQUE KEY `uk_tenant_distribution_detail` (`tenant_id`,`distribution_id`,`seq`);
ALTER TABLE `distribution_return`         DROP INDEX `distribution_return_index`, ADD UNIQUE KEY `uk_tenant_distribution_return_no` (`tenant_id`,`distribution_return_no`);
ALTER TABLE `distribution_return_detail`  DROP INDEX `distribution_return_detail_index`, ADD UNIQUE KEY `uk_tenant_distribution_return_detail` (`tenant_id`,`distribution_return_id`,`seq`);
ALTER TABLE `goods`                       DROP INDEX `goods_item_no`, ADD UNIQUE KEY `uk_tenant_goods_item_no` (`tenant_id`,`item_no`);
ALTER TABLE `goods_shop`                  DROP INDEX `goods_shop_item`, ADD UNIQUE KEY `uk_tenant_goods_shop_item` (`tenant_id`,`shop_id`,`item_id`);
ALTER TABLE `ptemplate`                   DROP INDEX `ptemplate_no_index`, ADD UNIQUE KEY `uk_tenant_ptemplate_no` (`tenant_id`,`ptemplate_no`);
ALTER TABLE `ptemplate_goods`             DROP INDEX `ptemplate_goods_index`, ADD UNIQUE KEY `uk_tenant_ptemplate_goods` (`tenant_id`,`ptemplate_id`,`seq`);
ALTER TABLE `ptemplate_shop`              DROP INDEX `ptemplate_shop_index`, ADD UNIQUE KEY `uk_tenant_ptemplate_shop` (`tenant_id`,`ptemplate_id`,`shop_id`);
ALTER TABLE `purchase`                    DROP INDEX `requisition_index`, ADD UNIQUE KEY `uk_tenant_purchase_no` (`tenant_id`,`purchase_no`);
ALTER TABLE `purchase_detail`             DROP INDEX `purchase_detail_index`, ADD UNIQUE KEY `uk_tenant_purchase_detail` (`tenant_id`,`purchase_id`,`seq`);
ALTER TABLE `purchase_return`             DROP INDEX `purchase_return_index`, ADD UNIQUE KEY `uk_tenant_purchase_return_no` (`tenant_id`,`purchase_return_no`);
ALTER TABLE `purchase_return_detail`      DROP INDEX `purchase_detail_index`, ADD UNIQUE KEY `uk_tenant_purchase_return_detail` (`tenant_id`,`purchase_return_id`,`seq`);
ALTER TABLE `requisition`                 DROP INDEX `requisition_index`, ADD UNIQUE KEY `uk_tenant_requisition_no` (`tenant_id`,`requisition_no`,`site_id`);
ALTER TABLE `requisition_detail`          DROP INDEX `requisition_detail_index`, ADD UNIQUE KEY `uk_tenant_requisition_detail` (`tenant_id`,`requisition_id`,`seq`);
ALTER TABLE `shop`                        DROP INDEX `shop_no_index`, ADD UNIQUE KEY `uk_tenant_shop_no` (`tenant_id`,`shop_no`);
ALTER TABLE `supplier`                    DROP INDEX `uk_supplier_code`, ADD UNIQUE KEY `uk_tenant_supplier_code` (`tenant_id`,`supplier_code`);
ALTER TABLE `sys_user`                    DROP INDEX `username`, ADD UNIQUE KEY `uk_tenant_username` (`tenant_id`,`username`);

