/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.query;

import ir.ac.ut.iis.person.evaluation.Evaluator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shayan
 */
public class QueryExpander extends QueryConverter {

    Map<String, List<String>> map = new HashMap<>();
    protected final int k;
    private final Evaluator evaluator;

    public QueryExpander(int k, Evaluator evaluator) {
        super();
        this.k = k;
        this.evaluator = evaluator;
    }

    @Override
    public Map<String, Double> convert(Map<String, Double> words, int userId, Query query) {
        words = super.convert(words, userId, query);
        Map<String, WordStats> profile = evaluator.getProfile(userId, query, k);
        addProfileWords(profile, words);

        return words;
    }


    protected void addProfileWords(Map<String, WordStats> sortByValue, Map<String, Double> words) {
        int i = 0;
        if (sortByValue == null) {
            System.out.println("");
        }
        for (Map.Entry<String, WordStats> w1 : sortByValue.entrySet()) {
            i++;
            if (i > k) {
                break;
            }
            addAdditiveWeight(words, w1.getKey(), 1.);
        }
    }

    public static class WordStats implements Comparable<WordStats> {

        public double tfidf;
        public double idf;

        public WordStats(double tfidf, double idf) {
            this.tfidf = tfidf;
            this.idf = idf;
        }

        @Override
        public int compareTo(WordStats o) {
            return Double.compare(tfidf, o.tfidf);
        }

    }

    public static class Profile {

        public int searcher;
        public Integer queryIndexId;
        public String profile;

        public Profile(int searcher, Integer queryIndexId, String profile) {
            this.searcher = searcher;
            this.queryIndexId = queryIndexId;
            this.profile = profile;
        }

    }
}
