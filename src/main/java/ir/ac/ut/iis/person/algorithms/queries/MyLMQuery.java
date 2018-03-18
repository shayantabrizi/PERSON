/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.queries;

import ir.ac.ut.iis.person.myretrieval.MyQuery;
import ir.ac.ut.iis.person.myretrieval.TermScorer;
import java.io.IOException;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.SmallFloat;

/**
 *
 * @author shayan
 */
public class MyLMQuery extends MyQuery {

    protected final double dirichletMu;

    public MyLMQuery(double dirichletMu) {
        this.dirichletMu = dirichletMu;
    }

    @Override
    protected float score(MyQuery.MyScorer myScorer) throws IOException {
        int[] freqs = new int[myScorer.scorers.size()];
        int i = 0;
        float docLen = 0;

        for (Scorer s : myScorer.scorers) {
            TermScorer ts = (TermScorer) s;
            if (s.docID() == myScorer.docID()) {
                docLen = decodeNormValue(Byte.toUnsignedInt((byte) ts.getDocLen()));
                freqs[i] = ts.freq();
            } else {
                freqs[i] = 0;
            }
//                collectionProbabilities[i] = ts.getCollectionProbability();
//                System.out.println(((TermScorer) s).getTerm().text() + " " + freqs[i] + " " + collectionProbabilities[i] + " " + docLen);
            i++;
        }

        float score = 0;
        for (i = 0; i < myScorer.scorers.size(); i++) {
//            if (freqs[i] > 0) {
            double v = myScorer.boosts[i] * Math.log((freqs[i] + (dirichletMu * myScorer.collectionProbabilities[i])) / (docLen + dirichletMu));

//              Identical to lucene implementation:
//                float v = myScorer.boosts[i] * (float) (Math.log(1 + freqs[i]
//                        / (dirichletMu * myScorer.collectionProbabilities[i]))
//                        + Math.log(dirichletMu / (docLen + dirichletMu)));
//
//                if (v < 0) {
//                    v = 0;
//                }
            score += v;
//            }
        }
        addFeature("MyLMScore", Double.valueOf(score));
//        if (myScorer.context.docBase + myScorer.docID() == 643888) {
//        System.out.println((myScorer.context.docBase + myScorer.docID()) + " " + score);
//        }

        return score;
    }
    private static final float[] NORM_TABLE = new float[256];

    static {
        for (int i = 0; i < 256; i++) {
            NORM_TABLE[i] = SmallFloat.byte4ToInt((byte) i);
        }
    }

    public static float decodeNormValue(int norm) {
        return NORM_TABLE[norm];
    }

}
