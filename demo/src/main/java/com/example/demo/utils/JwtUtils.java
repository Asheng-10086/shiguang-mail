package com.example.demo.utils;

import java.sql.Date;
import java.util.Map;

import com.example.demo.Config.ConfigString;

import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtils {
    public static String generateJwt(Map<String,Object> claims)
    {
		String jwt = Jwts.builder()
						  .signWith(SignatureAlgorithm.HS256,getSigningKey())
						  .setClaims(claims)//设置载荷中的数据
						  .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000))//设置过期时间
						  .compact();//生成令牌

        return jwt;
    }

    public static Claims parseJwt(String jwt)
    {
        Claims claim = Jwts.parser()
							.setSigningKey(getSigningKey())
							.parseClaimsJws(jwt)
							.getBody();

        return claim;
    }

    private static byte[] getSigningKey() {
        String secret = ConfigString.JWT_SECRET;
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 bytes");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
