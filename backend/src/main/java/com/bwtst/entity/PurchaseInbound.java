package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// add by codebuddy 20260818
@Data
@TableName("purchase_inbound")
public class PurchaseInbound {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 采购单号 */
    private String purchaseNo;

    /** 采购日期 */
    private LocalDate purchaseDate;

    /** 供应商编号 */
    private String supplierCode;

    /** 配送中心id */
    private Integer deliveryCenterId;

    /** ERP单号 */
    private String erpNo;

    /** 状态：空=未确认，Y=已确认，TJ=已提交 */
    private String status;

    /** 创建人 */
    private String creator;

    /** 创建时间 */
    private LocalDateTime createDatetime;

    // add by codebuddy 20260818: ERP 上传失败时的 ERP 报错信息
    /** ERP 上传失败时的 ERP 报错信息 */
    private String errorMessage;

    // add by codebuddy 20260818: 最近一次调用 ERP 接口的时间（传输时间）
    /** 最近一次调用 ERP 接口的时间（传输时间） */
    private LocalDateTime lastTransmitAt;

    // add by codebuddy 20260818: 是否终止上传：Y-终止自动上传
    /** 是否终止上传：Y-终止自动上传 */
    private String abortUpload;

    /** 供应商名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String supplierName;

    /** 配送中心编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterNo;

    /** 配送中心名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterName;
}
