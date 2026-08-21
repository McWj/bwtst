package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// add by codebuddy 20260817
@Data
@TableName("distribution_return")
public class DistributionReturn {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配送退货单号 */
    private String distributionReturnNo;

    /** 配送退货日期 */
    private LocalDate distributionReturnDate;

    /** erp单号 */
    private String erpNo;

    /** 收货门店id */
    private Integer siteId;

    /** 创建人 */
    private String creator;

    /** 创建时间 */
    private LocalDateTime createDatetime;

    // add by codebuddy 20260817: 状态：空-未确认 Y-已确认 TJ-已提交
    /** 状态：空-未确认 Y-已确认 TJ-已提交 */
    private String status;

    // add by codebuddy 20260817: 配送中心id
    /** 配送中心id */
    private Integer deliveryCenterId;

    // add by codebuddy 20260817: 来源配送收货单id
    /** 来源配送收货单id */
    private Integer sourceDistributionId;

    // add by codebuddy 20260817: 来源配送收货单号
    /** 来源配送收货单号 */
    private String sourceDistributionNo;

    // add by codebuddy 20260819: 门店编码（上传 ERP 时使用）
    /** 门店编码（上传 ERP 时使用） */
    private String siteNo;

    // add by codebuddy 20260818: ERP 上传失败时的 ERP 报错信息
    /** ERP 上传失败时的 ERP 报错信息 */
    private String errorMessage;

    // add by codebuddy 20260818: 最近一次调用 ERP 接口的时间（传输时间）
    /** 最近一次调用 ERP 接口的时间（传输时间） */
    private LocalDateTime lastTransmitAt;

    // add by codebuddy 20260818: 是否终止上传：Y-终止自动上传
    /** 是否终止上传：Y-终止自动上传 */
    private String abortUpload;

    /** 门店名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String siteName;

    /** 配送中心编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterNo;

    /** 配送中心名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterName;
}
