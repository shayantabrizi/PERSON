/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author shayan
 */
public class Instance {

    private final Map<String, Double> features;
    private int claz = -1;
    private final int docId;
    private final String queryId;

    public Instance(int docId, String queryId, Map<String, Double> features) {
        this.features = features;
        this.queryId = queryId;
        this.docId = docId;

    }

    public Map<String, Double> getFeatures() {
        return Collections.unmodifiableMap(features);
    }

    public int getClaz() {
        return claz;
    }

    public void setClaz(int claz) {
        this.claz = claz;
    }

    public String toString(Map<String, Integer> featureId) {
        String s = (1 - claz) + " qid:" + queryId;

        for (Map.Entry<String, Double> e : features.entrySet()) {
            s += " " + featureId.get(e.getKey()) + ":" + e.getValue().floatValue();
        }
        return s;
    }

    public static class Instances {

        Map<InstanceKey, Instance> instances = new TreeMap<>();

        public void addInstance(Instance i) {
            instances.put(new InstanceKey(i.docId, i.queryId), i);
        }

        public void addInstances(Instances i) {
            instances.putAll(i.instances);
        }

        public Instance getInstance(int docId, String queryId) {
            return instances.get(new InstanceKey(docId, queryId));
        }

        @Override
        public String toString() {
            Map<String, Integer> featureId = new HashMap<>();
            Instance get = instances.get(0);
            int i = 0;
            for (String f : get.features.keySet()) {
                i++;
                featureId.put(f, i);
            }
            StringBuilder sb = new StringBuilder();
            for (Instance in : instances.values()) {
                sb.append(in.toString(featureId)).append("\n");
            }
            return sb.toString();
        }
    }

    private static class InstanceKey implements Comparable<InstanceKey> {

        private final int docId;
        private final String queryId;

        InstanceKey(int docId, String queryId) {
            this.docId = docId;
            this.queryId = queryId;
        }

        @Override
        public int compareTo(InstanceKey o) {
            int compareTo = o.queryId.compareTo(queryId);
            if (compareTo != 0) {
                return compareTo;
            }
            compareTo = Integer.compare(o.docId, docId);
            return compareTo;
        }

    }
}
