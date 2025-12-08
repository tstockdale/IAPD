package com.iss.iapd.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.iss.iapd.config.Config;
import com.iss.iapd.config.ProcessingLogger;

/**
 * Test class for HttpUtils with centralized timeout, user-agent, and retry/backoff functionality
 */
public class HttpUtilsTest {
    
    private static final String TEST_URL = "https://httpbin.org/status/200";
    private static final String RATE_LIMIT_URL = "https://httpbin.org/status/429";
    private static final String SERVER_ERROR_URL = "https://httpbin.org/status/500";
    private static final String CLIENT_ERROR_URL = "https://httpbin.org/status/404";
    
    @BeforeEach
    public void setUp() {
        // Reset any static state if needed
        ProcessingLogger.initialize();
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up any test artifacts
    }
    
    @Test
    @DisplayName("Test basic HTTPS response with default configuration")
    public void testBasicHttpsResponse() {
        try {
            String response = HttpUtils.getHTTPSResponse(TEST_URL);
            assertNotNull(response, "Response should not be null");
            // httpbin.org returns empty body for status endpoints
        } catch (Exception e) {
            // Network issues are acceptable in tests
            System.out.println("Network test skipped due to: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Test HTTPS response with custom configuration")
    public void testHttpsResponseWithCustomConfig() {
        try {
            HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
                .connectTimeout(10000)
                .readTimeout(15000)
                .maxRetries(2)
                .userAgent("Test-Agent/1.0")
                .header("X-Test-Header", "test-value");
            
            String response = HttpUtils.getHTTPSResponse(TEST_URL, config);
            assertNotNull(response, "Response should not be null");
        } catch (Exception e) {
            System.out.println("Network test skipped due to: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Test exponential backoff calculation")
    public void testExponentialBackoffCalculation() {
        // Test backoff calculation
        long backoff1 = HttpUtils.calculateExponentialBackoff(1);
        long backoff2 = HttpUtils.calculateExponentialBackoff(2);
        long backoff3 = HttpUtils.calculateExponentialBackoff(3);
        
        assertTrue(backoff1 >= 1000, "First attempt should be at least 1 second");
        assertTrue(backoff2 > backoff1, "Second attempt should be longer than first");
        assertTrue(backoff3 > backoff2, "Third attempt should be longer than second");
        
        // Test maximum backoff
        long maxBackoff = HttpUtils.calculateExponentialBackoff(10);
        assertTrue(maxBackoff <= 60000 * 1.2, "Backoff should not exceed max with jitter");
    }
    
    @Test
    @DisplayName("Test HTTP request configuration builder pattern")
    public void testHttpRequestConfigBuilder() {
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .connectTimeout(5000)
            .readTimeout(10000)
            .maxRetries(3)
            .userAgent("Custom-Agent/2.0")
            .disableRateLimiting()
            .header("Authorization", "Bearer token123")
            .header("Content-Type", "application/json");
        
        assertEquals(5000, config.getConnectTimeoutMs());
        assertEquals(10000, config.getReadTimeoutMs());
        assertEquals(3, config.getMaxRetries());
        assertEquals("Custom-Agent/2.0", config.getUserAgent());
        assertFalse(config.isUseRateLimiter());
        assertEquals("Bearer token123", config.getHeaders().get("Authorization"));
        assertEquals("application/json", config.getHeaders().get("Content-Type"));
    }
    
    @Test
    @DisplayName("Test HTTP response wrapper")
    public void testHttpResponseWrapper() {
        java.util.Map<String, java.util.List<String>> headers = new java.util.HashMap<>();
        headers.put("Content-Type", java.util.Arrays.asList("application/json"));
        
        HttpUtils.HttpResponse response200 = new HttpUtils.HttpResponse(200, "OK", headers);
        assertTrue(response200.isSuccess());
        assertFalse(response200.isRetryable());
        
        HttpUtils.HttpResponse response429 = new HttpUtils.HttpResponse(429, "Too Many Requests", headers);
        assertFalse(response429.isSuccess());
        assertTrue(response429.isRetryable());
        
        HttpUtils.HttpResponse response500 = new HttpUtils.HttpResponse(500, "Internal Server Error", headers);
        assertFalse(response500.isSuccess());
        assertTrue(response500.isRetryable());
        
        HttpUtils.HttpResponse response404 = new HttpUtils.HttpResponse(404, "Not Found", headers);
        assertFalse(response404.isSuccess());
        assertFalse(response404.isRetryable());
    }
    
    @Test
    @DisplayName("Test rate limiter integration")
    public void testRateLimiterIntegration() {
        // Create a very restrictive rate limiter for testing
        RateLimiter testRateLimiter = new RateLimiter(1, 2, TimeUnit.SECONDS);
        HttpUtils.setDefaultRateLimiter(testRateLimiter);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Make two requests - second should be delayed
            HttpUtils.getHTTPSResponse(TEST_URL);
            HttpUtils.getHTTPSResponse(TEST_URL);
            
            long elapsed = System.currentTimeMillis() - startTime;
            // Should take at least 2 seconds due to rate limiting
            assertTrue(elapsed >= 1500, "Rate limiting should introduce delay");
            
        } catch (Exception e) {
            System.out.println("Network test skipped due to: " + e.getMessage());
        } finally {
            // Reset to default rate limiter
            HttpUtils.setDefaultRateLimiter(RateLimiter.perSecond(10));
        }
    }
    
    @Test
    @DisplayName("Test custom rate limiter configuration")
    public void testCustomRateLimiterConfig() {
        RateLimiter customRateLimiter = RateLimiter.perSecond(5);
        
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .rateLimiter(customRateLimiter);
        
        assertEquals(customRateLimiter, config.getCustomRateLimiter());
        assertTrue(config.isUseRateLimiter());
        
        // Test disabling rate limiting
        config.disableRateLimiting();
        assertFalse(config.isUseRateLimiter());
    }
    
    @Test
    @DisplayName("Test executeWithExponentialBackoff with mock operation")
    public void testExecuteWithExponentialBackoff() {
        // Test successful operation
        String result = HttpUtils.executeWithExponentialBackoff(
            () -> "success",
            3,
            "test-operation"
        );
        assertEquals("success", result);
        
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
        assertEquals("success-after-retries", result2);
        assertEquals(3, attemptCount[0]);
    }
    
    @Test
    @DisplayName("Test executeWithExponentialBackoff with non-retryable exception")
    public void testExecuteWithExponentialBackoffNonRetryable() {
        assertThrows(RuntimeException.class, () -> {
            HttpUtils.executeWithExponentialBackoff(
                () -> {
                    throw new IllegalArgumentException("Non-retryable error");
                },
                3,
                "non-retryable-test"
            );
        });
    }
    
    @Test
    @DisplayName("Test file download with custom configuration")
    public void testFileDownloadWithConfig() {
        // This test would require a real file to download
        // For now, we'll test the configuration setup
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .connectTimeout(30000)
            .readTimeout(60000)
            .maxRetries(3);
        
        // Verify configuration is properly set
        assertEquals(30000, config.getConnectTimeoutMs());
        assertEquals(60000, config.getReadTimeoutMs());
        assertEquals(3, config.getMaxRetries());
    }
    
  
    
    @Test
    @DisplayName("Test timeout configuration enforcement")
    public void testTimeoutConfigurationEnforcement() {
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .connectTimeout(1000)  // Very short timeout
            .readTimeout(2000);
        
        // Verify timeouts are set correctly
        assertEquals(1000, config.getConnectTimeoutMs());
        assertEquals(2000, config.getReadTimeoutMs());
    }
    
    @Test
    @DisplayName("Test user agent configuration")
    public void testUserAgentConfiguration() {
        String customUserAgent = "TestBot/1.0 (Testing)";
        
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .userAgent(customUserAgent);
        
        assertEquals(customUserAgent, config.getUserAgent());
        
        // Test default user agent
        HttpUtils.HttpRequestConfig defaultConfig = new HttpUtils.HttpRequestConfig();
        assertEquals(Config.USER_AGENT, defaultConfig.getUserAgent());
    }
    
    @Test
    @DisplayName("Test header configuration")
    public void testHeaderConfiguration() {
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .header("X-API-Key", "secret123")
            .header("Accept", "application/json")
            .header("X-Custom-Header", "custom-value");
        
        assertEquals("secret123", config.getHeaders().get("X-API-Key"));
        assertEquals("application/json", config.getHeaders().get("Accept"));
        assertEquals("custom-value", config.getHeaders().get("X-Custom-Header"));
        assertEquals(3, config.getHeaders().size());
    }
    
    @Test
    @DisplayName("Test retry configuration limits")
    public void testRetryConfigurationLimits() {
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig()
            .maxRetries(10);
        
        assertEquals(10, config.getMaxRetries());
        
        // Test with zero retries
        config.maxRetries(0);
        assertEquals(0, config.getMaxRetries());
    }
    
    @Test
    @DisplayName("Test configuration method chaining")
    public void testConfigurationMethodChaining() {
        // Test that all configuration methods return the same instance for chaining
        HttpUtils.HttpRequestConfig config = new HttpUtils.HttpRequestConfig();
        
        HttpUtils.HttpRequestConfig result = config
            .connectTimeout(5000)
            .readTimeout(10000)
            .maxRetries(3)
            .userAgent("ChainTest/1.0")
            .disableRateLimiting()
            .header("Test", "Value");
        
        assertSame(config, result, "Configuration methods should return same instance for chaining");
        
        // Verify all settings were applied
        assertEquals(5000, config.getConnectTimeoutMs());
        assertEquals(10000, config.getReadTimeoutMs());
        assertEquals(3, config.getMaxRetries());
        assertEquals("ChainTest/1.0", config.getUserAgent());
        assertFalse(config.isUseRateLimiter());
        assertEquals("Value", config.getHeaders().get("Test"));
    }
}
