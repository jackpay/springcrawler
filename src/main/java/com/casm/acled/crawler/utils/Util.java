package com.casm.acled.crawler.utils;


import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.IncorrectScraperJSONException;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import com.google.common.io.Files;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


// We have to exclude these classes, because they only work in a web context.
@EnableAutoConfiguration(exclude={CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})
public class Util implements CommandLineRunner {
    protected static final Logger logger = LoggerFactory.getLogger(Util.class);
    // keyword query specific to potential articles of interest to ACLED
//    public static String KEYWORDS2 = ".+(?:kill|killed|massacre|death|\\bdied\\b|\\bdead\\b|\\bbomb\\b|\\bbombed\\b|\\bbombing\\b|\\brebel\\b|\\battack\\b|\\battacked\\b|\\briot\\b|\\bbattle\\b|\\bprotest\\b|\\bclash\\b|\\bdemonstration\\b|\\bstrike\\b|\\bwound\\b|\\binjure\\b|\\bcasualty\\b|\\bdisplace\\b|\\bunrest\\b|\\bcasualties\\b|\\bvigilante\\b|\\btorture\\b|\\bmarch\\b|\\brape\\b).+";
    public static String KEYWORDS =              ".*\\b(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape)\\b.*";
//    private static final String matchingKeywords = "\\b(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape)\\b";

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    // Returns the originating domain of a given url - minus any trailing 'www'
    public static String getDomain(String urlString)  {
        try {
            URL url = new URL(urlString);
            return (url.getHost().startsWith("www")) ? url.getHost().substring(4) : url.getHost();
        } catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    // Processes a M52 job json to scraper rules
    public static String processJobJSON(String json) throws IncorrectScraperJSONException {
        JSONObject jobj = new JSONObject(json);
        try {
            return jobj.getJSONArray("components").getJSONObject(0).getJSONObject("opts").getJSONArray("fields").toString();
        } catch (Exception e) {
            throw new IncorrectScraperJSONException();
        }
    }

    public static String processScraperJSON(String json){
        // BUG FOUND - NEED TO USE job.json!!
        return null;
    }

    // returns a web scraper based on a job spect of last_scrape file
    public static String processJSON(File scraperLocation) throws IOException, IncorrectScraperJSONException {
        String json = Files.asCharSource(scraperLocation, Charset.defaultCharset()).read();
        return (scraperLocation.getName().equals("last_scrape.json")) ? processScraperJSON(json) : processJobJSON(json);
    }

//    public static void main(String[] args) throws Exception {
//        getDomain("http://www.0.com:5000");
//    }


    //    public LocalDate simplDateFallback(String dateString) {
//
//    }

    public void recoverArticleDates() {

//        Parser parser = new Parser();



        for(Article article : articleDAO.getAll() ) {


            String[] text = ((String)article.get(Article.TEXT)).split("\n");

            if(text.length <= 1) {
                continue;
            }

            LocalDate localDate = null;
            try {

                localDate = DateUtil.getDate(text[1]);
            } catch (RuntimeException e) {
                continue;
            }

            if(localDate == null) {

                articleDAO.delete(article);
                continue;
            }

            article = article.put(Article.DATE, localDate);

            if(article.hasValue(Article.SOURCE_ID)) {

                int sourceId = article.get(Article.SOURCE_ID);

                List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);

                for(SourceList sourceList : sourceLists) {

                    article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), localDate));
                }

            }

            articleDAO.update(article);
        }
    }


    private void deleteNonMatchingArticles() {
        Pattern pattern = Pattern.compile(KEYWORDS);
        for (Article article : articleDAO.getAll()) {
            String text = article.get(Article.TEXT);

            if(!pattern.matcher(text).matches()) {
                System.out.println("REMOVE " + text);
                articleDAO.delete(article);
            } else {

                //System.out.println("KEEP " + text);
            }
        }
    }

    private void linkExisting() {
        Map<String, Source> sources = sourceDAO.getAll().stream().filter(s->s.get(Source.LINK)!=null).collect(Collectors.toMap(s->s.get(Source.LINK), s -> s, (o, o2) -> o));

        for(Article article : articleDAO.getAll()) {
            String url = article.get(Article.URL);
            if(url == null) {
                continue;
            }
            for (Map.Entry<String, Source> e : sources.entrySet()) {

                if (url.contains(e.getKey())) {
                    int sourceId = e.getValue().id();
                    article = article.put(Article.SOURCE_ID, sourceId);

                    List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);

                    LocalDate localDate = article.get(Article.DATE);

                    for(SourceList sourceList : sourceLists) {

                        article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), localDate));
                    }

                    articleDAO.update(article);
//                    System.out.println("updating " + article.get(Article.URL));
                    break;
                }
            }

        }
    }

    public void run(String... args) throws Exception {
        deleteNonMatchingArticles();
        recoverArticleDates();
        linkExisting();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Util.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}