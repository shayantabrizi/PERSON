/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.campos;

import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryConverter;
import ir.ac.ut.iis.person.query.QueryExpander;
import java.util.List;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public abstract class BaseAlgorithm extends BasicSearcher {

    private final QueryExpander qe;

    public BaseAlgorithm(IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, QueryExpander qe) {
        super(searcher, name, similarity, queryConverter);
        this.qe = qe;
    }

    @Override
    public List<Query.Result> doSearch(Query q, int numOfResults) {
        QueryConverter.ParsedQuery convertedQuery = getQueryConverter().run(q);

        TopDocs orig = super.doSearch(convertedQuery, q, numOfResults).td;

        QueryConverter.ParsedQuery expandedQuery = qe.run(q);

        TopDocs exp = super.doSearch(expandedQuery, q, numOfResults).td;

        List<Query.Result> merge = merge(orig, exp, numOfResults);
        q.addResult(getName(), merge);
        return merge;
    }

    protected abstract List<Query.Result> merge(TopDocs orig, TopDocs exp, int numOfResults);
}
