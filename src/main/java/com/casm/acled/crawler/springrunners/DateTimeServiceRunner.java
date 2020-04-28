package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.dates.DateParsers;
import com.casm.acled.crawler.scraper.dates.DateTimeService;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.sourcelist.SourceList;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class DateTimeServiceRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(DateTimeServiceRunner.class);

    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private ArticleDAO articleDAO;

    private void attemptAllScrapers() {
        dateTimeService.setScrapersPath(Paths.get("allscrapers"));

        dateTimeService.attemptAllDateTimeParsers(DateParsers.ALL, DateTimeService.lastScrapeExampleGetter(Paths.get("/home/sw206/git/acled-scrapers")));

        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_SUCCESS.name())));
        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_FAILED.name())));
    }

    private void attemptSourceListExistingArticles(String name) {
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, name).get();
        dateTimeService.attemptSourceListDateTimeParsers(sourceList, DateParsers.ALL, s -> {
            List<Article> articles = articleDAO.bySource(s);
            return articles.stream()
                    .filter(a -> a.hasValue(Article.SCRAPE_DATE))
                    .map(a -> (String)a.get(Article.SCRAPE_DATE))
                    .collect(Collectors.toList());
        } );

        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_SUCCESS.name())));
        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_FAILED.name())));
    }

    @Override
    public void run(String... args) throws Exception {

        attemptSourceListExistingArticles("balkans");
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(DateTimeServiceRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}