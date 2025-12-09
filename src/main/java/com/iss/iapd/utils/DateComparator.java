package com.iss.iapd.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.iss.iapd.config.ProcessingLogger;

/**
 * Utility class for comparing dates in IAPD processing
 * Handles date parsing and comparison operations for filing dates
 */
public class DateComparator {

    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);

    // Configure formatter for strict parsing
    static {
        dateFormatter.setLenient(false); // Reject invalid dates like 02/29/2023
    }

    /**
     * Parses a filing date string in MM/dd/yyyy format
     * @param dateString the date string to parse
     * @return parsed Date object, or null if parsing fails
     */
    public static Date parseFilingDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        String trimmed = dateString.trim();

        // Check if the format matches MM/dd/yyyy pattern
        if (!trimmed.matches("\\d{2}/\\d{2}/\\d{4}")) {
            ProcessingLogger.logWarning("Invalid date format: " + dateString + " (expected MM/dd/yyyy)");
            return null;
        }

        try {
            return dateFormatter.parse(trimmed);
        } catch (ParseException e) {
            ProcessingLogger.logWarning("Failed to parse filing date: " + dateString + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Determines if the current filing date is more recent than the historical filing date
     * @param currentDate current filing date string
     * @param historicalDate historical filing date string
     * @return true if current date is more recent, false otherwise
     */
    public static boolean isFilingDateMoreRecent(String currentDate, String historicalDate) {
        // If no historical date, treat as new firm (process it)
        if (historicalDate == null || historicalDate.trim().isEmpty()) {
            return true;
        }

        // If no current date, don't process (conservative approach)
        if (currentDate == null || currentDate.trim().isEmpty()) {
            return false;
        }

        Date current = parseFilingDate(currentDate);
        Date historical = parseFilingDate(historicalDate);

        // If either date is unparseable, process it (conservative approach)
        if (current == null || historical == null) {
            ProcessingLogger.logWarning("Date parsing failed - processing firm conservatively. Current: " +
                    currentDate + ", Historical: " + historicalDate);
            return true;
        }

        // Return true if current date is after historical date
        return current.after(historical);
    }

    /**
     * Checks if a dateSubmitted is more recent than the maximum found in existing data
     * @param dateSubmitted the date to check
     * @param maxDateSubmitted the maximum date from existing data
     * @return true if dateSubmitted is more recent, false otherwise
     */
    public static boolean isDateMoreRecent(String dateSubmitted, String maxDateSubmitted) {
        if (maxDateSubmitted == null || maxDateSubmitted.trim().isEmpty()) {
            return true; // No existing data, so any date is "more recent"
        }

        if (dateSubmitted == null || dateSubmitted.trim().isEmpty()) {
            return false; // No date to compare
        }

        Date currentDate = parseFilingDate(dateSubmitted);
        Date maxDate = parseFilingDate(maxDateSubmitted);

        if (currentDate == null || maxDate == null) {
            ProcessingLogger.logWarning("Date parsing failed - including conservatively. Current: " +
                    dateSubmitted + ", Max: " + maxDateSubmitted);
            return true; // Conservative approach - include if we can't parse
        }

        return currentDate.after(maxDate);
    }

    /**
     * Validates a date string format without parsing
     * @param dateString the date string to validate
     * @return true if the format is valid, false otherwise
     */
    public static boolean isValidDateFormat(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return false;
        }

        String trimmed = dateString.trim();
        return trimmed.matches("\\d{2}/\\d{2}/\\d{4}");
    }

    /**
     * Gets the date format pattern used by this comparator
     * @return the date format pattern string
     */
    public static String getDateFormat() {
        return DATE_FORMAT;
    }

    // Private constructor to prevent instantiation
    private DateComparator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
