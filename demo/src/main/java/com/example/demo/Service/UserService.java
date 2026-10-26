package com.example.demo.Service;

import java.util.ArrayList;

import com.example.demo.pojo.User;

import jakarta.servlet.http.HttpSession;

public interface UserService {
    public String login(User user,String Phone,String code);

    public String adminLogin(String username, String password);

    public ArrayList<User> getAllUser();

    public User getUserById(Long id);

    public User updateUserById(Long id,User user);

    public void updateAccountStatus(Long id, Integer status);

    public void changePassword(Long userId, String oldPassword, String newPassword);

    public String sendCode(String Phone,HttpSession session);
}
