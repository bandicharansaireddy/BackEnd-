package com.freshmart.api.commerce.cart;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public final class CartDtos {
 private CartDtos(){}
 public record AddItemRequest(@NotNull @Positive Long productId,@Min(1) @Max(99) int quantity){}
 public record QuantityRequest(@Min(1) @Max(99) int quantity){}
 public record CartLine(Long id,Long productId,String name,String description,String imageUrl,String unit,BigDecimal unitPriceInr,int stockQuantity,int quantity,BigDecimal lineTotalInr){}
 public record CartResponse(List<CartLine> items,BigDecimal subtotalInr){public static CartResponse empty(){return new CartResponse(List.of(),BigDecimal.ZERO);}}
}
