package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// add by codebuddy 20260817
@Data
@TableName("goods_shop")
public class GoodsShop {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 门店id */
    private Integer shopId;

    /** 商品id */
    private Integer itemId;

    /** 配送方式：1.统采直送，2.统采配送 */
    private Integer deliveryType;

    /** 价格 */
    private BigDecimal price;

    /** 配送中心id */
    private Integer deliveryCenterId;

    // add by codebuddy 20260817: 主供应商id（关联 supplier.id）
    /** 主供应商id */
    private Integer supplierId;

    /** 门店名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String shopName;

    /** 商品编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemNo;

    /** 商品名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemName;

    /** 配送中心编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterNo;

    /** 配送中心名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterName;

    // add by codebuddy 20260817: 供应商编号/名称（非表字段，查询时填充）
    /** 供应商编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String supplierNo;

    /** 供应商名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String supplierName;
}
