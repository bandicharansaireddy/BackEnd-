package com.freshmart.api.commerce.cart;
import com.freshmart.api.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="carts")
public class Cart {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,unique=true) private User user;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @OneToMany(mappedBy="cart",cascade=CascadeType.ALL,orphanRemoval=true) private List<CartItem> items=new ArrayList<>();
 protected Cart(){}
 public Cart(User user){this.user=user;}
 @PrePersist void onCreate(){if(createdAt==null)createdAt=Instant.now();}
 public Long getId(){return id;} public User getUser(){return user;} public List<CartItem> getItems(){return items;}
 public void add(CartItem item){items.add(item);item.setCart(this);} public void remove(CartItem item){items.remove(item);item.setCart(null);} public void clear(){items.clear();}
}
