/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import ir.ac.ut.iis.person.Configs;

/**
 *
 * @author shayan
 */
public abstract class PPRCalculator {

    protected final int topicNodeId;

    public PPRCalculator(int topicNodeId) {
        this.topicNodeId = topicNodeId;
    }

    public int getTopicNodeId() {
        return topicNodeId;
    }

    protected abstract void updatePPRs(float[] zeroDegrees, double alpha);

    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    public float[] PPR(int numOfWeights, GraphNode node, Iterable<GraphNode> parent, int parentSize, short level, double alpha) {
        float[] get = node.getPPR(this);
        if (get != null) {
            return get;
        }
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
            u.addPPR(this, arr);
            u.setTmpPPR(tmp);
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
                float[] ppr = u.getPPR(this);
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
                    //                    if (otherSide.getId().equals("152068") && id == 1) {
                    //                        System.out.println("");
                    //                    }
                    for (int i = 0; i < numOfWeights; i++) {
                        otherSide.getTmpPPR()[i] = (float) (otherSide.getTmpPPR()[i] + e.getWeight()[i] / degree[i] * ppr[i] * (1 - alpha));
                    }
                }
            }
            updatePPRs(zeroDegrees, alpha);
            for (int i = 0; i < numOfWeights; i++) {
                diff[i] = 0;
            }
            //            double sum = 0;
            for (GraphNode u : parent) {
                final float[] ppr = u.getPPR(this);
                for (int i = 0; i < numOfWeights; i++) {
                    float abs = Math.abs(u.getTmpPPR()[i] - ppr[i]);
                    diff[i] = Math.max(diff[i], abs);
                    ppr[i] = u.getTmpPPR()[i];
                    //                    sum += u.getTmpPPR()[i];
                    u.getTmpPPR()[i] = 0.f;
                }
            }
            //            System.out.println(sum);
        }
        System.out.println("PPR: " + this);
        return node.getPPR(this);
    }

}
