package com.example.demo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;



@Configuration

//@Profile({"dev","test"})
public class SwaggerConfig {
    
    @Bean
    public OpenAPI createRestapi()
    {
        return new OpenAPI().
                   info(new Info().title("电商项目后端接口文档").
                                   description("这是整合整个项目的接口文档").
                                   version("1.0"));
    }

    
}
