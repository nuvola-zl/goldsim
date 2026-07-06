package com.gold.order.controller;

import com.gold.common.code.ErrorCode;
import com.gold.common.context.UserContext;
import com.gold.common.exception.BusinessException;
import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.order.domain.dto.OrderCreateDTO;
import com.gold.order.domain.vo.OrderDetailVO;
import com.gold.order.domain.vo.OrderListVO;
import com.gold.order.domain.vo.OrderValidVO;
import com.gold.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     * @param createDTO 下单所有业务参数统一封装
     */
    @PostMapping("/create")
     /**
      * 【已修复】安全漏洞 + 幂等控制
      *
      * 原问题(7-10):
      * - userId 来自 URL 路径参数，可伪造身份下单
      * - 缺少幂等键，客户端重试会产生重复订单
      *
      * 修复内容(07-10):
      * - userId 改为从 UserContext.getUserId() 获取，不可伪造
      * - 添加 requestId 幂等校验，同一 requestId 只能创建一个订单
      */
         public Result<String> createOrder(@RequestBody OrderCreateDTO createDTO) {
        Long userId = UserContext.getUserId();

        // 【新增】幂等键校验
        if (createDTO.getRequestId() == null || createDTO.getRequestId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少幂等键 requestId");
        }

        return Result.success(orderService.createOrder(userId, createDTO));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable String orderNo) {
        OrderDetailVO vo = orderService.getOrderDetail(orderNo);
        return Result.success(vo);
    }

    /**
     * 我的订单列表
     * 接口：GET /api/order/list?status=&page=&size=
     */
    @GetMapping("/list")
    public Result<PageResult<OrderListVO>> listOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(orderService.listOrders(userId, status, page, size));
    }

    /**
     * 手动取消订单
     * 接口：POST /api/order/cancel/{orderNo}
     */
    @PostMapping("/cancel/{orderNo}")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        orderService.cancelOrder(orderNo, userId);
        return Result.success();
    }

    /**
     * 支付前校验
     * 接口：GET /api/order/{orderNo}/valid
     */
    @GetMapping("/{orderNo}/valid")
    public Result<OrderValidVO> validateOrder(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        return Result.success(orderService.validateOrder(orderNo, userId));
    }
}