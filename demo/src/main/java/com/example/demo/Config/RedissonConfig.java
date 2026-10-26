package com.example.demo.Config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;



@Configuration
public class RedissonConfig {
    

    @Bean
    public RedissonClient redissonClient(){

        Config config = new Config();
        SingleServerConfig useSingleServer = config.useSingleServer();
        useSingleServer.setAddress("redis://127.0.0.1:6379").setDatabase(0).setConnectTimeout(1000);

        return Redisson.create(config);
    }
}
