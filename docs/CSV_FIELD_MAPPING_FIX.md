# CSV Field Mapping Fix

## Issue Description

During execution of the new 4-step processing workflow, the following error occurred:

```
Error processing brochure for firm 161408: Mapping for SECRgnCD not found, expected one of [dateAdded, SECRgmCD, FirmCrdNb, SECMb, Business Name, Legal Name, Street 1, Street 2, City, State, Country, Postal Code, Telephone #, Fax #, Registration Firm Type, Registration State, Registration Date, Filing Date, Filing Version, Total Employees, AUM, Total Accounts, BrochureURL]
```

## Root Cause

The error was caused by incorrect CSV field name mapping in the `BrochureProcessingService.writeMergedBrochureAnalysis()` method. The code was trying to access a field named `SECRgnCD` but the actual CSV field name is `SECRgmCD`.

## Fields Corrected

### Before (Incorrect):
- `SECRgnCD` → **Field does not exist**
- `SECNb` → **Field does not exist**

### After (Correct):
- `SECRgmCD` → **Correct field name**
- `SECMb` → **Correct field name**

## Code Changes Made

In `src/BrochureProcessingService.java`, the `writeMergedBrochureAnalysis()` method was updated:

```java
// BEFORE (incorrect field names)
record.append(csvEscape(firmRecord.get("SECRgnCD"))).append(",");
record.append(csvEscape(firmRecord.get("SECNb"))).append(",");

// AFTER (correct field names)
record.append(csvEscape(firmRecord.get("SECRgmCD"))).append(",");
record.append(csvEscape(firmRecord.get("SECMb"))).append(",");
```

## Actual CSV Field Names

Based on the error message, the actual CSV fields from the XML processing step are:

1. `dateAdded`
2. `SECRgmCD` ✓ (corrected)
3. `FirmCrdNb` ✓
4. `SECMb` ✓ (corrected)
5. `Business Name` ✓
6. `Legal Name`
7. `Street 1` ✓
8. `Street 2` ✓
9. `City` ✓
10. `State` ✓
11. `Country` ✓
12. `Postal Code` ✓
13. `Telephone #` ✓
14. `Fax #`
15. `Registration Firm Type`
16. `Registration State`
17. `Registration Date`
18. `Filing Date` ✓
19. `Filing Version`
20. `Total Employees` ✓
21. `AUM` ✓
22. `Total Accounts` ✓
23. `BrochureURL`

## Impact

This fix resolves the runtime error that was preventing the 4-step processing workflow from completing successfully. The brochure processing with data merging step (Step 4) can now properly access the firm data fields and create the final IAPD_Data output file.

## Testing

- Code compiles successfully after the fix
- Field names now match the actual CSV structure from XMLProcessingService
- The 4-step workflow should now complete without field mapping errors

## Prevention

To prevent similar issues in the future:

1. **Field Name Validation**: Consider adding validation to check if required fields exist in CSV records before processing
2. **Documentation**: Maintain clear documentation of CSV field names and their sources
3. **Testing**: Include integration tests that verify field mappings across the entire workflow
4. **Error Handling**: Improve error messages to clearly indicate which fields are missing and where they're expected
