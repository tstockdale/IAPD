package com.iss.iapd.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for DateComparator utility
 */
public class DateComparatorTest {

    @Test
    @DisplayName("Parse valid date in MM/dd/yyyy format")
    public void testParseValidDate() {
        Date date = DateComparator.parseFilingDate("03/15/2024");
        assertNotNull(date, "Valid date should parse successfully");
    }

    @Test
    @DisplayName("Parse invalid date format returns null")
    public void testParseInvalidFormat() {
        assertNull(DateComparator.parseFilingDate("2024-03-15"));
        assertNull(DateComparator.parseFilingDate("15/03/2024"));
        assertNull(DateComparator.parseFilingDate("03-15-2024"));
    }

    @Test
    @DisplayName("Parse null or empty date returns null")
    public void testParseNullOrEmptyDate() {
        assertNull(DateComparator.parseFilingDate(null));
        assertNull(DateComparator.parseFilingDate(""));
        assertNull(DateComparator.parseFilingDate("   "));
    }

    @Test
    @DisplayName("Parse invalid date values returns null")
    public void testParseInvalidDateValues() {
        assertNull(DateComparator.parseFilingDate("13/01/2024")); // Invalid month
        assertNull(DateComparator.parseFilingDate("02/29/2023")); // Not a leap year
        assertNull(DateComparator.parseFilingDate("04/31/2024")); // April has 30 days
    }

    @Test
    @DisplayName("Parse date with extra whitespace")
    public void testParseDateWithWhitespace() {
        Date date = DateComparator.parseFilingDate("  03/15/2024  ");
        assertNotNull(date, "Date with extra whitespace should parse after trimming");
    }

    @Test
    @DisplayName("More recent date comparison returns true")
    public void testIsFilingDateMoreRecent() {
        assertTrue(DateComparator.isFilingDateMoreRecent("03/16/2024", "03/15/2024"));
        assertTrue(DateComparator.isFilingDateMoreRecent("04/15/2024", "03/15/2024"));
        assertTrue(DateComparator.isFilingDateMoreRecent("03/15/2025", "03/15/2024"));
    }

    @Test
    @DisplayName("Older date comparison returns false")
    public void testIsFilingDateOlder() {
        assertFalse(DateComparator.isFilingDateMoreRecent("03/14/2024", "03/15/2024"));
        assertFalse(DateComparator.isFilingDateMoreRecent("02/15/2024", "03/15/2024"));
        assertFalse(DateComparator.isFilingDateMoreRecent("03/15/2023", "03/15/2024"));
    }

    @Test
    @DisplayName("Same date comparison returns false")
    public void testIsFilingDateSame() {
        assertFalse(DateComparator.isFilingDateMoreRecent("03/15/2024", "03/15/2024"));
    }

    @Test
    @DisplayName("No historical date returns true (new firm)")
    public void testIsFilingDateMoreRecentWithNoHistoricalDate() {
        assertTrue(DateComparator.isFilingDateMoreRecent("03/15/2024", null));
        assertTrue(DateComparator.isFilingDateMoreRecent("03/15/2024", ""));
        assertTrue(DateComparator.isFilingDateMoreRecent("03/15/2024", "   "));
    }

    @Test
    @DisplayName("No current date returns false (conservative)")
    public void testIsFilingDateMoreRecentWithNoCurrentDate() {
        assertFalse(DateComparator.isFilingDateMoreRecent(null, "03/15/2024"));
        assertFalse(DateComparator.isFilingDateMoreRecent("", "03/15/2024"));
        assertFalse(DateComparator.isFilingDateMoreRecent("   ", "03/15/2024"));
    }

    @Test
    @DisplayName("Invalid date parsing returns true (conservative)")
    public void testIsFilingDateMoreRecentWithInvalidDates() {
        // Conservative approach: process if we can't parse
        assertTrue(DateComparator.isFilingDateMoreRecent("invalid", "03/15/2024"));
        assertTrue(DateComparator.isFilingDateMoreRecent("03/15/2024", "invalid"));
        assertTrue(DateComparator.isFilingDateMoreRecent("invalid1", "invalid2"));
    }

    @Test
    @DisplayName("isDateMoreRecent with no max date returns true")
    public void testIsDateMoreRecentWithNoMaxDate() {
        assertTrue(DateComparator.isDateMoreRecent("03/15/2024", null));
        assertTrue(DateComparator.isDateMoreRecent("03/15/2024", ""));
        assertTrue(DateComparator.isDateMoreRecent("03/15/2024", "   "));
    }

