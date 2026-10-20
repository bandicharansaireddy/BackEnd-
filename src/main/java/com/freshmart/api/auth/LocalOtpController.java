package com.freshmart.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;

@RestController
@Profile("local")
@RequestMapping("/api/dev/otp")
public class LocalOtpController {
    private final LocalOtpMailbox mailbox;
    public LocalOtpController(LocalOtpMailbox mailbox) { this.mailbox = mailbox; }

    @GetMapping
    public LocalOtpMailbox.Message latest(@RequestParam String email, HttpServletRequest request) {
        try {
            if (!InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress())
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        } catch (java.net.UnknownHostException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        LocalOtpMailbox.Message message = mailbox.take(email);
        if (message == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No unexpired local OTP");
        return message;
    }
}
