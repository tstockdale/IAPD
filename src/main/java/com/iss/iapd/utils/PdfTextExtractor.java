package com.iss.iapd.utils;

import java.io.FileInputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.ToXMLContentHandler;

/**
 * Utility class for extracting text from PDF files using Apache Tika
 */
public class PdfTextExtractor {
    
    /**
     * Extracts text content from PDF brochure using Apache Tika
     */
    public static String getBrochureText(FileInputStream stream) {
        ToXMLContentHandler handler = null;
        try {
            handler = new ToXMLContentHandler();
            Metadata metadata = new Metadata();
            Parser parser = new AutoDetectParser();
            
            PDFParserConfig pdfConfig = new PDFParserConfig();
            pdfConfig.setExtractInlineImages(false);
            pdfConfig.setExtractUniqueInlineImagesOnly(false);
            
            ParseContext parseContext = new ParseContext();
            parseContext.set(PDFParserConfig.class, pdfConfig);
            parseContext.set(Parser.class, parser);
            
            parser.parse(stream, handler, metadata, parseContext);
        } catch (Exception e) {
            System.err.println("Error extracting text from PDF: " + e.getMessage());
            e.printStackTrace();
        }
        
        return handler != null ? handler.toString() : "";
    }
    
    /**
     * Extracts and cleans text from PDF for brochure analysis
     */
    public static String getCleanedBrochureText(FileInputStream stream) {
        String text = getBrochureText(stream);
        return text.replaceAll("<(?:|/)p>", " ")
                  .replaceAll("[\\r\\n]", " ");
    }
    
    /**
     * Extracts and heavily cleans text from PDF for custodial services analysis
     */
    public static String getHeavilyCleanedBrochureText(FileInputStream stream) {
        String text = getBrochureText(stream);
        return text.replaceAll("<.*?>", " ")
                  .replaceAll("[\\r\\n]", " ")
                  .replaceAll("\\s+", " ");
    }
    
    // Private constructor to prevent instantiation
    private PdfTextExtractor() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
