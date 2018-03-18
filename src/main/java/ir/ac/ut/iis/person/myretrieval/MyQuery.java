package ir.ac.ut.iis.person.myretrieval;

import com.google.common.base.Objects;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.base.Instance;
import ir.ac.ut.iis.person.base.Instance.Instances;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public abstract class MyQuery extends Query {

    private final List<TermQuery> termQueries = new LinkedList<>();
    private PointRangeQuery yearFilter;
    private Query ignoreQuery;
    private Instances instances = null;
    private ir.ac.ut.iis.person.query.Query query;
    private Map<String, Double> features;

    public MyQuery() {
    }

    public void startCollectingFeatures() {
        instances = new Instances();
        features = new HashMap<>();
    }

    protected void addFeature(String name, Double value) {
        if (features != null) {
            features.put(name, value);
        }
    }

    protected void addFeatures(Map<String, Double> map) {
        if (features != null) {
            features.putAll(map);
        }
    }

    public Instances getInstances() {
        return instances;
    }

    public void reuseQuery(String field, String convertedQuery, ir.ac.ut.iis.person.query.Query query, Query ignoreQuery) {
        instances = new Instances();
        termQueries.clear();
        String[] split = convertedQuery.split(" ");
        for (String s : split) {
            String[] split1 = s.split("\\^");
            Term termQuery = new Term(field, split1[0]);
            termQueries.add(new TermQuery(termQuery, (float) Double.parseDouble(split1[1])));
        }
        if (Configs.yearFiltering) {
            yearFilter = (PointRangeQuery) IntPoint.newRangeQuery("year", 1_900, query.getYear());
        } else {
            yearFilter = null;
        }
        this.query = query;
        if (ignoreQuery != null) {
            this.ignoreQuery = ignoreQuery;
        }
    }

    protected abstract float score(MyScorer myScorer) throws IOException;

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
        return new MyWeight(searcher, needsScores, boost);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
//        if (!super.equals(other)) {
//            return false;
//        }
        if (getClass() != other.getClass()) {
            return false;
        }
        MyQuery otherQ = (MyQuery) (other);
//        if (otherQ.getBoost() != getBoost()) {
//            return false;
//        }
        Iterator<TermQuery> iterator = termQueries.iterator();
        Iterator<TermQuery> iteratorQ = otherQ.termQueries.iterator();
        while (iterator.hasNext()) {
            TermQuery next = iterator.next();
            if (!iteratorQ.hasNext()) {
                return false;
            }
            TermQuery nextQ = iteratorQ.next();
            if (!next.equals(nextQ)) {
                return false;
            }
        }
        if (iteratorQ.hasNext()) {
            return false;
        }
        if (!Objects.equal(yearFilter, otherQ.yearFilter)) {
            return false;
        }
        if (!Objects.equal(ignoreQuery, otherQ.ignoreQuery)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return termQueries.hashCode();
    }

    public class MyWeight extends Weight {

        final List<Weight> weights = new LinkedList<>();
        final Weight yearWeight;
        final Weight ignoreWeight;

        public MyWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
            super(null);
            for (TermQuery t : termQueries) {
                weights.add(t.createWeight(searcher, needsScores, boost));
            }
            if (yearFilter == null) {
                yearWeight = null;
            } else {
                yearWeight = yearFilter.createWeight(searcher, false, 1.f);
            }
            if (ignoreQuery == null) {
                ignoreWeight = null;
            } else {
                ignoreWeight = ignoreQuery.createWeight(searcher, false, 1.f);
            }
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc)
                throws IOException {
            return null;
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            // TODO Auto-generated method stub
            List<Scorer> scorers = new LinkedList<>();
            for (Weight w : weights) {
                final Scorer scorer = w.scorer(context);
                scorers.add(scorer);
            }

            return new MyScorer(this, context, scorers, yearWeight == null ? null : yearWeight.scorer(context), ignoreWeight == null ? null : ignoreWeight.scorer(context));
        }

        @Override
        public void extractTerms(Set<Term> terms) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            return false;
        }
    }
    public static int t1 = 0;
    public static int t2 = 0;
    public static int t3 = 0;
    public static boolean chh = false;

    public class MyScorer extends Scorer {

        public float[] collectionProbabilities;
        public float[] boosts;

        public final List<Scorer> scorers;
        public final Scorer yearScorer;
        public final Scorer ignoreScorer;
        public final LeafReaderContext context;

        DocIdSetIterator disi = new DocIdSetIterator() {
            int docId = -1;

            @Override
            public int docID() {
                return docId;
            }

            @Override
            public int nextDoc() throws IOException {
                t1++;
                boolean ch = true;
                while (ch) {
                    int minDocId = Integer.MAX_VALUE;
                    boolean check = false;
                    for (Scorer s : scorers) {
                        int dId = Integer.MAX_VALUE;
                        if (s != null) {
                            dId = s.docID();
                            if (dId != DocIdSetIterator.NO_MORE_DOCS) {
                                check = true;
                            }
                        }
                        minDocId = Math.min(minDocId, dId);
                    }
                    if (check == false) {
                        docId = DocIdSetIterator.NO_MORE_DOCS;
                        return docId;
                    }
                    int minDocId2 = Integer.MAX_VALUE;
                    for (Scorer s : scorers) {
                        if (s != null) {
                            int id = s.docID();
                            if (id == minDocId && s.iterator() != null) {
                                id = s.iterator().nextDoc();
                            }
                            minDocId2 = Math.min(minDocId2, id);
                        }
                    }
                    boolean ch1 = false;
                    if (Configs.yearFiltering) {
                        int advance = yearScorer.docID();
                        if (advance < minDocId2) {
                            advance = yearScorer.iterator().advance(minDocId2);
                        }
                        if (advance == DocIdSetIterator.NO_MORE_DOCS) {
                            docId = DocIdSetIterator.NO_MORE_DOCS;
                            return docId;
                        }

                        if (advance == minDocId2) {
                            ch1 = true;
                            docId = minDocId2;
                        }
                    } else {
                        ch1 = true;
                        docId = minDocId2;
                    }
                    boolean ch2 = false;
                    if (ch1 == true) {
                        if (ignoreScorer != null) {
                            int advance = ignoreScorer.docID();
                            if (advance < minDocId2) {
                                advance = ignoreScorer.iterator().advance(minDocId2);
                            }
                            if (advance > minDocId2) {
                                ch2 = true;
                                docId = minDocId2;
                            }
                        } else {
                            ch2 = true;
                            docId = minDocId2;
                        }
                    }
                    if (ch1 && ch2) {
                        ch = false;
                    }
                }

                return docID();
            }

            @Override
            public int advance(int target) throws IOException {
                throw new UnsupportedOperationException();
//                for (Scorer s : scorers) {
//                    s.iterator().advance(target);
//                }
//
//                return docID();
            }

            @Override
            public long cost() {
                return 1;
            }
        };

        protected MyScorer(Weight weight, LeafReaderContext context,
                List<Scorer> scorers, Scorer yearScorer, Scorer ignoreScorer) throws IOException {
            super(weight);
            this.scorers = scorers;
            collectionProbabilities = new float[scorers.size()];
            boosts = new float[scorers.size()];
            int i = 0;
            for (Scorer s : scorers) {
//                collectionProbabilities[i] = (float) (((double) context.reader().totalTermFreq(((TermScorer) s).getTerm())) / context.reader().getSumTotalTermFreq("content"));
                collectionProbabilities[i] = ((TermScorer) s).getCollectionProbability();
                boosts[i] = ((TermScorer) s).getBoost();

                i++;
            }
            this.yearScorer = yearScorer;
            this.ignoreScorer = ignoreScorer;
            this.context = context;
        }

        @Override
        public float score() throws IOException {
            chh = false;
            t2++;
            float score = MyQuery.this.score(this);
            if (features != null) {
                t3++;
                instances.addInstance(new Instance(context.docBase + docID(), query.getQueryId(), new HashMap<>(features)));
                features.clear();
            }
            return score;
        }

        @Override
        public DocIdSetIterator iterator() {
            return disi;
        }

        @Override
        public int docID() {
            return disi.docID();
        }

    }

    @Override
    public String toString(String field) {
        return "MyQuery: " + termQueries.toString();
    }

    public String getParamsString() {
        return null;
    }

}
