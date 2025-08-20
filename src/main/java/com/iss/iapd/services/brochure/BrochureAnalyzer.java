package com.iss.iapd.services.brochure;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.utils.PatternMatchers;

/**
 * Improved analyzer for brochure content using strategy pattern
 * This version is more maintainable and extensible
 */
public class BrochureAnalyzer {
    
    private final List<AnalysisStrategy> strategies;
    
    public BrochureAnalyzer() {
        this.strategies = new ArrayList<>();
        initializeStrategies();
    }
    
    /**
     * Initialize all analysis strategies
     */
    private void initializeStrategies() {
        strategies.add(new ProxyProviderAnalysisStrategy());
        strategies.add(new ClassActionProviderAnalysisStrategy());
        strategies.add(new ESGProviderAnalysisStrategy());
        strategies.add(new EmailAnalysisStrategy());
        strategies.add(new NoVoteAnalysisStrategy());
    }
    
    /**
     * Analyzes brochure content using all configured strategies
     */
    public BrochureAnalysis analyzeBrochureContent(String text, String firmCrdNb) {
        BrochureAnalysis analysis = new BrochureAnalysis();
        ProcessingLogger.logProviderMatch("Analyzing brochure for Firm CRD#: " + firmCrdNb);
        // Handle null input gracefully
        if (text == null) {
            return analysis;           
        }
        
        // Apply all analysis strategies
        for (AnalysisStrategy strategy : strategies) {
            strategy.analyze(text, analysis);
        }
        
        // Build email set string
        analysis.buildEmailSetString();
        
        return analysis;
    }
    
    /**
     * Strategy interface for different types of analysis
     */
    private interface AnalysisStrategy {
        void analyze(String text, BrochureAnalysis analysis);
    }
    
    /**
     * Strategy for analyzing proxy providers
     */
    private static class ProxyProviderAnalysisStrategy implements AnalysisStrategy {
        @Override
        public void analyze(String text, BrochureAnalysis analysis) {
            // First, check for specific providers
            addProviderIfFound(PatternMatchers.GLASS_LEWIS_PATTERN, text, analysis.getProxyProvider(), "Glass Lewis");
            addProviderIfFound(PatternMatchers.BROADRIDGE_PATTERN, text, analysis.getProxyProvider(), "BroadRidge");
            addProviderIfFound(PatternMatchers.PROXYEDGE_PATTERN, text, analysis.getProxyProvider(), "ProxyEdge");
            addProviderIfFound(PatternMatchers.EGAN_JONES_PATTERN, text, analysis.getProxyProvider(), "Egan-Jones");
            addProviderIfFound(PatternMatchers.ISS_PROXY_PATTERN, text, analysis.getProxyProvider(), "ISS");
            
            // Only add "Third Party" if no specific providers were found
            if (analysis.getProxyProvider().length() == 0) {
                addProviderIfFound(PatternMatchers.THIRD_PARTY_PROXY_PATTERN, text, analysis.getProxyProvider(), "Third Party");
            }
        }
    }
    
    /**
     * Strategy for analyzing class action providers
     */
    private static class ClassActionProviderAnalysisStrategy implements AnalysisStrategy {
        @Override
        public void analyze(String text, BrochureAnalysis analysis) {
            addProviderIfFound(PatternMatchers.FRT_PATTERN, text, analysis.getClassActionProvider(), "FRT");
            addProviderIfFound(PatternMatchers.ISS_CLASS_ACTION_PATTERN, text, analysis.getClassActionProvider(), "ISS");
            addProviderIfFound(PatternMatchers.BATTEA_CLASS_ACTION_PATTERN, text, analysis.getClassActionProvider(), "Battea");
            addProviderIfFound(PatternMatchers.CCC_CLASS_ACTION_PATTERN, text, analysis.getClassActionProvider(), "CCC");
            addProviderIfFound(PatternMatchers.ROBBINS_GELLER_CLASS_ACTION_PATTERN, text, analysis.getClassActionProvider(), "Robbins Geller");
        }
    }
    
    /**
     * Strategy for analyzing ESG providers
     */
    private static class ESGProviderAnalysisStrategy implements AnalysisStrategy {
        @Override
        public void analyze(String text, BrochureAnalysis analysis) {
            addProviderIfFound(PatternMatchers.SUSTAINALYTICS_PATTERN, text, analysis.getEsgProvider(), "Sustainalytics");
            addProviderIfFound(PatternMatchers.MSCI_PATTERN, text, analysis.getEsgProvider(), "MSCI");
            
            if (PatternMatchers.ESG_PATTERN.matcher(text).find()) {
                analysis.getEsgInvestmentLanguage().append('Y');
            }
        }
    }
    
    /**
     * Strategy for analyzing email patterns
     */
    private static class EmailAnalysisStrategy implements AnalysisStrategy {
        @Override
        public void analyze(String text, BrochureAnalysis analysis) {
            extractEmailSentence(PatternMatchers.EMAIL_SENTENCE_PATTERN, text, analysis.getEmailSentence());
            extractEmailSentence(PatternMatchers.EMAIL_COMPLIANCE_SENTENCE_PATTERN, text, analysis.getEmailComplianceSentence());
            extractEmailSentence(PatternMatchers.EMAIL_PROXY_SENTENCE_PATTERN, text, analysis.getEmailProxySentence());
            extractEmailSentence(PatternMatchers.EMAIL_BROCHURE_SENTENCE_PATTERN, text, analysis.getEmailBrochureSentence());
            
            // Extract all email addresses
            Matcher emailMatcher = PatternMatchers.EMAIL_PATTERN.matcher(text);
            while (emailMatcher.find()) {
                analysis.getEmailSet().add(emailMatcher.group(1));
            }
        }
        
        private void extractEmailSentence(Pattern pattern, String text, StringBuilder result) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    result.append(matcher.group(1));
                } else if (matcher.group(0) != null) {
                    result.append(matcher.group(0));
                }
            }
        }
    }
    
    /**
     * Strategy for analyzing no-vote patterns
     */
    private static class NoVoteAnalysisStrategy implements AnalysisStrategy {
        @Override
        public void analyze(String text, BrochureAnalysis analysis) {
            Matcher matcher = PatternMatchers.NO_VOTE_PATTERN.matcher(text);
            if (matcher.find()) {
                analysis.getNoVoteString().append(matcher.group(0));
                // Add "Does Not Vote" if no provider is set, or replace "Third Party" with "Does Not Vote"
                if (analysis.getProxyProvider().length() == 0) {
                    analysis.getProxyProvider().append("Does Not Vote");
                } else if ("Third Party".equals(analysis.getProxyProvider().toString())) {
                    // Replace "Third Party" with "Does Not Vote" as it's more specific
                    analysis.getProxyProvider().setLength(0);
                    analysis.getProxyProvider().append("Does Not Vote");
                }
            }
        }
    }
    
    /**
     * Helper method to add provider if pattern is found
     */
    private static void addProviderIfFound(Pattern pattern, String text, StringBuilder providerList, String providerName) {
    	Matcher m = pattern.matcher(text);
    	if (m.find()) {
        	// Log to dedicated provider match strings log file with counting
        	ProcessingLogger.logProviderMatch(providerName, m.group(0));
            if (providerList.length() == 0) {
                providerList.append(providerName);
            } else {
                providerList.append("|").append(providerName);
            }
        }
    }

}
