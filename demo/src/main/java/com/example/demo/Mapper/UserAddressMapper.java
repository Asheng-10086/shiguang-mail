package com.example.demo.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.pojo.UserAddress;

@Mapper
public interface UserAddressMapper {

    List<UserAddress> getAddressListByUserId(@Param("userId") Long userId);

    UserAddress getAddressByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int createAddress(UserAddress address);

    int updateAddress(UserAddress address);

    int deleteAddressByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int clearDefaultByUserId(@Param("userId") Long userId);

    int setDefaultAddress(@Param("id") Long id, @Param("userId") Long userId);
}
