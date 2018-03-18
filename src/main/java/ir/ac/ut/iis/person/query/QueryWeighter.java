/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.query;

import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import ir.ac.ut.iis.person.hierarchy.HierarchyNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.lucene.index.Term;

/**
 *
 * @author shayan
 */
public class QueryWeighter extends QueryConverter {

    protected Hierarchy hier;
    private final double lambda;

    public QueryWeighter(Hierarchy hier, double lambda) {
        super();
        this.hier = hier;
        this.lambda = lambda;
    }

    @Override
    public Map<String, Double> convert(Map<String, Double> words, int userId, Query query) {
        if (getParentConverter() != null) {
            words = getParentConverter().convert(words, userId, query);
        }
        Map<String, Double> weighted = new TreeMap<>();
        for (Map.Entry<String, Double> e : words.entrySet()) {
            Term t = new Term("content", e.getKey());
            double calculateCollectionProb = calculateCollectionProb(t, userId);
            addAdditiveWeight(weighted, e.getKey(), Math.sqrt(calculateCollectionProb) * e.getValue());
        }
        return weighted;
    }

    protected double calculateCollectionProb(Term currentTerm, int userId) {
        try {
            double finalScore = .5;
            List<HierarchyNode> list = hier.getHierarchyNodesParentFirst(userId);
            for (HierarchyNode n : list) {
                double value = Math.pow(calcProb(n, hier.getRootNode(), currentTerm), 8);
                finalScore = (1 - lambda) * finalScore + lambda * value;
            }
            return finalScore;
        } catch (IOException ex) {
            Logger.getLogger(QueryWeighter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    protected double calcProb(HierarchyNode currentNode, HierarchyNode rootNode, Term currentTerm) throws IOException {

        final double freq = currentNode.getFreq(currentTerm);
        final double totalFreq = currentNode.getTotalFreq();
        double p1 = freq / totalFreq;
        double p2 = (rootNode.getFreq(currentTerm) - freq) / (rootNode.getTotalFreq() - totalFreq);

        double p = rootNode.getFreq(currentTerm) / rootNode.getTotalFreq();
        double z = (p1 - p2) / Math.sqrt(p * (1 - p) * ((1. / (rootNode.getTotalFreq() - totalFreq)) + (1. / totalFreq)));
        NormalDistribution dist = new NormalDistribution();
        return 1 - dist.cumulativeProbability(-z);
    }

    public static void main(String[] args) {

        double c1 = 7;
        double k1 = 50;
        double c2 = 10;
        double k2 = 100;
        double p1 = c1 / k1;
        double p2 = c2 / k2;

        double p = (c1 + c2) / (k1 + k2);
        double z = (p1 - p2) / Math.sqrt(p * (1 - p) * (1. / (k1) + (1. / k2)));
        NormalDistribution dist = new NormalDistribution();
        System.out.println(1. - dist.cumulativeProbability(-z));

    }
}
