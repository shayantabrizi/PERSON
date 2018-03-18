/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.queries;

import static ir.ac.ut.iis.person.algorithms.queries.MyLMQuery.decodeNormValue;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.myretrieval.MyQuery;
import ir.ac.ut.iis.person.myretrieval.TermScorer;
import ir.ac.ut.iis.person.paper.TopicsReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Scorer;

/**
 *
 * @author shayan
 */
public class MyWeiQuery extends MyQuery {

    protected final IndexReader ir;
    protected TopicsReader.WordStats wordStats;
    protected final float lambda;
    protected final double[] alphas;
    protected final double beta;
    protected final float dirichletMu;
    private static int nullCount = 0;
//    private static final Map<Integer, TopicsReader.DocTopics> cache = new HashMap<>();
    private static final Map<Integer, float[]> topicsCache = new HashMap<>();
//    private final LoadingCache<String, int[]> wordStatsCache = CacheBuilder.newBuilder()
//            .maximumSize(20)
//            .build(
//                    new CacheLoader<String, int[]>() {
//                @Override
//                public int[] load(String key) {
//                    return wordStats.wordTopics.get(key);
//                }
//            });
//

    public MyWeiQuery(float dirichletMu, float lambda, IndexReader ir, TopicsReader.WordStats wordStats, double[] alphas, double beta) {
        this.ir = ir;
        this.wordStats = wordStats;
        this.lambda = lambda;
        this.alphas = alphas;
        this.beta = beta;
        this.dirichletMu = dirichletMu;
    }

    @Override
    protected float score(MyQuery.MyScorer myScorer) throws IOException {
//        TopicsReader.DocTopics docTopicAssignments;
        float[] docTopics;
//        docTopicAssignments = cache.get(myScorer.docID() + myScorer.context.docBase);
        docTopics = topicsCache.get(myScorer.docID() + myScorer.context.docBase);
        if (docTopics == null) {
            try {
                final Document document = myScorer.context.reader().document(myScorer.docID());
//                docTopicAssignments = getDocTopicAssignments(document);
                docTopics = getDocTopics(document);
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
//            cache.put(myScorer.docID() + myScorer.context.docBase, docTopicAssignments);
            topicsCache.put(myScorer.docID() + myScorer.context.docBase, docTopics);
        }

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
        i = 0;
        for (Scorer scorer : myScorer.scorers) {
            int[] n_w;
            float[] p_w_z;
//            n_w = wordStats.wordCounts.get(((TermScorer) scorer).getTerm().bytes());
            p_w_z = wordStats.wordTopics.get(((TermScorer) scorer).getTerm().bytes());

            if (p_w_z == null) {
                nullCount++;
                System.out.println("NullCount: " + nullCount + " " + ((TermScorer) scorer).getTerm().text());
                throw new IgnoreQueryEx();
            }

            float weiScore;
            weiScore = weiScore(p_w_z, docTopics);

            float temp;
            temp = (freqs[i] + (dirichletMu * myScorer.collectionProbabilities[i])) / (docLen + dirichletMu);
            score += myScorer.boosts[i] * Math.log(lambda * temp + (1 - lambda) * weiScore);
            i++;
        }
        addFeature("MyWeiScore", Double.valueOf(score));

        return combineWithDocScore(myScorer.context, myScorer.docID(), score);
    }

    protected float combineWithDocScore(LeafReaderContext context, int docId, float score) {
        return score;
    }

    private float weiScore(float[] p_w_z, float[] p_z_d) {
        final int numOfTopics = p_w_z.length;
        float score2 = 0;
        for (int j = 0; j < numOfTopics; j++) {
//            score += ((n_w[j] + beta) / (n_all[j] + beta * sizeOfVocab)) * ((topicsAssignment.topicCounts[j] + alphas[j]) / (topicsAssignment.docLength + alphas[j] * numOfTopics));
            score2 += p_w_z[j] * p_z_d[j];
        }
        return score2;
    }

    private static TopicsReader.DocTopics getDocTopicAssignments(final Document document) throws IOException, ClassNotFoundException {
        TopicsReader.DocTopics topics;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(document.getBinaryValue("topicAssignments").bytes))) {
            topics = (TopicsReader.DocTopics) in.readObject();
        }
        return topics;
    }

    private static float[] getDocTopics(final Document document) throws IOException, ClassNotFoundException {
        float[] topics;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(document.getBinaryValue("topics").bytes))) {
            topics = (float[]) in.readObject();
        }
        return topics;
    }

}
