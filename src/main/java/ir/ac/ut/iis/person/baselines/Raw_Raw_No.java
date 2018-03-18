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
public class Raw_Raw_No extends ClassicSimilarity {

    @Override
    public float tf(float freq) {
        return freq;
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float) ((numDocs / (double) (docFreq + 1)) + 1.0);
    }

}
