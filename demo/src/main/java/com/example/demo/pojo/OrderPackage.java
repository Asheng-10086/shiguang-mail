package com.example.demo.pojo;

import java.util.Date;

import lombok.Data;

@Data
public class OrderPackage {
    private Long id;
    private Long orderId;
    private String carrierCode;
    private String carrierName;
    private String trackingNumber;
    private Date shippedAt;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
