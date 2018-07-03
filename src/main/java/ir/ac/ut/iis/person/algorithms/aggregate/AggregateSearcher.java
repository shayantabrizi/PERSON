/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.query.QueryConverter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public class AggregateSearcher extends BasicSearcher {

    private final float personalizationWeight;
    private final float textualWeight;
    private final boolean powTextual;
    private final boolean normalizeTextualWeight;

    private final MyValueSource vs;

    public AggregateSearcher(MyValueSource vs, IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, float personalizationWeight, float textualWeight) {
        this(vs, searcher, name, similarity, queryConverter, personalizationWeight, textualWeight, false, false);
    }

    public AggregateSearcher(MyValueSource vs, IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, float personalizationWeight, float textualWeight, boolean powTextual, boolean normalizeTextualWeight) {
        super(searcher, name + "-(" + ((personalizationWeight + textualWeight == 1.f) ? personalizationWeight : personalizationWeight + "," + textualWeight)
                + (powTextual ? ",PW" : "") + (normalizeTextualWeight ? ",NT" : "") + ")",
                similarity, queryConverter);
        this.personalizationWeight = personalizationWeight;
        this.textualWeight = textualWeight;
        this.vs = vs;
        this.powTextual = powTextual;
        this.normalizeTextualWeight = normalizeTextualWeight;
    }

    @Override
    public Query createQuery(String convertedQuery, ir.ac.ut.iis.person.query.Query query, Integer year) throws QueryNodeException {
        vs.initialize(query);
        Query createQuery = super.createQuery(convertedQuery, query, year);
        float maxWeight = 0f;
        if (normalizeTextualWeight) {
            try {
                TopDocs tops = searcher.search(createQuery, 1);
                maxWeight = tops.scoreDocs[0].score;
            } catch (IOException ex) {
                Logger.getLogger(AggregateSearcher.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        }
        FunctionQuery functionQuery = new FunctionQuery(vs);
        MyCustomScoreQuery customQuery = new MyCustomScoreQuery(createQuery, functionQuery, personalizationWeight, textualWeight, powTextual, normalizeTextualWeight ? 1 / maxWeight : 0f);
        return customQuery;
    }
}
