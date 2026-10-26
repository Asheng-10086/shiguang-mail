package com.example.demo.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Cart {
    private Long id;                // 主键 ID
    private Long memberId;         // 用户 ID
    private Long productId;         // 商品 ID
    private String productName;     // 商品名称
    private BigDecimal productPrice;// 商品单价
    private Integer quantity;       // 购买数量
    private Integer checked;        // 是否选中 (0：未选中，1：选中)
}
