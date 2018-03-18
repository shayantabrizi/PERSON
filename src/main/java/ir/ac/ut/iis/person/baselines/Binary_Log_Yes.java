/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.baselines;

import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 *
 * @author shayan
 */
public class Binary_Log_Yes extends ClassicSimilarity {

    @Override
    public float tf(float freq) {
        return freq > .01 ? 1 : 0;
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return super.idf(docFreq, numDocs);
    }

}
