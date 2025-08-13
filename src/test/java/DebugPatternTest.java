import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

public class DebugPatternTest {
    @Test
    void printPatternsAndMatches() {
        Pattern p = PatternMatchers.EMAIL_COMPLIANCE_SENTENCE_PATTERN;
        System.out.println("EMAIL_COMPLIANCE_SENTENCE_PATTERN=" + p.pattern());
        String s1 = "For compliance matters, contact compliance@firm.com";
        Matcher m1 = p.matcher(s1);
        System.out.println("compliance sentence find? " + m1.find());

        Pattern email = PatternMatchers.EMAIL_PATTERN;
        System.out.println("EMAIL_PATTERN=" + email.pattern());
        String invalid = "Contact us at user@domain..com for more info";
        Matcher m2 = email.matcher(invalid);
        System.out.println("double-dot email find? " + m2.find());

        assertTrue(PatternMatchers.EMAIL_PATTERN.matcher("Contact us at test@example.com").find());
    }
}
