package com.freshmart.api.commerce.address;

import com.freshmart.api.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="addresses")
public class Address {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(name="full_name",nullable=false,length=100) private String fullName;
 @Column(nullable=false,length=10) private String mobile;
 @Column(name="address_line",nullable=false,length=300) private String addressLine;
 @Column(nullable=false,length=100) private String city;
 @Column(nullable=false,length=6) private String pincode;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected Address(){}
 public Address(User user,String fullName,String mobile,String addressLine,String city,String pincode){this.user=user;this.fullName=fullName;this.mobile=mobile;this.addressLine=addressLine;this.city=city;this.pincode=pincode;}
 @PrePersist void onCreate(){if(createdAt==null)createdAt=Instant.now();}
 public Long getId(){return id;} public User getUser(){return user;} public String getFullName(){return fullName;} public String getMobile(){return mobile;} public String getAddressLine(){return addressLine;} public String getCity(){return city;} public String getPincode(){return pincode;} public Instant getCreatedAt(){return createdAt;}
}
