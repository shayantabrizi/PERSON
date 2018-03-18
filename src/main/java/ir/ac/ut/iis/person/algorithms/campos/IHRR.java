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
public class IHRR extends BaseAlgorithm {

    public IHRR(IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, QueryExpander qe) {
        super(searcher, name, similarity, queryConverter, qe);
    }

    @Override
    protected List<Query.Result> merge(TopDocs orig, TopDocs exp, int numOfResults) {
        List<Query.Result> res = new LinkedList<>();
        for (ScoreDoc e : orig.scoreDocs) {
            for (int i = 0; i < exp.scoreDocs.length; i++) {
                if (exp.scoreDocs[i] != null && e.doc == exp.scoreDocs[i].doc) {
                    res.add(new Query.Result(exp.scoreDocs[i].doc));
                    exp.scoreDocs[i] = null;
                    break;
                }
            }
        }

        for (ScoreDoc scoreDoc : exp.scoreDocs) {
            if (scoreDoc != null) {
                res.add(new Query.Result(scoreDoc.doc));
            }
        }

        return res;
    }

}
