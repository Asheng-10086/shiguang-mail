package com.example.demo.pojo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;


@Data
public class User {
    private Long id;                // 主键 ID
    private String username;        // 用户名
    @JsonIgnore
    private String password;        // 密码
    private String nickname;        // 昵称
    private String phone;           // 手机号
    private Integer status;         // 状态 (0：禁用，1：正常)
    private String role;            // 角色 (USER：普通用户，ADMIN：管理员)
    private Date createTime;        // 注册时间
}
