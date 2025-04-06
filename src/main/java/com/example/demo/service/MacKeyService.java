package com.example.demo.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class MacKeyService {

    private static final int KEY_LENGTH_BYTES = 32;

    public String generateMacKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[KEY_LENGTH_BYTES];
        random.nextBytes(keyBytes);
        
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
