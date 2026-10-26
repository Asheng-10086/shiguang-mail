package com.example.demo.Controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.UserService;
import com.example.demo.Filter.LoginFilter;
import com.example.demo.pojo.AdminLoginRequest;
import com.example.demo.pojo.ChangePasswordRequest;
import com.example.demo.pojo.Result;
import com.example.demo.pojo.User;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;




@RestController
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public Result login(@RequestBody User user, @RequestParam("Phone") String Phone, @RequestParam("code") String code)
    {
        String jwt = userService.login(user,Phone,code);
        

        if(jwt != null)
        {
            return Result.success(jwt); 
        }
        
        return Result.error("用户名或者密码错误");
    }

    @PostMapping("/admin/login")
    public Result adminLogin(@RequestBody AdminLoginRequest request)
    {
        if (request == null) {
            return Result.error("管理员账号或密码错误");
        }
        String jwt = userService.adminLogin(request.getUsername(), request.getPassword());
        if (jwt != null) {
            return Result.success(jwt);
        }
        return Result.error("管理员账号或密码错误");
    }

    @GetMapping("/users")
    public Result getAllUser(){
        if (!"ADMIN".equals(LoginFilter.getRole())) {
            return Result.error("无权查看用户列表");
        }
        ArrayList<User> userList = userService.getAllUser();

        return Result.success(userList);
    }

    @GetMapping("/sendCode")
    public Result sendCode(@RequestParam String Phone,HttpSession session) {
        
        String code = userService.sendCode(Phone, session);

        if(code.equals("手机号错误")){
            return Result.error(code);
        }

        return Result.success(code);
    }
    

    @GetMapping("/users/{id}")
    public Result getUserById(@PathVariable Long id){
        Long currentUserId = LoginFilter.getUserId();
        if (currentUserId == null || (!currentUserId.equals(id) && !"ADMIN".equals(LoginFilter.getRole()))) {
            return Result.error("无权查看该用户信息");
        }
        User user = userService.getUserById(id);

        if(user != null)
        {
            return Result.success(user);
        }else{
            return Result.error("该用户不存在");
        }
    }

    @PutMapping("/update/{id}")
    public Result updateUserById(@PathVariable Long id,@RequestBody User user)
    {
        Long currentUserId = LoginFilter.getUserId();
        if (currentUserId == null || (!currentUserId.equals(id) && !"ADMIN".equals(LoginFilter.getRole()))) {
            return Result.error("无权修改该用户信息");
        }
        try {
            User nUser = userService.updateUserById(id, user);
            return Result.success(nUser);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/users/change-password")
    public Result changePassword(@RequestBody ChangePasswordRequest request)
    {
        if (request == null) {
            return Result.error("旧密码和新密码不能为空");
        }
        try {
            userService.changePassword(
                    LoginFilter.getUserId(),
                    request.getOldPassword(),
                    request.getNewPassword());
            return Result.success("密码修改成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/admin/users/{id}/status")
    public Result updateAccountStatus(@PathVariable Long id, @RequestParam Integer status,
                                       HttpServletResponse response)
    {
        if (!"ADMIN".equals(LoginFilter.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.error("无权修改账号状态");
        }
        if (id.equals(LoginFilter.getUserId()) && Integer.valueOf(0).equals(status)) {
            return Result.error("不能禁用当前管理员账号");
        }
        try {
            userService.updateAccountStatus(id, status);
            return Result.success("账号状态更新成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    
}
