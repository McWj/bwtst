package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260817
@Data
@TableName("shop")
public class Shop {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 门店编号 */
    private String shopNo;

    /** 门店名称 */
    private String shopName;
}
