package com.example.demo.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.Mapper.UserAddressMapper;
import com.example.demo.Mapper.UserMapper;
import com.example.demo.pojo.UserAddress;
import com.example.demo.utils.isPhoneLegal;

@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<UserAddress> getAddresses(Long userId) {
        requireUserId(userId);
        return userAddressMapper.getAddressListByUserId(userId);
    }

    @Override
    public UserAddress getAddress(Long userId, Long addressId) {
        requireUserId(userId);
        requireAddressId(addressId);
        UserAddress address = userAddressMapper.getAddressByIdAndUserId(addressId, userId);
        if (address == null) {
            throw new IllegalArgumentException("收货地址不存在");
        }
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress createAddress(Long userId, UserAddress address) {
        lockUserAddresses(userId);
        validateAddress(address);

        List<UserAddress> existingAddresses = userAddressMapper.getAddressListByUserId(userId);
        boolean makeDefault = existingAddresses.isEmpty() || Integer.valueOf(1).equals(address.getIsDefault());
        address.setId(null);
        address.setUserId(userId);
        address.setIsDefault(makeDefault ? 1 : 0);

        if (makeDefault) {
            userAddressMapper.clearDefaultByUserId(userId);
        }
        if (userAddressMapper.createAddress(address) != 1) {
            throw new IllegalStateException("创建收货地址失败");
        }
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress updateAddress(Long userId, Long addressId, UserAddress address) {
        lockUserAddresses(userId);
        requireAddressId(addressId);
        validateAddress(address);

        UserAddress existingAddress = userAddressMapper.getAddressByIdAndUserId(addressId, userId);
        if (existingAddress == null) {
            throw new IllegalArgumentException("收货地址不存在");
        }

        Integer defaultValue = address.getIsDefault() == null
                ? existingAddress.getIsDefault()
                : address.getIsDefault();
        if (!Integer.valueOf(0).equals(defaultValue) && !Integer.valueOf(1).equals(defaultValue)) {
            throw new IllegalArgumentException("默认地址标记只能是0或1");
        }

        address.setId(addressId);
        address.setUserId(userId);
        address.setIsDefault(defaultValue);
        if (Integer.valueOf(1).equals(defaultValue)) {
            userAddressMapper.clearDefaultByUserId(userId);
        }
        if (userAddressMapper.updateAddress(address) != 1) {
            throw new IllegalStateException("更新收货地址失败");
        }
        return userAddressMapper.getAddressByIdAndUserId(addressId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long addressId) {
        lockUserAddresses(userId);
        requireAddressId(addressId);

        UserAddress address = userAddressMapper.getAddressByIdAndUserId(addressId, userId);
        if (address == null) {
            throw new IllegalArgumentException("收货地址不存在");
        }
        if (userAddressMapper.deleteAddressByIdAndUserId(addressId, userId) != 1) {
            throw new IllegalStateException("删除收货地址失败");
        }

        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            List<UserAddress> remainingAddresses = userAddressMapper.getAddressListByUserId(userId);
            if (!remainingAddresses.isEmpty()) {
                userAddressMapper.clearDefaultByUserId(userId);
                if (userAddressMapper.setDefaultAddress(remainingAddresses.get(0).getId(), userId) != 1) {
                    throw new IllegalStateException("设置新的默认地址失败");
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long addressId) {
        lockUserAddresses(userId);
        requireAddressId(addressId);
        if (userAddressMapper.getAddressByIdAndUserId(addressId, userId) == null) {
            throw new IllegalArgumentException("收货地址不存在");
        }

        userAddressMapper.clearDefaultByUserId(userId);
        if (userAddressMapper.setDefaultAddress(addressId, userId) != 1) {
            throw new IllegalStateException("设置默认地址失败");
        }
    }

    private void lockUserAddresses(Long userId) {
        requireUserId(userId);
        if (userMapper.lockUserForAddressUpdate(userId) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private static void validateAddress(UserAddress address) {
        if (address == null) {
            throw new IllegalArgumentException("收货地址不能为空");
        }
        address.setRecipient(trimRequired(address.getRecipient(), "收件人", 32));
        address.setPhone(trimRequired(address.getPhone(), "手机号", 20));
        address.setProvince(trimRequired(address.getProvince(), "省份", 32));
        address.setCity(trimRequired(address.getCity(), "城市", 32));
        address.setDistrict(trimRequired(address.getDistrict(), "区/县", 32));
        address.setDetail(trimRequired(address.getDetail(), "详细地址", 255));
        if (!isPhoneLegal.isLegalPhone(address.getPhone())) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (address.getIsDefault() != null
                && !Integer.valueOf(0).equals(address.getIsDefault())
                && !Integer.valueOf(1).equals(address.getIsDefault())) {
            throw new IllegalArgumentException("默认地址标记只能是0或1");
        }
    }

    private static String trimRequired(String value, String fieldName, int maxLength) {
        String trimmed = StringUtils.trimWhitespace(value);
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "长度不能超过" + maxLength + "个字符");
        }
        return trimmed;
    }

    private static void requireUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("当前用户未登录");
        }
    }

    private static void requireAddressId(Long addressId) {
        if (addressId == null || addressId <= 0) {
            throw new IllegalArgumentException("收货地址ID无效");
        }
    }
}
