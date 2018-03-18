/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.query;

import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.paper.PapersRetriever;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shayan
 */
public class QueryConverter {

    private QueryConverter parentConverter;
    protected static final org.apache.lucene.analysis.Analyzer analyzer;
//    private Map<String, String> singularsMap = new TreeMap<>();

    static {
        analyzer = new DatasetMain.MyAnalyzer();
    }

    public QueryConverter() {
//        this.readSingulars();
    }

    protected void addAdditiveWeight(Map<String, Double> map, String word, Double w) {
        Double get = map.get(word);
        if (get == null) {
            get = 0.;
        }
        get += w;
        map.put(word, get);
    }

    protected void addMaximumWeight(Map<String, Double> map, String word, Double w) {
        Double get = map.get(word);
        if (get == null) {
            get = 0.;
        }
        get = Math.max(get, w);
        map.put(word, get);
    }

    public ParsedQuery run(Query query) {
        Map<String, Double> words = queryMapConverter(query.getQuery());

        return new ParsedQuery(words, rewriteQuery(convert(words, query.getSearcher(), query)));
    }

    public Map<String, Double> convert(Map<String, Double> words, int userId, Query query) {
        if (parentConverter != null) {
            words = parentConverter.convert(words, userId, query);
        }
        return words;
    }

    public Map<String, Double> queryMapConverter(String query) throws RuntimeException {
        Map<String, Double> words = new HashMap<>();
//        System.out.println(query);
        List<String> tokenizeString = PapersRetriever.tokenizeString(query);
        for (String s : tokenizeString) {
            addAdditiveWeight(words, s, 1.);
        }
        return words;
    }

    public static String rewriteQuery(Map<String, Double> words) {
        StringBuilder expanded = new StringBuilder();
        for (Map.Entry<String, Double> e : words.entrySet()) {
            expanded.append(e.getKey()).append("^").append(String.format("%1.12f", e.getValue())).append(" ");
        }

        return expanded.toString();
    }

//    private void readSingulars() {
//        System.out.println("Reading singulars started");
//        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(Main.citeseerxRoot+"singulars.txt")))) {
//            while (sc.hasNextLine()) {
//                String[] split = sc.nextLine().split(" ");
//                singularsMap.put(split[0], split[1]);
//            }
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(QueryExpander.class.getName()).log(Level.SEVERE, null, ex);
//            throw new RuntimeException();
//        }
//        System.out.println("Reading singulars finished");
//    }
    protected QueryConverter getParentConverter() {
        return parentConverter;
    }

    public void setParentConverter(QueryConverter parentConverter) {
        this.parentConverter = parentConverter;
    }

    public static class ParsedQuery {
        public Map<String, Double> map;
        public String query;

        public ParsedQuery(Map<String, Double> map, String query) {
            this.map = map;
            this.query = query;
        }
        
    }
}
