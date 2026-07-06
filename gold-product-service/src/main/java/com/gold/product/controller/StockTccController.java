package com.gold.product.controller;

import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.common.result.Result;
import com.gold.product.tcc.StockTccAction;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TCC 远程调用入口
 * 
 * 订单服务通过 Feign 调这个接口
 * Seata 的 Feign 拦截器会自动把 XID（全局事务ID）通过请求头传过来
 * 商品服务收到后，Seata 绑定到线程上下文
 * 然后调用 StockTccAction（@LocalTCC），Seata AOP 自动注入 BusinessActionContext
 */
@RestController
@RequestMapping("/product/tcc")
public class StockTccController {

    @Autowired
    private StockTccAction stockTccAction;

    @PostMapping("/freeze")
    public Result<Void> tryFreeze(@RequestBody DeductStockRequest request) {
        String xid = RootContext.getXID();
        if (xid == null) {
            return Result.error("500", "未找到全局事务XID，TCC调用必须在@GlobalTransactional内");
        }

        BusinessActionContext context = new BusinessActionContext();
        context.setXid(xid);
        // TODO [TCC正确性] 手动构造 BusinessActionContext 只设置了 XID，缺少 Seata 标准流程中自动注入的 branchId 等字段。在 TCC 资源注册时可能导致 branch 信息不完整，建议改为让 Seata 拦截器自动构造 context（通过 @LocalTCC 的 AOP 代理传参）

        return stockTccAction.tryFreeze(
                context,
                request.getSpecId(),
                request.getQuantity(),
                request.getOrderNo()
        );
    }
}