package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BillDto;
import com.bwtst.common.BusinessException;
import com.bwtst.common.CodeGenerator;
import com.bwtst.common.PageResult;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.entity.Goods;
import com.bwtst.entity.Supplier;
import com.bwtst.entity.WarehouseReturn;
import com.bwtst.entity.WarehouseReturnDetail;
import com.bwtst.mapper.DistributionCenterMapper;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.SupplierMapper;
import com.bwtst.mapper.WarehouseReturnDetailMapper;
import com.bwtst.mapper.WarehouseReturnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// add by codebuddy 20260818
@Service
@RequiredArgsConstructor
public class WarehouseReturnService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String NO_PREFIX = "CGCT";

    private final WarehouseReturnMapper returnMapper;
    private final WarehouseReturnDetailMapper detailMapper;
    private final DistributionCenterMapper dcMapper;
    private final GoodsMapper goodsMapper;
    private final SupplierMapper supplierMapper;

    public PageResult<WarehouseReturn> page(long pageNum, long pageSize, String keyword, Integer deliveryCenterId) {
        LambdaQueryWrapper<WarehouseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(WarehouseReturn::getReturnNo, keyword)
                        .or().like(WarehouseReturn::getSupplierCode, keyword)
                        .or().like(WarehouseReturn::getCreator, keyword))
                .eq(deliveryCenterId != null, WarehouseReturn::getDeliveryCenterId, deliveryCenterId)
                .orderByDesc(WarehouseReturn::getId);
        Page<WarehouseReturn> p = returnMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillDcNames(p.getRecords());
        fillSupplierNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public BillDto<WarehouseReturn, WarehouseReturnDetail> getById(Integer id) {
        WarehouseReturn header = returnMapper.selectById(id);
        if (header == null) {
            return null;
        }
        fillDcNames(Collections.singletonList(header));
        fillSupplierNames(Collections.singletonList(header));
        BillDto<WarehouseReturn, WarehouseReturnDetail> dto = new BillDto<>();
        dto.setHeader(header);
        dto.setDetails(fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<WarehouseReturnDetail>()
                .eq(WarehouseReturnDetail::getReturnId, header.getReturnNo())
                .orderByAsc(WarehouseReturnDetail::getSeq, WarehouseReturnDetail::getId))));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BillDto<WarehouseReturn, WarehouseReturnDetail> dto) {
        WarehouseReturn header = dto.getHeader();
        if (header.getId() == null) {
            if (header.getCreateDatetime() == null) {
                header.setCreateDatetime(LocalDateTime.now());
            }
            if (!StringUtils.hasText(header.getReturnNo())) {
                header.setReturnNo(nextNo(header.getDeliveryCenterId()));
            }
            returnMapper.insert(header);
        } else {
            returnMapper.updateById(header);
        }

        detailMapper.delete(new LambdaQueryWrapper<WarehouseReturnDetail>()
                .eq(WarehouseReturnDetail::getReturnId, header.getReturnNo()));
        List<WarehouseReturnDetail> details = dto.getDetails();
        if (details != null) {
            int seq = 1;
            for (WarehouseReturnDetail d : details) {
                d.setId(null);
                d.setReturnId(header.getReturnNo());
                if (d.getSeq() == null) {
                    d.setSeq(seq);
                }
                seq++;
                detailMapper.insert(d);
            }
        }
    }

    // add by codebuddy 20260818: 确认采购仓退单，状态置为Y
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer id) {
        WarehouseReturn header = returnMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("采购仓退单不存在");
        }
        header.setStatus("Y");
        returnMapper.updateById(header);
    }

    // add by codebuddy 20260818: 提交采购仓退单（需先确认），状态置为TJ
    @Transactional(rollbackFor = Exception.class)
    public void submit(Integer id) {
        WarehouseReturn header = returnMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("采购仓退单不存在");
        }
        if ("TJ".equals(header.getStatus())) {
            throw new BusinessException("采购仓退单已提交，不能重复提交");
        }
        if (!"Y".equals(header.getStatus())) {
            throw new BusinessException("请先确认采购仓退单后再提交");
        }
        header.setStatus("TJ");
        returnMapper.updateById(header);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        WarehouseReturn header = returnMapper.selectById(id);
        if (header == null) {
            return;
        }
        // add by codebuddy 20260818: 已确认(Y)或已提交(TJ)的单据不允许删除
        if (StringUtils.hasText(header.getStatus())) {
            throw new BusinessException("单据已确认或已提交，不能删除");
        }
        detailMapper.delete(new LambdaQueryWrapper<WarehouseReturnDetail>()
                .eq(WarehouseReturnDetail::getReturnId, header.getReturnNo()));
        returnMapper.deleteById(id);
    }

    /** 单号：CGCT + 配送中心编号 + 年月日 + 三位流水号，例如 CGCTBWCS20260818001 */
    private String nextNo(Integer deliveryCenterId) {
        String dcNo = getDcNo(deliveryCenterId);
        Long count = returnMapper.selectCount(new LambdaQueryWrapper<WarehouseReturn>()
                .likeRight(WarehouseReturn::getReturnNo, NO_PREFIX + dcNo + LocalDate.now().format(DAY_FMT)));
        return CodeGenerator.genDaily(NO_PREFIX + dcNo, count.intValue());
    }

    private String getDcNo(Integer deliveryCenterId) {
        if (deliveryCenterId == null) {
            throw new BusinessException("请选择配送中心");
        }
        DistributionCenter dc = dcMapper.selectById(deliveryCenterId);
        if (dc == null) {
            throw new BusinessException("配送中心不存在");
        }
        return dc.getDcNo();
    }

    private void fillDcNames(List<WarehouseReturn> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(WarehouseReturn::getDeliveryCenterId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Integer, DistributionCenter> dcMap = dcMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(DistributionCenter::getId, Function.identity()));
        list.forEach(r -> {
            DistributionCenter dc = dcMap.get(r.getDeliveryCenterId());
            if (dc != null) {
                r.setDeliveryCenterNo(dc.getDcNo());
                r.setDeliveryCenterName(dc.getDcName());
            }
        });
    }

    private void fillSupplierNames(List<WarehouseReturn> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<String> codes = list.stream().map(WarehouseReturn::getSupplierCode)
                .filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        if (codes.isEmpty()) {
            return;
        }
        Map<String, Supplier> supplierMap = supplierMapper.selectList(new LambdaQueryWrapper<Supplier>()
                        .in(Supplier::getSupplierCode, codes)).stream()
                .collect(Collectors.toMap(Supplier::getSupplierCode, Function.identity()));
        list.forEach(r -> {
            Supplier s = supplierMap.get(r.getSupplierCode());
            if (s != null) {
                r.setSupplierName(s.getSupplierName());
            }
        });
    }

    private List<WarehouseReturnDetail> fillGoodsNames(List<WarehouseReturnDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<Integer> ids = details.stream().map(WarehouseReturnDetail::getItemId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!ids.isEmpty()) {
            Map<Integer, Goods> goodsMap = goodsMapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(Goods::getId, Function.identity()));
            details.forEach(d -> {
                Goods g = goodsMap.get(d.getItemId());
                if (g != null) {
                    d.setItemNo(g.getItemNo());
                    d.setItemName(g.getItemName());
                }
            });
        }
        return details;
    }
}
