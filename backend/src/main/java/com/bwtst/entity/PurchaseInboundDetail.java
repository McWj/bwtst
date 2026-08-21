package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// add by codebuddy 20260818
@Data
@TableName("purchase_inbound_detail")
public class PurchaseInboundDetail {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联采购单号 */
    private String purchaseId;

    /** 项次 */
    private Integer seq;

    /** 商品id */
    private Integer itemId;

    /** 采购单位 */
    private String unit;

    /** 采购数量 */
    private BigDecimal qty;

    /** 单价 */
    private BigDecimal price;

    /** 金额 */
    private BigDecimal amount;

    /** 商品编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemNo;

    /** 商品名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemName;
}
