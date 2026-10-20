package com.freshmart.api.commerce.order;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class OrderDtos {
 private OrderDtos(){}
 public record CheckoutRequest(@NotNull @Positive Long addressId){}
 public record Item(Long productId,String name,String unit,BigDecimal unitPriceInr,int quantity,BigDecimal lineTotalInr){
  public static Item from(OrderItem item){return new Item(item.getProductId(),item.getProductName(),item.getUnit(),item.getUnitPriceInr(),item.getQuantity(),item.getUnitPriceInr().multiply(BigDecimal.valueOf(item.getQuantity())));}
 }
 public record OrderResponse(Long id,String status,String paymentStatus,BigDecimal subtotalInr,BigDecimal deliveryFeeInr,BigDecimal totalInr,String fullName,String mobile,String addressLine,String city,String pincode,Instant createdAt,List<Item> items){
  public static OrderResponse from(Order order){return new OrderResponse(order.getId(),order.getStatus(),order.getPaymentStatus(),order.getSubtotalInr(),order.getDeliveryFeeInr(),order.getTotalInr(),order.getFullName(),order.getMobile(),order.getAddressLine(),order.getCity(),order.getPincode(),order.getCreatedAt(),order.getItems().stream().map(Item::from).toList());}
 }
}
