package com.example.demo.pojo;


import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class orderItem {
    private Long id;                // 主键 ID
    private Long orderId;           // 订单 ID
    private Long productId;         // 商品 ID
    private String productName;     // 商品名称
    private BigDecimal productPrice;// 成交价格
    private Integer productQuantity;// 购买数量
    private BigDecimal totalPrice;  // 小计价格
}
