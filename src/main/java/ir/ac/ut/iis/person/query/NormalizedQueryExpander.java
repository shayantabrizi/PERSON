/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.query;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.evaluation.Evaluator;
import java.util.Map;
import java.util.TreeMap;
import org.apache.lucene.search.IndexSearcher;

/**
 *
 * @author shayan
 */
public class NormalizedQueryExpander extends QueryExpander {

    private final double weight;
    private boolean ignoreOriginalQuery = false;

    public NormalizedQueryExpander(int k, Evaluator evaluator, double weight) {
        super(k, evaluator);
        this.weight = weight;
    }

    public NormalizedQueryExpander(IndexSearcher searcher, int k, Evaluator evaluator, double weight, boolean ignoreOriginalQuery) {
        super(k, evaluator);
        this.weight = weight;
        this.ignoreOriginalQuery = ignoreOriginalQuery;
    }
    
    @Override
    public ParsedQuery run(Query query) {
//            System.out.println("Query: " + query.getQuery());
//        System.out.println("----------");
        Map<String, Double> words;
        if (ignoreOriginalQuery) {
            words = new TreeMap<>();
        } else {
            words = queryMapConverter(query.getQuery());
        }

        return new ParsedQuery(words, rewriteQuery(convert(words, query.getSearcher(), query)));
    }

    
    @Override
    protected void addProfileWords(Map<String, WordStats> sortByValue, Map<String, Double> words) {
        int i = 0;
        Double maxWeight = 0.;
        for (Map.Entry<String, WordStats> w1 : sortByValue.entrySet()) {
            i++;
            if (i > k) {
                break;
            }
            maxWeight = Math.max(maxWeight, Configs.useTFIDFWeightingInCampos == true ? w1.getValue().tfidf : w1.getValue().idf);
        }
        i = 0;
        for (Map.Entry<String, WordStats> w1 : sortByValue.entrySet()) {
            i++;
            if (i > k) {
                break;
            }
            double termWeight = Configs.useTFIDFWeightingInCampos == true ? w1.getValue().tfidf : w1.getValue().idf;
//            System.out.println(w1.getKey() + " " + weight * termWeight / maxWeight + " " + w1.getValue().idf + " " + w1.getValue().tfidf + " " + maxWeight);
            
            addAdditiveWeight(words, w1.getKey(), weight * termWeight / maxWeight);
        }
//        System.out.println("--------------------------------------------");
    }

}
