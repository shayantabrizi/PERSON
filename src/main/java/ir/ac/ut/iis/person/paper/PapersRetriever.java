/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.Main;
import ir.ac.ut.iis.person.algorithms.aggregate.TopicsValueSource;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.base.Instance;
import ir.ac.ut.iis.person.base.Instance.Instances;
import ir.ac.ut.iis.person.base.Retriever;
import ir.ac.ut.iis.person.base.Searcher;
import ir.ac.ut.iis.person.datasets.citeseerx.old.MultiDisciplinaryAuthorExtractor;
import ir.ac.ut.iis.person.evaluation.Evaluator;
import ir.ac.ut.iis.person.hierarchy.User;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryBatch;
import ir.ac.ut.iis.person.topics.InstanceClassifier;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 *
 * @author shayan
 */
public class PapersRetriever extends Retriever {

    private final IndexSearcher rootSearcher;
    private final IndexReader rootReader;

    private final Scanner scanner;
    private Query lastQuery;

    private static final Instances instances = new Instances();

    public PapersRetriever(String name, Evaluator evaluator, IndexSearcher indexSearcher, String quriesFile) {
        super(name, evaluator);
        this.rootSearcher = indexSearcher;
        this.rootReader = indexSearcher.getIndexReader();
        if (Configs.runStage.equals(Configs.RunStage.NORMAL) || Configs.runStage.equals(Configs.RunStage.CREATE_METHOD_BASED_JUDGMENTS)) {
            try {
                this.scanner = new Scanner(new FileInputStream(quriesFile));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        } else {
            this.scanner = null;
        }
    }

    public void createQueries(String queryFile, String topic) {
        int c1 = 0, c2 = 0, c3 = 0, c4 = 0, c5 = 0, c6 = 0;
        Set<String> authorsSet = new TreeSet<>();
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(Configs.datasetRoot + "multidisciplinary-authors-web-not-web.txt"))).useDelimiter("\n")) {
            while (sc.hasNextLine()) {
                authorsSet.add(sc.nextLine().split(" ")[0]);
                c1++;
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        rootSearcher.setSimilarity(new ClassicSimilarity());
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(queryFile)))) {
            Map<Integer, List<String>> readWeights = ir.ac.ut.iis.person.datasets.citeseerx.old.GraphExtractor.readWeights(Configs.datasetRoot + "graph-weights.txt", 10);
            for (Map.Entry<Integer, List<String>> e : readWeights.entrySet()) {
                c2++;
//                if (e.getValue().isEmpty() || !e.getValue().get(0).equals(topic)) {
//                    continue;
//                }
                if (e.getValue().isEmpty()) {
                    continue;
                }
                boolean check = true;
                boolean check2 = false;
                int c = 0;
                for (String t : e.getValue()) {
//                    c++;
//                    if (c > 5) {
//                        break;
//                    }
//                    if (t.equals(topic)) {
                    if (MultiDisciplinaryAuthorExtractor.getSuperTopic(t) == MultiDisciplinaryAuthorExtractor.getSuperTopic(topic)) {
                        check = false;
                        break;
                    }
//                    if (t.equals(topic)) {
//                        check2 = true;
//                    }
                }
                if (!check || e.getValue().size() < 10) {
                    continue;
                }
//                if (!(check && check2)) {
//                    continue;
//                }
                c3++;
                TopDocs search = null;

                search = rootSearcher.search(new TermQuery(new Term("id", String.valueOf(e.getKey()))), 1);
                if (search != null && search.totalHits == 0) {
                    continue;
                }
                c4++;
                if (rootReader.document(search.scoreDocs[0].doc).get("refs").split(" ").length <= 5) {
                    continue;
                }
                c5++;
                String[] authors = rootReader.document(search.scoreDocs[0].doc).get("authors").split(" ");
                String author = null;
                for (String author1 : authors) {
                    if (authorsSet.contains(author1)) {
                        author = author1;
                        break;
                    }
                }
                if (author == null) {
                    continue;
                }
                c6++;
                writer.write(e.getKey() + " " + author + "\n");

            }
        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(c1 + " " + c2 + " " + c3 + " " + c4 + " " + c5 + " " + c6);
    }

    public void createSimpleQueries(String queryFile, Integer topic) {
//        for (int i = 0; i < 200_000; i++) {
//            Main.random(100);
//        }
        File yourFile = new File(queryFile);
        yourFile.getParentFile().mkdirs();
        Set<Integer> set = new HashSet<>();
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(queryFile)))) {
            for (int i = 0; i < 400_000; i++) {
                int docId = Main.random(rootReader.maxDoc());
                if (set.contains(docId)) {
                    continue;
                }

                set.add(docId);
                Document document = rootReader.document(docId);
                if (document == null) {
                    throw new RuntimeException();
                }
                if (document.get("refs") == null) {
                    throw new RuntimeException();
                }
                if (document.get("refs").split(" ").length <= 5) {
                    continue;
                }
                if (topic != null) {
                    float[] topics = DatasetMain.getInstance().getTopics(document.get("id"));
                    List<Integer> topClasses = InstanceClassifier.getTopClasses(topics, 1);
                    if (!topClasses.iterator().next().equals(topic)) {
                        continue;
                    }
                }

                String[] authors = document.get("authors").split(" ");
                if (authors.length == 0) {
                    throw new RuntimeException();
                }
                if (Configs.dontUseMergedPapersAsQuery) {
                    if (document.get("isMerged").equals("1")) {
                        continue;
                    }
                }

                String author = authors[0];

                writer.write(document.get("id") + " " + author + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public void createMultidisciplinaryQueries(String queryFile) {
//        for (int i = 0; i < 200_000; i++) {
//            Main.random(100);
//        }
        File yourFile = new File(queryFile);
        yourFile.getParentFile().mkdirs();
        Set<Integer> set = new HashSet<>();
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(queryFile)))) {
            for (int i = 0; i < 400_000; i++) {
                int docId = Main.random(rootReader.maxDoc());
                if (set.contains(docId)) {
                    continue;
                }

                set.add(docId);
                Document document = rootReader.document(docId);
                if (document == null) {
                    throw new RuntimeException();
                }
                if (document.get("refs") == null) {
                    throw new RuntimeException();
                }
                if (document.get("refs").split(" ").length <= 5) {
                    continue;
                }

                String[] authors = document.get("authors").split(" ");
                if (authors.length == 0) {
                    throw new RuntimeException();
                }
                if (Configs.dontUseMergedPapersAsQuery) {
                    if (document.get("isMerged").equals("1")) {
                        continue;
                    }
                }

                String author = authors[0];

                float[] authorTopics;
                try {
                    authorTopics = TopicsValueSource.getAuthorTopicsExcludingCurrentPaper(document);
                } catch (IgnoreQueryEx ex) {
                    continue;
                }
                float[] topics = TopicsValueSource.getTopics(document);
                final Integer topTopic = InstanceClassifier.getTopClasses(topics, 1).get(0);
                final Integer topAuthorTopic = InstanceClassifier.getTopClasses(authorTopics, 1).get(0);
//                final List<Integer> topAuthorTopics = InstanceClassifier.getTopClasses(authorTopics, 1);
                boolean check = false;
                for (int j = 0; j < topics.length; j++) {
                    if (topics[j] > .25 || j == topTopic) {
                        if (authorTopics[j] > .25 || j == topAuthorTopic) {
                            check = true;
                        }
                    }
                }
                if (check) {
                    continue;
                }

                writer.write(document.get("id") + " " + author + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

//    private String chooseOwner(String queryDocId) {
//        String get;
//        try {
//            rootSearcher.setSimilarity(new ClassicSimilarity());
//            TopDocs results = rootSearcher.search(new TermQuery(new Term("id", queryDocId)), 1);
//            get = rootReader.document(results.scoreDocs[0].doc).get("authors");
//        } catch (IOException ex) {
//            Logger.getLogger(CiteseerxRetriever.class.getName()).log(Level.SEVERE, null, ex);
//            throw new RuntimeException();
//        }
//
//        return get.split(" ")[0];
//    }
    @Override
    public boolean hasNextQueryBatch() {
        return scanner.hasNextLine();
    }

    @Override
    public QueryBatch nextQueryBatch() {
        String[] nextLine = scanner.nextLine().split(" ");
        String queryDocId = nextLine[0];
        String searcher = nextLine[1];
        rootSearcher.setSimilarity(new ClassicSimilarity());

//        return "graph";
        try {
            TopDocs results = rootSearcher.search(new TermQuery(new Term("id", queryDocId)), 1);
            final Document document = rootReader.document(results.scoreDocs[0].doc);
            String get = document.get(Configs.queryField);
            final String get1 = document.get("year");
            Integer year = get1 == null ? null : Integer.valueOf(get1);
//            if (Main.removeTitleTermsNotInAbstract) {
//                String abs = document.get("abstract");
//                QueryConverter queryConverter = new QueryConverter();
//                Map<String, Double> convertedGet = queryConverter.queryMapConverter(get);
//                Map<String, Double> convertedAbs = queryConverter.queryMapConverter(abs);
//                for (Map.Entry<String, Double> s : convertedGet.entrySet()) {
//                    if (convertedAbs.get(s.getKey()) == null) {
//                        return nextQueryBatch();
//                    }
//                }
//            }
            get = prepareQuery(get);
            if (tokenizeString(get).size() <= 2) {
                return nextQueryBatch();
            }

            Query query = evaluator.convertToQuery(queryDocId, results.scoreDocs[0].doc, get, searcher, year, document.get("authors"));
            if (query == null) {
                return nextQueryBatch();
            }
            lastQuery = query;
            return new QueryBatch(queryDocId, query);
        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    @Override
    protected void checkRelevancy(Query query) {
        setRelevancy(query, true);
    }

    public static void setRelevancy(Query query, boolean skipSelf) {
        List<Instance> inst = new LinkedList<>();
        for (Map.Entry<String, List<Query.Result>> r : query.getResults().entrySet()) {
            int i = 0;
            boolean ignoreQuery = true;
            for (Iterator<Query.Result> t = r.getValue().iterator(); t.hasNext();) {
                Query.Result next = t.next();
                i++;
                if (i == Configs.numOfResults + 1) {
                    t.remove();
                    continue;
                }
                if (next.getDocId() == query.getQueryIndexId()) {
                    if (Configs.InappropriateQueriesHeuristic) {
//                        if (r.getKey().equals("02-LM")) {
                        ignoreQuery = false;
//                        }
                    }
                    t.remove();
                    continue;
                }

//                try {
//                    Document document = Main.hier[0].getRootNode().getIndexReader().document(next.getDocId());
//                    if (Configs.yearFiltering) {
//                        final Integer year = Integer.valueOf(document.get("year"));
//                        if (year > query.getYear()) {
//                            throw new RuntimeException();
//                        }
//                    }
//                    if (query.getIgnoredResults() != null) {
//                        for (String s : query.getIgnoredResults()) {
//                            if (s.equals(document.get("id"))) {
//                                throw new RuntimeException();
//                            }
//                        }
//                    }
//                } catch (IOException ex) {
//                    Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
//                    throw new RuntimeException();
//                }
                Instance instance = next.getInstance();
                if (query.getRelevants().get(String.valueOf(next.getDocId())) != null) {
                    next.setRelevancy(1.);
                    if (instance != null) {
                        instance.setClaz(1);
                        inst.add(instance);
                    }
                } else {
                    next.setRelevancy(0.);
                    if (instance != null) {
                        instance.setClaz(0);
                        inst.add(instance);
                    }
                }
            }
            if (Configs.InappropriateQueriesHeuristic) {
//                if (r.getKey().equals("02-LM")) {
                if (!ignoreQuery) {
                    query.setIgnored(false);
                }
//                }
            } else {
                query.setIgnored(false);
            }
            if (!query.isIgnored()) {
                for (Instance in : inst) {
                    if (in.getClaz() != -1) {
                        instances.addInstance(in);
                    }
                }
            }
        }
//        logs.put(searcherName, count);
    }

    @Override
    protected void writeLogs(QueryBatch qb) {
        if (traceWriter == null) {
            return;
        }
        String method1 = "17-Sqrt_No_No";
        String method2 = "17-Log_No_No";

        for (Query q : qb) {
            List<Query.Result> r1 = q.getResults().get(method1);
            List<Query.Result> r2 = q.getResults().get(method2);

            double r1Relevancy = 0;
            for (Query.Result t : r1) {
                r1Relevancy += t.getRelevancy();
            }
            double r2Relevancy = 0;
            for (Query.Result t : r2) {
                r2Relevancy += t.getRelevancy();
            }
            boolean check = false;

            try {
                for (Query.Result r : r2) {
                    if (r.getRelevancy() > 0 && !r1.contains(r)) {
                        if (check == false) {
                            traceWriter.write(method1 + ": " + r1Relevancy + ", " + method2 + ": " + r2Relevancy + "\n");
                            traceWriter.write(lastQuery.getQuery() + "\n\n");
                            check = true;
                        }
                        try {
                            String get = rootReader.document(r.getDocId()).get("content");
                            traceWriter.write(get + "\n\n");
                        } catch (IOException ex) {
                            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                            throw new RuntimeException();
                        }

                        traceWriter.write(method1 + ": " + explain(method1, q, r.getDocId()) + "\n");
                        traceWriter.write(method2 + ": " + explain(method2, q, r.getDocId()) + "\n");
                        traceWriter.write("-----\n\n");
                        traceWriter.flush();
                    }
                }
                if (check == true) {
                    traceWriter.write("-------------------\n\n\n");
                    for (Query.Result r : r1) {
                        if (!r2.contains(r)) {
                            try {
                                String get = rootReader.document(r.getDocId()).get("content");
                                traceWriter.write(get + "\n\n");
                            } catch (IOException ex) {
                                Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                                throw new RuntimeException();
                            }

                            traceWriter.write(method1 + ": " + explain(method1, q, r.getDocId()) + "\n");
                            traceWriter.write(method2 + ": " + explain(method2, q, r.getDocId()) + "\n");
                            traceWriter.write("-----\n\n");
                            traceWriter.flush();
                        }
                    }

                    traceWriter.write("---------------------------------------------------------------\n\n\n\n\n");
                }
            } catch (IOException ex) {
                Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }

        }

    }

    public void calcDatasetStats(String outputFile) {
        int numOfNonEmptyTitles = 0;
        int numOfNonEmptyAbstracts = 0;
        long totalAbsLength = 0;
        long absLengthSTDEV = 0;
        long totalTitleLength = 0;
        long titleLengthSTDEV = 0;
        int numOfAuthors = 0;
        int numOfAuthorsSTDEV = 0;
        int numOfRefs = 0;
        int numOfRefsSTDEV = 0;
        int numOfPapers = 0;
        Set<Integer> uniqueRefs = new HashSet<>();
        Map<Integer, Integer> authorsCount = new HashMap<>();
        Map<Integer, Integer> authorsNumCount = new HashMap<>();
        Map<Integer, Integer> refsCount = new HashMap<>();
        Map<Integer, Integer> titleLengthCount = new HashMap<>();
        Map<Integer, Integer> abstractLengthCount = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> coauthorships = new HashMap<>();
        int coauthorsCount = 0;
        int uniqueCoauthorsCount = 0;
        int papersHavingRefs = 0;

        int tmp3 = 0;
        int tmp4 = 0;
        int tmp5 = 0;
        for (int i = 0; i < rootReader.maxDoc(); i++) {
            Document document;
            try {
                document = rootReader.document(i);
            } catch (IOException ex) {
                Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            if (document == null) {
                throw new RuntimeException();
            }
            String title = document.get("title");
            String abs = document.get("abstract");
            final String refsString = document.get("refs");
            final String authorsString = document.get("authors");

            if (title == null) {
//                if (abs == null) {
//                    if (refsString == null) {
//                        if (refsString == null) {
//                            continue;
//                        }
//                    }
//                }
                throw new RuntimeException(String.valueOf(i));
            }

            if (title.isEmpty()) {
                throw new RuntimeException(String.valueOf(i));
            }
            title = title.trim();
            Analyzer analyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
//            Analyzer analyzer = new Hierarchy.MyAnalyzer();
            final int sizeTemp = tokenizeString(title, analyzer).size();

            switch (sizeTemp) {
                case 3:
                    tmp3++;
                    break;
                case 4:
                    tmp4++;
                    break;
                case 5:
                    tmp5++;
                    break;
                default:
                    break;
            }
            totalTitleLength += sizeTemp;
            Integer getTemp = titleLengthCount.get(sizeTemp);
            if (getTemp == null) {
                getTemp = 0;
            }
            getTemp++;
            titleLengthCount.put(sizeTemp, getTemp);
            titleLengthSTDEV += Math.pow(sizeTemp, 2);
            numOfNonEmptyTitles++;

            if (abs.isEmpty()) {
                throw new RuntimeException(String.valueOf(i));
            }
            abs = abs.trim();
            final int size = tokenizeString(abs, analyzer).size();
            totalAbsLength += size;
            Integer get = abstractLengthCount.get(size);
            if (get == null) {
                get = 0;
            }
            get++;
            abstractLengthCount.put(size, get);
            absLengthSTDEV += Math.pow(size, 2);
            numOfNonEmptyAbstracts++;

            numOfPapers++;
            if (refsString != null && !refsString.isEmpty()) {
                papersHavingRefs++;
                String[] refs = refsString.split(" ");

                for (String r : refs) {
                    int parseInt = Integer.parseInt(r);
                    uniqueRefs.add(parseInt);
                }
                numOfRefs += refs.length;
                numOfRefsSTDEV += Math.pow(refs.length, 2);
                Integer getTemp2 = refsCount.get(refs.length);
                if (getTemp2 == null) {
                    getTemp2 = 0;
                }
                getTemp2++;
                refsCount.put(refs.length, getTemp2);
            }
            if (authorsString == null) {
                throw new RuntimeException(String.valueOf(i));
            }
            String[] authors = authorsString.split(" ");
            if (authorsString.isEmpty()) {
                throw new RuntimeException(String.valueOf(i));
            }
            for (String a : authors) {
                int parseInt = Integer.parseInt(a);
                Integer get1 = authorsCount.get(parseInt);
                if (get1 == null) {
                    get1 = 0;
                }
                get1++;
                authorsCount.put(parseInt, get1);
                for (String b : authors) {
                    int parseInt2 = Integer.parseInt(b);
                    if (parseInt < parseInt2) {
                        Map<Integer, Integer> get2 = coauthorships.get(parseInt);
                        if (get2 == null) {
                            get2 = new HashMap<>();
                            coauthorships.put(parseInt, get2);
                        }
                        Integer get3 = get2.get(parseInt2);
                        if (get3 == null) {
                            get3 = 0;
                            uniqueCoauthorsCount++;
                        }
                        get3++;
                        coauthorsCount++;
                        get2.put(parseInt2, get3);
                    }
                }
            }

            numOfAuthors += authors.length;
            numOfAuthorsSTDEV += Math.pow(authors.length, 2);
            Integer get1 = authorsNumCount.get(authors.length);
            if (get1 == null) {
                get1 = 0;
            }
            get1++;
            authorsNumCount.put(authors.length, get1);

        }

        System.out.println(tmp3 + " " + tmp4 + " " + tmp5);

        System.out.println(numOfPapers);
        System.out.println(numOfNonEmptyTitles);
        System.out.println(numOfNonEmptyAbstracts);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile))) {
            writer.write("Num of papers: " + numOfPapers + "\n");
            writer.write("Num of authorships: " + numOfAuthors + "\n");
            writer.write("Average num of authors: " + makeAverageString(numOfPapers, numOfAuthors, numOfAuthorsSTDEV) + "\n");
            writer.write("Num of unique authors: " + authorsCount.size() + "\n");
            writer.write("Num of refs: " + numOfRefs + "\n");
            writer.write("Average num of refs: " + makeAverageString(papersHavingRefs, numOfRefs, numOfRefsSTDEV) + "\n");
            writer.write("Num of unique refs: " + uniqueRefs.size() + "\n");
            writer.write("Num of coauthors: " + coauthorsCount + "\n");
            writer.write("Num of unique coauthors: " + uniqueCoauthorsCount + "\n");
            writer.write("Average title length: " + makeAverageString(numOfNonEmptyTitles, totalTitleLength, titleLengthSTDEV) + "\n");
            writer.write("Average abstract length: " + makeAverageString(numOfNonEmptyAbstracts, totalAbsLength, absLengthSTDEV) + "\n");

            writer.write("\n\nNumber of authors per paper:\n");

            Set<Integer> keySet = authorsNumCount.keySet();
            Integer max = Collections.max(keySet);
            for (int i = 0; i <= max; i++) {
                Integer get = authorsNumCount.get(i);
                writer.write(i + " " + (get == null ? 0 : get) + "\n");
            }

            writer.write("\n\nNumber of references per paper:\n");
//            Map<Integer, Integer> refsCountHistogram = new HashMap<>();
//            for (Map.Entry<Integer, Integer> e : refsCount.entrySet()) {
//                Integer get = refsCountHistogram.get(e.getValue());
//                if (get == null) {
//                    get = 0;
//                }
//                get++;
//                refsCountHistogram.put(e.getValue(), get);
//            }

            keySet = refsCount.keySet();
            max = Collections.max(keySet);
            for (int i = 0; i <= max; i++) {
                Integer get = refsCount.get(i);
                writer.write(i + " " + (get == null ? 0 : get) + "\n");
            }
            writer.write("\n\nNumber of papers per author:\n");

            Map<Integer, Integer> authorsCountHistogram = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : authorsCount.entrySet()) {
                Integer get = authorsCountHistogram.get(e.getValue());
                if (get == null) {
                    get = 0;
                }
                get++;
                authorsCountHistogram.put(e.getValue(), get);
            }

            keySet = authorsCountHistogram.keySet();
            max = Collections.max(keySet);
            for (int i = 0; i <= max; i++) {
                Integer get = authorsCountHistogram.get(i);
                writer.write(i + " " + (get == null ? 0 : get) + "\n");
            }

            writer.write("\n\nTitle length:\n");

//            Map<Integer, Integer> titleLengthCountHistogram = new HashMap<>();
//            for (Map.Entry<Integer, Integer> e : titleLengthCount.entrySet()) {
//                Integer get = titleLengthCountHistogram.get(e.getValue());
//                if (get == null) {
//                    get = 0;
//                }
//                get++;
//                titleLengthCountHistogram.put(e.getValue(), get);
//            }
            keySet = titleLengthCount.keySet();
            max = Collections.max(keySet);
            for (int i = 0; i <= max; i++) {
                Integer get = titleLengthCount.get(i);
                writer.write(i + " " + (get == null ? 0 : get) + "\n");
            }

            writer.write("\n\nAbstract length:\n");

//            Map<Integer, Integer> abstractLengthCountHistogram = new HashMap<>();
//            for (Map.Entry<Integer, Integer> e : abstractLengthCount.entrySet()) {
//                Integer get = abstractLengthCountHistogram.get(e.getValue());
//                if (get == null) {
//                    get = 0;
//                }
//                get++;
//                abstractLengthCountHistogram.put(e.getValue(), get);
//            }
            keySet = abstractLengthCount.keySet();
            max = Collections.max(keySet);
            for (int i = 0; i <= max; i++) {
                Integer get = abstractLengthCount.get(i);
                writer.write(i + " " + (get == null ? 0 : get) + "\n");
            }

        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private String makeAverageString(int count, long sum, long sum2) {
        final double avg = ((double) sum) / count;
        return avg + "+-" + Math.sqrt(((double) sum2) / (count - 1) - (Math.pow(avg, 2) * count / (count - 1)));
    }

    public static List<String> tokenizeString(String string) {
        Analyzer analyzer = new DatasetMain.MyAnalyzer();
        return tokenizeString(string, analyzer);
    }

    public static List<String> tokenizeString(String string, Analyzer analyzer) {
        List<String> result = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream(null, new StringReader(string))) {
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            // not thrown b/c we're using a string reader...
            throw new RuntimeException(e);
        }
        return result;
    }

    private String explain(String methodName, Query q, int docId) {
        for (Searcher s : searchers) {
            if (s.getName().equals(methodName)) {
                return s.explain(q, docId);
            }
        }
        return null;
    }

    @Override
    public Set<User> getPublishers(String docId) {
        return null;
    }

    @Override
    public void skipQueryBatch(int n) {
        for (int i = 0; i < n; i++) {
            scanner.nextLine();
        }
    }
}
