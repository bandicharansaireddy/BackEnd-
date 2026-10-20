package com.freshmart.api.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="category_id",nullable=false) private Category category;
 @Column(nullable=false,length=120) private String name;
 @Column(nullable=false,length=1000) private String description;
 @Column(name="price_inr",nullable=false,precision=10,scale=2) private BigDecimal priceInr;
 @Column(nullable=false,length=40) private String unit;
 @Column(name="stock_quantity",nullable=false) private int stockQuantity;
 @Column(name="image_url",length=500) private String imageUrl;
 @Column(nullable=false) private boolean active;
 protected Product(){}
 public Product(Category category,String name,String description,BigDecimal priceInr,String unit,int stockQuantity,String imageUrl){this.category=category;this.name=name;this.description=description;this.priceInr=priceInr;this.unit=unit;this.stockQuantity=stockQuantity;this.imageUrl=imageUrl;this.active=true;}
 public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;}
 public BigDecimal getPriceInr(){return priceInr;} public String getUnit(){return unit;} public int getStockQuantity(){return stockQuantity;}
 public String getImageUrl(){return imageUrl;} public Long getCategoryId(){return category.getId();} public String getCategoryName(){return category.getName();}
 public boolean isActive(){return active;} public void setStockQuantity(int quantity){this.stockQuantity=quantity;}
 public void update(Category category,String name,String description,BigDecimal priceInr,String unit,int stockQuantity,String imageUrl,boolean active){this.category=category;this.name=name;this.description=description;this.priceInr=priceInr;this.unit=unit;this.stockQuantity=stockQuantity;this.imageUrl=imageUrl;this.active=active;}
}
