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
@TableName("delivery_shipment")
public class DeliveryShipment {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 发货单号：配送中心编号+FH+年月日+三位流水号，如 BWCSFH20260817001 */
    private String shipmentNo;

    /** 发货日期 */
    private LocalDate shipmentDate;

    /** 发货体积 */
    private BigDecimal volume;

    /** 发货重量 */
    private BigDecimal weight;

    /** ERP单号 */
    private String erpNo;

    // add by codebuddy 20260817: ERP配送单号（发货前必填，上传 DocnoUpload 时作为 sourceNo）
    /** ERP配送单号 */
    private String erpShipmentNo;

    // add by codebuddy 20260817: 发货状态：空=未发货，Y=已发货待上传，SC=已上传
    /** 发货状态：空=未发货，Y=已发货待上传，SC=已上传 */
    private String status;

    /** 配送中心id */
    private Integer deliveryCenterId;

    // add by codebuddy 20260818: 收货门店id（上传 ERP 成功后据此生成对应门店的配送收货单）
    /** 收货门店id */
    private Integer siteId;

    // add by codebuddy 20260818: ERP 上传失败时的 ERP 报错信息
    /** ERP 上传失败时的 ERP 报错信息 */
    private String errorMessage;

    // add by codebuddy 20260818: 最近一次调用 ERP 接口的时间（传输时间）
    /** 最近一次调用 ERP 接口的时间（传输时间） */
    private LocalDateTime lastTransmitAt;

    // add by codebuddy 20260818: 是否终止上传：Y-终止自动上传
    /** 是否终止上传：Y-终止自动上传 */
    private String abortUpload;

    /** 创建人 */
    private String creator;

    /** 创建时间 */
    private LocalDateTime createDatetime;

    /** 配送中心编号（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterNo;

    /** 配送中心名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String deliveryCenterName;

    // add by codebuddy 20260818: 门店名称（非表字段，查询时填充）
    /** 门店名称（非表字段，查询时填充） */
    @TableField(exist = false)
    private String siteName;
}
