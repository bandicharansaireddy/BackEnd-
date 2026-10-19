package com.freshmart.api.catalog;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api")
public class CatalogController {
 private final ProductRepository products; private final CategoryRepository categories;
 public CatalogController(ProductRepository products,CategoryRepository categories){this.products=products;this.categories=categories;}
 @GetMapping("/products") public PageResponse<ProductResponse> products(@RequestParam(required=false) @Size(max=100) String q,@RequestParam(required=false) String category,@RequestParam(required=false) @DecimalMin("0.00") BigDecimal minPrice,@RequestParam(required=false) @DecimalMin("0.00") BigDecimal maxPrice,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="name") String sort){
  if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page size or number");
  if(minPrice!=null&&maxPrice!=null&&minPrice.compareTo(maxPrice)>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Minimum price must not exceed maximum price");
  Sort order=switch(sort){case "price-asc"->Sort.by("priceInr").ascending();case "price-desc"->Sort.by("priceInr").descending();case "newest"->Sort.by("id").descending();default->Sort.by("name").ascending();};
  return PageResponse.from(products.search(category,blankToNull(q),minPrice,maxPrice,PageRequest.of(page,size,order)),ProductResponse::from);
 }
 @GetMapping("/products/{id}") public ProductResponse product(@PathVariable Long id){return ProductResponse.from(products.findByIdAndActiveTrue(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found")));}
 @GetMapping("/categories") public List<CategoryResponse> categories(){return categories.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();}
 @GetMapping("/categories/{id}/products") public PageResponse<ProductResponse> categoryProducts(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size){
  Category c=categories.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Category not found"));
  if(page<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page number");
  return PageResponse.from(products.findByActiveTrueAndCategory_Slug(c.getSlug(),PageRequest.of(page,Math.min(Math.max(size,1),100))),ProductResponse::from);
 }
 private static String blankToNull(String v){return v==null||v.isBlank()?null:v.trim();}
}
