package com.example.demo.Mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.pojo.OrderPackage;

@Mapper
public interface OrderPackageMapper {

    int createPackage(OrderPackage orderPackage);
}
