/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.campos;

import ir.ac.ut.iis.retrieval_tools.other.StopWordsExtractor;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryConverter;
import ir.ac.ut.iis.person.query.QueryExpander;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public class IRR extends BaseAlgorithm {

    public IRR(IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, QueryExpander qe) {
        super(searcher, name, similarity, queryConverter, qe);
    }

    @Override
    protected List<Query.Result> merge(TopDocs orig, TopDocs exp, int numOfResults) {
        List<Query.Result> res = new LinkedList<>();
        float maxOrig = orig.scoreDocs[0].score;
        float maxExp = exp.scoreDocs[0].score;
        Map<Integer, Float> map = new TreeMap<>();
        for (ScoreDoc e : exp.scoreDocs) {
            boolean check = false;
            for (int i = 0; i < orig.scoreDocs.length; i++) {
                if (orig.scoreDocs[i] != null && e.doc == orig.scoreDocs[i].doc) {
                    orig.scoreDocs[i].score = orig.scoreDocs[i].score / maxOrig + e.score / maxExp;
                    map.put(orig.scoreDocs[i].doc, orig.scoreDocs[i].score);
                    orig.scoreDocs[i] = null;
                    check = true;
                    break;
                }
            }
            if (check == false) {
                map.put(e.doc, e.score / maxExp);
            }
        }

        for (ScoreDoc scoreDoc : orig.scoreDocs) {
            if (scoreDoc != null) {
                map.put(scoreDoc.doc, scoreDoc.score / maxOrig);
            }
        }

        Map<Integer, Float> sortByValue = StopWordsExtractor.MapUtil.sortByValue(map);
        int i = 0;
        for (Map.Entry<Integer, Float> e : sortByValue.entrySet()) {
            i++;
            res.add(new Query.Result(e.getKey()));
            if (i == numOfResults) {
                break;
            }
        }

        return res;
    }

}
