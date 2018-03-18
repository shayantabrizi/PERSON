/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.campos;

import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryConverter;
import ir.ac.ut.iis.person.query.QueryExpander;
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public class HRR extends BaseAlgorithm {

    public HRR(IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, QueryExpander qe) {
        super(searcher, name, similarity, queryConverter, qe);
    }

    @Override
    protected List<Query.Result> merge(TopDocs orig, TopDocs exp, int numOfResults) {
        List<Query.Result> res = new LinkedList<>();
        for (ScoreDoc e : exp.scoreDocs) {
            for (int i = 0; i < orig.scoreDocs.length; i++) {
                if (orig.scoreDocs[i] != null && e.doc == orig.scoreDocs[i].doc) {
                    res.add(new Query.Result(orig.scoreDocs[i].doc));
                    orig.scoreDocs[i] = null;
                    break;
                }
            }
        }

        for (ScoreDoc scoreDoc : orig.scoreDocs) {
            if (scoreDoc != null) {
                res.add(new Query.Result(scoreDoc.doc));
            }
        }

        return res;
    }

}
