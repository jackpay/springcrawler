package com.casm.acled.crawler.management;

import com.casm.acled.crawler.DateFilter;
import com.casm.acled.crawler.scraper.ScraperFields;
import com.casm.acled.crawler.scraper.dates.CustomDateMetadataFilter;
import com.casm.acled.crawler.utils.Util;
import com.norconex.collector.core.crawler.ICrawlerConfig;
import com.norconex.collector.core.data.store.impl.mvstore.MVStoreCrawlDataStoreFactory;
import com.norconex.collector.core.filter.impl.RegexReferenceFilter;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.collector.http.delay.impl.GenericDelayResolver;
import com.norconex.collector.http.url.impl.GenericLinkExtractor;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;
import com.norconex.importer.handler.filter.impl.RegexMetadataFilter;
import com.norconex.importer.parser.GenericDocumentParserFactory;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NorconexConfiguration {


    private final HttpCollectorConfig collector;
    private final HttpCrawlerConfig crawler;
    private final ImporterConfig importer;

    private Path crawlStore;
    private String userAgent;
    private int threadsPerSeed;
    private boolean ignoreRobots;
    private boolean ignoreSiteMap;
    private int depth;
    private String urlRegex;
    private String seed;
    private long politeness;
    private int numThreads;
    private List<String> regexFilterPatterns;
    private ZonedDateTime from;
    private ZonedDateTime to;

    private static String PROGRESS = "progress";
    private static String LOGS = "logs";

    public NorconexConfiguration(ZonedDateTime from, ZonedDateTime to) {
        this.from = from;
        this.to = to;
        importer = new ImporterConfig();
        collector = new HttpCollectorConfig();
        crawler = new HttpCrawlerConfig();

    }

    public HttpCollectorConfig collector (){
        return collector;
    }
    public HttpCrawlerConfig crawler() {
        return crawler;
    };
    public ImporterConfig importer() {
        return importer;
    };

    private void configureCollector() {

        collector.setProgressDir(crawlStore.resolve(PROGRESS).toString());
        collector.setLogsDir(crawlStore.resolve(LOGS).toString());
    }

    private void configureCrawler() {


//        MD5DocumentChecksummer checksummer = new MD5DocumentChecksummer();
//        checksummer.setSourceFields(CrawlerArguments.SCRAPEDARTICLE);
//        checksummer.setTargetField(CrawlerArguments.CONTENTHASH);
//        crawler.setDocumentChecksummer(checksummer);
//        crawler.setDocumentChecksummer(new ArticleExtractorChecksum());

        // Basic crawler config
        crawler.setUserAgent(userAgent);
        crawler.setMaxDepth(depth); // -1 for inf
        crawler.setIgnoreRobotsMeta(ignoreRobots);
        crawler.setIgnoreRobotsTxt(ignoreRobots);
        crawler.setIgnoreCanonicalLinks(false);
        crawler.setIgnoreSitemap(ignoreSiteMap);
        crawler.setNumThreads(numThreads);
        // only store a crawl cache, not content
        crawler.setKeepDownloads(false);
        // Page found but record of its parent lost - process the content and links anyway
        crawler.setOrphansStrategy(ICrawlerConfig.OrphansStrategy.PROCESS);
        // Keeps the crawler within the same domain
        URLCrawlScopeStrategy ucs = new URLCrawlScopeStrategy();
        ucs.setStayOnDomain(true);
        ucs.setIncludeSubdomains(true);
        ucs.setStayOnPort(false);
        ucs.setStayOnProtocol(false);
        crawler.setUrlCrawlScopeStrategy(ucs);
        // set to false so crawl cache is only those of interest
        crawler.setKeepOutOfScopeLinks(false);

        crawler.setWorkDir(crawlStore.toFile());

        crawler.setCrawlDataStoreFactory(new MVStoreCrawlDataStoreFactory());

//        crawler.setId(id);
//            crawler.setStartSitemapURLs(seeds);
//        crawler.setStartURLs(seeds);


        // Used to set the politeness delay for consecutive post calls to the site (helps prevent being blocked)
        GenericDelayResolver gdr = new GenericDelayResolver();
        gdr.setDefaultDelay((politeness <= 50) ? 50 : politeness); // safety check to avoid to to small a delay
        gdr.setIgnoreRobotsCrawlDelay(ignoreRobots);
        gdr.setScope(GenericDelayResolver.SCOPE_SITE);
        crawler.setDelayResolver(gdr);

        GenericLinkExtractor gle = new GenericLinkExtractor();
        gle.setIgnoreNofollow(ignoreRobots);
        gle.setCharset(StandardCharsets.UTF_8.toString());
        crawler.setLinkExtractors(gle);

        // create the url filters - e.g. regex filters
        // url regex match
        // parent link prevention
        if(regexFilterPatterns!=null) {
            crawler.setReferenceFilters(
                    regexFilterPatterns.stream()
                            .map(regex -> new RegexReferenceFilter(regex))
                            .collect(Collectors.toList())
                            .toArray(new RegexReferenceFilter[regexFilterPatterns.size()])
            );
        }
    }


    private void configureImporter() {

        RegexMetadataFilter regexFilter = new RegexMetadataFilter(CrawlerArguments.SCRAPEDARTICLE, Util.KEYWORDS);
        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,CrawlerArguments.SCRAPEDARTICLE);
        int week = 7;
        DateFilter df = new DateFilter(LocalDate.now().minusDays(week));

        DateMetadataFilter dateMetadataFilter = new CustomDateMetadataFilter(ScraperFields.SCRAPED_DATE, null);
        dateMetadataFilter.addCondition(DateMetadataFilter.Operator.GREATER_THAN, Date.from(from.toInstant()));
        dateMetadataFilter.addCondition(DateMetadataFilter.Operator.LOWER_EQUAL, Date.from(to.toInstant()));

        List<IImporterHandler> handlers = new ArrayList<>();

        handlers.add(emptyArticle);
        handlers.add(regexFilter);
        importer.setPostParseHandlers(handlers.toArray(new IImporterHandler[handlers.size()]));


        // set this to correctly manage file sizes etc...
//        importer.setMaxFileCacheSize(10);
//        importer.setMaxFilePoolCacheSize(200);
        //effectively disables importer
        GenericDocumentParserFactory gdpf = new GenericDocumentParserFactory();
        gdpf.setIgnoredContentTypesRegex(".*");
        importer.setParserFactory(gdpf);
        importer.setTempDir(crawlStore.toFile());

    }

}