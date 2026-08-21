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
@TableName("purchase")
public class Purchase {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 采购收货单号 */
    private String purchaseNo;

    /** 采购收货日期 */
    private LocalDate purchaseDate;

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
}
