/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.baselines;

/**
 *
 * @author shayan
 */
public class Binary_No_No extends MyClassicSimilarity {

    @Override
    public float tf(float freq) {
        return freq > .01 ? 1 : 0;
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float) 1.0;
    }

}
