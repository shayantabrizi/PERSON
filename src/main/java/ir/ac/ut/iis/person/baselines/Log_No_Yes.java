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
public class Log_No_Yes extends ClassicSimilarity {

    @Override
    public float tf(float freq) {
        return (float) Math.log(freq + 1);
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float) 1.0;
    }

}
