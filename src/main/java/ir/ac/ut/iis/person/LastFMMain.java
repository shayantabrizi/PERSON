/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person;

import ir.ac.ut.iis.person.datasets.lastfm.LastFMExtractor;
import ir.ac.ut.iis.person.datasets.lastfm.LastFMRetriever;
import ir.ac.ut.iis.person.datasets.lastfm.LastFMUserFactory;
import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class LastFMMain extends DatasetMain {

    public void main() {
        Configs.datasetRoot += "LastFM/";
        DatasetMain.setInstance(this);
        uf = new LastFMUserFactory();
//        createLastFMIndexes("/home/shayan/Desktop/Taval/thesis/lastfm/lastdb_small/hier_friends_clean.tree", "../datasets/lastfm/lastfm-index", "../datasets/lastfm/lastfm-index/general", false);
        initialize();
    }

    @Override
    protected Hierarchy loadHierarchy() {
        Hierarchy hier;
        try {
            hier = new Hierarchy<>("general");
            hier.load(false, "../datasets/lastfm/lastfm-index", "../datasets/lastfm/lastfm-index/general-only", "/home/shayan/Desktop/Taval/thesis/lastfm/lastdb_small/hier_friends_clean.tree", false);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        return hier;
    }

    public void initialize() {
        final LastFMExtractor lastFMExtractor = new LastFMExtractor();

        String name = Configs.queryField + "," + Configs.baseSimilarityName + ",NOR=" + Configs.numOfResults + ",MB=" + Configs.mapBias + ",DUMPAQ=" + Configs.dontUseMergedPapersAsQuery + ",IQH=" + Configs.InappropriateQueriesHeuristic + ",ISC=" + Configs.ignoreSelfCitations + ",YF=" + Configs.yearFiltering + ",OQWAHMTTP=" + Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers + ",OQWAHLTTP=" + Configs.onlyQueriesWhoseAuthorHasLessThan_THIS_Papers;

        Hierarchy hier = loadHierarchy();
        lastFMExtractor.loadTracks(hier, "/home/shayan/Desktop/Taval/thesis/lastfm/lastdb_small/users_track.csv", "/home/shayan/Desktop/Taval/thesis/lastfm/lastdb_small/track_tag.csv");
        Main.retriever = new LastFMRetriever(openSearcher(), name, null, "/home/shayan/Desktop/Taval/thesis/citedata/crowled/queriesFinal.csv", hier.getUserNodeMapping(), lastFMExtractor.getTrackMap());
        Main.outputPath = "../datasets/lastfm/results/" + name;

        hier.readGraph("/home/shayan/Desktop/Taval/thesis/lastfm/lastdb_small/friends.csv", false);

        Configs.database_name = "lastdb_small";
    }

    private static void createIndexes(String tree, String rootIndex, String index, boolean makeRootIndex) {
        try {
            Hierarchy hier = new Hierarchy<>("temp");
            hier.load(true, rootIndex, index, !makeRootIndex ? tree : null, false);

            LastFMExtractor extractor = new LastFMExtractor();
            extractor.doParseAndIndex(hier, makeRootIndex, "/home/shayan/Desktop/Taval/thesis/lastfm/lastdb_small/users_track.csv", "/home/shayan/Desktop/Taval/thesis/lastfm/lastdb_small/track_tag.csv");
            hier.getRootNode().close();
            System.exit(0);

        } catch (IOException ex) {
            Logger.getLogger(Main.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public static void temp() {
//        double conservativeness = .01;
//        double alpha = .75;
//        LastFM fm = new LastFM();
//        Node node = null;
//        LastFMTSSimilarityMinLevel lastFMTsSimilarityDiritchlet = new LastFMTSSimilarityMinLevel(conservativeness, alpha, node, fm);
//        HirarchyQueryParser hirarchyQueryParser = new HirarchyQueryParser(loadAnalyzer(), lastFMTsSimilarityDiritchlet);
//
//    }
//
//    public static AnalyzerWrapper loadAnalyzer() {
//        return new AnalyzerWrapper() {
//            @Override
//            protected Analyzer getWrappedAnalyzer(String string) {
//                return new StandardAnalyzer(Version.LUCENE_40, new CharArraySet(Version.LUCENE_40, Config.stopWords, true));
//            }
//
//            @Override
//            protected Analyzer.TokenStreamComponents wrapComponents(String string, Analyzer.TokenStreamComponents tsc) {
//                if (string.equals("id")) {
//                    return tsc;
//                }
//                return tsc;
//                //return new Analyzer.TokenStreamComponents(tsc.getTokenizer(), (new PorterStemFilterFactory()).create(tsc.getTokenStream()));
//            }
//        };
//    }
//    public static void main(String[] args) throws IOException {
//        Map<String, Map<String, Integer>> map = new TreeMap<>();
//        String[] list = new String[164597];
//        int k = 0;
//        try (Scanner docs = new Scanner(new FileInputStream("docs.txt"))) {
//            while (docs.hasNextLine()) {
//                String s = docs.nextLine();
//                if (s.isEmpty()) {
//                    continue;
//                }
//                list[k] = s;
//                k++;
//            }
//        }
//        try (Scanner jud = new Scanner(new FileInputStream("jud-ap.txt"))) {
//            while (jud.hasNextLine()) {
//                String s = jud.nextLine();
//                if (s.isEmpty()) {
//                    continue;
//                }
//                String[] split = s.split(" ");
//                Map<String, Integer> get = map.get(split[0]);
//                if (get == null) {
//                    get = new TreeMap<>();
//                    map.put(split[0], get);
//                }
//                get.put(split[2], Integer.parseInt(split[3]));
//            }
//        }
//        int n = 5;
//        createJudgeFile(n, map, list);
//        eval("res.simple_kl_dir");
//        eval("res.simple_okapi");
//        eval("res.simple_tfidf");
//    }
//
//    private static void eval(String result) throws IOException {
//        Runtime rt = Runtime.getRuntime();
//        Process pr = rt.exec("trec_eval -m map my-judge.txt " + result);
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//            throw new RuntimeException();
//        }
//        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//
//        String line;
//
//        while ((line = input.readLine()) != null) {
//            System.out.println(line);
//        }
//        
//    }
//
//    private static void createJudgeFile(int n, Map<String, Map<String, Integer>> map, String[] list) throws IOException {
//        try (Writer out = new OutputStreamWriter(new FileOutputStream(n + "my-judge.txt"))) {
//            for (Map.Entry<String, Map<String, Integer>> e : map.entrySet()) {
//                Map<String, Integer> qValue = e.getValue();
//                for (Map.Entry<String, Integer> t : qValue.entrySet()) {
//                    out.write(e.getKey() + " 0 " + t.getKey() + " " + t.getValue());
//                }
//                for (int i = 0; i < n; i++) {
//                    boolean check = true;
//
//                    String s = null;
//                    while (check) {
//                        s = list[(int) (random.nextDouble() * list.length)];
//                        if (!qValue.containsKey(s)) {
//                            check = false;
//                        }
//                    }
//                    out.write(e.getKey() + " 0 " + s + " " + "1");
//                }
//            }
//        }
//    }
    @Override
    public Hierarchy loadHierarchy(String graphFile, String clustersFile, String name, boolean ignoreLastWeight, boolean addNodesAsClusters) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
