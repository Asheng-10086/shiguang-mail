package com.example.demo.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.Mapper.OrderMapper;
import com.example.demo.Mapper.ProductsMapper;
import com.example.demo.pojo.Order;
import com.example.demo.pojo.SeckillOrder;
import com.example.demo.pojo.orderItem;

@Service
public class OrderCancellationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCancellationService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductsMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderSn, Long memberId) {
        if (orderSn == null || orderSn.isBlank()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("用户未登录");
        }

        Order order = orderMapper.getOrderByOrderSnAndMemberId(orderSn, memberId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            throw new IllegalStateException("只有待付款订单可以取消");
        }

        SeckillOrder seckillOrder = productMapper.getSeckillOrderById(order.getId());
        List<orderItem> items = orderMapper.getOrderItemsByOrderId(order.getId());
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("订单缺少商品明细，无法安全回补库存");
        }

        int cancelled = orderMapper.cancelPendingOrder(orderSn, memberId);
        if (cancelled != 1) {
            throw new IllegalStateException("订单状态已变化，取消未生效");
        }

        if (seckillOrder != null) {
            if (productMapper.updateSeckillOrderPayStatus(seckillOrder.getId(), 2) != 1) {
                throw new IllegalStateException("秒杀订单状态已变化，取消未生效");
            }
            if (productMapper.restoreSeckillStock(seckillOrder.getGoodsId()) != 1) {
                throw new IllegalStateException("回补秒杀库存失败，活动 ID：" + seckillOrder.getGoodsId());
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        Long restoredStock = stringRedisTemplate.opsForValue()
                                .increment("seckill:stock:" + seckillOrder.getGoodsId());
                        if (restoredStock == null) {
                            logger.error("Redis 秒杀库存回补未返回结果，活动 ID {}", seckillOrder.getGoodsId());
                        }
                    } catch (RuntimeException e) {
                        logger.error("数据库已取消秒杀订单，但 Redis 库存回补失败，活动 ID {}",
                                seckillOrder.getGoodsId(), e);
                    }
                }
            });
            return;
        }

        for (orderItem item : items) {
            if (item.getProductId() == null || item.getProductQuantity() == null
                    || item.getProductQuantity() <= 0) {
                throw new IllegalStateException("订单商品明细无效，无法回补库存");
            }
            int restored = productMapper.restoreStock(item.getProductId(), item.getProductQuantity());
            if (restored != 1) {
                throw new IllegalStateException("回补商品库存失败，商品 ID：" + item.getProductId());
            }
        }
    }
}
