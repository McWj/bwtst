package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260819: 用户-角色关联（多对多）
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    /** 角色：hq-总部 dc-配送中心 shop-门店 */
    private String role;
}
