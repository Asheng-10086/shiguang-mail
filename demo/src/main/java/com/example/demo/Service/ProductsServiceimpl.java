package com.example.demo.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Config.EsInit;
import com.example.demo.Filter.LoginFilter;
import com.example.demo.Mapper.OrderMapper;
import com.example.demo.Mapper.ProductsMapper;
import com.example.demo.Mapper.UserAddressMapper;
import com.example.demo.Service.ProductsServiceimpl.SeckillGoodsOrder;
import com.example.demo.pojo.Order;
import com.example.demo.pojo.UserAddress;
import com.example.demo.pojo.Products;
import com.example.demo.pojo.SeckillGoods;
import com.example.demo.pojo.SeckillOrder;
import com.example.demo.pojo.orderItem;
import com.example.demo.utils.RedisIdCreate;

import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;


@Service
@Slf4j
public class ProductsServiceimpl implements ProductsService{
    @Autowired
    private ProductsMapper productsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redisClient;

    @Autowired
    private EsInit esInit;

    private static final RedisScript<Long> redisScript;
    static{

        DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("seckillOrder.lua")));
        defaultRedisScript.setResultType(Long.class);
        redisScript = defaultRedisScript;
    }

    private static final ExecutorService executionException = Executors.newSingleThreadExecutor();

    private final ProductsServiceimpl proxy;

    public ProductsServiceimpl(@Lazy ProductsServiceimpl proxy) {
        this.proxy = proxy;
    }

    
    public class SeckillGoodsOrder implements Runnable{

        @Override
        public void run() {
            
            //异步下单的逻辑
            while(true){

                try{

                    List<MapRecord<String,Object,Object>> records = stringRedisTemplate.opsForStream().read(
                                            Consumer.from("seckill-order-group", "consumer-1"),
                                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                            StreamOffset.create("seckill:stream:", ReadOffset.lastConsumed())
                    );

                    if(records == null || records.isEmpty()){
                        continue;
                    }

                    for(MapRecord<String,Object,Object> record : records){

                        Map<Object,Object> value = record.getValue();
                        Long productId = Long.valueOf(value.get("productId").toString());
                        Long userId = Long.valueOf(value.get("userId").toString());
                        Long id = Long.valueOf(value.get("orderId").toString());
                        Long addressId = Long.valueOf(value.get("addressId").toString());

                        proxy.createSeckillOrder(userId, productId, id, addressId);
                        //确认消费
                        stringRedisTemplate.opsForStream().acknowledge("seckill:stream:","seckill-order-group",record.getId());
                    }

                }catch (Exception e) {
                    log.error("处理秒杀订单消息失败", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList(){
            while(true){

                try{

                    List<MapRecord<String,Object,Object>> records = stringRedisTemplate.opsForStream().read(
                                            Consumer.from("seckill-order-group", "consumer-1"),
                                            StreamReadOptions.empty(),
                                            StreamOffset.create("seckill:stream:", ReadOffset.from("0"))
                    );

                    if(records == null || records.isEmpty()){
                        return;
                    }

                    for(MapRecord<String,Object,Object> record : records){

                        Map<Object,Object> value = record.getValue();
                        Long productId = Long.valueOf(value.get("productId").toString());
                        Long userId = Long.valueOf(value.get("userId").toString());
                        Long orderId = Long.valueOf(value.get("orderId").toString());
                        Long addressId = Long.valueOf(value.get("addressId").toString());

                        proxy.createSeckillOrder(userId, productId, orderId, addressId);
                        //确认消费
                        stringRedisTemplate.opsForStream().acknowledge("seckill:stream:","seckill-order-group",record.getId());
                    }
                }catch (Exception e) {
                    log.error("处理秒杀订单消息失败", e);
                    return;
                }
                
            }
        }

    }

    @PostConstruct
    public void ascSeckill(){
        try {
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(StandardCharsets.UTF_8),
                    "seckill:stream:".getBytes(StandardCharsets.UTF_8),
                    "seckill-order-group".getBytes(StandardCharsets.UTF_8),
                    "$".getBytes(StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(StandardCharsets.UTF_8)));
        } catch (RedisSystemException e) {
            Throwable cause = e;
            while (cause != null && !(cause instanceof io.lettuce.core.RedisBusyException)) {
                cause = cause.getCause();
            }
            if (cause == null) {
                throw e;
            }
        }
        SeckillGoodsOrder seckillGoodsOrder = new SeckillGoodsOrder();
        executionException.submit(seckillGoodsOrder);
    }

    @Override
    public ArrayList<Products> getProductsList() {
        ArrayList<Products> productsList = productsMapper.getProductsList();

        return productsList;
    }

    @Override
    public Products getProductById(Long id) {
        
        Products product = productsMapper.getProductById(id);
        return product;
    }

    @Override
    public ArrayList<Products> getProductsByCategoryId(Long categoryid) {
        ArrayList<Products> productsList = productsMapper.getProductsByCategoryId(categoryid);
        
        return productsList;
    }

    @Override
    public List<SeckillGoods> getSeckillGoodsList() {
        List<SeckillGoods> seckillGoodsList = productsMapper.getSeckillGoodsList();
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            String stockKey = "seckill:stock:" + seckillGoods.getId();
            String currentStock = stringRedisTemplate.opsForValue().get(stockKey);
            if (currentStock == null) {
                stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(seckillGoods.getStock()));
                currentStock = stringRedisTemplate.opsForValue().get(stockKey);
            }
            if (currentStock != null) {
                seckillGoods.setStock(Integer.valueOf(currentStock));
            }
        }
        return seckillGoodsList;
    }

    @Override
    public void addProducts(Products product) {
        if(LoginFilter.getUserId() == null){
            throw new IllegalStateException("用户未进行登录");
        }
        
        if(!LoginFilter.getRole().equals("ADMIN")){
            throw new IllegalStateException("用户没有权限添加商品");
        }

        if(product == null){
            throw new IllegalStateException("商品信息不能为空");
        }

        productsMapper.addProducts(product);
    }

    @Override
    public void addSeckillProduct(SeckillGoods seckillGoods){

        if(LoginFilter.getUserId() == null){
            throw new IllegalStateException("用户未进行登录");
        }
        
        if(!"ADMIN".equals(LoginFilter.getRole())){
            throw new IllegalStateException("用户没有权限添加商品");
        }

        if(seckillGoods == null){
            throw new IllegalStateException("商品信息不能为空");
        }

        if (seckillGoods.getGoodsId() == null
                || seckillGoods.getSeckillPrice() == null
                || seckillGoods.getSeckillPrice().signum() <= 0
                || seckillGoods.getStartTime() == null
                || seckillGoods.getEndTime() == null
                || !seckillGoods.getStartTime().isBefore(seckillGoods.getEndTime())
                || !seckillGoods.getEndTime().isAfter(LocalDateTime.now())
                || seckillGoods.getStock() == null
                || seckillGoods.getStock() <= 0) {
            throw new IllegalArgumentException("秒杀活动信息无效");
        }

        Products product = productsMapper.getProductById(seckillGoods.getGoodsId());
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new IllegalArgumentException("请选择已上架的有效商品");
        }

        LocalDateTime now = LocalDateTime.now();
        seckillGoods.setStatus(seckillGoods.getStartTime().isAfter(now) ? 0 : 1);
        seckillGoods.setCreateTime(now);
        seckillGoods.setUpdateTime(now);
        if (productsMapper.addSeckillProduct(seckillGoods) != 1 || seckillGoods.getId() == null) {
            throw new IllegalStateException("创建秒杀活动失败");
        }

        stringRedisTemplate.opsForValue().set(
                "seckill:stock:" + seckillGoods.getId(),
                seckillGoods.getStock().toString());
    }

    @Override
    public Long seckillProduct(Long productId, Long addressId){

        // productId is the seckill_goods primary key used by the client.
        SeckillGoods seckillGoods = productsMapper.getSeckillGoodsById(productId);
        if (seckillGoods == null || seckillGoods.getStartTime() == null || seckillGoods.getEndTime() == null) {
            return 5L;
        }

        //查看秒杀是否开始
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(seckillGoods.getStartTime())){

            return 1L;
        }
        //查看秒杀是否结束
        if(now.isAfter(seckillGoods.getEndTime())){

            return 2L;
        }

        Long userId = LoginFilter.getUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未进行登录");
        }
        if (addressId == null) {
            List<UserAddress> addresses = userAddressMapper.getAddressListByUserId(userId);
            if (addresses != null) {
                for (UserAddress address : addresses) {
                    if (Integer.valueOf(1).equals(address.getIsDefault())) {
                        addressId = address.getId();
                        break;
                    }
                }
            }
            if (addressId == null) {
                throw new IllegalArgumentException("未提供收货地址且当前用户没有默认地址");
            }
        }
        if (userAddressMapper.getAddressByIdAndUserId(addressId, userId) == null) {
            throw new IllegalArgumentException("收货地址不存在或不属于当前用户");
        }
        RedisIdCreate redisIdCreate = new RedisIdCreate(stringRedisTemplate);
        long id = redisIdCreate.createRedisId("seckill_order:");
        Long result = stringRedisTemplate.execute(
                redisScript,
                Collections.emptyList(),
                String.valueOf(productId),
                String.valueOf(userId),
                String.valueOf(id),
                String.valueOf(addressId));

        if (result == null) {
            throw new IllegalStateException("秒杀请求处理失败");
        }
        if(result == 3L)
            return 3L;
        if(result == 4L)
            return 4L;
        if(result != 0L) {
            throw new IllegalStateException("秒杀请求返回未知状态");
        }

        return id;
        
    }
        
    @Transactional
    public void createSeckillOrder(Long userId, Long productId, Long orderId, Long addressId){

        RLock lock = redisClient.getLock("lock:order" + userId);

        lock.lock();
        try {
        
            // productId identifies the seckill campaign.
            SeckillGoods seckillGoods = productsMapper.getSeckillGoodsById(productId);
            if (seckillGoods == null) {
                throw new IllegalStateException("秒杀活动不存在");
            }
            Products product = productsMapper.getProductById(seckillGoods.getGoodsId());
            if (product == null) {
                throw new IllegalStateException("秒杀商品不存在");
            }
            UserAddress address = userAddressMapper.getAddressByIdAndUserId(addressId, userId);
            if (address == null) {
                throw new IllegalStateException("收货地址不存在或不属于当前用户");
            }

            // Create both order records in one transaction; their IDs link them.
            SeckillOrder seckillOrder = new SeckillOrder();
            
            BigDecimal payAmount = seckillGoods.getSeckillPrice();
            LocalDateTime now = LocalDateTime.now();
            
            seckillOrder.setId(orderId);
            seckillOrder.setGoodsId(productId);
            seckillOrder.setCreateTime(now);
            seckillOrder.setPayStatus(0);
            seckillOrder.setPayTime(null);
            seckillOrder.setPayableAmount(payAmount);
            seckillOrder.setUserId(userId);

            if (productsMapper.updateSeckillStock(productId, seckillGoods.getStock()) != 1) {
                throw new IllegalStateException("秒杀库存已变化，请重试");
            }
            if (productsMapper.creatSeckillOrder(seckillOrder) != 1) {
                throw new IllegalStateException("创建秒杀订单失败");
            }

            Order order = new Order();
            order.setId(orderId);
            order.setMemberId(userId);
            order.setAddressId(address.getId());
            order.setShippingRecipient(address.getRecipient());
            order.setShippingPhone(address.getPhone());
            order.setShippingProvince(address.getProvince());
            order.setShippingCity(address.getCity());
            order.setShippingDistrict(address.getDistrict());
            order.setShippingDetail(address.getDetail());
            order.setOrderSn("SK" + orderId);
            order.setTotalAmount(payAmount);
            order.setPayAmount(payAmount);
            order.setStatus(0);
            order.setPayType(0);
            order.setCreateTime(new Date());
            if (orderMapper.creatOrder(order) != 1) {
                throw new IllegalStateException("创建普通订单失败");
            }

            orderItem item = new orderItem();
            item.setId(IdUtil.getSnowflakeNextId());
            item.setOrderId(orderId);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductPrice(payAmount);
            item.setProductQuantity(1);
            item.setTotalPrice(payAmount);
            if (orderMapper.createOrderItem(item) != 1) {
                throw new IllegalStateException("创建秒杀订单商品明细失败");
            }

        }finally{
            lock.unlock();
        }
    }

    @Override
    public List<Products> searchProduct(String keyword) {
        
        SearchHits<Products> searchProducts = esInit.searchProducts(keyword);
        List<Products> listProducts = new ArrayList<>();

        for(SearchHit<Products> searchHit : searchProducts){

            Products content = searchHit.getContent();

            listProducts.add(content);
        }

        return listProducts;
    }



}
