package com.freshmart.api.admin;

import com.freshmart.api.catalog.*;
import com.freshmart.api.commerce.order.*;
import com.freshmart.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
 private final ProductRepository products; private final CategoryRepository categories; private final OrderRepository orders; private final UserRepository users;
 public AdminController(ProductRepository products,CategoryRepository categories,OrderRepository orders,UserRepository users){this.products=products;this.categories=categories;this.orders=orders;this.users=users;}
 public record ProductInput(@NotNull Long categoryId,@NotBlank @Size(max=120) String name,@NotBlank @Size(max=1000) String description,@NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal priceInr,@NotBlank @Size(max=40) String unit,@Min(0) @Max(100000) int stockQuantity,@Size(max=500) String imageUrl,Boolean active){}
 public record CategoryInput(@NotBlank @Size(max=80) String name,@NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=90) String slug){}
 public record StatusInput(@NotBlank String status){}
 public record UserSummary(Long id,String fullName,String email,String mobile,String role,Instant createdAt){}
 public record Dashboard(long products,long users,long orders,long pendingOrders,BigDecimal demoOrderTotalInr){}
 @GetMapping("/dashboard") @Transactional(readOnly=true) public Dashboard dashboard(){BigDecimal total=orders.sumTotals();return new Dashboard(products.count(),users.countByActiveTrue(),orders.count(),orders.countByStatus("PLACED"),total==null?BigDecimal.ZERO:total);}
 @PostMapping("/products") @Transactional public ProductResponse createProduct(@Valid @RequestBody ProductInput in){return ProductResponse.from(products.save(new Product(category(in.categoryId()),in.name().trim(),in.description().trim(),in.priceInr(),in.unit().trim(),in.stockQuantity(),in.imageUrl())));}
 @PutMapping("/products/{id}") @Transactional public ProductResponse updateProduct(@PathVariable Long id,@Valid @RequestBody ProductInput in){Product p=products.findById(id).orElseThrow(()->missing("Product not found"));p.update(category(in.categoryId()),in.name().trim(),in.description().trim(),in.priceInr(),in.unit().trim(),in.stockQuantity(),in.imageUrl(),in.active()==null||in.active());return ProductResponse.from(p);}
 @DeleteMapping("/products/{id}") @Transactional public void deactivateProduct(@PathVariable Long id){Product p=products.findById(id).orElseThrow(()->missing("Product not found"));p.update(category(p.getCategoryId()),p.getName(),p.getDescription(),p.getPriceInr(),p.getUnit(),p.getStockQuantity(),p.getImageUrl(),false);}
 @PostMapping("/categories") @Transactional public CategoryResponse createCategory(@Valid @RequestBody CategoryInput in){return CategoryResponse.from(categories.save(new Category(in.name().trim(),in.slug())));}
 @GetMapping("/orders") @Transactional(readOnly=true) public PageResponse<OrderDtos.OrderResponse> listOrders(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){if(page<0||size<1||size>100)throw badPage();return PageResponse.from(orders.findAll(PageRequest.of(page,size,Sort.by("createdAt").descending())),OrderDtos.OrderResponse::from);}
 @PatchMapping("/orders/{id}/status") @Transactional public OrderDtos.OrderResponse updateOrder(@PathVariable Long id,@Valid @RequestBody StatusInput in){Order order=orders.findByIdForUpdateWithItems(id).orElseThrow(()->missing("Order not found"));String current=order.getStatus(),next=in.status();boolean allowed=switch(current){case "PLACED"->Set.of("PROCESSING","CANCELLED").contains(next);case "PROCESSING"->Set.of("READY","CANCELLED").contains(next);case "READY"->Set.of("DELIVERED").contains(next);default->false;};if(!allowed)throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid order status transition");if(next.equals("CANCELLED")){for(var item:order.getItems().stream().sorted(java.util.Comparator.comparing(OrderItem::getProductId)).toList()){var product=products.findByIdForUpdate(item.getProductId()).orElseThrow(()->missing("Product not found"));product.setStockQuantity(product.getStockQuantity()+item.getQuantity());}}order.setStatus(next);return OrderDtos.OrderResponse.from(order);}
 @GetMapping("/users") @Transactional(readOnly=true) public PageResponse<UserSummary> listUsers(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){if(page<0||size<1||size>100)throw badPage();return PageResponse.from(users.findAll(PageRequest.of(page,size,Sort.by("createdAt").descending())),u->new UserSummary(u.getId(),u.getFullName(),u.getEmail(),u.getMobile(),u.getRole(),u.getCreatedAt()));}
 private Category category(Long id){return categories.findById(id).orElseThrow(()->missing("Category not found"));}
 private static ResponseStatusException missing(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
 private static ResponseStatusException badPage(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page size or number");}
}
