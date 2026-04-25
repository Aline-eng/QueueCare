package com.queuecare.queuecare.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenService {
    public static final Map<String, Long> tokenStore = new HashMap<>();

    public static String generateToken(Long userId) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, userId);
        return token;
    }
    public static Long validateToken(String token) {
        return tokenStore.get(token);
    }
}
