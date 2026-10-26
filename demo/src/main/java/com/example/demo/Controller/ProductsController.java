package com.example.demo.Controller;

import com.example.demo.DemoApplication;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.demo.Service.ProductsService;
import com.example.demo.pojo.Products;
import com.example.demo.pojo.Result;
import com.example.demo.pojo.SeckillGoods;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class ProductsController {
    
    private final DemoApplication demoApplication;
    @Autowired
    private ProductsService productsService;

    ProductsController(DemoApplication demoApplication) {
        this.demoApplication = demoApplication;
    }

    @GetMapping("/products")
    public Result getProductsList()
    {
        ArrayList<Products> productList = productsService.getProductsList();

        return Result.success(productList);
    }

    @GetMapping("/products/{id}")
    public Result getProductById(@PathVariable Long id)
    {
        Products product = productsService.getProductById(id);

        return Result.success(product);
    }

    @GetMapping("/category/{categoryId}")
    public Result getProductsByCategoryId(@PathVariable Long categoryId)
    {
        ArrayList<Products> productList = productsService.getProductsByCategoryId(categoryId);

        return Result.success(productList);
    }

    @GetMapping("/seckillGoods")
    public Result getSeckillGoodsList()
    {
        return Result.success(productsService.getSeckillGoodsList());
    }

    @PostMapping("/addProduct")
    public Result addProducts(@RequestBody Products product)
    {
        productsService.addProducts(product);
        return Result.success(product);
    }

    @PostMapping("/searchProduct/{keyString}")
    public Result searchProduct(@PathVariable String keyString){
        List<Products> searchProduct = productsService.searchProduct(keyString);

        return Result.success(searchProduct);
    }

    @PostMapping("/seckillProduct")
    public Result seckillProduct(@RequestParam Long productId, @RequestParam(required = false) Long addressId){

        Long resultLong = productsService.seckillProduct(productId, addressId);

        if(resultLong == 1L)
            return Result.error("秒杀还未开始");
        if(resultLong == 2L)
            return Result.error("秒杀已经结束");
        if(resultLong == 3L)
            return Result.error("库存不足");
        if(resultLong == 4L)
            return Result.error("您已经下过一单");
        if(resultLong == 5L)
            return Result.error("秒杀活动不存在");

        return Result.success(resultLong);
    }
   
    @PostMapping("/addSeckillGoods")
    public Result addSeckillGoods(@RequestBody SeckillGoods seckillGoods){

        productsService.addSeckillProduct(seckillGoods);
        return Result.success();
    }
    

}
