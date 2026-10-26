package com.example.demo.utils;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

import cn.hutool.core.lang.UUID;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String name;
    private static final String keyPrif = "lock:";
    private static final String threadPrif = UUID.randomUUID().toString(true);

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate,String name){

        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryGetLock(long timeOut) {
        
        Long threadId = Thread.currentThread().threadId();

        Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent(keyPrif + name, threadPrif + threadId.toString() , timeOut,TimeUnit.SECONDS);
        return Boolean.TRUE.equals(setIfAbsent);
    }

    @Override
    public void unlock() {
        
        Long threadId = Thread.currentThread().threadId();
        String threadString = threadPrif + threadId.toString();

        String Value = stringRedisTemplate.opsForValue().get(keyPrif + name);
        if(Value.equals(threadString)){
            
            stringRedisTemplate.delete(keyPrif + name);
        }

    }
    
}
