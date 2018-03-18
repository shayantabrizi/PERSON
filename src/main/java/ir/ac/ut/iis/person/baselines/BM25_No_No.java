/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.baselines;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 *
 * @author shayan
 */
public class BM25_No_No extends ClassicSimilarity {

    private final float avgdl;
    private final float k1;
    private final float b;
    private final NumericDocValues norms;

    public BM25_No_No(float avgdl, float k1, float b, NumericDocValues norms) {
        this.avgdl = avgdl;
        this.k1 = k1;
        this.b = b;
        this.norms = norms;
    }

    @Override
    public float tf(float freq) {
        float x = freq + k1 * (1 - b + b * avgdl / avgdl);
        return (k1 * freq / x);
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float) 1.0;
    }

}
