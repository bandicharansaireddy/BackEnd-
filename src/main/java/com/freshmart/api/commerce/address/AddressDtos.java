package com.freshmart.api.commerce.address;
import jakarta.validation.constraints.*;
import java.time.Instant;
public final class AddressDtos {
 private AddressDtos(){}
 public record CreateRequest(@NotBlank @Size(min=3,max=100) String fullName,@NotBlank @Pattern(regexp="[0-9]{10}") String mobile,@NotBlank @Size(min=8,max=300) String addressLine,@NotBlank @Size(max=100) String city,@NotBlank @Pattern(regexp="[0-9]{6}") String pincode){}
 public record AddressResponse(Long id,String fullName,String mobile,String addressLine,String city,String pincode,Instant createdAt){static AddressResponse from(Address a){return new AddressResponse(a.getId(),a.getFullName(),a.getMobile(),a.getAddressLine(),a.getCity(),a.getPincode(),a.getCreatedAt());}}
}
