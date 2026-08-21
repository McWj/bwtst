package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// add by codebuddy 20260818
@Data
@TableName("warehouse_return_detail")
public class WarehouseReturnDetail {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联仓退单号 */
    private String returnId;

    /** 项次 */
    private Integer seq;

    /** 商品id */
    private Integer itemId;

    /** 仓退单位 */
    private String unit;

    /** 仓退数量 */
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
