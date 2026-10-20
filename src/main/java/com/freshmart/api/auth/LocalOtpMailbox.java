package com.freshmart.api.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory delivery used only with the local Spring profile; it never writes OTPs to logs. */
@Service
@Profile("local")
public class LocalOtpMailbox implements OtpDelivery {
    public record Message(String otp, Instant expiresAt) { }
    private final ConcurrentHashMap<String, Message> messages = new ConcurrentHashMap<>();

    @Override public void deliver(String email, String otp) { messages.put(email.toLowerCase(), new Message(otp, Instant.now().plusSeconds(300))); }
    public Message take(String email) {
        Message value = messages.remove(email.toLowerCase());
        return value != null && value.expiresAt().isAfter(Instant.now()) ? value : null;
    }
}
