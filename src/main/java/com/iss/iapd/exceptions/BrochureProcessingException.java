package com.iss.iapd.exceptions;    

/**
 * Custom exception for brochure processing errors
 */
public class BrochureProcessingException extends Exception {
    
    public BrochureProcessingException(String message) {
        super(message);
    }
    
    public BrochureProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
