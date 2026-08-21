package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// add by codebuddy 20260819: 供应商价格表
@Data
@TableName("supplier_price")
public class SupplierPrice {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 供应商ID */
    private Integer supplierId;

    /** 商品ID */
    private Integer itemId;

    /** 单价 */
    private BigDecimal price;

    /** 状态：1-启用 0-停用 */
    private String status;

    /** 备注 */
    private String remark;
}
