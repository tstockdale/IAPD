package com.iss.iapd.exceptions;

/**
 * Custom exception for XML processing errors
 */
public class XMLProcessingException extends Exception {
    
    public XMLProcessingException(String message) {
        super(message);
    }
    
    public XMLProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
