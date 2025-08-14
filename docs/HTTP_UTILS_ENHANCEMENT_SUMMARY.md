# HttpUtils Enhancement Summary

## Overview

The HttpUtils class has been completely refactored to centralize and enforce timeouts, user-agent settings, and retry/backoff logic with exponential backoff and proper handling of HTTP 429 (Too Many Requests) and 5xx server errors.

## Key Enhancements

### 1. Centralized Configuration

- **HttpRequestConfig**: New builder-pattern configuration class for all HTTP requests
- **Default Settings**: Centralized timeout, user-agent, and retry configurations
- **Fluent API**: Method chaining for easy configuration setup

```java
HttpRequestConfig config = new HttpRequestConfig()
    .connectTimeout(30000)
    .readTimeout(60000)
    .maxRetries(5)
    .userAgent("Custom-Agent/1.0")
    .header("Authorization", "Bearer token123");
```

### 2. Exponential Backoff with Jitter

- **Base Delay**: 1 second initial retry delay
- **Multiplier**: 2.0x exponential backoff
- **Maximum Delay**: 60 seconds cap
- **Jitter**: 10% randomization to prevent thundering herd

```java
// Backoff calculation: 1s, 2s, 4s, 8s, 16s, 32s, 60s (max)
long delay = HttpUtils.calculateExponentialBackoff(attempt);
```

### 3. Enhanced Error Handling

#### HTTP 429 (Too Many Requests)
- Respects `Retry-After` header when present
- Falls back to exponential backoff if header missing
- Automatic retry with proper delay

#### HTTP 5xx (Server Errors)
- Treats all 5xx errors as retryable
- Uses exponential backoff for retry timing
- Comprehensive logging of server errors

#### HTTP 4xx (Client Errors)
- Generally non-retryable (except 429)
- Immediate failure with detailed error messages
- Proper exception handling

### 4. Rate Limiting Integration

- **Default Rate Limiter**: 10 requests per second
- **Custom Rate Limiters**: Per-request configuration
- **Disable Option**: Can be disabled for specific requests
- **Automatic Application**: Applied before each HTTP request

```java
// Set global rate limiter
HttpUtils.setDefaultRateLimiter(RateLimiter.perSecond(5));

// Use custom rate limiter for specific request
HttpRequestConfig config = new HttpRequestConfig()
    .rateLimiter(RateLimiter.perSecond(2));
```

### 5. Timeout Enforcement

- **Connect Timeout**: Default 30 seconds
- **Read Timeout**: Default 60 seconds
- **Per-Request Configuration**: Customizable timeouts
- **Large File Support**: Extended timeouts for downloads

### 6. User-Agent Centralization

- **Default User-Agent**: Uses Config.USER_AGENT
- **Custom User-Agent**: Per-request override capability
- **Consistent Application**: Applied to all HTTP requests

### 7. HTTP Response Wrapper

New `HttpResponse` class provides:
- Status code access
- Response body content
- HTTP headers
- Success/retry status helpers

```java
HttpResponse response = executeHttpRequest(url, "GET", null, config);
if (response.isSuccess()) {
    String body = response.getBody();
    int status = response.getStatusCode();
}
```

## Configuration Constants

```java
// Timeout Settings
DEFAULT_CONNECT_TIMEOUT_MS = 30000  // 30 seconds
DEFAULT_READ_TIMEOUT_MS = 60000     // 60 seconds

// Retry Settings
MAX_RETRIES = 5                     // Maximum retry attempts
BASE_RETRY_DELAY_MS = 1000         // 1 second base delay
BACKOFF_MULTIPLIER = 2.0           // Exponential multiplier
MAX_RETRY_DELAY_MS = 60000         // 60 second maximum delay
JITTER_FACTOR = 0.1                // 10% jitter

// Rate Limiting
DEFAULT_RATE_LIMIT = 10            // 10 requests per second
```

## Usage Examples

### Basic HTTPS Request
```java
// Simple request with defaults
String response = HttpUtils.getHTTPSResponse("https://api.example.com/data");

// Request with custom configuration
HttpRequestConfig config = new HttpRequestConfig()
    .connectTimeout(15000)
    .maxRetries(3)
    .userAgent("MyApp/1.0");
String response = HttpUtils.getHTTPSResponse("https://api.example.com/data", config);
```

### File Download
```java
// Basic download
File file = HttpUtils.downloadHTTPSFile("https://example.com/file.zip", "file.zip");

// Download with custom settings
HttpRequestConfig config = new HttpRequestConfig()
    .connectTimeout(45000)
    .readTimeout(120000)
    .maxRetries(3);
File file = HttpUtils.downloadHTTPSFile("https://example.com/file.zip", "file.zip", config);
```

### Advanced Configuration
```java
HttpRequestConfig config = new HttpRequestConfig()
    .connectTimeout(20000)
    .readTimeout(45000)
    .maxRetries(3)
    .userAgent("CustomBot/2.0")
    .rateLimiter(RateLimiter.perSecond(5))
    .header("Authorization", "Bearer " + token)
    .header("Accept", "application/json")
    .header("X-Custom-Header", "value");

String response = HttpUtils.getHTTPSResponse(url, config);
```

