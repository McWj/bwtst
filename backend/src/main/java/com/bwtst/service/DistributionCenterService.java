package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.common.PageResult;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.mapper.DistributionCenterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

// add by codebuddy 20260817
@Service
@RequiredArgsConstructor
public class DistributionCenterService {

    private final DistributionCenterMapper dcMapper;

    public PageResult<DistributionCenter> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<DistributionCenter> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(DistributionCenter::getDcNo, keyword)
                        .or().like(DistributionCenter::getDcName, keyword))
                .orderByAsc(DistributionCenter::getId);
        Page<DistributionCenter> p = dcMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public List<DistributionCenter> list() {
        return dcMapper.selectList(new LambdaQueryWrapper<DistributionCenter>().orderByAsc(DistributionCenter::getId));
    }

    public DistributionCenter getById(Integer id) {
        return dcMapper.selectById(id);
    }

    public void save(DistributionCenter dc) {
        if (!StringUtils.hasText(dc.getDcNo()) || !StringUtils.hasText(dc.getDcName())) {
            throw new BusinessException("配送中心编号和名称不能为空");
        }
        LambdaQueryWrapper<DistributionCenter> w = new LambdaQueryWrapper<>();
        w.eq(DistributionCenter::getDcNo, dc.getDcNo())
                .ne(dc.getId() != null, DistributionCenter::getId, dc.getId());
        if (dcMapper.selectCount(w) > 0) {
            throw new BusinessException("配送中心编号已存在");
        }
        if (dc.getId() == null) {
            dcMapper.insert(dc);
        } else {
            dcMapper.updateById(dc);
        }
    }

    public void delete(Integer id) {
        dcMapper.deleteById(id);
    }
}
