/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.citeseerx.old;

import ir.ac.ut.iis.retrieval_tools.azimi.TaxonomyHierarchyReader;
import ir.ac.ut.iis.retrieval_tools.other.StopWordsExtractor.MapUtil;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.others.ExtractTopicGraphs.AuthorsPair;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class GraphExtractor {

    public static void main(String[] args) throws FileNotFoundException {
//        extractWeights(new File(Main.datasetRoot+"topics/topics"));
        createGraphs(Configs.datasetRoot+"papers_giant.txt", Configs.datasetRoot+"authors_giant.txt", Configs.datasetRoot +"graph-weights.txt", Configs.datasetRoot + "graphs/");
//        calcWeightStats("graph-weights.txt","weights-stats.txt");
//        convertWeightsToGraph(Main.citeseerxRdatasetRootopics/topics.txt", "topics_graph.txt", "topics_mapping.txt");
//        findRepresentatives("topics_clusters.txt", "topics_mapping.txt", "topics_graph.txt");
    }

    public static void calcWeightStats(String weightsFile, String outputFile) {
        Map<String, Integer> map = new TreeMap<>();
        int u = 0;
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(weightsFile)))) {
                while (sc.hasNextLine()) {
                    u++;
                    if (u % 10_000 == 0) {
                        System.out.println(u);
                    }
                    sc.nextLine();
//                Integer parseLong = Integer.parseInt(sc.nextLine());
                    List<String> list = new LinkedList<>();
                    for (int i = 0; i < 10; i++) {
                        String nextLine = sc.nextLine().split(":")[0];
                        Integer get = map.get(nextLine);
                        if (get == null) {
                            get = 0;
                        }
                        get += 1;
                        map.put(nextLine, get);
                    }
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
            Map<String, Integer> sortByValue = MapUtil.sortByValue(map);
            for (Map.Entry<String, Integer> e : sortByValue.entrySet()) {
                writer.write(e.getKey() + " " + e.getValue() + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void extractWeights(final File folder) {
        File[] listFiles = folder.listFiles();
        Arrays.sort(listFiles);
//        Map<Long, Map<String, Double>> map = new TreeMap<>();
        int count = 0;
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("graph-weights.txt")))) {
            for (final File fileEntry : listFiles) {
                try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(fileEntry)))) {
                    while (sc.hasNextLine()) {
                        String s = sc.nextLine();
                        String[] split = s.split("::");
                        String id = split[0].substring(0, split[0].indexOf(' '));
//                        String id = split[0];
                        String[] split1 = split[1].split(";");
                        Map<String, Double> tmp = new TreeMap<>();
//                        if (split1.length != 524) {
//                            throw new RuntimeException(s + " " + id);
//                        }
                        for (String split11 : split1) {
                            String[] split2 = split11.split(": ");
                            String topic = split2[0].replace("\"", "");
                            Double val = Double.parseDouble(split2[1]);
                            tmp.put(topic, val);
                        }
                        Map<String, Double> sortByValue = MapUtil.sortByValue(tmp);
                        int i = 0;
                        Map<String, Double> tmp2 = new TreeMap<>();
                        writer.write(id + "\n");
                        for (Map.Entry<String, Double> e : sortByValue.entrySet()) {
                            i++;
                            if (i == 11) {
                                break;
                            }
                            tmp2.put(e.getKey(), e.getValue());
                            writer.write(e.getKey() + ":" + e.getValue() + "\n");
                        }
                        count++;
                        if (count % 1_000 == 0) {
                            System.out.println(count);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Map<Integer, List<String>> readWeights(String weightsFile) {
        return readWeights(weightsFile, 10);
    }

    public static Map<Integer, List<String>> readWeights(String weightsFile, int n) {
        Map<Integer, List<String>> map = new TreeMap<>();
        int u = 0;
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(weightsFile)))) {
            while (sc.hasNextLine()) {
                u++;
                if (u % 10_000 == 0) {
                    System.out.println(u);
                }
                Integer parseLong = Integer.parseInt(sc.nextLine());
                List<String> list = new LinkedList<>();
                for (int i = 0; i < 10; i++) {
                    String nextLine = sc.nextLine();
                    if (i < n) {
                        list.add(nextLine.split(":")[0]);
                    }
                }
                map.put(parseLong, list);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return map;
    }

    public static Set<AuthorsPair> calc(String papersFile, Map<Integer, List<String>> weights, Map<String, Writer> writers) {
        System.out.println("Strting calc...");
        int i = 0;
//        Map<AuthorsPair, Map<String, Integer>> map = new TreeMap<>();
        Set<AuthorsPair> set = new TreeSet<>();
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(papersFile)))) {
            sc.useDelimiter("\n");
            while (sc.hasNext()) {
                i++;
                if (i % 10_000 == 0) {
                    System.out.println(i);
                }
                String nextLine = sc.next();
                Integer id = Integer.parseInt(nextLine);
                sc.next();
                sc.next();
                sc.next();
                sc.next();
                sc.next();
                String title = sc.next();
                String abs = sc.next();
                sc.next();
                sc.next();
                sc.next();
                sc.next();
                String authors = sc.next();
                if (abs.length() + title.length() < 150) {
                    continue;
                }
                if (!authors.isEmpty()) {
                    String[] split = authors.split(",");
                    if (split.length > 1) {
                        for (String split1 : split) {
                            for (String split2 : split) {
                                Integer a1 = Integer.parseInt(split1);
                                Integer a2 = Integer.parseInt(split2);
                                if (a1 < a2) {
                                    AuthorsPair authorsPair = new AuthorsPair(a1, a2);
                                    set.add(authorsPair);
//                                    Map<String, Integer> get = map.get(authorsPair);
//                                    if (get == null) {
//                                        get = new TreeMap<>();
//                                        map.put(authorsPair, get);
//                                    }
List<String> l = weights.get(id);
//                                    if (l == null) {
//                                        System.out.println(id);
//                                        ss.add(id);
//                                        continue;
//                                    }
for (String topic : l) {
//                                        Integer count = get.get(topic);
//                                        if (count == null) {
//                                            count = 0;
//                                        }
//                                        count++;
//                                        get.put(topic, count);
//                                        System.out.println(topic);
writers.get(topic).write(authorsPair.author1 + " " + authorsPair.author2 + "\n");
}
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return set;
    }

    public static void createGraphs(String papersFile, String authorsFile, String weightsFile, String outputFolder) {
        try {
            Map<Integer, List<String>> weights = readWeights(weightsFile);
            TaxonomyHierarchyReader thr = new TaxonomyHierarchyReader(Configs.datasetRoot + "taxonomy2012.txt");
//            System.out.println(thr.getTopics(3).size());
            Map<String, Writer> writers = new TreeMap<>();
            for (TaxonomyHierarchyReader.Node t : thr.getTopics(3)) {
//                System.out.println(t.getNodeName());
                writers.put(t.getNodeName(), new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputFolder + t.getNodeName() + "-counts.txt"))));
            }
            Set<AuthorsPair> calc = calc(papersFile, weights, writers);
            for (Writer w : writers.values()) {
                w.close();
            }
            boolean check = false;
            for (TaxonomyHierarchyReader.Node t : thr.getTopics(3)) {
                if (t.getNodeName().equals("Web searching and information discovery") || t.getNodeName().equals("Software verification and validation")) {
                    check = true;
                }
                if (check) {
                    try (Writer wr = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputFolder + t.getNodeName() + ".txt")))) {
                        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(outputFolder + t.getNodeName() + "-counts.txt")))) {
//                System.out.println(t.getNodeName());
                            writeGraphToFile(calc, sc, wr);
                        }
                    }
                }
                new File(outputFolder + t.getNodeName() + "-counts.txt").delete();
            }
