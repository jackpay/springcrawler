package com.casm.acled.crawler;

// apache commons
import com.casm.acled.crawler.utils.DateUtil;
import com.norconex.importer.handler.filter.OnMatch;
import org.apache.commons.configuration.XMLConfiguration;

// norconex
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;


public class DateFilter extends AbstractDocumentFilter {

    protected static final Logger logger = LoggerFactory.getLogger(DateFilter.class);

    private static final int DEFAULT = 1;
    private final LocalDate threshold;

    public DateFilter() {
        this(LocalDate.now().minusWeeks(DEFAULT));
    }

    public DateFilter(LocalDate threshold) {
        this.threshold = threshold;
        this.setOnMatch(OnMatch.EXCLUDE);

    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input, ImporterMetadata metadata, boolean parsed) throws ImporterHandlerException {

        String dateStr = metadata.get(ACLEDScraperPreProcessor.SCRAPEDATE).get(0);
        boolean rejected = false;
        if(dateStr == null || dateStr.length() <= 0) {
            logger.debug("INFO: No date found for url: " + reference);
            rejected = false;
        }
        try{
            LocalDate date = DateUtil.getDate(dateStr);
//            logger.info("INFO: filtering article by date: " + reference + " date: " + date + " " + threshold.toString()
//                    + " article date: " + dateStr + "after?: " + date.isAfter(threshold));
            if(date != null) {
                if(date.isBefore(threshold)) {
                    logger.info("DATE-PRIOR-TO-THRESH " + date.toString() + " | " + dateStr);
                    rejected = true;
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing date: " + reference);
        }

        return rejected;
    }


    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer) throws XMLStreamException {
        // NAH MATE
    }

    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        // NAH MATE
    }
}
