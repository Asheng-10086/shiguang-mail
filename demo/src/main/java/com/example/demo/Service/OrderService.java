package com.example.demo.Service;

import java.util.ArrayList;

import com.example.demo.pojo.Order;
import com.example.demo.pojo.OrderPackage;
import com.example.demo.pojo.orderItem;

public interface OrderService {
    public void creatOrder(Long memberId, Order order);

    public ArrayList<Order> getOrderList(Long memberId);

    public orderItem getOrderItemById(Long id);

    public Order getOrderByOrderSn(String orderSn);

    public String creatAlipayOrder(String orderSn, Long memberId) throws Exception;

    public void handlePaymentSuccess(String orderSn,String tradeNo,String totalAmount);

    public String selectProductFromCart(Long addressId) throws Exception;

    public OrderPackage shipOrder(String orderSn, OrderPackage orderPackage);

    public void confirmOrderReceipt(String orderSn, Long memberId);

}
