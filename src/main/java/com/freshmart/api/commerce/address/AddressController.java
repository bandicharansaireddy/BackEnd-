package com.freshmart.api.commerce.address;

import com.freshmart.api.commerce.address.AddressDtos.*;
import com.freshmart.api.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController @RequestMapping("/api/addresses")
public class AddressController {
 private final AddressRepository addresses; private final UserRepository users;
 public AddressController(AddressRepository addresses,UserRepository users){this.addresses=addresses;this.users=users;}
 @GetMapping @Transactional(readOnly=true) public List<AddressResponse> list(Authentication auth){Long uid=userId(auth);return addresses.findAllByUser_IdOrderByIdDesc(uid).stream().map(AddressResponse::from).toList();}
 @PostMapping @Transactional public AddressResponse create(@Valid @RequestBody CreateRequest request,Authentication auth){Long uid=userId(auth);var user=users.findByIdAndActiveTrue(uid).orElseThrow(AddressController::unauthorized);Address address=addresses.save(new Address(user,request.fullName().trim(),request.mobile(),request.addressLine().trim(),request.city().trim(),request.pincode()));return AddressResponse.from(address);}
 private static Long userId(Authentication auth){try{return Long.valueOf(auth.getName());}catch(Exception e){throw unauthorized();}}
 private static ResponseStatusException unauthorized(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");}
}
