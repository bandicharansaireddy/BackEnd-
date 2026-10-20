package com.freshmart.api.commerce.order;

import com.freshmart.api.catalog.PageResponse;
import com.freshmart.api.catalog.Product;
import com.freshmart.api.catalog.ProductRepository;
import com.freshmart.api.commerce.address.Address;
import com.freshmart.api.commerce.address.AddressRepository;
import com.freshmart.api.commerce.cart.Cart;
import com.freshmart.api.commerce.cart.CartItem;
import com.freshmart.api.commerce.cart.CartRepository;
import com.freshmart.api.user.User;
import com.freshmart.api.user.UserRepository;
import com.freshmart.api.commerce.order.OrderDtos.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController @RequestMapping("/api/orders")
public class OrderController {
 private static final BigDecimal FREE_DELIVERY_THRESHOLD=new BigDecimal("499.00");
 private static final BigDecimal DELIVERY_FEE=new BigDecimal("40.00");
 private final OrderRepository orders; private final CartRepository carts; private final AddressRepository addresses;
 private final ProductRepository products; private final UserRepository users; private final EntityManager entityManager;
 public OrderController(OrderRepository orders,CartRepository carts,AddressRepository addresses,ProductRepository products,UserRepository users,EntityManager entityManager){this.orders=orders;this.carts=carts;this.addresses=addresses;this.products=products;this.users=users;this.entityManager=entityManager;}

 @PostMapping @Transactional public OrderResponse create(@Valid @RequestBody CheckoutRequest request,Authentication auth){
  Long uid=userId(auth);Cart cart=carts.findWithItemsByUserId(uid).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Your cart is empty"));
  if(cart.getItems().isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Your cart is empty");
  Address address=addresses.findByIdAndUser_Id(request.addressId(),uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Delivery address not found"));
  Map<Long,Product> locked=new LinkedHashMap<>();
  for(CartItem item:cart.getItems().stream().sorted(java.util.Comparator.comparing(i->i.getProduct().getId())).toList()){
   Long pid=item.getProduct().getId();Product product=products.findActiveByIdForUpdate(pid).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"A product in your cart is no longer available"));
   entityManager.refresh(product,LockModeType.PESSIMISTIC_WRITE);
   if(item.getQuantity()>product.getStockQuantity())throw new ResponseStatusException(HttpStatus.CONFLICT,"A product in your cart no longer has enough stock");
   locked.put(pid,product);
  }
  BigDecimal subtotal=cart.getItems().stream().map(i->locked.get(i.getProduct().getId()).getPriceInr().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);
  BigDecimal delivery=subtotal.compareTo(FREE_DELIVERY_THRESHOLD)>=0?BigDecimal.ZERO:DELIVERY_FEE;
  User user=users.findByIdAndActiveTrue(uid).orElseThrow(OrderController::unauthorized);
  var snapshot=new Order.AddressSnapshot(address.getFullName(),address.getMobile(),address.getAddressLine(),address.getCity(),address.getPincode());
  Order order=new Order(user,subtotal,delivery,snapshot);
  for(CartItem item:cart.getItems()){
   Product product=locked.get(item.getProduct().getId());product.setStockQuantity(product.getStockQuantity()-item.getQuantity());order.addItem(new OrderItem(product,item.getQuantity()));
  }
  Order saved=orders.save(order);cart.clear();
  return OrderResponse.from(saved);
 }

 @GetMapping @Transactional(readOnly=true) public PageResponse<OrderResponse> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,Authentication auth){
  if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page size or number");
  return PageResponse.from(orders.findAllByUser_Id(userId(auth),PageRequest.of(page,size,Sort.by("createdAt").descending())),OrderResponse::from);
 }
 @GetMapping("/{id}") @Transactional(readOnly=true) public OrderResponse get(@PathVariable Long id,Authentication auth){
  return orders.findByIdAndUser_Id(id,userId(auth)).map(OrderResponse::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));
 }
 private static Long userId(Authentication auth){try{return Long.valueOf(auth.getName());}catch(Exception e){throw unauthorized();}}
 private static ResponseStatusException unauthorized(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");}
}
