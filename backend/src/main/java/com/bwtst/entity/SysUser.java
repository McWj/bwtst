package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

// add by codebuddy 20260819: 系统用户
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 登录账户 */
    private String username;

    /** 密码哈希（PBKDF2 加盐存储，兼容历史 MD5） */
    private String password;

    /** 用户名（真实姓名） */
    private String realName;

    /** 状态：1-启用 0-停用 */
    private String status;

    // add by codebuddy 20260923
    /** 是否平台超级管理员：1-是 0-否，可跨租户管理租户 */
    private String isPlatformAdmin;

    /** 角色列表（非表字段，登录/详情返回用） */
    @TableField(exist = false)
    private List<String> roles;

    /** 可访问门店ID列表（非表字段） */
    @TableField(exist = false)
    private List<Integer> shopIds;

    /** 可访问配送中心ID列表（非表字段） */
    @TableField(exist = false)
    private List<Integer> dcIds;

    /** 默认门店ID（非表字段） */
    @TableField(exist = false)
    private Integer defaultShopId;

    /** 默认配送中心ID（非表字段） */
    @TableField(exist = false)
    private Integer defaultDcId;
}
