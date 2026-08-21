package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260817
@Data
@TableName("distribution_center")
public class DistributionCenter {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配送中心编号 */
    private String dcNo;

    /** 配送中心名称 */
    private String dcName;
}
