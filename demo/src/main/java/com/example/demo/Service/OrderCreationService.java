package com.example.demo.Service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Mapper.OrderMapper;
import com.example.demo.Mapper.ProductsMapper;
import com.example.demo.Mapper.UserAddressMapper;
import com.example.demo.pojo.CartVoItem;
import com.example.demo.pojo.Order;
import com.example.demo.pojo.Products;
import com.example.demo.pojo.UserAddress;
import com.example.demo.pojo.orderItem;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;

@Service
public class OrderCreationService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductsMapper productMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long memberId, Long addressId, List<CartVoItem> selectProductFromCart) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member is required");
        }
        if (addressId == null) {
            throw new IllegalArgumentException("收货地址不能为空");
        }
        if (selectProductFromCart == null || selectProductFromCart.isEmpty()) {
            throw new IllegalArgumentException("购物车没有选中的商品");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setMemberId(memberId);
        setShippingAddressSnapshot(order, memberId, addressId);

        String orderSn = generateOrderSn();
        order.setOrderSn(orderSn);
        order.setStatus(0);
        order.setPayType(0);
        order.setCreateTime(DateUtil.date());

        List<orderItem> orderItems = new java.util.ArrayList<>();
        for (CartVoItem cartVoItem : selectProductFromCart) {
            if (cartVoItem == null) {
                throw new IllegalArgumentException("Cart item is required");
            }

            Long productId = cartVoItem.getProductId();
            Integer quantity = cartVoItem.getQuantity();

            if (productId == null) {
                throw new IllegalArgumentException("Product is required");
            }
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            Products product = productMapper.getProductById(productId);
            if (product == null || product.getPrice() == null) {
                throw new IllegalArgumentException("Product does not exist or has no price");
            }

            int updateResult = productMapper.updateStock(productId, quantity);
            if (updateResult != 1) {
                throw new IllegalStateException("Insufficient stock for product " + productId);
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(lineTotal);

            orderItem item = new orderItem();
            item.setId(IdUtil.getSnowflakeNextId());
            item.setOrderId(order.getId());
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setProductPrice(product.getPrice());
            item.setProductQuantity(quantity);
            item.setTotalPrice(lineTotal);
            orderItems.add(item);
        }

        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        if (orderMapper.creatOrder(order) != 1) {
            throw new IllegalStateException("创建订单失败");
        }
        for (orderItem item : orderItems) {
            if (orderMapper.createOrderItem(item) != 1) {
                throw new IllegalStateException("创建订单商品明细失败");
            }
        }

        return orderSn;
    }

    public void setShippingAddressSnapshot(Order order, Long memberId, Long addressId) {
        if (order == null || memberId == null || addressId == null) {
            throw new IllegalArgumentException("订单、用户和收货地址均不能为空");
        }

        UserAddress address = userAddressMapper.getAddressByIdAndUserId(addressId, memberId);
        if (address == null) {
            throw new IllegalArgumentException("收货地址不存在或不属于当前用户");
        }

        order.setMemberId(memberId);
        order.setAddressId(address.getId());
        order.setShippingRecipient(address.getRecipient());
        order.setShippingPhone(address.getPhone());
        order.setShippingProvince(address.getProvince());
        order.setShippingCity(address.getCity());
        order.setShippingDistrict(address.getDistrict());
        order.setShippingDetail(address.getDetail());
    }

    private static String generateOrderSn() {
        String snowflakeId = IdUtil.getSnowflakeNextIdStr();
        String date = DateUtil.format(new java.util.Date(), "yyyyMMdd");
        return date + snowflakeId.substring(snowflakeId.length() - 10);
    }
}
