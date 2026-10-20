package com.freshmart.api.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
public class NoopOtpDelivery implements OtpDelivery {
    @Override public void deliver(String email, String otp) { /* Configure a mail provider before enabling production recovery. */ }
}
