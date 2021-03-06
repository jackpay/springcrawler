package com.casm.acled.crawler.scraper.dates;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Co-ordinates a bunch of `DateParserCoverage` wrappers to produce overlap
 * and coverage statistics.
 */
public class CoverageCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageCalculator.class);

    // All (stats-wrapped) DateParsers
    private List<DateParserCoverage> coverageCalcs;

    // List of successful (stats-wrapped) DateParsers for each instance
    private List<List<DateParserCoverage>> successfulCoverage;

    // General stats
    private long successCount;
    private long failureCount;
    private double coverage;
    private long overlapsCount;
    private List<Boolean> successMask;

    private boolean warnParseOverlap = false;
    private boolean warnNoParse = true;
    private int maxShownExamples = 10;

    // Dates to calculate coverage over
    private List<String> dates;

    public CoverageCalculator(List<DateParserCoverage> coverageCalcs, List<String> dates) {
        this.coverageCalcs = coverageCalcs;
        this.dates = dates;
    }

    private List<DateParserCoverage> getMatches(int maskIdx, List<DateParserCoverage> calcs) {
        List<DateParserCoverage> matches = new ArrayList<>();

        for (DateParserCoverage cc : calcs) {
            if (cc.getSuccessMask().get(maskIdx)) {
                matches.add(cc);
            }
        }
        return matches;
    }

    public List<Boolean> calculate(){

        // For each parser, calc stats for given dates
        for (DateParserCoverage pc : coverageCalcs) {
            pc.calculate(dates);
        }

        successfulCoverage = new ArrayList<>(dates.size());
        successMask = new ArrayList<>(dates.size());
        overlapsCount = 0;

        for (int i = 0; i < dates.size(); i++) {
            List<DateParserCoverage> parserMatches = getMatches(i, coverageCalcs);

            successfulCoverage.add(parserMatches);
            successMask.add(!parserMatches.isEmpty());

            if (parserMatches.size() > 1) {
                overlapsCount++;
            }
        }

        // calc stats
        successCount = successMask.stream().filter(b -> b).count();
        failureCount = successMask.size() - successCount;
        coverage = successCount / (double) successMask.size();

        return successMask;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public double getCoverage() {
        return coverage;
    }

    public long getOverlapsCount() {
        return overlapsCount;
    }

    public int numParsers() {
        return this.coverageCalcs.size();
    }


    public void setWarnParseOverlap(boolean warnParseOverlap) {
        this.warnParseOverlap = warnParseOverlap;
    }

    public void setWarnNoParse(boolean warnNoParse) {
        this.warnNoParse = warnNoParse;
    }

    public void setMaxShownExamples(int maxShown) {
        this.maxShownExamples = maxShownExamples;
    }

    /**
     * Log individual and overall coverage stats.
     */
    public void logStats() {

        for (int i = 0; i < dates.size(); i++) {

            List<DateParserCoverage> parserMatches = successfulCoverage.get(i);

            if (parserMatches.isEmpty() && this.warnNoParse) {
                LOG.warn("no parse for '" + dates.get(i) + "'");

            } else if (parserMatches.size() > 1 && this.warnParseOverlap) {
                String overlappingSpecs = parserMatches.stream()
                        .flatMap(pc -> pc.getParser().getFormatSpec().stream())
                        .map(p -> "\"" + p + "\"")
                        .collect(Collectors.joining(" OVERLAPS "));

                LOG.warn("parser overlap for '" + dates.get(i) + "' (" + overlappingSpecs + ")");
            }
        }

        for (DateParserCoverage dpc : coverageCalcs) {
            LOG.info("Parser: " + dpc.getParser().getFormatSpec());
            LOG.info("Success: {}, Failure: {}, Coverage: {}",
                    dpc.getSuccessCount(), dpc.getFailureCount(), dpc.getCoverage()
            );
            LOG.info("------------------------------------------");


            // Log some examples of successfully parsed dates
            int examplesShown = 0;

            for (int i = 0; i < dates.size(); i++) {
                if (dpc.getSuccessMask().get(i)) {
                    LOG.info(dates.get(i));

                    examplesShown++;
                    if (examplesShown >= this.maxShownExamples) {
                        break;
                    }
                }
            }
        }

        LOG.info("Summary");
        LOG.info("Success: {}, Failure: {}, Coverage: {}, Overlaps: {}, Parsers: {}",
                getSuccessCount(), getFailureCount(), getCoverage(), getOverlapsCount(), numParsers()
        );
    }

    public static void main(String... args) throws IOException {

//        List<String> examples = ImmutableList.of("Monday, 27 January 2020 4:44 PM  [ Last Update: Monday, 27 January 2020 6:24 PM ]");
//        List<String> examples = ImmutableList.of("JUBA, May 7 (Agencies) | Publish Date: 5/7/2019 12:04:09 PM IST");

        // Load examples
//        List<String> examples = CoverageUtils.loadExamplesFromLineSep(args[0]);
        List<String> examples = CoverageUtils.loadExamplesFromCsv(args[0]);

        // Wrap each `DateParser` as `DateParserCoverage`
        List<DateParserCoverage> parsers = DateParsers.ALL
                .stream()
                .map(DateParserCoverage::new)
                .collect(Collectors.toList());

        CoverageCalculator cc = new CoverageCalculator(parsers, examples);

        cc.setMaxShownExamples(10);
        cc.setWarnNoParse(true);
        cc.setWarnParseOverlap(false);

        // Parse and keep track of stats.
        cc.calculate();

        // Print
        cc.logStats();
    }
}