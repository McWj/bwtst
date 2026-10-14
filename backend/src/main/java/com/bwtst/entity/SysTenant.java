package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// add by codebuddy 20260923: SaaS 租户（企业）——每个租户独立一套 ERP 账套凭证
@Data
@TableName("sys_tenant")
public class SysTenant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业编码（登录时使用，全局唯一） */
    private String tenantCode;

    /** 企业名称 */
    private String tenantName;

    /** 状态：1-启用 0-停用 */
    private String status;

    // add by codebuddy 20260923: 以下 ERP 账套凭证字段为阶段 3 预留，当前仍沿用全局配置
    /** ERP 接口地址 */
    private String erpUrl;

    /** ERP 数字签名密钥 */
    private String erpDigiKey;

    /** ERP 产品码 */
    private String erpProd;

    /** ERP 账号 */
    private String erpAcct;

    /** ERP 企业账套 ID */
    private String erpEntId;

    private LocalDateTime createDatetime;
}
