package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class DateParsers {

    public static final DateParser dp1 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMMM d, yyyy/en",
            "ISO:/MMMM d, yyyy, HH:mm a/en",
            "ISO:/dd MMM yyyy/en",
            "ISO:/dd.MM.yyyy/en",
            "ISO:/ddF-MMM-yyyy/en",
            "ISO:/ddF-MMM-yyyy/en/ORD",
            "ISO:/dd MMM yyyy HH:mm a z/en_GB/BST/RE.*Updated: (.*)" //Published: 26 Nov 2019 01:43 AM BdST Updated: 26 Nov 2019 01:44 AM BdST
    ));


    public static final List<DateParser> all = ImmutableList.of(dp1);
}
