package com.example.demo.Service;

import java.util.List;

import com.example.demo.pojo.UserAddress;

public interface UserAddressService {

    List<UserAddress> getAddresses(Long userId);

    UserAddress getAddress(Long userId, Long addressId);

    UserAddress createAddress(Long userId, UserAddress address);

    UserAddress updateAddress(Long userId, Long addressId, UserAddress address);

    void deleteAddress(Long userId, Long addressId);

    void setDefaultAddress(Long userId, Long addressId);
}
