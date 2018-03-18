/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.evaluation;

import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryExpander;
import java.util.Map;
import org.apache.lucene.search.IndexSearcher;

/**
 *
 * @author shayan
 */
public interface Evaluator {

    void setBaselineSearcher(BasicSearcher baselineSearcher);
    void setSearcher(IndexSearcher searcher);
    String getName();
    Query convertToQuery(String queryDocId, Integer queryIndexId, String query, String searcher, int year, String authorsS);
    Map<String, QueryExpander.WordStats> getProfile(int userId, Query query, int k);
    boolean ignoreUnfoundRelevants();
}
