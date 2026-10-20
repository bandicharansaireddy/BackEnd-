package com.freshmart.api.commerce.order;
import com.freshmart.api.catalog.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="order_items")
public class OrderItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private Order order;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id",nullable=false) private Product product;
 @Column(name="product_name",nullable=false,length=120) private String productName;
 @Column(nullable=false,length=40) private String unit;
 @Column(name="unit_price_inr",nullable=false,precision=10,scale=2) private BigDecimal unitPriceInr;
 @Column(nullable=false) private int quantity;
 protected OrderItem(){}
 public OrderItem(Product product,int quantity){this.product=product;this.productName=product.getName();this.unit=product.getUnit();this.unitPriceInr=product.getPriceInr();this.quantity=quantity;}
 public void setOrder(Order order){this.order=order;}
 public Long getId(){return id;} public Long getProductId(){return product.getId();} public String getProductName(){return productName;} public String getUnit(){return unit;} public BigDecimal getUnitPriceInr(){return unitPriceInr;} public int getQuantity(){return quantity;}
}
