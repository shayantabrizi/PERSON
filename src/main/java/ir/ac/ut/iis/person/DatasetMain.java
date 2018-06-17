/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person;

import ir.ac.ut.iis.person.evaluation.person.PERSONEvaluator;
import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import ir.ac.ut.iis.person.hierarchy.HierarchyNode;
import ir.ac.ut.iis.person.hierarchy.User;
import ir.ac.ut.iis.person.hierarchy.UserFactory;
import ir.ac.ut.iis.person.myretrieval.CachedIndexSearcher;
import ir.ac.ut.iis.person.paper.TopicsReader;
import ir.ac.ut.iis.retrieval_tools.Config;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import static org.apache.lucene.analysis.Analyzer.GLOBAL_REUSE_STRATEGY;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author shayan
 */
public abstract class DatasetMain implements Closeable {

    protected IndexSearcher indexSearcher;
    private final Map<String, Integer> dfCache = new HashMap<>();
    private Map<String, float[]> documentCompositions;
    private final Map<Integer, User> usersMap = new HashMap<>();
    private Hierarchy hiers[];
    private static DatasetMain instance;
    protected UserFactory uf;

    protected DatasetMain() {
    }

    public static void setInstance(DatasetMain instance) {
        DatasetMain.instance = instance;
    }

    protected void setIndexSearcher(IndexSearcher indexSearcher) {
        this.indexSearcher = indexSearcher;
    }

    public static DatasetMain getInstance() {
        return instance;
    }

    public Hierarchy getHierarchy() {
        if (hiers == null) {
            hiers = new Hierarchy[1];
            hiers[0] = loadHierarchy();
        }
        return hiers[0];
    }

    public User getUser(Integer id) {
        User get = usersMap.get(id);
        if (get == null) {
            get = uf.createUser(id);
            usersMap.put(id, get);
        }
        return get;
    }

    protected abstract Hierarchy loadHierarchy();

    public abstract Hierarchy loadHierarchy(String graphFile, String clustersFile, String name, boolean ignoreLastWeight, boolean addNodesAsClusters, boolean loadAsFlatHierarchy);

    public IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }

    public IndexReader getIndexReader() {
        return indexSearcher.getIndexReader();
    }

    public float[] getTopics(String docId) {
        if (documentCompositions == null) {
            documentCompositions = new TopicsReader().readComposition(Configs.datasetRoot + "topics/" + Configs.topicsName + "/doc-topics.mallet");
        }
        return documentCompositions.get(docId);
    }

    public Integer getDF(String word) {
        return getDF(word, true);
    }

    public Integer getDF(String word, boolean ignoreSmallValues) {
        if (Configs.useDFCache) {
            if (dfCache.isEmpty()) {
                try {
                    System.out.println("DFCache started");
                    Fields fields = MultiFields.getFields(indexSearcher.getIndexReader());
                    Terms terms = fields.terms("content");
                    TermsEnum termsEnum = terms.iterator();
                    BytesRef next = termsEnum.next();
                    while (next != null) {
                        final String string = next.utf8ToString();
                        int df = indexSearcher.getIndexReader().docFreq(new Term("content", string));
                        if (!ignoreSmallValues || df > 100) {
                            dfCache.put(string, df);
                        }
                        next = termsEnum.next();
                    }
                    System.out.println("DFCache finished");
                } catch (IOException ex) {
                    Logger.getLogger(PERSONEvaluator.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            }
            return dfCache.get(word);
        }
        try {
            int docFreq = indexSearcher.getIndexReader().docFreq(new Term("content", word));
            if (!ignoreSmallValues || docFreq > 100) {
                return docFreq;
            }
            return null;
        } catch (IOException ex) {
            Logger.getLogger(PERSONEvaluator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    protected IndexSearcher openSearcher() throws RuntimeException {
        IndexSearcher indexSearcher;
        File file = new File(Configs.datasetRoot + "/index/" + Configs.indexName);
        if (file.isDirectory()) {
            try {
                SimpleFSDirectory fs = new SimpleFSDirectory(file.toPath());
                DirectoryReader indexReader = DirectoryReader.open(fs);
                if (Configs.useSearchCaching) {
                    indexSearcher = new CachedIndexSearcher(new IndexSearcher(indexReader), 15);
                } else {
                    indexSearcher = new IndexSearcher(indexReader);
                }
                indexSearcher.setSimilarity(new LMDirichletSimilarity());
            } catch (IOException e) {
                Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, e);
                throw new RuntimeException();
            }
        } else {
            throw new RuntimeException("Index folder not found: " + Configs.datasetRoot + "/index/" + Configs.indexName);
        }
        return indexSearcher;
    }

    @Override
    public void close() throws IOException {
        if (hiers != null) {
            for (Hierarchy hier : hiers) {
                hier.getRootNode().close();
            }
        }
    }

    public static class MyAnalyzer extends AnalyzerWrapper {

        final StandardAnalyzer standardAnalyzer = new StandardAnalyzer(Config.stopWordsSet);

        public MyAnalyzer() {
            super(GLOBAL_REUSE_STRATEGY);
        }

        @Override
        protected Analyzer getWrappedAnalyzer(String string) {
            return standardAnalyzer;
        }

        @Override
        protected Analyzer.TokenStreamComponents wrapComponents(String string, Analyzer.TokenStreamComponents tsc) {
//            LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(tsc.getTokenStream());
            KStemFilter kStemFilter = new KStemFilter(tsc.getTokenStream());
            return new Analyzer.TokenStreamComponents(tsc.getTokenizer(), kStemFilter);
        }
    }

//    static class PorterAnalyzer extends AnalyzerWrapper {
//
//        public PorterAnalyzer() {
//            super(new StandardAnalyzer().getReuseStrategy());
//        }
//
//        @Override
//        protected Analyzer getWrappedAnalyzer(String string) {
//            return new StandardAnalyzer();
//        }
//
//        @Override
//        protected TokenStreamComponents wrapComponents(String string, TokenStreamComponents tsc) {
//            if (string.equals("id")) {
//                return tsc;
//            }
//            return new TokenStreamComponents(tsc.getTokenizer(), new PorterStemFilter(tsc.getTokenStream()));
//        }
//    }
}
