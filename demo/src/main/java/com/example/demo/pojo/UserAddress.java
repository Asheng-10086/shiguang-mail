package com.example.demo.pojo;

import java.util.Date;

import lombok.Data;

@Data
public class UserAddress {
    private Long id;
    private Long userId;
    private String recipient;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
    private Date createdAt;
    private Date updatedAt;
}
