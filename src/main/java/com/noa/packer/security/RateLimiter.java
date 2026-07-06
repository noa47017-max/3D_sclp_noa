package com.noa.packer.security;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private final int maxRequests = 100;          // מקסימום בקשות בדקה
    private final long windowMillis = 60_000;     // דקה אחת
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();

    public boolean isAllowed(String clientIp) {
        long now = System.currentTimeMillis();

        ClientInfo info = clients.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart > windowMillis) {
                return new ClientInfo(now, new AtomicInteger(1));
            }
            existing.requestCount.incrementAndGet();
            return existing;
        });

        return info.requestCount.get() <= maxRequests;
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        clients.entrySet().removeIf(
                entry -> now - entry.getValue().windowStart > windowMillis * 2
        );
    }

    private static class ClientInfo {
        final long windowStart;
        final AtomicInteger requestCount;

        ClientInfo(long windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}