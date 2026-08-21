package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260817
@Data
@TableName("ptemplate")
public class Ptemplate {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 模板编号 */
    private String ptemplateNo;

    /** 模板名称 */
    private String ptemplateName;
}
