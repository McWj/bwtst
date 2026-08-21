package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// add by codebuddy 20260817
@Data
@TableName("distribution_return_detail")
public class DistributionReturnDetail {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配送退货单id */
    private String distributionReturnId;

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

    // add by codebuddy 20260819: 上传 ERP 所需字段
    /** 商品条码 */
    private String itemBarcode;

    /** 批次号 */
    private String itemBatchNo;

    /** 商品编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemNo;

    /** 商品名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemName;
}
