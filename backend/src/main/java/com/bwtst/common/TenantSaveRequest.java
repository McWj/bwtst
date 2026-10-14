package com.bwtst.common;

import lombok.Data;

// add by codebuddy 20260923: 租户新增/编辑入参（含管理员初始账号）
@Data
public class TenantSaveRequest {

    /** 为空表示新增，否则为编辑 */
    private Long id;

    private String tenantCode;

    private String tenantName;

    private String status;

    /** 管理员登录账户，新增时使用，缺省为 admin */
    private String adminUsername;

    /** 管理员初始密码，新增时必填 */
    private String adminPassword;

    // add by codebuddy 20260923: ERP 账套凭证（阶段 3 使用，先支持维护）
    private String erpUrl;

    private String erpDigiKey;

    private String erpProd;

    private String erpAcct;

    private String erpEntId;
}
