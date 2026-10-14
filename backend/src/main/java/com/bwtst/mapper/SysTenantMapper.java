package com.bwtst.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bwtst.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;

// add by codebuddy 20260923: SaaS 租户 Mapper
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
}
