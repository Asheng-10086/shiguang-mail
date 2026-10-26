package com.example.demo.Mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.pojo.Cart;
import com.example.demo.pojo.CartVoItem;

@Mapper
public interface CartMapper {
    public ArrayList<Cart> getCartlist(@Param("memberId") Long memberId);
    
    public void creatProduct(Cart cart);

    public void updateCart(@Param("memberId") Long memberId, @Param("cart") Cart cart);

    public void deleteCart(Long id);

    public List<CartVoItem> selectIdQuantity(@Param("memberId") Long memberId);

    public void deleteCartByProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);
}
