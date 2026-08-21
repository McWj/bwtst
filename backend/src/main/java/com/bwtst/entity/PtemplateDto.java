package com.bwtst.entity;

import lombok.Data;

import java.util.List;

// add by codebuddy 20260817
@Data
public class PtemplateDto {

    private Ptemplate header;
    private List<PtemplateGoods> goodsList;
    private List<PtemplateShop> shopList;
}
