/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.base.Threads;

/**
 *
 * @author shayan
 */
public abstract class PPRCalculator implements MeasureCalculator {

    protected final int topicNodeId;
    protected final double alpha;

    public PPRCalculator(int topicNodeId, double alpha) {
        this.topicNodeId = topicNodeId;
        this.alpha = alpha;
    }

    @Override
    public int getSeedsId() {
        return topicNodeId;
    }

    protected abstract void updatePPRs(float[] zeroDegrees, double alpha);

    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public float[] calc(int numOfWeights, GraphNode node, Iterable<GraphNode> parent, int parentSize, short level) {
        float[] get = node.getMeasure(this);
        if (get != null) {
            return get;
        }
        float[] measure;
        Hierarchy hier = node.getHierarchyNode().getHierarchy();
        Threads.enterSafeArea();
        synchronized (hier) {
            if (parentSize > 400_000) {
                synchronized (PPRCalculator.class) {
                    measure = doCalculate(parent, numOfWeights, parentSize, level, node);
                }
            } else {
                measure = doCalculate(parent, numOfWeights, parentSize, level, node);
            }
        }
        Threads.enterUnsafeArea();

        return measure;
    }

    private float[] doCalculate(Iterable<GraphNode> parent, int numOfWeights, int parentSize, short level, GraphNode node) throws RuntimeException {
        if (Configs.useCachedPPRs) {
            /*
            float[] pprFromDB = HierarchyNode.getUserPPRFromDB(HierarchyNode.id, numOfWeights, node);
            if (pprFromDB != null) {
            node.addPPR(this, pprFromDB);
            return pprFromDB;
            }
             */
        }
        //        double defaultVal;
        //        if (parent.size() > 10 * topic.size()) {
        //            defaultVal = .01 / (parent.size() - size);
        //        } else {
        //            defaultVal =
        //        }
        for (GraphNode u : parent) {
            float[] arr = new float[numOfWeights];
            float[] tmp = new float[numOfWeights];
            for (int i = 0; i < numOfWeights; i++) {
                arr[i] = 1.f / parentSize;
                tmp[i] = 0;
            }
            u.addMeasure(this, arr);
            u.setTmpArray(tmp);
        }
        //        for (User u : topic) {
        //            u.addPPR(id, .99 / size);
        //            u.setTmpPPR(0.);
        //        }
        float[] diff = new float[numOfWeights];
        for (int i = 0; i < numOfWeights; i++) {
            diff[i] = 1.f;
        }
        float threshold = 1.f / parentSize / 1_000;
        boolean[] check = new boolean[numOfWeights];
        while (true) {
            boolean ch = true;
            for (int i = 0; i < numOfWeights; i++) {
                if (diff[i] < threshold) {
                    check[i] = true;
                }
                if (check[i] == false) {
                    ch = false;
                }
            }
            if (ch) {
                break;
            }
            float[] zeroDegrees = new float[numOfWeights];
            for (GraphNode u : parent) {
                float[] degree = u.getDegree(level);
                float[] ppr = u.getMeasure(this);
                for (int i = 0; i < numOfWeights; i++) {
                    if (degree[i] == 0.) {
                        zeroDegrees[i] += ppr[i];
                    }
                }
                for (GraphNode.HierarchicalEdge e : u.getEdges()) {
                    if (e.hierarchyThreshold < level) {
                        break;
                    }
                    GraphNode otherSide = e.getOtherSide(u);
                    for (int i = 0; i < numOfWeights; i++) {
                        otherSide.getTmpArray()[i] = (float) (otherSide.getTmpArray()[i] + e.getWeight()[i] / degree[i] * ppr[i] * (1 - alpha));
                    }
                }
            }
            updatePPRs(zeroDegrees, alpha);
            for (int i = 0; i < numOfWeights; i++) {
                diff[i] = 0;
            }
            double sum = 0;
            for (GraphNode u : parent) {
                final float[] ppr = u.getMeasure(this);
                for (int i = 0; i < numOfWeights; i++) {
                    float abs = Math.abs(u.getTmpArray()[i] - ppr[i]);
                    diff[i] = Math.max(diff[i], abs);
                    ppr[i] = u.getTmpArray()[i];
                    sum += u.getTmpArray()[i];
                    u.getTmpArray()[i] = 0.f;
                }
            }
            if (Math.abs(sum - numOfWeights) > 1e-2) {
                throw new RuntimeException(String.valueOf(sum));
            }
        }
        for (GraphNode u : parent) {
            u.setTmpArray(null);
        }
        System.out.println("PPR: " + this);
        return node.getMeasure(this);
    }

}
