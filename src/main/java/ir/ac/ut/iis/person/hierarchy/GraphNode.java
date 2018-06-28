/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import ir.ac.ut.iis.retrieval_tools.domain.Edge;
import ir.ac.ut.iis.retrieval_tools.domain.Node;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author shayan
 */
public class GraphNode extends Node<GraphNode, GraphNode.HierarchicalEdge, User> {

    private final ObjectArrayList<HierarchicalEdge> edges = new ObjectArrayList<>();
    private final Object2ObjectOpenHashMap<MeasureCalculator, float[]> measure = new Object2ObjectOpenHashMap<>(1, .75f);
    private float[][] degree;
    private final HierarchyNode hierarchyNode;
//    private Object processingDummy;

    public GraphNode(User id, HierarchyNode hierarchyNode) {
        super(id);
        this.hierarchyNode = hierarchyNode;
    }

    @Override
    public void addEdge(HierarchicalEdge edge) {
        edges.add(edge);
    }

    @Override
    public List<HierarchicalEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public void optimize() {
        Collections.sort(edges, new Comparator<HierarchicalEdge>() {

            @Override
            public int compare(HierarchicalEdge o1, HierarchicalEdge o2) {
                return Integer.compare(o2.hierarchyThreshold, o1.hierarchyThreshold);
            }
        });
        int maxThreshold = 0;
        int weightsSize;
        for (HierarchicalEdge e : edges) {
            if (e.hierarchyThreshold > maxThreshold) {
                maxThreshold = e.hierarchyThreshold;
            }
        }
        if (!edges.isEmpty()) {
            weightsSize = edges.iterator().next().getWeight().length;
            degree = new float[maxThreshold + 1][weightsSize];
            for (HierarchicalEdge e : edges) {
                for (int i = 0; i <= e.hierarchyThreshold; i++) {
                    for (int j = 0; j < weightsSize; j++) {
                        degree[i][j] += e.getWeight()[j];
                    }
                }
            }
            edges.trim();
        }
    }

    public float[] getTmpPPR() {
        return getId().getTmpArray();
    }

    public void setTmpPPR(float[] tmpPPR) {
        getId().setTmpArray(tmpPPR);
    }

    public float[] getDegree(short level) {
        if (degree == null) {
            return new float[hierarchyNode.getNumberOfWeights()];
        }
        final float[] get = degree[level];
        if (get == null) {
            throw new RuntimeException();
//            return new float[Main.numOfTopics];
        }
        return get;
    }

    public Object2ObjectOpenHashMap<MeasureCalculator, float[]> getMeasure() {
        return measure;
    }

    public float[] getMeasure(MeasureCalculator clusterId) {
        return measure.get(clusterId);
    }

    public void addMeasure(MeasureCalculator clusterId, float[] measure) {
        this.measure.put(clusterId, measure);
    }
    
    public void resetMeasure() {
        this.measure.clear();
    }    

//    public Object getProcessingDummy() {
//        return processingDummy;
//    }
//
//    public void setProcessingDummy(Object processingDummy) {
//        this.processingDummy = processingDummy;
//    }
    public static class HierarchicalEdge extends Edge<GraphNode> {

        public short hierarchyThreshold;

        public HierarchicalEdge(GraphNode src, GraphNode dst) {
            super(src, dst);
        }

        public HierarchicalEdge(GraphNode src, GraphNode dst, float[] weight) {
            super(src, dst, weight);
        }
    }

    public HierarchyNode getHierarchyNode() {
        return hierarchyNode;
    }

}
