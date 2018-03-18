/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.query.QueryConverter;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public class AggregateSearcher extends BasicSearcher {

    private final float personalizationWeight;
    private final float textualWeight;
    private final MyValueSource vs;

    public AggregateSearcher(MyValueSource vs, IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, float personalizationWeight, float textualWeight) {
        super(searcher, name, similarity, queryConverter);
        this.personalizationWeight = personalizationWeight;
        this.textualWeight = textualWeight;
        this.vs = vs;
    }

    @Override
    public Query createQuery(String convertedQuery, ir.ac.ut.iis.person.query.Query query, Integer year) throws QueryNodeException {
        vs.initialize(query);
        Query createQuery = super.createQuery(convertedQuery, query, year);
        FunctionQuery functionQuery = new FunctionQuery(vs);
        MyCustomScoreQuery customQuery = new MyCustomScoreQuery(createQuery, functionQuery, personalizationWeight, textualWeight);
        return customQuery;
    }
}
