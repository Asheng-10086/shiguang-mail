package com.example.demo.Controller;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Filter.LoginFilter;
import com.example.demo.Service.CartService;
import com.example.demo.pojo.Cart;
import com.example.demo.pojo.Result;

@RestController
public class CartController {
    
    @Autowired
    private CartService cartService;

    @GetMapping("/cart")
    public Result getCartList()
    {
        Long userId = LoginFilter.getUserId();
        ArrayList<Cart> cartList = cartService.getCartlist(userId);
        return Result.success(cartList);
    }

    @PostMapping("/addToCart")
    public Result creatProduct(Cart cart)
    {
        cart.setMemberId(LoginFilter.getUserId());
        cartService.creatProduct(cart);
        return Result.success(cart);
    }

    @PutMapping("/cart")
    public Result updateCart(Cart cart)
    {
        Long userId = LoginFilter.getUserId();

        cartService.updateCart(userId, cart);
        return Result.success(cart);
    }

    @DeleteMapping("delcart")
    public Result deleteCart()
    {   
        Long userId = LoginFilter.getUserId();

        cartService.deleteCart(userId);
        return Result.success();
    }

}
