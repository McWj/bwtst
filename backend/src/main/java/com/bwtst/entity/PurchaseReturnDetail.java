package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// add by codebuddy 20260817
@Data
@TableName("purchase_return_detail")
public class PurchaseReturnDetail {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 采购退货单id */
    private String purchaseReturnId;

    /** 项次 */
    private Integer seq;

    /** 商品id */
    private Integer itemId;

    /** 收数量 */
    private BigDecimal receiptQty;

    /** 退数量 */
    private BigDecimal qty;

    /** 单位 */
    private String unit;

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
