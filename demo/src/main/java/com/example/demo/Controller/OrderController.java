package com.example.demo.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.esotericsoftware.minlog.Log;
import com.example.demo.Config.AlipayProperties;
import com.example.demo.Filter.LoginFilter;
import com.example.demo.Service.OrderCancellationService;
import com.example.demo.Service.OrderService;
import com.example.demo.pojo.Order;
import com.example.demo.pojo.OrderPackage;
import com.example.demo.pojo.Result;
import com.example.demo.pojo.orderItem;

import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class OrderController {
    
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderCancellationService orderCancellationService;

    @Autowired
    private AlipayProperties alipayProperties;

    @PostMapping("/creatOrder")
    public Result creatOrder(@RequestBody Order order)
    {
        orderService.creatOrder(LoginFilter.getUserId(), order);
        return Result.success(order);
    }

    @GetMapping("/orders")
    public Result getOrderList()
    {
        ArrayList<Order> orderList = orderService.getOrderList(LoginFilter.getUserId());

        return Result.success(orderList);
    }

    @GetMapping("/order/{id}")
    public Result getOrderItemById(@PathVariable Long id)
    {
        orderItem oderItm = orderService.getOrderItemById(id);

        return Result.success(oderItm);
    }

    @PostMapping("/order/checkout")
    public Result checkout(@RequestParam Long addressId) throws Exception
    {
        String alipayForm = orderService.selectProductFromCart(addressId);
        return Result.success(alipayForm);
    }

    @PostMapping("/order/pay/{orderSn}")
    public Result pay(@PathVariable String orderSn) throws Exception
    {
        
            String alipayForm = orderService.creatAlipayOrder(orderSn, LoginFilter.getUserId());

            return Result.success(alipayForm);
        
    }

    @PostMapping("/cancelOrderSn")
    public Result cancelOrder(@RequestBody String orderSn)
    {
        try{
            orderCancellationService.cancelOrder(orderSn, LoginFilter.getUserId());
            return Result.success("订单取消成功");
        }catch(IllegalArgumentException | IllegalStateException e)
        {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/order/{orderSn}/confirm-receipt")
    public Result confirmOrderReceipt(@PathVariable String orderSn)
    {
        try {
            orderService.confirmOrderReceipt(orderSn, LoginFilter.getUserId());
            return Result.success("确认收货成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/admin/orders/{orderSn}/ship")
    public Result shipOrder(@PathVariable String orderSn, @RequestBody OrderPackage orderPackage,
                            HttpServletResponse response)
    {
        if (!"ADMIN".equals(LoginFilter.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.error("只有管理员可以发货");
        }
        try {
            return Result.success(orderService.shipOrder(orderSn, orderPackage));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }
    

    @PostMapping("/order/pay/notify")
    public void handleNotify(HttpServletRequest request,HttpServletResponse response) throws Exception
    {
        log.info("收到支付宝支付回调通知");
        
        //1.获取支付宝发送过来的数组参数
        Map<String,String> params = new HashMap<>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for(String name : requestParams.keySet())
        {
            String[] values = requestParams.get(name);
            String valueStr = String.join(",", values);
            params.put(name,valueStr);
        }

        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayProperties.getAlipayPublicKey(), alipayProperties.getCharset(),alipayProperties.getSignType());
        
        if(signVerified)
        {
            String tradeStatus = params.get("trade_status");//交易状态
            log.info("支付状态: {}", tradeStatus);
            String outTradeNo = params.get("out_trade_no");//订单号
            String tradeNo = params.get("trade_no");//支付宝交易号
            String totalAmount = params.get("total_amount");//交易金额
            if("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))
            {
                //更新交易状态，得调用Service层的补充逻辑
                orderService.handlePaymentSuccess(outTradeNo, tradeNo, totalAmount);
            }

            response.getWriter().print("success");
        }else
        {
            response.getWriter().print("failed");
        }
    }

    @GetMapping("/return")
    public void handelReturn(HttpServletRequest request,HttpServletResponse response) throws Exception
    {
        log.info("收到支付宝返回页面通知");

        //1.获取支付宝发送过来的数组参数
        Map<String,String> params = new HashMap<>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for(String name : requestParams.keySet())
        {
            String[] values = requestParams.get(name);
            String valueStr = String.join(",", values);
            params.put(name,valueStr);
        }

        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayProperties.getAlipayPublicKey(), alipayProperties.getCharset(),alipayProperties.getSignType());

        if(signVerified)
        {
            log.info("支付宝返回页面通知验证通过");
            response.sendRedirect("/?payment=success");
        }else
        {
            log.warn("支付宝返回页面通知验证失败");
            response.sendRedirect("/?payment=failed");
        }
    }
}
