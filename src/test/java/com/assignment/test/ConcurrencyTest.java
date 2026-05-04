package com.assignment.test;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTest {

    public static void main(String[] args) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://localhost:8080";

        System.out.println("=== Concurrency Test: 200 Bots Commenting ===");

        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger throttleCount = new AtomicInteger(0);

        Long postId = 1L;

        for (int i = 1; i <= 200; i++) {
            final int botIndex = i;
            executor.submit(() -> {
                try {
                    Map<String, Object> request = new HashMap<>();
                    request.put("authorId", (botIndex % 50) + 101);
                    request.put("content", "Bot comment " + botIndex);
                    request.put("depthLevel", 0);

                    try {
                        restTemplate.postForObject(
                            baseUrl + "/api/posts/" + postId + "/comments",
                            request,
                            Map.class
                        );
                        successCount.incrementAndGet();
                    } catch (HttpClientErrorException e) {
                        if (e.getStatusCode().value() == 429) {
                            throttleCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        System.out.println("\n=== Results ===");
        System.out.println("Successful Comments: " + successCount.get());
        System.out.println("Throttled (429): " + throttleCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Total: " + (successCount.get() + throttleCount.get() + failureCount.get()));
        System.out.println("\nExpected: Exactly 100 successful, rest throttled");
    }
}
