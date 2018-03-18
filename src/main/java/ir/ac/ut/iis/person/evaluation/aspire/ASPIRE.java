/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.evaluation.aspire;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.evaluation.person.PERSONEvaluator;
import ir.ac.ut.iis.person.hierarchy.HierarchyNode;
import ir.ac.ut.iis.person.paper.PapersInvertedRetriever;
import ir.ac.ut.iis.person.paper.TopicsReader;
import ir.ac.ut.iis.person.query.NormalizedQueryExpander;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryExpander;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 *
 * @author shayan
 */
public class ASPIRE {

    static Map<Integer, Map<String, Double>> profiles = new HashMap<>();
    private static final Set<Integer> badTopics = new HashSet<>();
    private static final Set<String> badDocuments = new HashSet<>();

    public static void main(String[] args) {
        Configs.dontUseAuthorsRelevantPapersInProfile = false;
        Configs.ignoreQueryDocumentInQueryExpansion = false;
        Configs.useTFIDFWeightingInCampos = true;
        Configs.useLogTFInProfilesCreation = false;

//        createClusters(true);
        createProfiles(true);
    }

    protected static void createClusters(boolean ignoreBadTopics) throws RuntimeException {
        final String inputFile = Configs.datasetRoot + "topics/" + Configs.topicsName + "/doc-topics.mallet";
        final String badTopicsFile = Configs.datasetRoot + "topics/" + Configs.topicsName + "/badTopics.txt";
        final String outputFile = Configs.datasetRoot + "topics/" + Configs.topicsName + "/clusters.txt";
        final String badDocumentsFile = Configs.datasetRoot + "topics/" + Configs.topicsName + "/badDocuments.txt";
        Map<String, float[]> topics = new TopicsReader().readComposition(inputFile);
        if (ignoreBadTopics) {
            try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(badTopicsFile)))) {
                while (sc.hasNextLine()) {
                    String nextLine = sc.nextLine();
                    badTopics.add(Integer.parseInt(nextLine));
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ASPIRE.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile))) {
            for (Map.Entry<String, float[]> e : topics.entrySet()) {
                int maxId = -1;
                float max = 0;
                int maxIdConsideringBads = -1;
                float maxConsideringBads = 0;
                float[] value = e.getValue();
                for (int i = 0; i < value.length; i++) {
                    if (value[i] > max) {
                        max = value[i];
                        maxId = i;
                    }
                    if (!badTopics.contains(i)) {
                        if (value[i] > maxConsideringBads) {
                            maxConsideringBads = value[i];
                            maxIdConsideringBads = i;
                        }
                    }
                }
                if (maxId != maxIdConsideringBads) {
                    badDocuments.add(e.getKey());
                }
                writer.write(e.getKey() + " " + maxIdConsideringBads + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(ASPIRE.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(badDocumentsFile))) {
            for (String d : badDocuments) {
                writer.write(d + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(ASPIRE.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    protected static void createProfiles(boolean ignoreBadDocuments) throws RuntimeException {
        final String clustersFileName = Configs.datasetRoot + "topics/" + Configs.topicsName + "/clusters.txt";
        final String outputFile = Configs.datasetRoot + "topics/" + Configs.topicsName + "/profiles.txt";
        final Map<Integer, List<Integer>> clusters = new HashMap<>();
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(clustersFileName)))) {
            while (sc.hasNextLine()) {
                String[] nextLine = sc.nextLine().split(" ");
                final Integer clusterId = Integer.valueOf(nextLine[1]);
                List<Integer> get = clusters.get(clusterId);
                if (get == null) {
                    get = new LinkedList<>();
                    clusters.put(clusterId, get);
                }
                get.add(Integer.valueOf(nextLine[0]));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ASPIRE.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        if (Configs.badDocumentsFileName != null) {
            try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(Configs.datasetRoot + "topics/" + Configs.topicsName + "/" + Configs.badDocumentsFileName)))) {
                while (sc.hasNextLine()) {
                    String nextLine = sc.nextLine();
                    badDocuments.add(nextLine);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ASPIRE.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }

        IndexSearcher searcher;
        try {
            SimpleFSDirectory fs = new SimpleFSDirectory(new File(Configs.datasetRoot + "index/index").toPath());
            DirectoryReader indexReader = DirectoryReader.open(fs);
            searcher = new IndexSearcher(indexReader);
        } catch (IOException e) {
            Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException();
        }
        final PERSONEvaluator personEvaluator = new PERSONEvaluator() {
            @Override
            protected List<Integer> getProfile(IndexSearcher searcher, int userId) throws IOException {
                List<Integer> get = clusters.get(userId);
                List<Integer> idList = new LinkedList<>();
                for (Integer d : get) {
                    if (!ignoreBadDocuments || !badDocuments.contains(String.valueOf(d))) {
                        try {
                            TopDocs results = searcher.search(new TermQuery(new Term("id", d.toString())), 1);
                            idList.add(results.scoreDocs[0].doc);
                        } catch (IOException ex) {
                            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
                            throw new RuntimeException();
                        }
                    }
                }
                return idList;
            }
        };
        personEvaluator.setSearcher(searcher);
        QueryExpander queryExpander = new NormalizedQueryExpander(40, personEvaluator, 1.);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile))) {
            final Integer max = Collections.max(clusters.keySet());
            for (int i = 0; i <= max; i++) {
                if (clusters.get(i) != null) {
                    final HashMap<String, Double> hashMap = new HashMap<>();
                    queryExpander.convert(hashMap, i, new Query(null, i, null, null, null, null, null, null));
                    writer.write(i + ":");
                    for (Map.Entry<String, Double> e : hashMap.entrySet()) {
                        writer.write(e.getKey() + " " + e.getValue() + ";");
                    }
                    writer.write("\n");
//                profiles.put(i, hashMap);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ASPIRE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
