/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.query;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shayan
 */
public class QueryBatch implements Iterable<Query>, Comparable<QueryBatch> {

    private final String batchName;
    private final List<Query> queries;
    private Map<String, Stats> stats;

    public QueryBatch() {
        this.batchName = null;
        this.queries = null;
    }

    public QueryBatch(String batchName) {
        this.batchName = batchName;
        this.queries = new LinkedList<>();
    }

    public QueryBatch(String batchName, Query query) {
        this.batchName = batchName;
        this.queries = new LinkedList<>();
        this.queries.add(query);
    }

    public void addQuery(Query query) {
        queries.add(query);
    }

    public String getBatchName() {
        return batchName;
    }

    @Override
    public Iterator<Query> iterator() {
        return queries.iterator();
    }

    public int properQueryCount() {
        int i = 0;
        for (Query q : queries) {
            if (!q.getFoundRelevants().isEmpty()) {
                i++;
            }
        }
        return i;
    }

    public Map<String, Stats> getStats() {
        return Collections.unmodifiableMap(stats);
    }

    public void setStats(Map<String, Stats> stats) {
        this.stats = stats;
    }

    @Override
    public int compareTo(QueryBatch o) {
        return batchName.compareTo(o.getBatchName());
    }

    public static class Stats {

        public Double rr;
        public Double err;
        public Double pAt10;
        public double[] counts;
        public double[] ndcg;
        public Double ap;
        public Integer rels;

        public Stats() {
        }
    }

}
