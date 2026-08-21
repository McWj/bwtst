package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260817
@Data
@TableName("ptemplate_shop")
public class PtemplateShop {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 模板id */
    private Integer ptemplateId;

    /** 项次 */
    private Integer seq;

    /** 门店id */
    private Integer shopId;

    /** 门店名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String shopName;
}
