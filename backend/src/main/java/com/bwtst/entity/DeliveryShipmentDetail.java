package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// add by codebuddy 20260817
@Data
@TableName("delivery_shipment_detail")
public class DeliveryShipmentDetail {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 发货单号（关联主表 shipment_no） */
    private String shipmentId;

    /** 项次 */
    private Integer seq;

    /** 项序 */
    private Integer orderSeq;

    /** 商品id */
    private Integer itemId;

    /** 发货数量 */
    private BigDecimal qty;

    /** 批号 */
    private String lotNo;

    /** 商品编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemNo;

    /** 商品名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String itemName;
}
