package com.casm.acled.crawler.scraper.keywords;

import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ScraperService;
import com.casm.acled.dao.entities.*;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import com.opencsv.CSVReader;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;

@Service
public class KeywordsService {

    protected static final Logger logger = LoggerFactory.getLogger(KeywordsService.class);

    @Autowired
    private DeskDAO deskDAO;
    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;
    @Autowired
    private SourceListDAO sourceListDAO;
    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private ScraperService scraperService;

    public boolean checkURL(SourceList sourceList, Source source, String url) {

        String article = scraperService.getText(source, url);

        String query = getKeyword(sourceList);

        boolean matched = test(query, article);

        return matched;
    }

    public void determineKeywordsList() {


        for(Desk desk : deskDAO.getAll()) {

            List<SourceList> lists = sourceListDAO.byDesk(desk.id());

            for(SourceList list : lists) {

                List<Source> sources = sourceDAO.byList(list);

                for(Source source : sources) {

                    Optional<SourceSourceList> maybeLink = sourceSourceListDAO.get(source, list);

                    if(maybeLink.isPresent()) {
                        SourceSourceList link = maybeLink.get();
                        determineKeywords(list, link, source);
                    }
                }
            }
        }

    }

    public String getKeyword(SourceList sourceList) {
        String query = sourceList.get(SourceList.KEYWORDS);

        return query;
    }

    private Set<String> determineKeywords(SourceList sourceList, SourceSourceList link, Source source) {

        Set<String> baseKeywords = sourceList.get(SourceList.KEYWORDS);
        List<String> keywordDiffs = link.get(SourceSourceList.KEYWORDS_DIFF);

        Set<String> keywords = new HashSet<>(baseKeywords);

        for(String keywordDiff : keywordDiffs) {
            String diff = keywordDiff.substring(0, 1);
            if(diff.equals("-")) {
                String keyword = keywordDiff.substring(1);
                if(!keywords.remove(keyword)) {
                    logger.warn("Attempted to remove {} when not it list - source: {} ", keyword, source.get(Source.NAME));
                }
            } else if(diff.equals("+")) {
                String keyword = keywordDiff.substring(1);
                keywords.add(keyword);
            } else {
                keywords.add(keywordDiff);
            }
        }

        return keywords;
    }



    public static void main(String[] args) throws Exception {

//        MorphologicalProcessor
        Dictionary dictionary = Dictionary.getDefaultResourceInstance();

        IndexWordSet words = dictionary.lookupAllIndexWords("kill");
        for(IndexWord word : words.getIndexWordCollection()) {

            for(Synset synset : word.getSenses()){
                for(Word w2 : synset.getWords()){
//                    dictionary.getMorphologicalProcessor()
                    System.out.println(w2);
                }
            }
            System.out.println(word);
        }

    }

    public String importFromCSV(Path path) throws IOException {

        List<String> terms = new ArrayList<>();

        try (
                Reader reader = java.nio.file.Files.newBufferedReader(path);
                CSVReader csvReader = new CSVReader(reader);
        ) {
            Iterator<String[]> itr = csvReader.iterator();

            String[] headers = itr.next();

            while(itr.hasNext()) {
                String[] row = itr.next();
                String term = row[0];

                if(term.contains(" ")) {
                    terms.add("\""+term+"\"");
                } else {
                    terms.add(term);
                }
            }
        }

        String query = "(" + StringUtils.join(terms, " ") + ")";
        return query;
    }

    public void filter(List<Article> articles, String query) {
        LuceneMatcher lm = new LuceneMatcher(query);

        List<Article> remove = new ArrayList<>();

        for(Article article : articles) {
            if(!lm.isMatched(article.get(Article.TEXT))) {
                remove.add(article);
            }
        }

        articleDAO.delete(remove);
    }

    public boolean test(String query, String text) {
        LuceneMatcher matcher = new LuceneMatcher(query);
        boolean matched = matcher.isMatched(text);
        return matched;
    }

    public SourceList assignKeywords(SourceList sourceList, String query) {

        sourceList = sourceList.put(SourceList.KEYWORDS, query);
        sourceListDAO.upsert(sourceList);
        return sourceList;
    }
}
