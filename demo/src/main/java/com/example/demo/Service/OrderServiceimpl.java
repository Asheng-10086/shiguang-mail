package com.example.demo.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.esotericsoftware.minlog.Log;
import com.example.demo.Filter.LoginFilter;
import com.example.demo.Mapper.OrderMapper;
import com.example.demo.Mapper.OrderPackageMapper;
import com.example.demo.Mapper.ProductsMapper;
import com.example.demo.pojo.Order;
import com.example.demo.pojo.OrderPackage;
import com.example.demo.pojo.SeckillOrder;
import com.example.demo.pojo.orderItem;

import lombok.extern.slf4j.Slf4j;

import com.example.demo.pojo.CartVoItem;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class OrderServiceimpl implements OrderService{

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceimpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductsMapper productsMapper;

    @Autowired
    private OrderPackageMapper orderPackageMapper;

    @Autowired
    private CartService cartService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderCreationService orderCreationService;

    @Autowired
    private OrderCancellationService orderCancellationService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void creatOrder(Long memberId, Order order) {
        if (order == null) {
            throw new IllegalArgumentException("订单不能为空");
        }
        orderCreationService.setShippingAddressSnapshot(order, memberId, order.getAddressId());
        if (orderMapper.creatOrder(order) != 1) {
            throw new IllegalStateException("创建订单失败");
        }
    }

    @Override
    public ArrayList<Order> getOrderList(Long memberId) {
        ArrayList<Order> orderList = orderMapper.getOrderList(memberId);
        return orderList;
    }

    @Override
    public orderItem getOrderItemById(Long id) {
        orderItem oderItm = orderMapper.getOrderItemById(id);

        return oderItm;
    }

    @Override
    public Order getOrderByOrderSn(String orderSn)
    {
        Order order = orderMapper.getOrderByOrderSn(orderSn);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPackage shipOrder(String orderSn, OrderPackage orderPackage) {
        if (LoginFilter.getUserId() == null || !"ADMIN".equals(LoginFilter.getRole())) {
            throw new IllegalStateException("只有管理员可以发货");
        }
        if (!StringUtils.hasText(orderSn)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (orderPackage == null) {
            throw new IllegalArgumentException("包裹信息不能为空");
        }
        if (!StringUtils.hasText(orderPackage.getCarrierName())) {
            throw new IllegalArgumentException("承运商名称不能为空");
        }
        if (!StringUtils.hasText(orderPackage.getTrackingNumber())) {
            throw new IllegalArgumentException("物流运单号不能为空");
        }

        Order order = orderMapper.getOrderByOrderSn(orderSn);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!Integer.valueOf(1).equals(order.getStatus())) {
            throw new IllegalStateException("只有待发货订单可以创建包裹");
        }

        orderPackage.setId(null);
        orderPackage.setOrderId(order.getId());
        orderPackage.setCarrierCode(StringUtils.trimWhitespace(orderPackage.getCarrierCode()));
        orderPackage.setCarrierName(StringUtils.trimWhitespace(orderPackage.getCarrierName()));
        orderPackage.setTrackingNumber(StringUtils.trimWhitespace(orderPackage.getTrackingNumber()));
        orderPackage.setShippedAt(new Date());
        orderPackage.setStatus(1);

        if (orderPackageMapper.createPackage(orderPackage) != 1) {
            throw new IllegalStateException("创建包裹失败");
        }
        if (orderMapper.markOrderShipped(order.getId()) != 1) {
            throw new IllegalStateException("订单状态已变化，发货未生效");
        }

        return orderPackage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrderReceipt(String orderSn, Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("当前用户未登录");
        }
        if (!StringUtils.hasText(orderSn)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        Order order = orderMapper.getOrderByOrderSnAndMemberId(orderSn, memberId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!Integer.valueOf(2).equals(order.getStatus())) {
            throw new IllegalStateException("只有待收货订单可以确认收货");
        }
        if (orderMapper.confirmOrderReceipt(orderSn, memberId) != 1) {
            throw new IllegalStateException("订单状态已变化，确认收货未生效");
        }
    }

    @Override
    public String selectProductFromCart(Long addressId) throws Exception {
        Long userId = LoginFilter.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user is available for checkout");
        }

        //筛选购物车中选中状态的商品
        List<CartVoItem> selectProductFromCart = cartService.selectIdQuantity();

        String orderSn = orderCreationService.createOrder(userId, addressId, selectProductFromCart);

        for(CartVoItem item : selectProductFromCart)
        {
            cartService.deleteCartByProductId(userId, item.getProductId());
        }

        return creatAlipayOrder(orderSn, userId);
    }

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void cancelUnpaidOrders() {
        List<Order> unpaidOrders = orderMapper.findUnpaidBefore();
        for (Order order : unpaidOrders) {
            try {
                orderCancellationService.cancelOrder(order.getOrderSn(), order.getMemberId());
            } catch (Exception e) {
                logger.warn("Failed to cancel expired order {}", order.getOrderSn(), e);
            }
        }
    }

    @Override
    public String creatAlipayOrder(String orderSn, Long memberId) throws Exception
    {
        Order order = orderMapper.getOrderByOrderSnAndMemberId(orderSn, memberId);

        if(order == null)
        {
            throw new Exception("订单不存在");
         
        }

        if(order.getStatus() != 0)
        {
            throw new Exception("订单不在支付状态");
        }

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        request.setReturnUrl("http://localhost:8080/return");

        request.setNotifyUrl("http://localhost:8080/order/pay/notify");

        Map<String,Object> bizMap = new HashMap<>();
        bizMap.put("out_trade_no",order.getOrderSn());
        bizMap.put("total_amount",order.getTotalAmount());
        bizMap.put("subject", "电商订单 " + order.getOrderSn());
        bizMap.put("product_code","FAST_INSTANT_TRADE_PAY");

        String bizContent = JSONObject.toJSONString(bizMap);

        request.setBizContent(bizContent);

        AlipayTradePagePayResponse response;
        
        response = alipayClient.pageExecute(request);
        if(response.isSuccess())
        {
            return response.getBody();
        }else
        {
            throw new IllegalStateException("支付宝支付接口调用失败：code=" + response.getCode()
                    + ", subCode=" + response.getSubCode()
                    + ", message=" + response.getMsg()
                    + ", subMessage=" + response.getSubMsg());
        }

        
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(String orderSn, String tradeNo,String totalAmount) {
        Order order = orderMapper.getOrderByOrderSn(orderSn);

        if (order == null || !Integer.valueOf(0).equals(order.getStatus())) {
            return;
        }

        if (BigDecimal.valueOf(Double.parseDouble(totalAmount)).compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalStateException("支付金额与订单金额不匹配");
        }

        if (orderMapper.updateOrderStatusByOrderSn(orderSn) != 1) {

            log.info("支付状态修改未成功");
            return;
        }

        SeckillOrder seckillOrder = productsMapper.getSeckillOrderById(order.getId());
        if (seckillOrder != null && productsMapper.updateSeckillOrderPayStatus(seckillOrder.getId(), 1) != 1) {
            throw new IllegalStateException("同步秒杀订单支付状态失败");
        }
    }

}
