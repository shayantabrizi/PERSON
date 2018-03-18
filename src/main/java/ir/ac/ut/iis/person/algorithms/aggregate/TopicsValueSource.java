/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.base.Statistic;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.topics.InstanceClassifier;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author shayan
 */
public class TopicsValueSource extends MyValueSource implements Closeable {

    private float[] searcherTopics;
    private List<Integer> topQueryTopics;
    private float[] queryTopics;
    private final IndexSearcher rootSearcher;
    private final InstanceClassifier ic;
    private final Connection conn;
    private final List<TopicAlgorithm> algorithms;
    private static final Map<Integer, TopicsCache> topicsCache = new HashMap<>();
    private final Statistic.Statistics statistics = new Statistic.Statistics();

    public TopicsValueSource(IndexSearcher rootSearcher, String databaseName, InstanceClassifier ic, List<TopicAlgorithm> algorithms) {
        this.rootSearcher = rootSearcher;
        this.conn = MySQLConnector.connect(databaseName);
        this.ic = ic;
        this.algorithms = algorithms;
        for (TopicAlgorithm ta : algorithms) {
            statistics.addStatistic(ta);
        }
    }

    public String getParamsString() {
        StringBuilder sb = new StringBuilder();
        for (TopicAlgorithm ta : algorithms) {
            sb.append(ta.getName()).append("=").append(ta.getWeight()).append(",");
        }
        return sb.toString();
    }

    @Override
    public void initialize(Query query) {
        statistics.printStatistics();
        for (TopicAlgorithm ta : algorithms) {
            ta.initialize(query);
        }
        //Avvale query document az profile owner hazf beshe va bad retrieval anjam beshe
        int docCount;
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement("select * from TopicsProfiles" + Configs.profileTopicsDBTable + " where userId=?");
            pstmt.setInt(1, query.getSearcher());
            ResultSet executeQuery = pstmt.executeQuery();
            executeQuery.next();
            byte[] bytes = executeQuery.getBytes("topics");
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                searcherTopics = (float[]) in.readObject();
            }
            docCount = executeQuery.getInt("docCount");
        } catch (SQLException | IOException | ClassNotFoundException ex) {
            Logger.getLogger(TopicsValueSource.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        float[] docTopics = null;
        try {
            TopDocs results = rootSearcher.search(new TermQuery(new Term("id", query.getQueryId())), 1);
            final Document document = rootSearcher.getIndexReader().document(results.scoreDocs[0].doc);
            docTopics = getTopics(document);
        } catch (IOException ex) {
            Logger.getLogger(TopicsValueSource.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        for (int i = 0; i < docTopics.length; i++) {
            searcherTopics[i] = ((searcherTopics[i] * docCount) - docTopics[i]) / (docCount - 1);
        }
        queryTopics = ic.getQueryTopics(query.getQuery());

        topQueryTopics = InstanceClassifier.getTopClasses(queryTopics, 10);
    }

    @Override
    public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
        return new FunctionValues() {

            @Override
            public float floatVal(int doc) {
                TopicsCache tc = topicsCache.get(readerContext.docBase + doc);
                if (tc == null) {
                    try {
                        final Document document = readerContext.reader().document(doc);
                        tc = new TopicsCache(getTopics(document), getAuthorTopics(document));
                        topicsCache.put(readerContext.docBase + doc, tc);
                    } catch (IOException ex) {
                        Logger.getLogger(TopicsValueSource.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException();
                    }
                }

                double score = 0.;
                double weightNormal = 0.;
                for (TopicAlgorithm ta : algorithms) {
                    score += ta.getWeight() * ta.getSimilarity(readerContext.docBase + doc, tc.authorTopics, searcherTopics, queryTopics, tc.documentTopics, topQueryTopics);
                    weightNormal += ta.getWeight();
                }

                return (float) (score / weightNormal);
            }

            @Override
            public String toString(int doc) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static float[] getTopics(final Document document, String topicsField) {
        byte[] bytes = document.getBinaryValue(topicsField).bytes;
        float[] topics = getTopics(bytes);
        return topics;
    }

    public static float[] getTopics(byte[] bytes) throws RuntimeException {
        float[] topics;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            topics = (float[]) in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(TopicsValueSource.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return topics;
    }

    public static float[] getAuthorTopics(Document document) {
        float[] authorTopics = getTopics(document, "authorTopics");
        return authorTopics;
    }

    public static float[] getAuthorTopicsExcludingCurrentPaper(Document document) {
        return getAuthorTopicsExcludingCurrentPaper(document, "topics");
    }

    public static float[] getAuthorTopicsExcludingCurrentPaper(Document document, String fieldName) {
        float[] authorTopics = getAuthorTopics(document);
        if (Configs.removeCurrentPaperFromAuthorTopics == false) {
            return authorTopics;
        }
        int parseInt = Integer.parseInt(document.get("authorPapersCount"));
        if (parseInt == 1) {
            throw new IgnoreQueryEx("Getting topics for author with only one paper");
        }
        float[] topics = getTopics(document, fieldName);
        for (int i = 0; i < authorTopics.length; i++) {
            authorTopics[i] = (float) ((((double) authorTopics[i]) * parseInt - topics[i]) / (parseInt - 1));
        }
        return authorTopics;
    }

    public static float[] getTopics(final Document document) {
        if (Configs.useCachedTopics) {
            float[] topics;
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(document.getBinaryValue("topics").bytes))) {
                topics = (float[]) in.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(TopicsValueSource.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            return topics;
        } else {
            return DatasetMain.getInstance().getTopics(document.get("id"));
        }
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String description() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(TopicsValueSource.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private static class TopicsCache {

        float[] documentTopics;
        float[] authorTopics;

        TopicsCache(float[] documentTopics, float[] authorTopics) {
            this.documentTopics = documentTopics;
            this.authorTopics = authorTopics;
        }

    }
}
