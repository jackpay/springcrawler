package com.casm.acled.crawler.springrunners;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.management.*;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ScraperService;
import com.casm.acled.crawler.scraper.dates.DateTimeService;
import com.casm.acled.crawler.scraper.locale.LocaleService;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.dao.util.ExportCSV;
import com.casm.acled.entities.source.Source;
import com.ibm.icu.util.ULocale;
import com.norconex.importer.handler.ImporterHandlerException;

import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;

import javax.validation.Valid;
import java.util.Set;
import java.util.TimeZone;


@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
@ShellComponent
public class ShellRunner {

    protected static final Logger logger = LoggerFactory.getLogger(ShellRunner.class);

    @Autowired
    private CheckListService checkListService;

    @Autowired
    private ImportExportService importExportService;

    @Autowired
    private DataOperationService dataOperationService;

    @Autowired
    private UtilService utilService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private CrawlArgsService argsService;

//    private CrawlArgs crawlArgs;

//    @Autowired
//    LineReader reader;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private ExportCSV exportCSV;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private DateTimeService dateTimeService;


    @Autowired
    private LocaleService localeService;

    @ShellMethod(value = "Copy a Source (-s) or SourceList (-sl) to a with a new name (-N) or suffix if flag 'S' is provided")
    public void copy(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();
        dataOperationService.copy(crawlArgs);
    }

    @ShellMethod(key = "candidate-timezones", value = "[-s SOURCE] Requires COUNTRY to be set on source. Lists all known timezones for source country.")
    public Set<TimeZone> candidateTimezones(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        Source source = crawlArgs.source;

        return localeService.candidateTimeZones(source);
    }

    @ShellMethod(key = "candidate-locales", value = "[-s SOURCE] Requires LANGUAGES or COUNTRY to be set. Lists all known locales for source languages or country.")
    public Set<ULocale> candidateLocales(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        Source source = crawlArgs.source;

        return localeService.candidateLocales(source);
    }

    @ShellMethod(key = "fix-links", value = "Attempt to load source links, with attempts to ensure protocol and follow any redirects. [-F U] to update database entry with resolved links.")
    public void fixLinks(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        checkListService.fixSourceLinks(crawlArgs);
    }


    @ShellMethod(value = "check source list (-sl)", key = "check")
    // the help command still not working:
    // Action: Correct the classpath of your application so that it contains a single, compatible version of com.beust.jcommander.JCommander
    public void checkSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;
        crawlArgs.init();

