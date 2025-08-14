package com.iss.iapd.exceptions;    

/**
 * Custom exception for file download errors
 */
public class FileDownloadException extends Exception {
    
    public FileDownloadException(String message) {
        super(message);
    }
    
    public FileDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
