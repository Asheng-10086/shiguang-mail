package com.example.demo;

import java.lang.reflect.Member;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.pojo.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@SpringBootTest
class DemoApplicationTests {

	@Test
    void test(){
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 0, 0,0);
        long stamp = time.toEpochSecond(ZoneOffset.UTC);

        System.out.println(stamp);
    }

    @Test
    int productSeckill(int proId){

        /*
        //根据id查询商品
        Product product = getProductById(proId)

        //查询是否开始秒杀
        LocalDateTime begin = product.getBeginTime()
        if(begin.isAfter(LocalDateTime.now())){

            return "秒杀未开始"
        }

        //查询是否秒杀结束
        LocalDateTime end = product.getEndTime()
        if(!end.isAfter(LocalDateTime.now())){
        
            return "秒杀已经结束"
        }

        //查询库存，看是否卖完
        int curCount = product.getCount()
        if(curCount == 0){

            return "库存以无剩余"
        }

        //库存有剩余，创建订单信息，并且扣减库存
        int cur = product.getCount()

        if(cur == curCount){
        
            //操作数据库将库存减少一个
            //创建秒杀商品的订单
        }



        //返回订单的id
        */
        
        return 0;
    }

}
