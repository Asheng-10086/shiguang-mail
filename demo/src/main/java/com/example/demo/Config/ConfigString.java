package com.example.demo.Config;

import cn.hutool.core.lang.UUID;

public class ConfigString {
    
    public static final String JWT_SECRET = UUID.randomUUID().toString().replace("-", "");
}
