package com.example.demo.Mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.pojo.Order;
import com.example.demo.pojo.orderItem;

@Mapper
public interface OrderMapper {

    public int creatOrder(Order order);

    public ArrayList<Order> getOrderList(@Param("memberId") Long memberId);

    public orderItem getOrderItemById(Long id);

    public List<orderItem> getOrderItemsByOrderId(Long orderId);

    public int createOrderItem(orderItem item);

    public Order getOrderByOrderSn(@Param("orderSn") String orderSn);

    public Order getOrderByOrderSnAndMemberId(@Param("orderSn") String orderSn, @Param("memberId") Long memberId);

    public int updateOrderStatusByOrderSn(@Param("orderSn") String orderSn);

    public int markOrderShipped(@Param("orderId") Long orderId);

    public int confirmOrderReceipt(@Param("orderSn") String orderSn, @Param("memberId") Long memberId);

    public int cancelPendingOrder(@Param("orderSn") String orderSn, @Param("memberId") Long memberId);

    public List<Order> findUnpaidBefore();
}