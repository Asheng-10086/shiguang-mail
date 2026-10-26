package com.example.demo.Filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.Mapper.UserMapper;
import com.example.demo.pojo.Result;
import com.example.demo.pojo.User;
import com.example.demo.utils.JwtUtils;

import io.jsonwebtoken.Claims;

@Component
public class LoginFilter implements Filter{

    private static final Logger logger = LoggerFactory.getLogger(LoginFilter.class);
    private static ThreadLocal<Long> userHolder = new ThreadLocal<>();
    private static ThreadLocal<String> roleHolder = new ThreadLocal<>();

    private final UserMapper userMapper;

    public LoginFilter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public static Long getUserId(){
        return userHolder.get();
    }

    public static String getRole() {
        return roleHolder.get();
    }

    @Override
    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException, ServletException{
        //1.获取请求中的url
        HttpServletRequest httpReq = (HttpServletRequest) arg0;
        HttpServletResponse httpResp = (HttpServletResponse) arg1;

        String path = httpReq.getServletPath();

        if(isPublicRequest(path))
        {
            arg2.doFilter(arg0, arg1);
            return;
        }

        //2.不是登录操作
        String jwt = httpReq.getHeader("token");

        //3.看jwt令牌是否为空
        if(!StringUtils.hasLength(jwt))
        {
            writeError(httpResp, HttpServletResponse.SC_UNAUTHORIZED, "NOT_LOGIN");
            return;
        }

        Long userId;
        try {
            Claims claim = JwtUtils.parseJwt(jwt);
            Object claimUserId = claim.get("id");
            if (claimUserId == null) {
                claimUserId = claim.getSubject();
            }
            if (claimUserId == null) {
                throw new IllegalArgumentException("JWT does not contain a user id");
            }
            userId = Long.valueOf(claimUserId.toString());
        } catch (Exception e) {
            writeError(httpResp, HttpServletResponse.SC_UNAUTHORIZED, "NOT_LOGIN");
            return;
        }

        User account;
        try {
            account = userMapper.getAccountById(userId);
        } catch (RuntimeException e) {
            logger.error("Unable to validate account status for user {}", userId, e);
            writeError(httpResp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "ACCOUNT_CHECK_UNAVAILABLE");
            return;
        }
        if (account == null || !Integer.valueOf(1).equals(account.getStatus())) {
            writeError(httpResp, HttpServletResponse.SC_UNAUTHORIZED, "ACCOUNT_DISABLED");
            return;
        }

        userHolder.set(userId);
        roleHolder.set(account.getRole());
        try {
            arg2.doFilter(arg0, arg1);
        } finally {
            userHolder.remove();
            roleHolder.remove();
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(JSONObject.toJSONString(Result.error(message)));
    }

    private boolean isPublicRequest(String path) {
        return path.isEmpty()
                || "/".equals(path)
                || "/index.html".equals(path)
                || "/favicon.ico".equals(path)
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || "/login".equals(path)
                || "/admin/login".equals(path)
                || "/sendCode".equals(path)
                || "/products".equals(path)
                || "/seckillGoods".equals(path)
                || path.startsWith("/products/")
                || path.startsWith("/category/")
                || path.startsWith("/searchProduct/")
                || "/return".equals(path)
                || "/order/pay/notify".equals(path);
    }
}
