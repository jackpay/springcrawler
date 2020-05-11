package com.casm.acled.crawler.management;

import com.casm.acled.crawler.springrunners.CrawlerServicelRunner;
import org.junit.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

import java.time.LocalDate;

public class CrawlRunnerTest {


    @Test
    public void testRun() {

        String sourceListId = "1";
        String sourceId = "2158";
        //http://www.0.com:5000
//        String sourceId = "3547";

//        LocalDate to = LocalDate.now();
        LocalDate to = null;
//        LocalDate from = to.minusDays(7);
        LocalDate from = null;

        String[] args  = new String[]{sourceListId, sourceId, "", "", "true" };

        SpringApplication app = new SpringApplication(CrawlerServicelRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);

    }

}