package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260817
@Data
@TableName("goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 商品编号 */
    private String itemNo;

    /** 商品名称 */
    private String itemName;
}
