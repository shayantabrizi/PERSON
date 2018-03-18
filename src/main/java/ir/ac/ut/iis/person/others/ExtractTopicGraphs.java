/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.others;

import com.google.common.io.Files;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.PapersMain;
import ir.ac.ut.iis.person.algorithms.aggregate.TopicsValueSource;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.datasets.citeseerx.PapersExtractor;
import ir.ac.ut.iis.person.hierarchy.GraphNode;
import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import ir.ac.ut.iis.person.hierarchy.HierarchyNode;
import ir.ac.ut.iis.person.paper.TopicsReader;
import ir.ac.ut.iis.person.topics.InstanceClassifier;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

/**
 *
 * @author shayan
 */
public class ExtractTopicGraphs {

    private final IndexReader indexReader;
    private final double smoothingWeight;

    public ExtractTopicGraphs(IndexReader indexReader, double smoothingWeight) {
        this.indexReader = indexReader;
        this.smoothingWeight = smoothingWeight;
    }

    protected Map<AuthorsPair, double[]> extractGraph(WeightUpdater weightUpdater, EdgeTopicsExtractor ete) {
        return extractGraph(weightUpdater, null, ete);
    }

    protected Map<AuthorsPair, double[]> extractGraph(WeightUpdater weightUpdater, Set<Integer> validNodes, EdgeTopicsExtractor ete) {
        Set<Integer> authors = new HashSet<>();
        Map<AuthorsPair, double[]> graph = new TreeMap<>();
        for (int i = 0; i < indexReader.maxDoc(); i++) {
            Document d;
            try {
                d = indexReader.document(i);
            } catch (IOException ex) {
                Logger.getLogger(ExtractTopicGraphs.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }

            String[] as = d.get("authors").split(" ");

            Integer[] a = new Integer[as.length];
            for (int j = 0; j < as.length; j++) {
                a[j] = Integer.parseInt(as[j]);
            }

            int size = weightUpdater != null ? 1 : ete.getNumberOfTopics() + 1;
            for (Integer a1 : a) {
                if (validNodes != null && !validNodes.contains(a1)) {
                    continue;
                }
                for (Integer a2 : a) {
                    if (a1 < a2) {
                        if (validNodes != null && !validNodes.contains(a2)) {
                            continue;
                        }
                        final float[] get = ete.extract(d, a1, a2);
                        if (weightUpdater != null) {
                            final double update = weightUpdater.update(get);
                            double[] get1;
                            if (update > 0) {
                                get1 = temp(a1, a2, graph, size);

                                get1[0] += update;
                                authors.add(a1);
                                authors.add(a2);
                            }
                        } else {
                            double[] get1;
                            get1 = temp(a1, a2, graph, size);
                            for (int j = 0; j < get.length; j++) {
                                get1[j] += get[j] + smoothingWeight;
                            }
                            get1[get.length] += 1;
                        }
                    }
                }
            }
        }
        System.out.println("Number of authors: " + authors.size());
        return graph;
    }

    private static interface EdgeTopicsExtractor {

        public float[] extract(Document d, int src, int dst);

        public int getNumberOfTopics();
    }

    private static class TextualEdgeTopicsExtractor implements EdgeTopicsExtractor {

        int numOfTopics;
        Map<String, float[]> topics = new HashMap<>();

        public TextualEdgeTopicsExtractor() {
            topics = new TopicsReader().readComposition(Configs.datasetRoot + "topics/" + Configs.topicsName + "/doc-topics.mallet");
        }

        @Override
        public float[] extract(Document d, int src, int dst) {
            String id = d.get("id");
            return topics.get(id);
        }

        @Override
        public int getNumberOfTopics() {
            return numOfTopics;
        }

    }

    private static class UniformEdgeTopicsExtractor implements EdgeTopicsExtractor {

        private final int numOfTopics;

        public UniformEdgeTopicsExtractor(int numOfTopcis) {
            this.numOfTopics = numOfTopcis;
        }

        @Override
        public float[] extract(Document d, int src, int dst) {
            float[] arr = new float[numOfTopics];
            for (int i = 0; i < numOfTopics; i++) {
                arr[i] = (float) (1. / numOfTopics);
            }
            return arr;
        }

        @Override
        public int getNumberOfTopics() {
            return numOfTopics;
        }
    }

    private static class UnsupervisedEdgeTopicsExtractor implements EdgeTopicsExtractor {

        private final int numOfTopics;
        private final Map<Integer, float[]> authorTopics = new HashMap<>();

        public UnsupervisedEdgeTopicsExtractor(String databaseName) {
            Connection conn = MySQLConnector.connect(databaseName);
            PreparedStatement pstmt;
            try {
                pstmt = conn.prepareStatement("select * from TopicsProfiles" + Configs.profileTopicsDBTable);
                ResultSet executeQuery = pstmt.executeQuery();
                while (executeQuery.next()) {
                    authorTopics.put(executeQuery.getInt("userId"), TopicsValueSource.getTopics(executeQuery.getBytes("topics")));
                }
            } catch (SQLException ex) {
                Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            numOfTopics = authorTopics.values().iterator().next().length;
        }

        @Override
        public float[] extract(Document d, int src, int dst) {
            float[] arr = new float[numOfTopics];
            float[] sr = authorTopics.get(src);
            float[] ds = authorTopics.get(dst);
            double sum = 0;
            for (int i = 0; i < numOfTopics; i++) {
                arr[i] = sr[i] * ds[i];
                sum += arr[i];
            }
            for (int i = 0; i < numOfTopics; i++) {
                arr[i] /= sum;
            }
            return arr;
        }

        @Override
        public int getNumberOfTopics() {
            return numOfTopics;
        }
    }

    protected double[] temp(Integer a1, Integer a2, Map<AuthorsPair, double[]> graph, int size) {
        double[] get1;
        AuthorsPair authorsPair = new AuthorsPair(a1, a2);
        get1 = graph.get(authorsPair);
        if (get1 == null) {
            get1 = new double[size];
            graph.put(authorsPair, get1);
        }
        return get1;
    }

    private void extractClusters(String outputFile, String clustersFile) {
        String ppc = "/home/shayan/NetBeansProjects/PPC/dist/Release/GNU-Linux/ppc";
        File file = new File("Folder.tmp");
        if (file.exists()) {
            throw new RuntimeException();
        }
        file.mkdir();
        File cwdFile = new File(".");
        String cwd = cwdFile.getAbsolutePath();
        final File testFolder = new File("Folder.tmp/Datasets/test");
        testFolder.mkdirs();
        try {
            final File tmpFile = new File(testFolder.getAbsoluteFile() + "/test.txt");
            Files.copy(new File(outputFile), tmpFile);
        } catch (IOException ex) {
            Logger.getLogger(ExtractTopicGraphs.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(ppc, "test", "0");
            builder.directory(file);
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process p = builder.start();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            while (reader.readLine() != null);
            p.waitFor();
            Files.copy(new File(testFolder.getAbsolutePath() + "/hier_test.tree"), new File(clustersFile));
            FileUtils.deleteDirectory(file);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ExtractTopicGraphs.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private static interface WeightUpdater {

        double update(float[] topics);

    }

    private static class MaxUpdater implements WeightUpdater {

        private final double maxScore;
        private final double nonMaxScore;
        private final int topic;

        MaxUpdater(int topic, double maxScore, double notMaxScore) {
            this.maxScore = maxScore;
            this.nonMaxScore = notMaxScore;
            this.topic = topic;
        }

        @Override
        public double update(float[] topics) {
            boolean check = true;
            for (int j = 0; j < topics.length; j++) {
                if (topics[j] > topics[topic]) {
                    check = false;
                }
            }
            if (check) {
                return maxScore;
            } else {
                return nonMaxScore;
            }
        }
    }

    private static class ConstantUpdater implements WeightUpdater {

        @Override
        public double update(float[] topics) {
            return 1.;
        }
    }

    private static class ThresholdUpdater implements WeightUpdater {

        private final double threshold;
        private final int topic;

        ThresholdUpdater(int topic, double threshold) {
            this.threshold = threshold;
            this.topic = topic;
        }

        @Override
        public double update(float[] topics) {
            boolean check = true;
            for (int j = 0; j < topics.length; j++) {
                if (topics[j] > topics[topic]) {
                    check = false;
                }
            }
            if (check || topics[topic] > threshold) {
                double sum = 0;
                for (int j = 0; j < topics.length; j++) {
                    if (j == topic || topics[j] > threshold) {
                        sum += topics[j];
                    }
                }
                return topics[topic] / sum;
            } else {
                return 0.;
            }
        }
    }

    private static class SmoothedUpdater implements WeightUpdater {

        private final WeightUpdater baseWeightUpdater;
        private final double smoothingWeight;

        SmoothedUpdater(WeightUpdater baseWeightUpdater, double smoothingWeight) {
            this.baseWeightUpdater = baseWeightUpdater;
            this.smoothingWeight = smoothingWeight;
        }

        @Override
        public double update(float[] topics) {
            double update = baseWeightUpdater.update(topics);
            return update + smoothingWeight;
        }
    }

    public void extract(String outputFile, EdgeTopicsExtractor ete) {
        Map<AuthorsPair, double[]> graph = extractGraph(null, ete);
        try (Writer wr = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            for (Map.Entry<AuthorsPair, double[]> e : graph.entrySet()) {
                StringBuilder sb = new StringBuilder();
                for (double d : e.getValue()) {
                    sb.append((float) d).append(",");
                }
                wr.write(e.getKey().author1 + "," + e.getKey().author2 + "," + sb.toString() + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(ExtractTopicGraphs.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public void extractIndividualGraph(String outputFile, WeightUpdater topic, String clustersFile, WeightUpdater smoothedWeightUpdater, EdgeTopicsExtractor ete) {
        Map<AuthorsPair, double[]> graph = extractGraph(topic, ete);
        final String tempFileName = outputFile + ".tmp";
        writeOutput(tempFileName, graph, false, " ", false);

        Set<Integer> giant = extractGiantComponent(graph, tempFileName);
        new File(tempFileName).delete();
        if (smoothedWeightUpdater != null) {
            graph = extractGraph(smoothedWeightUpdater, giant, ete);
        }
        writeOutput(outputFile, graph, true, ",", false);
        if (clustersFile != null) {
            for (Map.Entry<AuthorsPair, double[]> e : graph.entrySet()) {
                e.getValue()[0] *= 20;
            }
            writeOutput(outputFile, graph, true, " ", true);
            extractClusters(outputFile, clustersFile);
        }
    }

    protected void writeOutput(final String fileName, Map<AuthorsPair, double[]> graph, boolean writeWeights, String delimiter, boolean intWeight) throws RuntimeException {
        try (Writer wr = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            Set<Integer> authors = new HashSet<>();
            for (Map.Entry<AuthorsPair, double[]> e : graph.entrySet()) {
                authors.add(e.getKey().author1);
                authors.add(e.getKey().author2);
                Object weight;
                if (intWeight) {
                    weight = ((int) Math.round(e.getValue()[0]));
                } else {
                    weight = e.getValue()[0];
                }
                wr.write(e.getKey().author1 + delimiter + e.getKey().author2 + (writeWeights ? (delimiter + weight) : "") + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(ExtractTopicGraphs.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    protected Set<Integer> extractGiantComponent(Map<AuthorsPair, double[]> graph, String tempFileName) {
        Set<Integer> authors = new HashSet<>();
        String giantComponentExtractor = "/home/shayan/NetBeansProjects/shayan-thesis/src/main/resources/giant_component_extractor/a.out";
        Map<Integer, Set<Integer>> map = new HashMap<>();
        try {
            Process p = Runtime.getRuntime().exec(giantComponentExtractor + " " + tempFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            Thread.sleep(2_000);
            String temp1 = reader.readLine();
            String temp2 = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                final int node = Integer.parseInt(split[0]);
                final int cluster = Integer.parseInt(split[1]);
                Set<Integer> get = map.get(cluster);
                if (get == null) {
                    get = new HashSet<>();
                    map.put(cluster, get);
                }
                get.add(node);
            }
            p.waitFor();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ExtractTopicGraphs.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        int maxSize = 0;
        Set<Integer> giant = null;
        for (Map.Entry<Integer, Set<Integer>> e : map.entrySet()) {
            if (maxSize < e.getValue().size()) {
                maxSize = e.getValue().size();
                giant = e.getValue();
            }
        }
        for (Iterator<Map.Entry<AuthorsPair, double[]>> it = graph.entrySet().iterator(); it.hasNext();) {
            Map.Entry<AuthorsPair, double[]> e = it.next();
            if (!giant.contains(e.getKey().author1) || !giant.contains(e.getKey().author2)) {
                it.remove();
            } else {
                authors.add(e.getKey().author1);
                authors.add(e.getKey().author2);
            }
        }
        System.out.println("Number of authors in giant component: " + authors.size());
        return giant;
    }

    public static void main(String[] args) {
        Configs.topicsName = "15-SymmetricAlpha";
        new PapersMain().main("ExtractTopicGraphs");
//        EdgeTopicsExtractor ete = new TextualEdgeTopicsExtractor();
//        EdgeTopicsExtractor ete = new UnsupervisedEdgeTopicsExtractor(Configs.database_name);
        EdgeTopicsExtractor ete = new UniformEdgeTopicsExtractor(15);
        ExtractTopicGraphs extractTopicGraphs;
        {
//            extractTopicGraphs = new ExtractTopicGraphs(DatasetMain.getInstance().getIndexReader(), .05);
        }
        {
            extractTopicGraphs = new ExtractTopicGraphs(DatasetMain.getInstance().getIndexReader(), 0);
        }
        extractTopicGraphs.extract(Configs.datasetRoot + "topics/" + Configs.topicsName + "/authors_giant_graph_uniform.csv", ete);
//        for (int i = 0; i < 15; i++) {
//            WeightUpdater weightUpdater = new ThresholdUpdater(i, .15);
////            extractTopicGraphs.extractIndividualGraph(Configs.datasetRoot + "topics/" + Configs.topicsName + "/topic" + i + ".csv",
////                    weightUpdater,
////                    Configs.datasetRoot + "topics/" + Configs.topicsName + "/hier_test" + i + ".tree",
////                    new SmoothedUpdater(weightUpdater, .05));
//            extractTopicGraphs.extractIndividualGraph(Configs.datasetRoot + "topics/" + Configs.topicsName + "/topic" + i + ".csv",
//                    weightUpdater,
//                    Configs.datasetRoot + "topics/" + Configs.topicsName + "/hier_test" + i + ".tree",
//                    new ConstantUpdater(),
//                    ete);
//        }
//        Configs.loadGraph = true;
//        Hierarchy h = DatasetMain.getInstance().loadHierarchy("/home/shayan/NetBeansProjects/datasets/aminer3/topics/15-SymmetricAlpha/IndividualTopics/graphs/topic0.csv", "/home/shayan/NetBeansProjects/datasets/aminer3/topics/15-SymmetricAlpha/IndividualTopics/graphs/hier_topic0.tree", String.valueOf(0));
//
//        extractTopicGraphs.calcClusterStats(h, (short) 0);
    }

    private void calcClusterStats(Hierarchy hier, short topicId, EdgeTopicsExtractor ete) {
        Map<HierarchyNode, ClusterStats> map = new HashMap<>();
        for (int i = 0; i < indexReader.maxDoc(); i++) {
            Document d;
            try {
                d = indexReader.document(i);
            } catch (IOException ex) {
                Logger.getLogger(ExtractTopicGraphs.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }

            String[] as = d.get("authors").split(" ");

            GraphNode[] a = new GraphNode[as.length];
            for (int j = 0; j < as.length; j++) {
                a[j] = hier.getUserNode(Integer.parseInt(as[j]));
            }

            for (GraphNode u1 : a) {
                HierarchyNode c1 = hier.getRootNode().getChild(u1);
                ClusterStats get1 = map.get(c1);
                for (GraphNode u2 : a) {
                    if (!u2.equals(u1)) {
                        HierarchyNode c2 = hier.getRootNode().getChild(u2);

                        float[] get = ete.extract(d, u1.getId().getId(), u2.getId().getId());
                        int topClass = InstanceClassifier.getTopClasses(get, 1).get(0);
                        if (get1 == null) {
                            get1 = new ClusterStats();
                            map.put(c1, get1);
                        }
                        if (c1.equals(c2)) {
                            if (u1.compareTo(u2) < 0) {
                                get1.numOfIntraEdges++;
                                if (topicId == topClass) {
                                    get1.topicalIntraEdgeWeight++;
                                }
                            }
                        } else {
                            get1.numOfInterEdges++;
                            if (topicId == topClass) {
                                get1.topicalInterEdgeWeight++;
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<HierarchyNode, ClusterStats> e : map.entrySet()) {
            System.out.println(e.getKey() + " " + e.getValue());
        }
    }

    public static class AuthorsPair implements Comparable<AuthorsPair> {

        public Integer author1;
        public Integer author2;

        public AuthorsPair(Integer author1, Integer author2) {
            this.author1 = author1;
            this.author2 = author2;
        }

        @Override
        public int compareTo(AuthorsPair o) {
            int c1 = author1.compareTo(o.author1);
            if (c1 != 0) {
                return c1;
            }
            int c2 = author2.compareTo(o.author2);
            if (c2 != 0) {
                return c2;
            }
            return 0;
        }
    }

    private static class ClusterStats {

        double topicalIntraEdgeWeight;
        double topicalInterEdgeWeight;
        int numOfIntraEdges;
        int numOfInterEdges;

        @Override
        public String toString() {
            return "ClusterStats{" + "topicalIntraEdgeWeight=" + topicalIntraEdgeWeight + ", topicalInterEdgeWeight=" + topicalInterEdgeWeight + ", numOfIntraEdges=" + numOfIntraEdges + ", numOfInterEdges=" + numOfInterEdges + '}';
        }

    }

    public static class AuthorStats {

        float[] topics;
        int numOfPapers;

        public AuthorStats(float[] topics, int numOfPapers) {
            this.topics = topics;
            this.numOfPapers = numOfPapers;
        }

    }
}
