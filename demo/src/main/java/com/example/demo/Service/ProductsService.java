package com.example.demo.Service;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.pojo.Products;
import com.example.demo.pojo.SeckillGoods;

public interface ProductsService {
    public ArrayList<Products> getProductsList();

    public Products getProductById(Long id);

    public ArrayList<Products> getProductsByCategoryId(Long categoryid);

    public void addProducts(Products product);

    public List<SeckillGoods> getSeckillGoodsList();

    public Long seckillProduct(Long productId, Long addressId);

    public List<Products> searchProduct(String queryString);

    public void addSeckillProduct(SeckillGoods seckillGoods);
}
