package com.freshmart.api.catalog;
import java.math.BigDecimal;
public record ProductResponse(Long id,String name,String description,BigDecimal priceInr,String unit,int stockQuantity,String imageUrl,Long categoryId,String categoryName){
 public static ProductResponse from(Product p){return new ProductResponse(p.getId(),p.getName(),p.getDescription(),p.getPriceInr(),p.getUnit(),p.getStockQuantity(),p.getImageUrl(),p.getCategoryId(),p.getCategoryName());}
}
