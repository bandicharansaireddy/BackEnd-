package com.freshmart.api.commerce.order;
import com.freshmart.api.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="orders")
public class Order {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false,length=20) private String status="PLACED";
 @Column(name="payment_status",nullable=false,length=30) private String paymentStatus="DEMO_UNPAID";
 @Column(name="subtotal_inr",nullable=false,precision=10,scale=2) private BigDecimal subtotalInr;
 @Column(name="delivery_fee_inr",nullable=false,precision=10,scale=2) private BigDecimal deliveryFeeInr;
 @Column(name="total_inr",nullable=false,precision=10,scale=2) private BigDecimal totalInr;
 @Column(name="full_name",nullable=false,length=100) private String fullName;
 @Column(nullable=false,length=10) private String mobile;
 @Column(name="address_line",nullable=false,length=300) private String addressLine;
 @Column(nullable=false,length=100) private String city;
 @Column(nullable=false,length=6) private String pincode;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) private List<OrderItem> items=new ArrayList<>();
 protected Order(){}
 public Order(User user,BigDecimal subtotal,BigDecimal delivery,AddressSnapshot a){this.user=user;this.subtotalInr=subtotal;this.deliveryFeeInr=delivery;this.totalInr=subtotal.add(delivery);this.fullName=a.fullName();this.mobile=a.mobile();this.addressLine=a.addressLine();this.city=a.city();this.pincode=a.pincode();}
 @PrePersist void onCreate(){if(createdAt==null)createdAt=Instant.now();}
 public void addItem(OrderItem item){items.add(item);item.setOrder(this);}
 public Long getId(){return id;} public Long getUserId(){return user.getId();} public String getStatus(){return status;} public String getPaymentStatus(){return paymentStatus;} public BigDecimal getSubtotalInr(){return subtotalInr;} public BigDecimal getDeliveryFeeInr(){return deliveryFeeInr;} public BigDecimal getTotalInr(){return totalInr;} public String getFullName(){return fullName;} public String getMobile(){return mobile;} public String getAddressLine(){return addressLine;} public String getCity(){return city;} public String getPincode(){return pincode;} public Instant getCreatedAt(){return createdAt;} public List<OrderItem> getItems(){return items;}
 public record AddressSnapshot(String fullName,String mobile,String addressLine,String city,String pincode){}
}
