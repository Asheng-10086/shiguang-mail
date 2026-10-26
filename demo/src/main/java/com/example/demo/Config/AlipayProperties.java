package com.example.demo.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;


@Data
@Component
@ConfigurationProperties(prefix = "alipay")//这个注解可以将配置文件中的指定前缀的所有数据封装到这个java对象中
public class AlipayProperties {
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String gatewayUrl;
    private String signType;
    private String charset;
    private String format;
}
