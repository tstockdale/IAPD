package com.iss.iapd.integration;

import com.iss.iapd.utils.HttpUtils;
import com.iss.iapd.utils.RateLimiter;

/**
 * Simple test runner for HttpUtils functionality
 */
public class HttpUtilsTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== HttpUtils Enhancement Test Runner ===");
        
        try {
            // Test 1: Configuration Builder Pattern
            testConfigurationBuilder();
            
            // Test 2: Exponential Backoff Calculation
            testExponentialBackoff();
            
            // Test 3: HTTP Response Wrapper
            testHttpResponseWrapper();
            
            // Test 4: Rate Limiter Integration
            testRateLimiterIntegration();
            
            // Test 5: Retry Logic with Mock Operation
            testRetryLogic();
            
            System.out.println("\n=== All Tests Passed Successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testConfigurationBuilder() {
        System.out.println("\n1. Testing Configuration Builder Pattern...");
        
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .connectTimeout(5000)
            .readTimeout(10000)
            .maxRetries(3)
            .userAgent("Test-Agent/1.0")
            .disableRateLimiting()
            .header("Authorization", "Bearer token123")
            .header("Content-Type", "application/json");
        
        assert config.getConnectTimeoutMs() == 5000 : "Connect timeout not set correctly";
        assert config.getReadTimeoutMs() == 10000 : "Read timeout not set correctly";
        assert config.getMaxRetries() == 3 : "Max retries not set correctly";
        assert "Test-Agent/1.0".equals(config.getUserAgent()) : "User agent not set correctly";
        assert !config.isUseRateLimiter() : "Rate limiter should be disabled";
        assert "Bearer token123".equals(config.getHeaders().get("Authorization")) : "Authorization header not set";
        assert "application/json".equals(config.getHeaders().get("Content-Type")) : "Content-Type header not set";
        
        System.out.println("   ✓ Configuration builder pattern works correctly");
    }
    
    private static void testExponentialBackoff() {
        System.out.println("\n2. Testing Exponential Backoff Calculation...");
        
        long backoff1 = HttpUtils.calculateExponentialBackoff(1);
        long backoff2 = HttpUtils.calculateExponentialBackoff(2);
        long backoff3 = HttpUtils.calculateExponentialBackoff(3);
        
        assert backoff1 >= 1000 : "First attempt should be at least 1 second";
        assert backoff2 > backoff1 : "Second attempt should be longer than first";
        assert backoff3 > backoff2 : "Third attempt should be longer than second";
        
        // Test maximum backoff
        long maxBackoff = HttpUtils.calculateExponentialBackoff(10);
        assert maxBackoff <= 60000 * 1.2 : "Backoff should not exceed max with jitter";
        
        System.out.println("   ✓ Exponential backoff calculation works correctly");
        System.out.println("     - Attempt 1: " + backoff1 + "ms");
        System.out.println("     - Attempt 2: " + backoff2 + "ms");
        System.out.println("     - Attempt 3: " + backoff3 + "ms");
        System.out.println("     - Attempt 10: " + maxBackoff + "ms");
    }
    
    private static void testHttpResponseWrapper() {
        System.out.println("\n3. Testing HTTP Response Wrapper...");
        
        java.util.Map<String, java.util.List<String>> headers = new java.util.HashMap<>();
        headers.put("Content-Type", java.util.Arrays.asList("application/json"));
        
        HttpUtils.HttpResponse response200 = new HttpUtils.HttpResponse(200, "OK", headers);
        assert response200.isSuccess() : "200 response should be success";
        assert !response200.isRetryable() : "200 response should not be retryable";
        
        HttpUtils.HttpResponse response429 = new HttpUtils.HttpResponse(429, "Too Many Requests", headers);
        assert !response429.isSuccess() : "429 response should not be success";
        assert response429.isRetryable() : "429 response should be retryable";
        
        HttpUtils.HttpResponse response500 = new HttpUtils.HttpResponse(500, "Internal Server Error", headers);
        assert !response500.isSuccess() : "500 response should not be success";
        assert response500.isRetryable() : "500 response should be retryable";
        
        HttpUtils.HttpResponse response404 = new HttpUtils.HttpResponse(404, "Not Found", headers);
        assert !response404.isSuccess() : "404 response should not be success";
        assert !response404.isRetryable() : "404 response should not be retryable";
        
        System.out.println("   ✓ HTTP response wrapper works correctly");
    }
    
    private static void testRateLimiterIntegration() {
        System.out.println("\n4. Testing Rate Limiter Integration...");
        
        // Create a test rate limiter
        RateLimiter testRateLimiter = RateLimiter.perSecond(5);
        HttpUtils.setDefaultRateLimiter(testRateLimiter);
        
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .rateLimiter(RateLimiter.perSecond(2));
        
        assert config.getCustomRateLimiter() != null : "Custom rate limiter should be set";
        assert config.isUseRateLimiter() : "Rate limiter should be enabled by default";
        
        // Test disabling rate limiting
        config.disableRateLimiting();
        assert !config.isUseRateLimiter() : "Rate limiter should be disabled";
        
        // Reset to default
        HttpUtils.setDefaultRateLimiter(RateLimiter.perSecond(10));
        
        System.out.println("   ✓ Rate limiter integration works correctly");
    }
    
    private static void testRetryLogic() {
        System.out.println("\n5. Testing Retry Logic...");
        
        // Test successful operation
        String result = HttpUtils.executeWithExponentialBackoff(
            () -> "success",
            3,
            "test-operation"
        );
        assert "success".equals(result) : "Successful operation should return correct result";
        
        // Test operation that fails then succeeds
        final int[] attemptCount = {0};
        String result2 = HttpUtils.executeWithExponentialBackoff(
            () -> {
                attemptCount[0]++;
                if (attemptCount[0] < 3) {
                    throw new RuntimeException("Transient failure");
                }
                return "success-after-retries";
            },
            5,
            "retry-test-operation"
        );
        assert "success-after-retries".equals(result2) : "Retry operation should succeed";
        assert attemptCount[0] == 3 : "Should have made exactly 3 attempts";
        
        System.out.println("   ✓ Retry logic works correctly");
        System.out.println("     - Operation succeeded after " + attemptCount[0] + " attempts");
    }
}
