package com.bwtst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// add by codebuddy 20260817
@Data
@TableName("requisition")
public class Requisition {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 要货单号 */
    private String requisitionNo;

    /** 要货日期 */
    private LocalDate requisitionDate;

    /** 门店id */
    private Integer siteId;

    /** 要货模板id */
    private Integer templateNo;

    /** 需求日期 */
    private LocalDate demandDate;

    /** 创建人 */
    private String creator;

    /** 创建时间 */
    private LocalDateTime createDatetime;

    // add by codebuddy 20260817: 状态：空-未确认 Y-已确认 TJ-已提交
    /** 状态：空-未确认 Y-已确认 TJ-已提交 */
    private String status;

    /** 是否追加要货：Y是，N否 */
    private String isAddPo;

    /** 是否紧急要货：Y是，N否 */
    private String isUrgentOrder;

    /** 预估运费 */
    private BigDecimal distributionFee;

    /** 预估重量 */
    private BigDecimal weight;

    /** 重量单位 */
    private String weightUnit;

    /** 预估体积 */
    private BigDecimal volume;

    /** 体积单位 */
    private String volumeUnit;

    // add by codebuddy 20260817: ERP单号（多张单号用|分隔）
    /** ERP单号（多张单号用|分隔） */
    private String erpNo;

    // add by codebuddy 20260817: 配送中心id（要货时按门店商品配置表取出）
    /** 配送中心id */
    private Integer deliveryCenterId;

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
