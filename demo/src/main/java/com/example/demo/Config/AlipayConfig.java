package com.example.demo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;

@Configuration
public class AlipayConfig {
     private final AlipayProperties alipayProperties;

     public AlipayConfig(AlipayProperties alipayProperties)
     {
        this.alipayProperties = alipayProperties;
     }

     @Bean
     public AlipayClient alipayClient(){
        return new DefaultAlipayClient(
            alipayProperties.getGatewayUrl(),
            alipayProperties.getAppId(),
            alipayProperties.getMerchantPrivateKey(),
            alipayProperties.getFormat(),
            alipayProperties.getCharset(),
            alipayProperties.getAlipayPublicKey(),
            alipayProperties.getSignType()
        );
     }
}
