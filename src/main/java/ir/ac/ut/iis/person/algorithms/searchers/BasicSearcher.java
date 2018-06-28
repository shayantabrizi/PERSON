/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.searchers;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.algorithms.aggregate.AggregateSearcher;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.base.Instance;
import ir.ac.ut.iis.person.base.Instance.Instances;
import ir.ac.ut.iis.person.base.Searcher;
import ir.ac.ut.iis.person.myretrieval.MyDummySimilarity;
import ir.ac.ut.iis.person.myretrieval.MyQuery;
import ir.ac.ut.iis.person.query.QueryConverter;
import ir.ac.ut.iis.person.query.QueryConverter.ParsedQuery;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public class BasicSearcher extends Searcher {

    protected final IndexSearcher searcher;
    private final StandardQueryParser parser;
    private final Similarity similarity;
    private FeedbackExpander fe;
    private Instance.Instances instances = null;

    public void startCollectingFeatures() {
        instances = new Instance.Instances();
    }

    public BasicSearcher(IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter) {
        super(name, queryConverter);
        this.searcher = searcher;
        this.similarity = similarity;
        parser = new StandardQueryParser(new DatasetMain.MyAnalyzer());
        fe = null;
    }

    public BasicSearcher(IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, FeedbackExpander fe) {
        super(name, queryConverter);
        this.searcher = searcher;
        this.similarity = similarity;
        parser = new StandardQueryParser(new DatasetMain.MyAnalyzer());
        this.fe = fe;
    }

    @Override
    public List<ir.ac.ut.iis.person.query.Query.Result> search(ir.ac.ut.iis.person.query.Query q, int numOfResults) {
        ParsedQuery convertedQuery = getQueryConverter().run(q);
        if (convertedQuery.query.isEmpty()) {
            throw new IgnoreQueryEx();
        }
        Res res = doSearch(convertedQuery, q, numOfResults);
        final List<ir.ac.ut.iis.person.query.Query.Result> convertTopDocsToResultList = convertTopDocsToResultList(res.td, res.ins, q.getQueryId());
        q.addResult(getName(), convertTopDocsToResultList);
        return convertTopDocsToResultList;
    }

    public Res doSearch(ParsedQuery convertedQuery, ir.ac.ut.iis.person.query.Query q, int numOfResults) throws RuntimeException {
        //        if (convertedQuery.)
        searcher.setSimilarity(similarity);
        Res res;
        try {
            Query createQuery = createQuery(convertedQuery.query, q, q.getYear());
            if (instances != null && createQuery instanceof MyQuery) {
                MyQuery mq = (MyQuery) createQuery;
                mq.startCollectingFeatures();
            }
            TopDocs tops = searcher.search(createQuery, numOfResults);
//            for (int i = 0; i < tops.scoreDocs.length; i++) {
//                if (tops.scoreDocs[i].score < 0) {
//                System.out.println("final: " + tops.scoreDocs[i].doc + " " + tops.scoreDocs[i].score);
//                }
//            }
            if (fe != null) {
                String eQuery = fe.expand(tops, convertedQuery.map, q.getQueryIndexId());
                Query expandedQuery = createQuery(eQuery, q, q.getYear());
                tops = searcher.search(expandedQuery, numOfResults);
            }

            Instances ins = null;
            if (instances != null && createQuery instanceof MyQuery) {
                MyQuery mq = (MyQuery) createQuery;
                ins = mq.getInstances();
            }
            res = new Res(tops, ins);
//            Explanation explain = searcher.explain(createQuery, tops.scoreDocs[0].doc);
//            if (explain != null) {
//                System.out.println(tops.scoreDocs[0].doc);
//                System.out.println(explain.getDescription());
//                System.out.println(explain.toString());
////                System.out.println(explain.toHtml());
//            }

        } catch (QueryNodeException | IOException ex) {
//            String t = getQueryConverter().run(q.getQuery());
            Logger.getLogger(BasicSearcher.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
        if (this instanceof AggregateSearcher) {
//            System.out.println(MyCustomScoreProvider.getSubQueryScoreSum() + " " + MyCustomScoreProvider.getValSrcScoreSum());
//            System.out.println(MyCustomScoreProvider.getAllSubQueryScoreSum()+ " " + MyCustomScoreProvider.getAllValSrcScoreSum());
        }
        return res;
    }

    public Query createQuery(String convertedQuery, ir.ac.ut.iis.person.query.Query query, Integer year) throws QueryNodeException {
        Query parse;
        Query ignoreQuery = null;
        if (Configs.ignoreSelfCitations && query.getIgnoredResults() != null && !query.getIgnoredResults().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String s : query.getIgnoredResults()) {
                sb.append(s).append(" ");
            }
            ignoreQuery = parser.parse(sb.toString(), "id");
        }

        if (similarity instanceof MyDummySimilarity) {
            parse = ((MyDummySimilarity) similarity).initializeQuery("content", convertedQuery, query, ignoreQuery);
        } else {
            parse = new StandardQueryParser(new StandardAnalyzer(CharArraySet.EMPTY_SET)).parse(convertedQuery, "content");     // Don't use MyAnalyzer. A word like "named" that is already stemmed to "name" may be removed because it's in the stopwords list.
            if (Configs.yearFiltering || Configs.ignoreSelfCitations) {
                Query newRangeQuery = IntPoint.newRangeQuery("year", 1_900, year);
                BooleanQuery.Builder builder = new BooleanQuery.Builder()
                        .add(parse, BooleanClause.Occur.MUST);
                if (Configs.yearFiltering) {
                    builder = builder.add(newRangeQuery, BooleanClause.Occur.FILTER);
                }
                if (ignoreQuery != null) {
                    builder.add(ignoreQuery, BooleanClause.Occur.MUST_NOT);
                }
                parse = builder.build();
            }
        }
        return parse;

    }

    @Override
    public String explain(ir.ac.ut.iis.person.query.Query q, int docId) {
        QueryConverter.ParsedQuery convertedQuery = getQueryConverter().run(q);
        searcher.setSimilarity(similarity);
        try {
            return searcher.explain(createQuery(convertedQuery.query, q, q.getYear()), docId).toString();
        } catch (QueryNodeException | IOException ex) {
            Logger.getLogger(BasicSearcher.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public static class Res {

        public TopDocs td;
        public Instances ins;

        public Res(TopDocs td, Instances ins) {
            this.td = td;
            this.ins = ins;
        }

    }
}
