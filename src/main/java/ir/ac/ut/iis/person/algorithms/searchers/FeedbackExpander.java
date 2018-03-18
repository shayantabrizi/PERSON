/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.searchers;

import ir.ac.ut.iis.retrieval_tools.other.StopWordsExtractor;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.query.QueryConverter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

/**
 * `
 *
 * @author shayan
 */
public class FeedbackExpander {

    private final IndexReader ir;
    private final boolean useDocScores;
    private final double beta;
    private final double formulaC;
    private final int numOfTerms;
    private int feedbackDocumentCount = 10;

    public FeedbackExpander(IndexReader ir, boolean useDocScores, double beta, double formulaC, int numOfTerms, int feedbackDocumentCount) {
        this.ir = ir;
        this.useDocScores = useDocScores;
        this.beta = beta;
        this.formulaC = formulaC;
        this.numOfTerms = numOfTerms;
        this.feedbackDocumentCount = feedbackDocumentCount;
    }

    public String expand(TopDocs tops, Map<String, Double> query, int queryDocId) {
        Map<String, Double> map = new HashMap<>();
        int i = 0;
        int myFeedbackDocumentCount = this.feedbackDocumentCount;
        if (myFeedbackDocumentCount > tops.scoreDocs.length) {
            throw new IgnoreQueryEx();
        }
        while (myFeedbackDocumentCount > 0) {
            ScoreDoc s = tops.scoreDocs[i];
            i++;
            if (s.doc == queryDocId) {
                continue;
            }
            myFeedbackDocumentCount--;
            try {
                Terms termVector = ir.getTermVector(s.doc, "content");
                int docLen = 0;
                for (TermsEnum it = termVector.iterator();;) {
                    BytesRef next = it.next();
                    if (next == null) {
                        break;
                    }
                    docLen += it.totalTermFreq();
                }
                termVector = ir.getTermVector(s.doc, "content");
                for (TermsEnum it = termVector.iterator();;) {
                    BytesRef next = it.next();
                    if (next == null) {
                        break;
                    }
                    String st = next.utf8ToString();
                    Double get = map.get(st);
                    if (get == null) {
                        get = 0.;
                    }
                    int docFreq = DatasetMain.getInstance().getDF(st, false);
                    Double avgDL = ((double) ir.getSumTotalTermFreq("content")) / ir.numDocs();
                    Double normalizedCount = it.totalTermFreq() * Math.log(1 + formulaC * avgDL / docLen);
                    double lambdaW = ((double) docFreq) / ir.numDocs();
                    get += Math.log((normalizedCount + lambdaW) / lambdaW) * (useDocScores ? Math.pow(Math.E, s.score) : 1.);
                    map.put(st.replace(":", "\\:"), get);
                }
            } catch (IOException ex) {
                Logger.getLogger(BasicSearcher.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        }

        double max = Double.NEGATIVE_INFINITY;
        for (Double e : map.values()) {
            if (max < e) {
                max = e;
            }
        }

        double maxc = Double.NEGATIVE_INFINITY;
        for (Double e : query.values()) {
            if (maxc < e) {
                maxc = e;
            }
        }

        for (Map.Entry<String, Double> e : map.entrySet()) {
            Double c = query.get(e.getKey());
            if (c == null) {
                c = 0.;
            }
//            System.out.println(useDocScores + " " + e.getValue() / max);
            e.setValue(c / maxc + beta * e.getValue() / max);
        }

        for (Map.Entry<String, Double> e : query.entrySet()) {
            if (!map.containsKey(e.getKey())) {
                map.put(e.getKey(), e.getValue() / maxc);
            }
        }

        Map<String, Double> sortByValue = StopWordsExtractor.MapUtil.sortByValue(map);
        Map<String, Double> res = new HashMap<>();
        int k = 0;
        for (Map.Entry<String, Double> e : sortByValue.entrySet()) {
            k++;
            res.put(e.getKey(), e.getValue());
            if (k == numOfTerms) {
                break;
            }
        }
//        System.out.println(QueryConverter.rewriteQuery(res));
        return QueryConverter.rewriteQuery(res);
    }

    public String getName() {
        return useDocScores + "," + beta + "," + numOfTerms + "," + formulaC + "," + feedbackDocumentCount;
    }
}
