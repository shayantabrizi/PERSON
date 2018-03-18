/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.Main;
import ir.ac.ut.iis.person.base.Retriever;
import ir.ac.ut.iis.person.base.Searcher;
import ir.ac.ut.iis.person.evaluation.Evaluator;
import ir.ac.ut.iis.person.hierarchy.User;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryBatch;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 *
 * @author shayan
 */
public class PapersInvertedRetriever extends Retriever {

    private final IndexSearcher rootSearcher;
    private final IndexReader rootReader;

    private final Scanner scanner;
    private final Map<String, String> logs = new TreeMap<>();

    public PapersInvertedRetriever(IndexSearcher indexSearcher, IndexReader indexReader, String name, Evaluator evaluator, String quriesFile) {
        super(name, evaluator);
        this.rootSearcher = indexSearcher;
        this.rootReader = indexReader;
        try {
            this.scanner = new Scanner(new FileInputStream(quriesFile));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public void createQueries(String queryFile, String topic) {
        int c1 = 0, c2 = 0, c3 = 0, c4 = 0, c5 = 0;
        rootSearcher.setSimilarity(new ClassicSimilarity());
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(queryFile)))) {
            Map<Integer, List<String>> readWeights = ir.ac.ut.iis.person.datasets.citeseerx.old.GraphExtractor.readWeights(Configs.datasetRoot + "graph-weights.txt", 1);
            for (Map.Entry<Integer, List<String>> e : readWeights.entrySet()) {
                c1++;
                if (e.getValue().get(0).equals(topic)) {
                    c2++;
                    TopDocs search;
                    search = rootSearcher.search(new TermQuery(new Term("id", String.valueOf(e.getKey()))), 1);
                    if (search != null && search.totalHits == 0) {
                        continue;
                    }
                    c3++;
                    TopDocs cites = rootSearcher.search(new TermQuery(new Term("refs", String.valueOf(e.getKey()))), 1_000_000);
                    StringBuilder sb = new StringBuilder();
                    int i = 0;
                    for (ScoreDoc d : cites.scoreDocs) {
                        final Document cite = rootReader.document(d.doc);
                        c4++;
                        if (cite.get("authors").isEmpty()) {
                            continue;
                        }
                        c5++;
                        String[] split = cite.get("authors").split(" ");
                        sb.append(e.getKey()).append(" ").append(split[0]).append(" ").append(cite.get("id")).append(" " + "\n");
                        i++;
                    }
                    if (i != 0) {
                        writer.write(e.getKey() + " " + String.valueOf(i) + "\n");
                        writer.write(sb.toString());
                    }
                }
            }
            System.out.println(c1 + " " + c2 + " " + c3 + " " + c4 + " " + c5);
        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void createSimpleQueries(String queryFile) {
        rootSearcher.setSimilarity(new ClassicSimilarity());
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(queryFile)))) {
            for (int i = 0; i < 10_000; i++) {
                int docId = Main.random(rootReader.maxDoc());
                Document document = rootReader.document(docId);
                if (document == null) {
                    continue;
                }
                if (document.get("refs") == null) {
                    continue;
                }

                TopDocs cites = rootSearcher.search(new TermQuery(new Term("refs", document.get("id"))), 1_000_000);
                StringBuilder sb = new StringBuilder();
                int j = 0;
                for (ScoreDoc d : cites.scoreDocs) {
                    final Document cite = rootReader.document(d.doc);
                    if (cite.get("authors").isEmpty()) {
                        continue;
                    }
                    String[] split = cite.get("authors").split(" ");
                    sb.append(document.get("id")).append(" ").append(split[0]).append(" ").append(cite.get("id")).append(" " + "\n");
                    j++;
                }
                if (j != 0) {
                    writer.write(document.get("id") + " " + String.valueOf(j) + "\n");
                    writer.write(sb.toString());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean hasNextQueryBatch() {
        return scanner.hasNextLine();
    }

    @Override
    public QueryBatch nextQueryBatch() {
        rootSearcher.setSimilarity(new ClassicSimilarity());
        String[] nextLine = scanner.nextLine().split(" ");
        Integer n = Integer.valueOf(nextLine[1]);
        QueryBatch queryBatch = new QueryBatch(nextLine[0]);
        Integer queryIndexId;
        TopDocs td = null;
        try {
            td = rootSearcher.search(new TermQuery(new Term("id", nextLine[0])), 1);
            queryIndexId = td.scoreDocs[0].doc;
        } catch (IOException ex) {
            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        for (int i = 0; i < n; i++) {
            nextLine = scanner.nextLine().split(" ");
            String queryDocId = nextLine[0];
            String searcherUserId = nextLine[1];
            String answerDocId = nextLine[2];
            Map<String, Double> relevants = new TreeMap<>();
            String relevant = null;
            TopDocs t = null;
            try {
                t = rootSearcher.search(new TermQuery(new Term("id", answerDocId)), 1);
            } catch (IOException ex) {
                Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (t.totalHits != 0) {
                relevant = String.valueOf(t.scoreDocs[0].doc);
            }
            relevants.put(relevant, 1.);

//        return "graph";
            String[] authors;
            try {
                TopDocs results = rootSearcher.search(new TermQuery(new Term("id", queryDocId)), 1);
                final Document document = rootReader.document(results.scoreDocs[0].doc);
                String get = document.get("title");
                get = prepareQuery(get);
                authors = document.get("authors").split(" ");
                Set<String> authorsSet = new HashSet<>(Arrays.asList(authors));
                final String get1 = document.get("year");
                Integer year = get1 == null ? null : Integer.valueOf(get1);
                queryBatch.addQuery(new Query(queryDocId, queryIndexId, get, Integer.parseInt(searcherUserId), year, relevants, authorsSet, null));
            } catch (IOException ex) {
                Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        }
        return queryBatch;
    }

    @Override
    protected void checkRelevancy(Query query) {
        PapersRetriever.setRelevancy(query, true);
    }

    public void logSearch(String kind, Query query, String result, int location) {
        if (!kind.equals("RawTF") && !kind.equals("TFIDF") && !kind.equals("7-RawTF_WithNormalization")
                && !kind.equals("8-RawTFIDF") && !kind.equals("9-RawTFIDF_NoNormalization")
                && !kind.equals("10-RawTFLogIDF")) {
            return;
        }
        String answer = null;
        String answerId = null;
        try {
            Document t;
            t = rootReader.document(Integer.parseInt(result));
            answer = t.get("content");
            answerId = t.get("id");
        } catch (IOException ex) {
            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
        }
        String key = query.getQueryId() + " " + query.getSearcher();

        String previous = logs.get(key);
        if (previous == null) {
            previous = "";
        }

        Searcher searcher = null;
        Searcher tfidf = null;
        for (Searcher s : getSearchers()) {
            if (s.getName().equals(kind)) {
                searcher = s;
            }
            if (s.getName().equals("TFIDF")) {
                tfidf = s;
            }
        }
        String explain = searcher.explain(query, Integer.parseInt(result));
        String tfidf_explain = tfidf.explain(query, Integer.parseInt(result));

        logs.put(kind, previous + kind + " " + key + " " + answerId + " " + location
                + "\n" + query.getQuery() + "\n" + answer + "\n" + explain + "\n" + "TFIDF:\n" + tfidf_explain + "\n\n");
    }

    @Override
    public void writeResults(Writer out, Writer perQueryOutput) {
        super.writeResults(out, perQueryOutput);
    }

    @Override
    public void close() throws IOException {
        scanner.close();
        super.close();
    }

    @Override
    protected void writeLogs(QueryBatch qb) {
        if (!logs.keySet().contains("RawTF")) {
            logs.clear();
            return;
        }
        try {
            for (String v : logs.values()) {
                traceWriter.write(v);
            }
            traceWriter.flush();
            logs.clear();
        } catch (IOException ex) {
            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    @Override
    public Set<User> getPublishers(String docId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void skipQueryBatch(int n) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
