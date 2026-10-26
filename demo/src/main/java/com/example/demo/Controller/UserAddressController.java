package com.example.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Filter.LoginFilter;
import com.example.demo.Service.UserAddressService;
import com.example.demo.pojo.Result;
import com.example.demo.pojo.UserAddress;

@RestController
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    @GetMapping("/addresses")
    public Result getAddresses() {
        return Result.success(userAddressService.getAddresses(LoginFilter.getUserId()));
    }

    @GetMapping("/addresses/{id}")
    public Result getAddress(@PathVariable Long id) {
        try {
            return Result.success(userAddressService.getAddress(LoginFilter.getUserId(), id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/addresses")
    public Result createAddress(@RequestBody UserAddress address) {
        try {
            return Result.success(userAddressService.createAddress(LoginFilter.getUserId(), address));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/addresses/{id}")
    public Result updateAddress(@PathVariable Long id, @RequestBody UserAddress address) {
        try {
            return Result.success(userAddressService.updateAddress(LoginFilter.getUserId(), id, address));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/addresses/{id}")
    public Result deleteAddress(@PathVariable Long id) {
        try {
            userAddressService.deleteAddress(LoginFilter.getUserId(), id);
            return Result.success("收货地址删除成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/addresses/{id}/default")
    public Result setDefaultAddress(@PathVariable Long id) {
        try {
            userAddressService.setDefaultAddress(LoginFilter.getUserId(), id);
            return Result.success("默认地址设置成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }
}
