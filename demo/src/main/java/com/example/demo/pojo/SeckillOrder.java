package com.example.demo.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SeckillOrder {

    /** 订单ID */
    
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 秒杀商品ID（对应seckill_goods.id） */
    private Long goodsId;

    /** 应付金额 */
    private BigDecimal payableAmount;

    /** 支付状态：0-未支付 1-已支付 2-已取消 3-已退款 */
    private Integer payStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;
}
