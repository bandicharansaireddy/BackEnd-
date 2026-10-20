package com.freshmart.api.catalog;
import jakarta.persistence.*;
@Entity @Table(name="categories")
public class Category {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String name;
 @Column(nullable=false,unique=true,length=90) private String slug;
 protected Category(){}
 public Category(String name,String slug){this.name=name;this.slug=slug;}
 public Long getId(){return id;} public String getName(){return name;} public String getSlug(){return slug;}
}
