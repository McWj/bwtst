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
  `password` VARCHAR(64) NOT NULL,
  `real_name` VARCHAR(64),
  `status` VARCHAR(1) DEFAULT 1,
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