        checkListService.checkSourceList(crawlArgs);

    }

    @ShellMethod(value = "import source list (-sl), must specify working-dir (-wd) and path to file (-P) specify the L flag (-F L) to also link the sources", key = "import-source")
    public void importSources(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        importExportService.importSources(crawlArgs);
    }

    @ShellMethod(value = "import source list (-sl), must specify working-dir (-wd) and path to file (-P) specify the C flag (-F C) to actually update the database", key = "import-list")
    public void importList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        importExportService.importList(crawlArgs);
    }

    @ShellMethod(value = "export source list (-sl), must specify working dir (-wd) and path to file (-P)", key = "export")
    public void exportSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;
        crawlArgs.init();

        importExportService.exportSources(crawlArgs);
    }

    @ShellMethod(value = "Link a Source to a source list (-sl). Either using -s or sources can be read from CSV (-P).", key="link")
    public void linkSourceToSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.raw = args;
        crawlArgs.init();

        dataOperationService.linkSourceToSourceList(crawlArgs);
    }

    @ShellMethod(value = "unlink a Source (-s) from a source list (-sl). Either using -s or sources can be read from CSV (-P)", key="unlink")
    public void unlinkSourceFromSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.raw = args;
        crawlArgs.init();

        dataOperationService.unlinkSourceFromSourceList(crawlArgs);
    }

    @ShellMethod(value = "unlink all sources from source list (-sl)", key="unlink-all")
    public void unlinkAllFromSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        dataOperationService.unlinkAllFromSourceList(crawlArgs);
    }

    @ShellMethod(value = "Re-scrape the articles for a given source (-s), be sure to specify the scraper dir (-sd). Optionally use -f and -t to constrain to only articles within a from-to date. Articles that have no existing date will always be attempted.", key="re-scrape")
    public void rescrapeSource(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws ImporterHandlerException {
//        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;
        crawlArgs.init();

        if (crawlArgs.source.hasValue(Source.DATE_FORMAT)){
            System.out.println("Source has date format specified - will attempt to re-parse scraped dates.");
        } else {
            System.err.println("Source does not have date format specifications - only scraped fields will be updated.");
        }

        // Rescrape
        int changed = scraperService.reScrape(crawlArgs.source, crawlArgs.from, crawlArgs.to, crawlArgs.scrapersDir);

        if (changed > 0) {
            System.out.println(changed + " articles were updated.");
        } else {
            System.err.println("No changes found.");
        }
    }


    @ShellMethod(value = "output example urls ", key = "output")
    public void outputExampleURLCheck(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
//        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "example-urls";

        crawlArgs.init();

        checkListService.outputExampleURLCheck(crawlArgs);

//        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    @ShellMethod(value = "Check crawl reports, usage: check-reports -s/sl -r value", key = "check-reports")
    public void checkSourceCrawlRuns(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                                     @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                                     @ShellOption(value = {"-r", "--runs"}, defaultValue = "10") int numRuns,
                                     @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        checkListService.checkCrawlReports(crawlArgs, numRuns);
    }

    // generic set / get commands for sources and source lists, in the form
    // generic, only handle single instance
    @ShellMethod(value = "get specific value from the corresponding field; usage: get -s/sl name -field value", key = "get")
    public String getField(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                           @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                           @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                           @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return dataOperationService.getFied(crawlArgs, field);

    }

    @ShellMethod(value = "set specific value to the corresponding field; usage: set -s/sl name -field value -value value", key = "set")
    // generic, only handle single instance
    public <T> String setField(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                               @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                               @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                               @ShellOption({"-value"}) String value,
                               @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return dataOperationService.setField(crawlArgs, field, value);

    }

    @ShellMethod(value = "add field/property value to existing list; usage: add -sl/s name -field value -value value", key = "add")
    public String addValue(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                           @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                           @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                           @ShellOption({"-value"}) String value,
                           @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        // test command: add -s "Imagen del Golfo" -field CRAWL_SCHEDULE -value "*"

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return dataOperationService.addValue(crawlArgs, field, value);
    }

    @ShellMethod(value = "show source/sourcelist names and entries, if sourcelist(-sl), will show all source names and ids under it. usage: show -s/sl NAME", key = "show")
    public String showValue(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        // test sample: show -s "Imagen del Golfo"
        // test sample: show -sl "mexico-1"

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return dataOperationService.showValue(crawlArgs);

    }

    // this method cannot be compatible with CrawlArgs parameters for now.
    @ShellMethod(value = "delete source/sourcelist field value. usage: delete -s/sl name -field value", key = "delete")
    public String deleteValue(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                              @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                              @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                              @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return dataOperationService.deleteValue(crawlArgs, field);
    }

    // this method cannot be compatible with CrawlArgs parameters for now.
    @ShellMethod(value = "batch update all source values via sourcelist, modify all source under given sourcelist. usage: update -sl name -field value -value value", key = "update")
    public String updateValue(@ShellOption(value = {"-sl"}) String sourceListName,
                              @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                              @ShellOption({"-value"}) String value,
                              @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return dataOperationService.updateValue(crawlArgs, field, value);

    }

    // this method cannot be compatible with CrawlArgs parameters for now.
    @ShellMethod(value = "download html from link provided, run the Jsoup pattern and print the results. usage: jsoup -l LINK -p JSOUP_PATTERN", key = "jsoup")
    public String jsoupSearch(@ShellOption({"-l","--link"}) String url,
                              @ShellOption({"-p","--pattern"}) String pattern) {

        // test sample: jsoup -l "https://imagendelgolfo.mx/xalapa/a-morena-en-veracruz-lo-persigue-fantasma-del-perredismo-aseveran/50047104" -p "div.siete60 div.SlaBLK22"

        return utilService.jsoupSearch(url, pattern);
    }

    @ShellMethod(value = "clear PIDs, usage: clear-pids", key = "clear-pids")
    public String clearPIDs(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        schedulerService.clearPIDs(crawlArgs);

        return String.format("PIDs have been cleared");
    }

    @ShellMethod(value = "run scheduler, if no source list is specified, run all. Usage: schedule -wd WORKING-DIR -sd SCRAPER-DIR -d DEPTH [-sl SOURCE-LIST]", key = "schedule")
    public String schedule(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
//        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;
        crawlArgs.init();
        schedulerService.schedule(crawlArgs);

        return String.format("scheduling done");

    }

    @ShellMethod(value = "dump articles to local csv file, path should be specified to folder. Usage: dump -s/sl name -f FROM-DATE -t TO-DATE -P OUTPUT-DIR", key = "dump")
    public String dump(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{

        // test sample: dump -s "Imagen del Golfo" -f "2020-09-01" -t "2020-09-24" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/compare"
        // test sample: dump -sl "mexico-1" -f "2020-09-01" -t "2020-09-24" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"
        // test sample: dump -s "Imagen del Golfo" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"
        // test sample: dump -sl "mexico-1" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"
        // test sample: dump -sl "mexico-1" -t "2020-09-24" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return dataOperationService.dump(crawlArgs);

    }

    @ShellMethod(value = "generate JEF configuration for source/sourcelists to given folder. Usage: jef -s/sl -wd working_dir -P output_path", key = "jef")
    public String jef(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        return utilService.jef(crawlArgs);

    }

//    @Bean
//    public ParameterResolver commandParameterResolver() {
//        return new ParameterResolver(){
//
//            @Override
//            public boolean supports(MethodParameter parameter) {
//                return parameter.getParameterType().isAssignableFrom(List.class);
//            }
//
//            /**
//             * This implementation simply returns all the words (arguments) present
//             * 'Infinite arity'
//             */
//            @Override
//            public ValueResult resolve(MethodParameter methodParameter, List<String> words) {
//                return new ValueResult(methodParameter, words);
//            }
//
//            @Override
//            public Stream<ParameterDescription> describe(MethodParameter parameter) {
//                return Stream.of(ParameterDescription.outOf(parameter));
//            }
//
//            @Override
//            public List<CompletionProposal> complete(MethodParameter parameter, CompletionContext context) {
//                return Collections.emptyList();
//            }
//        };
//    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ShellRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }

}
