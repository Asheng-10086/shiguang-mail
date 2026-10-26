package com.example.demo.Mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.pojo.User;

@Mapper
public interface UserMapper {

    public User getAdminByUsername(@Param("username") String username);

    public ArrayList<User> getAlluser();

    public User getUserByid(Long id);

    public User getAccountById(@Param("id") Long id);

    public Long lockUserForAddressUpdate(@Param("id") Long id);

    public String getPasswordHashById(@Param("id") Long id);

    public int updatePassword(@Param("id") Long id,
                              @Param("oldPasswordHash") String oldPasswordHash,
                              @Param("newPasswordHash") String newPasswordHash);

    public List<Long> getActiveAdminIdsForUpdate();

    public int updateUserById(User user);

    public int updateAccountStatus(@Param("id") Long id, @Param("status") Integer status);

    public User getUserByPhone(String phone);

    public int insert(User user);
} 
