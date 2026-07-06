package com.gold.order.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data

@Builder
@TableName("mall_order_item")
public class MallOrderItem {
    /**
     * 订单明细自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联订单主表id，绑定MallOrder.id
     */
    private Long orderId;

    /**
     * 关联商品主表id，绑定MallGoods.id
     */
    private Long goodsId;

    /**
     * 关联商品规格id，绑定MallGoodsSpec.id，精准锁定下单克重规格
     */
    private Long specId;

    /**
     * 下单时商品名称快照，冗余存储，无需联表查询商品表
     */
    private String goodsName;

    /**
     * 下单时商品封面图快照
     */
    private String goodsImage;

    /**
     * 下单商品克重
     */
    private BigDecimal weight;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 下单瞬间行情金价快照，后续金价波动不影响订单结算
     */
    private BigDecimal goldPrice;

    /**
     * 下单瞬间规格工费快照
     */
    private BigDecimal craftFee;

    /**
     * 单件商品下单单价（金价×克重+工费×克重）
     */
    private BigDecimal unitPrice;

    /**
     * 当前商品小计金额 = 单价 × 购买数量
     */
    private BigDecimal totalPrice;

    /**
     * 完整商品快照JSON，存储下单时商品、规格全部图文参数，商品修改/删除后订单详情可正常展示
     */
    private String snapshotJson;

    /**
     * 明细创建时间
     */
    private LocalDateTime createTime;
}