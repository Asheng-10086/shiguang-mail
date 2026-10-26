package com.example.demo.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SeckillGoods {

    /** 主键ID */
    private Long id;

    /** 商品ID，逻辑关联pms_product.id */
    private Long goodsId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /** 秒杀库存 */
    private Integer stock;

    /** 状态：0-未开始 1-进行中 2-已结束 */
    private Integer status;

    private String productName;

    private String productDetail;

    private BigDecimal productPrice;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