### Exponential Backoff Operation
```java
String result = HttpUtils.executeWithExponentialBackoff(
    () -> performRiskyOperation(),
    5,  // max retries
    "risky-operation"
);
```

## Backward Compatibility

All existing method signatures remain unchanged:
- `getHTTPSResponse(String url)`
- `downloadHTTPSFile(String url, String fileName)`
- `downloadLatestIAPDData(String outputPath)`
- `downloadIAPDDataByDate(int year, int month, String outputPath)`
- `extractGZFile(File gzFile, String extractToDir)`

New overloaded methods provide enhanced functionality:
- `getHTTPSResponse(String url, HttpRequestConfig config)`
- `downloadHTTPSFile(String url, String fileName, HttpRequestConfig config)`

## Integration with Existing Components

### RetryUtils Integration
- Uses existing `RetryUtils.executeWithRetry()` for compatibility
- Adds new `executeWithExponentialBackoff()` for enhanced retry logic
- Maintains existing transient exception detection

### RateLimiter Integration
- Seamless integration with existing `RateLimiter` class
- Automatic rate limiting application
- Configurable per-request rate limiting

### ProcessingLogger Integration
- Comprehensive logging of HTTP operations
- Retry attempt logging with backoff delays
- Error categorization (retryable vs non-retryable)
- Success/failure tracking

## Error Handling Improvements

### Retryable Exceptions
- HTTP 429 (Too Many Requests)
- HTTP 5xx (Server Errors: 500, 502, 503, 504)
- Network timeouts and connection issues
- Transient I/O exceptions

### Non-Retryable Exceptions
- HTTP 4xx (Client Errors: 400, 401, 403, 404, etc.)
- Authentication/authorization failures
- Invalid request format errors
- Non-transient application errors

### Retry Logic Flow
1. Attempt HTTP request
2. Check response status code
3. If retryable error:
   - Calculate exponential backoff delay
   - Apply jitter to prevent thundering herd
   - Sleep for calculated delay
   - Retry request (up to max retries)
4. If non-retryable error or max retries exceeded:
   - Log final error
   - Throw appropriate exception

## Testing

Comprehensive test suite (`HttpUtilsTest.java`) covers:
- Configuration builder pattern
- Exponential backoff calculation
- Rate limiter integration
- Timeout enforcement
- User-agent configuration
- Header management
- Error handling scenarios
- Method chaining validation

## Performance Considerations

### Rate Limiting
- Prevents overwhelming target servers
- Configurable limits per use case
- Minimal overhead with efficient token bucket implementation

### Connection Reuse
- Proper connection management
- Resource cleanup in finally blocks
- Connection pooling through HttpsURLConnection

### Memory Management
- Streaming file downloads
- Configurable buffer sizes
- Proper resource disposal

## Security Considerations

### SSL/TLS Configuration
- Maintains existing SSL trust-all configuration (development only)
- Proper certificate validation should be implemented for production
- Hostname verification controls

### Header Security
- User-Agent spoofing capabilities for legitimate testing
- Custom header support for authentication
- No sensitive data logging

## Future Enhancements

### Potential Improvements
1. **Circuit Breaker Pattern**: Fail-fast for consistently failing endpoints
2. **Metrics Collection**: Request timing and success rate tracking
3. **Connection Pooling**: Explicit connection pool management
4. **Async Support**: Non-blocking HTTP operations
5. **Response Caching**: Intelligent response caching for repeated requests
6. **Health Checks**: Endpoint availability monitoring

### Configuration Enhancements
1. **Profile-Based Configuration**: Different settings for dev/test/prod
2. **Dynamic Configuration**: Runtime configuration updates
3. **Endpoint-Specific Settings**: Per-URL configuration rules
4. **Fallback Strategies**: Alternative endpoints for failures

## Migration Guide

### For Existing Code
No changes required - all existing method calls continue to work with enhanced functionality.

### For New Code
Use the new configuration-based approach:

```java
// Old approach (still works)
String response = HttpUtils.getHTTPSResponse(url);

// New approach (recommended)
HttpRequestConfig config = new HttpRequestConfig()
    .connectTimeout(30000)
    .maxRetries(3);
String response = HttpUtils.getHTTPSResponse(url, config);
```

### Best Practices
1. **Use Configuration Objects**: Leverage HttpRequestConfig for better control
2. **Set Appropriate Timeouts**: Match timeouts to expected response times
3. **Configure Rate Limiting**: Respect target server capabilities
4. **Handle Exceptions Properly**: Distinguish between retryable and permanent failures
5. **Monitor and Log**: Use comprehensive logging for troubleshooting

## Conclusion

The enhanced HttpUtils class provides a robust, configurable, and maintainable foundation for all HTTP operations in the IAPD application. The centralized configuration, intelligent retry logic, and comprehensive error handling significantly improve reliability and user experience while maintaining full backward compatibility.