    @Test
    @DisplayName("isDateMoreRecent with no current date returns false")
    public void testIsDateMoreRecentWithNoCurrentDate() {
        assertFalse(DateComparator.isDateMoreRecent(null, "03/15/2024"));
        assertFalse(DateComparator.isDateMoreRecent("", "03/15/2024"));
        assertFalse(DateComparator.isDateMoreRecent("   ", "03/15/2024"));
    }

    @Test
    @DisplayName("isDateMoreRecent comparison works correctly")
    public void testIsDateMoreRecentComparison() {
        assertTrue(DateComparator.isDateMoreRecent("03/16/2024", "03/15/2024"));
        assertFalse(DateComparator.isDateMoreRecent("03/14/2024", "03/15/2024"));
        assertFalse(DateComparator.isDateMoreRecent("03/15/2024", "03/15/2024"));
    }

    @Test
    @DisplayName("Validate date format returns correct results")
    public void testIsValidDateFormat() {
        assertTrue(DateComparator.isValidDateFormat("03/15/2024"));
        assertTrue(DateComparator.isValidDateFormat("12/31/2024"));
        assertTrue(DateComparator.isValidDateFormat("01/01/2024"));

        assertFalse(DateComparator.isValidDateFormat("2024-03-15"));
        // Note: "15/03/2024" matches the PATTERN (format check only, not date validity)
        assertTrue(DateComparator.isValidDateFormat("15/03/2024")); // Matches pattern even if invalid date
        assertFalse(DateComparator.isValidDateFormat("03-15-2024"));
        assertFalse(DateComparator.isValidDateFormat("invalid"));
        assertFalse(DateComparator.isValidDateFormat(null));
        assertFalse(DateComparator.isValidDateFormat(""));
        assertFalse(DateComparator.isValidDateFormat("abc"));
    }

    @Test
    @DisplayName("Get date format returns correct pattern")
    public void testGetDateFormat() {
        assertEquals("MM/dd/yyyy", DateComparator.getDateFormat());
    }

    @Test
    @DisplayName("Leap year date parsing works correctly")
    public void testLeapYearDates() {
        assertNotNull(DateComparator.parseFilingDate("02/29/2024")); // 2024 is a leap year
        assertNull(DateComparator.parseFilingDate("02/29/2023")); // 2023 is not a leap year
        assertNotNull(DateComparator.parseFilingDate("02/29/2020")); // 2020 is a leap year
    }

    @Test
    @DisplayName("Month boundary dates parse correctly")
    public void testMonthBoundaryDates() {
        assertNotNull(DateComparator.parseFilingDate("01/31/2024"));
        assertNull(DateComparator.parseFilingDate("02/30/2024"));
        assertNotNull(DateComparator.parseFilingDate("03/31/2024"));
        assertNull(DateComparator.parseFilingDate("04/31/2024"));
        assertNotNull(DateComparator.parseFilingDate("05/31/2024"));
        assertNull(DateComparator.parseFilingDate("06/31/2024"));
        assertNotNull(DateComparator.parseFilingDate("07/31/2024"));
        assertNotNull(DateComparator.parseFilingDate("08/31/2024"));
        assertNull(DateComparator.parseFilingDate("09/31/2024"));
        assertNotNull(DateComparator.parseFilingDate("10/31/2024"));
        assertNull(DateComparator.parseFilingDate("11/31/2024"));
        assertNotNull(DateComparator.parseFilingDate("12/31/2024"));
    }

    @Test
    @DisplayName("Year 2000 problem dates work correctly")
    public void testY2KDates() {
        assertNotNull(DateComparator.parseFilingDate("12/31/1999"));
        assertNotNull(DateComparator.parseFilingDate("01/01/2000"));
        assertNotNull(DateComparator.parseFilingDate("02/29/2000")); // 2000 is a leap year
    }

    @Test
    @DisplayName("Compare dates across year boundaries")
    public void testCompareAcrossYearBoundaries() {
        assertTrue(DateComparator.isFilingDateMoreRecent("01/01/2024", "12/31/2023"));
        assertFalse(DateComparator.isFilingDateMoreRecent("12/31/2023", "01/01/2024"));
    }

    @Test
    @DisplayName("Utility class cannot be instantiated")
    public void testUtilityClassCannotBeInstantiated() {
        try {
            java.lang.reflect.Constructor<DateComparator> constructor =
                DateComparator.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Should have thrown UnsupportedOperationException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The UnsupportedOperationException is wrapped in InvocationTargetException
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
            assertEquals("This is a utility class and cannot be instantiated",
                        e.getCause().getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }
}
