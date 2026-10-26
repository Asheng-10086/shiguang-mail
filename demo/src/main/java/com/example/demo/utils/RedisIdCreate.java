package com.example.demo.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisIdCreate {
    private StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIME = 1767225600L;

    public RedisIdCreate(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long createRedisId(String keyPrif){
        //生成一个时间戳
        LocalDateTime now = LocalDateTime.now();
        long timeSeconds = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = timeSeconds - BEGIN_TIME;

        //生成一个自增序列
        LocalDateTime nowTime = LocalDateTime.now();
        DateTimeFormatter ofPattern = DateTimeFormatter.ofPattern("yyMMDD");
        String format = now.format(ofPattern);
        Long increment = stringRedisTemplate.opsForValue().increment("idRedis:" + keyPrif + format);
        //拼接返回即可
        return timeStamp << 32 | increment;
    }
}
