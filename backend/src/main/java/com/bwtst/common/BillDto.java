package com.bwtst.common;

import lombok.Data;

import java.util.List;

// add by codebuddy 20260817
// 通用主从单据传输对象：header 为主表，details 为明细列表
@Data
public class BillDto<T, D> {

    private T header;
    private List<D> details;
}
