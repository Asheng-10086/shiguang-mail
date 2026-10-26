package com.example.demo.Mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.pojo.Products;
import com.example.demo.pojo.SeckillGoods;
import com.example.demo.pojo.SeckillOrder;

@Mapper
public interface ProductsMapper {
    public ArrayList<Products> getProductsList();

    public List<Products> getAllProductsForIndex();

    public Products getProductById(Long id);

    public ArrayList<Products> getProductsByCategoryId(Long categoryId);

    public List<SeckillGoods> getSeckillGoodsList();

    public void addProducts(Products product);

    public SeckillGoods getSeckillGoodsById(Long id);

    public int creatSeckillOrder(SeckillOrder seckillOrder);

    public SeckillOrder getSeckillOrderById(@Param("id") Long id);

    public int updateSeckillOrderPayStatus(@Param("id") Long id, @Param("payStatus") Integer payStatus);

    public int updateSeckillStock(@Param("productId") Long productId,@Param("stock") Integer stock);

    public int restoreSeckillStock(@Param("goodsId") Long goodsId);

    public int addSeckillProduct(SeckillGoods seckillGoods);

    public int updateStock(@Param("productId") Long productId,@Param("quantity") Integer quantity);

    public int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
