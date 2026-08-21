package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.DeliveryShipment;
import com.bwtst.entity.DeliveryShipmentDetail;
import com.bwtst.service.DeliveryShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/delivery-shipment")
@RequiredArgsConstructor
public class DeliveryShipmentController {

    private final DeliveryShipmentService deliveryShipmentService;

    @GetMapping("/page")
    public Result<PageResult<DeliveryShipment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer deliveryCenterId) {
        return Result.ok(deliveryShipmentService.page(pageNum, pageSize, keyword, deliveryCenterId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<DeliveryShipment, DeliveryShipmentDetail>> getById(@PathVariable Integer id) {
        return Result.ok(deliveryShipmentService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<DeliveryShipment, DeliveryShipmentDetail> dto) {
        deliveryShipmentService.save(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody BillDto<DeliveryShipment, DeliveryShipmentDetail> dto) {
        deliveryShipmentService.save(dto);
        return Result.ok();
    }

    // add by codebuddy 20260817: 配送发货（需已录入 ERP 配送单号），body 可带 erpShipmentNo/volume/weight
    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Integer id, @RequestBody(required = false) DeliveryShipment body) {
        deliveryShipmentService.deliver(id, body);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        deliveryShipmentService.delete(id);
        return Result.ok();
    }
}
