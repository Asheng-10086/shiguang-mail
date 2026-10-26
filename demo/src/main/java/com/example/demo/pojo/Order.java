package com.example.demo.pojo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class Order {
    private Long id;                // 主键 ID
    private Long memberId;          // 用户 ID
    private Long addressId;         // 下单时使用的收货地址 ID
    private String shippingRecipient; // 下单时收件人姓名
    private String shippingPhone;   // 下单时收件人手机号
    private String shippingProvince;// 下单时省份
    private String shippingCity;    // 下单时城市
    private String shippingDistrict;// 下单时区/县
    private String shippingDetail;  // 下单时详细地址
    private String orderSn;         // 订单号
    private BigDecimal totalAmount; // 订单总金额
    private BigDecimal payAmount;   // 应付金额
    private Integer status;         // 订单状态 (0->待付款; 1->待发货; 2->待收货; 3->已完成; -1->已取消)
    private Integer payType;        // 支付方式 (0：未支付; 1：支付宝; 2：微信)
    private Date createTime;        // 下单时间
}
