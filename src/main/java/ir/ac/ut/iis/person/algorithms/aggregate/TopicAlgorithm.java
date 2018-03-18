/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.base.Statistic;
import ir.ac.ut.iis.person.query.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shayan
 */
public abstract class TopicAlgorithm extends Statistic {

    private final float weight;
    private final String name;
    private Query queryCache;
    private final Map<Integer, Float> cache = new HashMap<>();

    public TopicAlgorithm(String name, float weight) {
        super(name);
        this.name = name;
        this.weight = weight;
    }

    public void initialize(Query query) {
        if (!query.equals(queryCache)) {
            cache.clear();
            super.initialize();
        }
    }

    public String getName() {
        return name;
    }

    public float getWeight() {
        return weight;
    }

    protected float getSimilarity(int docId, float[] authorTopics, float[] searcherTopics, float[] queryTopics, float[] documentTopics, List<Integer> topQueryTopics) {
        Float get = cache.get(docId);
        if (get == null) {
            get = calcSimilarity(authorTopics, searcherTopics, queryTopics, documentTopics, topQueryTopics);
        }
        add(get);
        return get;
    }

    protected abstract float calcSimilarity(float[] authorTopics, float[] searcherTopics, float[] queryTopics, float[] documentTopics, List<Integer> topQueryTopics);

    protected float cosineSimilarity(float[] d1Array, float[] d2Array) {
        double product = 0;
        double d1Norm = 0;
        double d2Norm = 0;
        for (int i = 0; i < d1Array.length; i++) {
            product += d1Array[i] * d2Array[i];
            d1Norm += Math.pow(d1Array[i], 2);
            d2Norm += Math.pow(d2Array[i], 2);
        }

        return (float) (product / Math.sqrt(d1Norm) / Math.sqrt(d2Norm));
    }

    protected float myCosineSimilarity(float alpha, float[] weightArray, float[] d1Array, float[] d2Array) {
        float[] weights = new float[weightArray.length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (1 - alpha) / weightArray.length + alpha * weightArray[i];
//            weights[i] = ((1 - alpha) / weightArray.length + alpha * weightArray[i]) * 100;
        }
        double product = 0;
        double d1Norm = 0;
        double d2Norm = 0;
        for (int i = 0; i < d1Array.length; i++) {
            if (Configs.myCosineWeightType) {
                product += (d1Array[i] * weights[i]) * (d2Array[i] * weights[i]);
                d1Norm += Math.pow(d1Array[i] * weights[i], 2);
                d2Norm += Math.pow(d2Array[i] * weights[i], 2);
            } else {
                product += weights[i] * d1Array[i] * d2Array[i];
                d1Norm += Math.pow(d1Array[i], 2);
                d2Norm += Math.pow(d2Array[i], 2);
            }
        }

        return (float) (product / Math.sqrt(d1Norm) / Math.sqrt(d2Norm));
    }

    protected float mySimilarity(List<Integer> indices, float[] d1Array, float[] d2Array) {
        double sim = 0;
        for (Integer i : indices) {
            sim += d1Array[i] / (1 - d2Array[i] + 1);
//            sim += Math.pow(d1Array[i] * d2Array[i], .5);
        }
        return (float) sim;
    }

    protected float sumSelectTopics(List<Integer> indices, float[] d1Array) {
        double sim = 0;
        for (Integer i : indices) {
            sim += d1Array[i];
//            sim += Math.pow(d1Array[i] * d2Array[i], .5);
        }
        return (float) sim;
    }

    protected float klDivergence(float[] d1Array, float[] d2Array) {
        double eps = 0.01;
        double r = 0;
        for (int i = 0; i < d1Array.length; i++) {
            double d1 = d1Array[i];
            double d2 = d2Array[i];

            double inc = ((d1 + eps) * Math.log((d1 + eps) / (d2 + eps)));
            r += inc;
        }
        return (float) r;
    }

    protected float jsDivergence(float[] doc1, float[] doc2) {
        float[] avg = new float[doc1.length];
        for (int i = 0; i < doc1.length; i++) {
            avg[i] = (doc1[i] + doc2[i]) / 2;
        }

        double kl1;
        double kl2;
        kl1 = klDivergence(doc1, avg);
        kl2 = klDivergence(doc2, avg);

        return (float) ((kl1 + kl2) / 2);
    }

}
