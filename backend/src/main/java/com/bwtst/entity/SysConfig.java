package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260817
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 备注 */
    private String remark;
}