//            for (Map.Entry<AuthorsPair, Map<String, Integer>> p : calc.entrySet()) {
//                for (TaxonomyHierarchyReader.Node t : thr.getTopics(3)) {
//                    Integer get = p.getValue().get(t.getNodeName());
//                    double w = .2;
//                    if (get != null) {
//                        w = get;
//                    }
//                    writers.get(t.getNodeName()).write(p.getKey().author1 + " " + p.getKey().author2 + " " + w + "\n");
//                }
//            }
        } catch (IOException e) {
            Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException();
        }
    }

    private static void writeGraphToFile(Set<AuthorsPair> graph, Scanner sc, Writer wr) throws IOException {
        Map<AuthorsPair, Integer> map = new TreeMap<>();
        while (sc.hasNextLine()) {
            String s = sc.nextLine();
            String[] split = s.split(" ");
            int a1 = Integer.parseInt(split[0]);
            int a2 = Integer.parseInt(split[1]);
            if (a1 == 1_143_439 || a2 == 1_143_439) {
                System.out.println("yes");
            }
            AuthorsPair authorsPair = new AuthorsPair(a1, a2);
            Integer get = map.get(authorsPair);
            if (get == null) {
                get = 0;
            }
            map.put(authorsPair, get + 1);
        }
        for (AuthorsPair p : graph) {
            Integer get = map.get(p);
            Integer weight = 1;
            if (get != null) {
                weight = get * 20;
            }
            wr.write(p.author1 + " " + p.author2 + " " + weight + "\n");
        }
    }

    private static void findRepresentatives(String clustersFile, String mappingFile, String graphFile) {
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("clusters.txt")))) {
            Map<Integer, String> mapping = new TreeMap<>();
            try (Scanner mappings = new Scanner(new BufferedInputStream(new FileInputStream(mappingFile)))) {
                while (mappings.hasNextLine()) {
                    String s = mappings.nextLine();
                    String[] split = s.split(" ", 2);
                    mapping.put(Integer.parseInt(split[0]), split[1]);
                }
            }
            Map<Integer, List<Integer>> clusters = new TreeMap<>();
            Map<Integer, Integer> cluster = new TreeMap<>();
            try (Scanner clustersReader = new Scanner(new BufferedInputStream(new FileInputStream(clustersFile)))) {
                while (clustersReader.hasNextLine()) {
                    String s = clustersReader.nextLine();
                    String[] split = s.split(" ");
                    List<Integer> get = clusters.get(Integer.parseInt(split[1]));
                    if (get == null) {
                        get = new LinkedList<>();
                    }
                    final int parseInt = Integer.parseInt(split[0]);
                    get.add(parseInt);
                    clusters.put(Integer.parseInt(split[1]), get);
                    cluster.put(parseInt, Integer.parseInt(split[1]));
                }
            }

            Map<Integer, Integer> counts = new TreeMap<>();
            try (Scanner graph = new Scanner(new BufferedInputStream(new FileInputStream(graphFile)))) {
                while (graph.hasNextLine()) {
                    String s = graph.nextLine();
                    String[] split = s.split(" ");
                    Integer src = Integer.parseInt(split[0]);
                    Integer dst = Integer.parseInt(split[1]);
                    if (cluster.get(src).equals(cluster.get(dst))) {
                        Integer get = counts.get(src);
                        if (get == null) {
                            get = 0;
                        }
                        get++;
                        counts.put(src, get);
                        get = counts.get(dst);
                        if (get == null) {
                            get = 0;
                        }
                        get++;
                        counts.put(dst, get);
                    }
                }
            }
            for (Map.Entry<Integer, List<Integer>> e : clusters.entrySet()) {
                Integer key = e.getKey();
                List<Integer> value = e.getValue();
                Map<Integer, Integer> map = new TreeMap<>();
                for (Integer i : value) {
                    Integer get = counts.get(i);
                    if (get == null) {
                        get = 0;
                    }
                    map.put(i, get);
                }
                Map<Integer, Integer> sortByValue = MapUtil.sortByValue(map);
                writer.write(key + "\n");
                for (Map.Entry<Integer, Integer> s : sortByValue.entrySet()) {
                    writer.write(mapping.get(s.getKey()) + " " + s.getValue() + "\n");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void convertWeightsToGraph(String weightsFile, String graphFile, String mappingFile) {
        int count = 0;
        Map<String, Integer> map = new TreeMap<>();
        try (Writer mappingWriter = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(mappingFile)))) {
            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(graphFile)))) {
                try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(weightsFile)))) {
                    while (sc.hasNextLine()) {
                        String s = sc.nextLine();
                        String[] split = s.split("::");
//                        String id = split[0].substring(0, split[0].indexOf(" "));
                        String id = split[0];
                        Integer src = map.get(id);
                        if (src == null) {
                            src = map.size();
                            map.put(id, src);
                            mappingWriter.write(src + " " + id + "\n");
                        }
                        String[] split1 = split[1].split(";");
                        Map<Integer, Double> tmp = new TreeMap<>();
//                        if (split1.length != 524) {
//                            throw new RuntimeException(s + " " + id);
//                        }
                        for (String split11 : split1) {
                            String[] split2 = split11.split(": ");
                            String topic = split2[0].replace("\"", "");
                            Integer dst = map.get(topic);
                            if (dst == null) {
                                dst = map.size();
                                map.put(topic, dst);
                                mappingWriter.write(dst + " " + topic + "\n");
                            }
                            Double val = Double.parseDouble(split2[1]);
                            tmp.put(dst, val);
                        }
                        Map<Integer, Double> sortByValue = MapUtil.sortByValue(tmp);
                        int i = 0;
//                    writer.write(id + "\n");
                        for (Map.Entry<Integer, Double> e : sortByValue.entrySet()) {
                            i++;
                            if (i == 4) {
                                break;
                            }
                            writer.write(src + " " + e.getKey() + "\n");
                        }
                        count++;
                        if (count % 1_000 == 0) {
                            System.out.println(count);
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
