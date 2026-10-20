package com.freshmart.api.auth;

public interface OtpDelivery {
    void deliver(String email, String otp);
}
