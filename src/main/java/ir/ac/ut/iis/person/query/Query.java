/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.query;

import ir.ac.ut.iis.person.base.Instance;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author shayan
 */
public class Query implements Comparable<Query> {

    private final String queryId;
    private final Integer queryIndexId;
    private final String query;
    private final Integer searcher;
    private final Integer year;
    private final Map<String, Double> relevants;
//        private final Set<String> foundRelevants = new TreeSet<>();
    private final Map<String, List<Result>> results = new TreeMap<>();
    private boolean ignored = true;
    private Set<Result> foundRelevantsCache;
    private final Set<String> authors;
    private Set<String> ignoredResults;

    public Query(String queryId, Integer queryIndexId, String query, Integer searcher, Integer year, Map<String, Double> relevants, Set<String> authors, Set<String> ignoredResults) {
        this.queryId = queryId;
        this.queryIndexId = queryIndexId;
        this.query = query;
        this.searcher = searcher;
        this.year = year;
        this.relevants = relevants;
        this.authors = authors;
        this.ignoredResults = ignoredResults;
    }

    public Set<String> getIgnoredResults() {
        if (ignoredResults == null) {
            return null;
        } else {
            return Collections.unmodifiableSet(ignoredResults);
        }
    }

    public Integer getQueryIndexId() {
        return queryIndexId;
    }

    public void addResult(String searcherName, List<Result> list) {
        results.put(searcherName, list);
    }

    public Map<String, Double> getRelevants() {
        return Collections.unmodifiableMap(relevants);
    }

    public Map<String, List<Result>> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public String getQueryId() {
        return queryId;
    }

    public String getQuery() {
        return query;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public int getSearcher() {
        return searcher;
    }

    public Set<String> getAuthors() {
        return Collections.unmodifiableSet(authors);
    }

    public Integer getYear() {
        return year;
    }

    public Set<Result> getFoundRelevants() {
        if (foundRelevantsCache == null) {
            foundRelevantsCache = new TreeSet<>();
            for (List<Result> list : results.values()) {
                for (Result r : list) {
                    if (!r.getRelevancy().equals(0.)) {
                        foundRelevantsCache.add(r);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(foundRelevantsCache);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Query other = (Query) obj;
        if (!Objects.equals(this.queryId, other.queryId)) {
            return false;
        }
        if (!Objects.equals(this.searcher, other.searcher)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Query o) {
        int c1 = queryId.compareTo(o.getQueryId());
        if (c1 != 0) {
            return c1;
        }
        int c2 = Integer.compare(searcher, o.getSearcher());
        return c2;
    }

    public static class Result implements Comparable<Result> {

        final int docId;
        final Instance instance;
        Double relevancy = null;

        public Result(int docId) {
            this.docId = docId;
            this.instance = null;
        }

        public Result(int docId, Instance instance) {
            this.docId = docId;
            this.instance = instance;
        }

        public void setRelevancy(Double relevancy) {
            this.relevancy = relevancy;
        }

        public int getDocId() {
            return docId;
        }

        public Double getRelevancy() {
            return relevancy;
        }

        public Instance getInstance() {
            return instance;
        }

        @Override
        public int compareTo(Result o) {
            return Integer.compare(docId, o.getDocId());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Result other = (Result) obj;
            return this.docId == other.docId;
        }

    }

}
