package com.example.demo.utils;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.ibatis.cache.CacheKey;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CacheUtil {

    //缓存工具类
     private StringRedisTemplate stringRedisTemplate;

     //初始化方法
     public CacheUtil(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
     }

     //将数据缓存到redis中
     public void set(String keyPrif,Long id,Object value){

        String valueStr = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(keyPrif + id.toString(),valueStr,30,TimeUnit.MINUTES);
     }

     //将设计好逻辑过期时间的数据缓存
     public void setWithLogicExpire(String keyPrif,Long id,Object value){

        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(30));

        String redisDataStr = JSONUtil.toJsonStr(redisData);
        stringRedisTemplate.opsForValue().set(keyPrif + id.toString(),redisDataStr);
        
     }

     //利用缓存空值解决缓存穿透的问题
     public <R ,ID> R passWithThrough(ID id,Class<R> type,String keyPrif,
        Function<ID,R> dbFallBack,TimeUnit unit,Long time
     ){
        //1.查询redis
        String rStr = stringRedisTemplate.opsForValue().get(keyPrif + id.toString());

        //redis存在，将其转化成user对象返回
        if(StrUtil.isNotBlank(rStr)){
            R r = JSONUtil.toBean(rStr, type);

            return r;
        }

        
        
        //redis不存在，查询数据库   
        R r = dbFallBack.apply(id);

        //查询到结果，将其转化为对应的存储类型存储到redis
        if(r == null){
            stringRedisTemplate.opsForValue().set("cache:null" + id.toString(),"",5,TimeUnit.MINUTES);
            return null;
        }

        String jsonStr = JSONUtil.toJsonStr(r);
        stringRedisTemplate.opsForValue().set(keyPrif + id.toString(), jsonStr,time,unit);

        return r;
        
     }


     private static final ExecutorService executorService = Executors.newFixedThreadPool(8);


     //利用逻辑过期时间解决缓存击穿的问题
     public <R,ID> R passWithLogicExpire(ID id,Class<R> type,String cacheKey,String keyPrif,
        Function<ID,R> dbFunction
     ){
        //1.查询redis
        String redisStr = stringRedisTemplate.opsForValue().get(keyPrif + id.toString());

        //redis不存在，直接返回null
        if(StrUtil.isBlank(redisStr)){

            return null;
        }

        //redis命中，检查是否过期
        
        RedisData redisData = JSONUtil.toBean(redisStr, RedisData.class);
        Object data = redisData.getData();
        R r = JSONUtil.toBean(JSONUtil.parseObj(data), type);

        if(LocalDateTime.now().isAfter(redisData.getExpireTime())){
            //没有过期直接返回数据
            return r;
        }

        //过期了，需要缓存重建,获取互斥锁
        boolean islock = tryGetLock(cacheKey + id.toString());
        if(islock){
            executorService.submit(()->{
                try {
                    this.redisUserData(id,cacheKey,dbFunction);
                } catch (Exception e) {
                    e.printStackTrace();
                }finally{
                    unlock(cacheKey + id.toString());
                }
            });
        }


        return r;
     }


     private boolean tryGetLock(String key){

        Boolean isTried = stringRedisTemplate.opsForValue().setIfAbsent(key, "success", 10, TimeUnit.MINUTES);

        return BooleanUtil.isTrue(isTried);

    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    private <R,ID> RedisData redisUserData(ID id,String cacheKey,Function<ID,R> dbFunction ){

        R r = dbFunction.apply(id);
        
        RedisData redisUser = new RedisData();
        redisUser.setExpireTime(LocalDateTime.now().plusSeconds(10));
        redisUser.setData(r);

        String redisJsonStr = JSONUtil.toJsonStr(redisUser);;

        stringRedisTemplate.opsForValue().set(cacheKey + id.toString(),redisJsonStr);

        return redisUser;
    }
}
