package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260819: 用户可访问组织范围（门店/配送中心）
@Data
@TableName("sys_user_scope")
public class SysUserScope {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    /** 组织类型：shop-门店 dc-配送中心 */
    private String orgType;

    /** 组织ID（门店ID或配送中心ID） */
    private Integer orgId;

    /** 是否默认：1-是 0-否 */
    private String isDefault;
}
