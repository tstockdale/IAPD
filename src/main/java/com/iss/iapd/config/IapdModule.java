package com.iss.iapd.config;

import com.google.inject.AbstractModule;
import com.iss.iapd.services.brochure.BrochureAnalyzer;
import com.iss.iapd.services.brochure.BrochureDownloadService;
import com.iss.iapd.services.brochure.BrochureProcessingService;
import com.iss.iapd.services.brochure.BrochureURLExtractionService;
import com.iss.iapd.services.csv.CSVWriterService;
import com.iss.iapd.services.download.FileDownloadService;
import com.iss.iapd.services.xml.XMLProcessingService;

public class IapdModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FileDownloadService.class).toInstance(new FileDownloadService());
        bind(XMLProcessingService.class).toInstance(new XMLProcessingService());
        bind(BrochureURLExtractionService.class).toInstance(new BrochureURLExtractionService());
        bind(BrochureDownloadService.class).toInstance(new BrochureDownloadService(new FileDownloadService()));
        bind(BrochureAnalyzer.class).toInstance(new BrochureAnalyzer());
        bind(CSVWriterService.class).toInstance(new CSVWriterService());
        bind(BrochureProcessingService.class).toInstance(
            new BrochureProcessingService(new BrochureAnalyzer(), new CSVWriterService())
        );
    }
}
