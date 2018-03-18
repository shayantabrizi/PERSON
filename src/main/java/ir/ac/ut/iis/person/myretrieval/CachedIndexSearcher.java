package ir.ac.ut.iis.person.myretrieval;

import ir.ac.ut.iis.person.algorithms.aggregate.MyCustomScoreQuery;
import ir.ac.ut.iis.person.others.LittleCache;
import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;

public class CachedIndexSearcher extends IndexSearcher {

    private final IndexSearcher base;
    private final LittleCache<SearchParams, TopDocs> cache;

    public CachedIndexSearcher(IndexSearcher base, int maxSize) {
        super(base.getIndexReader());
        this.base = base;
        this.cache = new LittleCache<>(maxSize);
    }

    @Override
    public TopDocs search(Query query, int n)
            throws IOException {
        if (query instanceof org.apache.lucene.search.TermQuery || query instanceof MyQuery || query instanceof MyCustomScoreQuery) {
            return base.search(query, n);
        }
        final SearchParams searchParams = new SearchParams(getSimilarity(true), query, n);
        TopDocs get = cache.get(searchParams);
        if (get != null) {
            TopDocs topDocs = cloneTopDocs(get);
            return topDocs;
        }
        final TopDocs search = base.search(query, n);
        cache.add(searchParams, search);
        return cloneTopDocs(search); //To change body of generated methods, choose Tools | Templates.    }
    }

    protected TopDocs cloneTopDocs(TopDocs get) {
        ScoreDoc[] scoreDocs = new ScoreDoc[get.scoreDocs.length];
        for (int i = 0; i < get.scoreDocs.length; i++) {
            final ScoreDoc scoreDoc = get.scoreDocs[i];
            scoreDocs[i] = new ScoreDoc(scoreDoc.doc, scoreDoc.score, scoreDoc.shardIndex);
        }
        TopDocs topDocs = new TopDocs(get.totalHits, scoreDocs, get.getMaxScore());
        return topDocs;
    }

    public static class SearchParams {

        Similarity similarity;
        Query query;
        int n;

        public SearchParams(Similarity similarity, Query query, int n) {
            this.similarity = similarity;
            this.query = query;
            this.n = n;
        }

        @Override
        public int hashCode() {
            int hash = 7;
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
            final SearchParams other = (SearchParams) obj;
            if (this.n != other.n) {
                return false;
            }
            if (!(this.similarity == other.similarity)) {
                return false;
            }
            if (!Objects.equals(this.query, other.query)) {
                return false;
            }
            return true;
        }

    }

    @Override
    public void setSimilarity(Similarity similarity) {
        super.setSimilarity(similarity);
        base.setSimilarity(similarity);
    }

}
