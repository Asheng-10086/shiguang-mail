package com.example.demo.Service;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.pojo.Cart;
import com.example.demo.pojo.CartVoItem;

public interface CartService {
    public ArrayList<Cart> getCartlist(Long memberId);
    
    public Cart creatProduct(Cart cart);

    public Cart updateCart(Long memberId, Cart cart);

    public void deleteCart(Long id);

    public List<CartVoItem> selectIdQuantity();

    public void deleteCartByProductId(Long memberId, Long productId);
}
