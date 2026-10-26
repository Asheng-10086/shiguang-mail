package com.example.demo.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Filter.LoginFilter;
import com.example.demo.Mapper.CartMapper;
import com.example.demo.pojo.Cart;
import com.example.demo.pojo.CartVoItem;

@Service
public class CartServiceimpl implements CartService{

    @Autowired
    private CartMapper cartMapper;
    
    @Override
    public ArrayList<Cart> getCartlist(Long memberId) {
        
        ArrayList<Cart> cartList = cartMapper.getCartlist(memberId);

        if(cartList == null){
            throw new IllegalArgumentException("购物车为空");
        }

        return cartList;
    }

    @Override
    public Cart creatProduct(Cart cart) {
        cartMapper.creatProduct(cart);

        return cart;
    }

    @Override
    public Cart updateCart(Long memberId, Cart cart) {
       
        cartMapper.updateCart(memberId, cart);
        return cart;
    }

    @Override
    public void deleteCart(Long id) {
        cartMapper.deleteCart(id);
    }
    
    @Override
    public List<CartVoItem> selectIdQuantity() {
        
        Long userId = LoginFilter.getUserId();
        List<CartVoItem> cartVoItems = cartMapper.selectIdQuantity(userId);
        return cartVoItems;
    }

    @Override
    public void deleteCartByProductId(Long memberId, Long productId) {
        cartMapper.deleteCartByProductId(memberId, productId);
    }
}
