package com.freshmart.api.commerce.cart;
import com.freshmart.api.catalog.Product;
import jakarta.persistence.*;
@Entity @Table(name="cart_items",uniqueConstraints=@UniqueConstraint(name="uk_cart_items_product",columnNames={"cart_id","product_id"}))
public class CartItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cart_id",nullable=false) private Cart cart;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id",nullable=false) private Product product;
 @Column(nullable=false) private int quantity;
 protected CartItem(){}
 public CartItem(Product product,int quantity){this.product=product;this.quantity=quantity;}
 public Long getId(){return id;} public Cart getCart(){return cart;} public Product getProduct(){return product;} public int getQuantity(){return quantity;}
 public void setCart(Cart cart){this.cart=cart;} public void setQuantity(int quantity){this.quantity=quantity;}
}
