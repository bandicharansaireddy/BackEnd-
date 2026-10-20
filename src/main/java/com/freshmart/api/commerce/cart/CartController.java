package com.freshmart.api.commerce.cart;

import com.freshmart.api.catalog.Product;
import com.freshmart.api.catalog.ProductRepository;
import com.freshmart.api.user.UserRepository;
import com.freshmart.api.commerce.cart.CartDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController @RequestMapping("/api/cart")
public class CartController {
 private final CartRepository carts; private final UserRepository users; private final ProductRepository products;
 public CartController(CartRepository carts,UserRepository users,ProductRepository products){this.carts=carts;this.users=users;this.products=products;}

 @GetMapping @Transactional(readOnly=true) public CartResponse get(Authentication auth){
  Cart cart=carts.findWithItemsByUserId(userId(auth)).orElse(null);return cart==null?CartResponse.empty():response(cart);
 }

 @PostMapping("/items") @Transactional public CartResponse add(@Valid @RequestBody AddItemRequest request,Authentication auth){
  Long uid=userId(auth);Cart cart=carts.findWithItemsByUserId(uid).orElseGet(()->carts.save(new Cart(users.findByIdAndActiveTrue(uid).orElseThrow(CartController::unauthorized))));
  Product product=products.findByIdAndActiveTrue(request.productId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));
  CartItem existing=cart.getItems().stream().filter(i->i.getProduct().getId().equals(product.getId())).findFirst().orElse(null);
  int target=request.quantity()+(existing==null?0:existing.getQuantity());
  if(target>product.getStockQuantity())throw new ResponseStatusException(HttpStatus.CONFLICT,"Requested quantity exceeds available stock");
  if(existing==null)cart.add(new CartItem(product,target));else existing.setQuantity(target);
  return response(cart);
 }

 @PutMapping("/items/{id}") @Transactional public CartResponse change(@PathVariable Long id,@Valid @RequestBody QuantityRequest request,Authentication auth){
  Cart cart=carts.findWithItemsByUserId(userId(auth)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cart item not found"));
  CartItem item=cart.getItems().stream().filter(i->i.getId().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cart item not found"));
  if(request.quantity()>item.getProduct().getStockQuantity())throw new ResponseStatusException(HttpStatus.CONFLICT,"Requested quantity exceeds available stock");
  item.setQuantity(request.quantity());return response(cart);
 }

 @DeleteMapping("/items/{id}") @Transactional public CartResponse remove(@PathVariable Long id,Authentication auth){
  Cart cart=carts.findWithItemsByUserId(userId(auth)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cart item not found"));
  CartItem item=cart.getItems().stream().filter(i->i.getId().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cart item not found"));
  cart.remove(item);return response(cart);
 }

 private static CartResponse response(Cart cart){
  List<CartLine> lines=cart.getItems().stream().map(i->{Product p=i.getProduct();BigDecimal line=p.getPriceInr().multiply(BigDecimal.valueOf(i.getQuantity()));return new CartLine(i.getId(),p.getId(),p.getName(),p.getDescription(),p.getImageUrl(),p.getUnit(),p.getPriceInr(),p.getStockQuantity(),i.getQuantity(),line);}).toList();
  BigDecimal subtotal=lines.stream().map(CartLine::lineTotalInr).reduce(BigDecimal.ZERO,BigDecimal::add);return new CartResponse(lines,subtotal);
 }
 private static Long userId(Authentication auth){try{return Long.valueOf(auth.getName());}catch(Exception e){throw unauthorized();}}
 private static ResponseStatusException unauthorized(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");}
}
