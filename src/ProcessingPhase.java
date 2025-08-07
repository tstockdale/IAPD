/**
 * Enumeration of processing phases for tracking application state
 */
public enum ProcessingPhase {
    INITIALIZATION("Initializing application"),
    DOWNLOADING_XML("Downloading XML data"),
    PARSING_XML("Parsing XML file and extracting firm data"),
    DOWNLOADING_BROCHURES("Downloading brochure PDF files"),
    PROCESSING_BROCHURES("Processing and analyzing brochures"),
    GENERATING_OUTPUT("Generating output files"),
    CLEANUP("Cleaning up resources"),
    COMPLETED("Processing completed"),
    ERROR("Error occurred during processing");
    
    private final String description;
    
    ProcessingPhase(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
