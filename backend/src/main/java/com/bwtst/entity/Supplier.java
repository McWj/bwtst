package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// add by codebuddy 20260817
@Data
@TableName("supplier")
public class Supplier {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 供应商编号 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    // add by codebuddy 20260819: 供应商资料扩展字段
    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 联系地址 */
    private String address;

    /** 状态：1-启用 0-停用 */
    private String status;

    /** 备注 */
    private String remark;
}
