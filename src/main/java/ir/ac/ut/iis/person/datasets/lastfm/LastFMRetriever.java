package ir.ac.ut.iis.person.datasets.lastfm;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import ir.ac.ut.iis.person.Main;
import ir.ac.ut.iis.person.base.Retriever;
import ir.ac.ut.iis.person.evaluation.Evaluator;
import ir.ac.ut.iis.person.hierarchy.GraphNode;
import ir.ac.ut.iis.person.hierarchy.User;
import ir.ac.ut.iis.person.paper.PapersRetriever;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryBatch;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 *
 * @author shayan
 */
public class LastFMRetriever extends Retriever {

    private final Int2ObjectOpenHashMap<GraphNode> usersMap;
    private final Map<String, Track> tracksMap;
    private final IndexSearcher rootSearcher;
    private final Scanner scanner;

    public LastFMRetriever(IndexSearcher rootSearcher, String name, Evaluator evaluator, String queryFile, Int2ObjectOpenHashMap<GraphNode> usersMap, Map<String, Track> tracksMap) {
        super(name, evaluator);
        this.rootSearcher = rootSearcher;
        try {
            this.scanner = new Scanner(new FileInputStream(queryFile));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LastFMRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        this.usersMap = usersMap;
        this.tracksMap = tracksMap;
    }

    @Override
    public boolean hasNextQueryBatch() {
        return scanner.hasNextLine();
    }

    @Override
    public QueryBatch nextQueryBatch() {
        String[] nextLine = scanner.nextLine().split(";");
        String queryId = nextLine[0];
        String query = nextLine[1];
        String searcher = chooseRandomOwner();
        rootSearcher.setSimilarity(new ClassicSimilarity());
        query = prepareQuery(query);
        final Map<String, Double> relevants = findRelevants(searcher);
        if (relevants == null || relevants.isEmpty()) {
            return nextQueryBatch();
        }
        return new QueryBatch(queryId, new Query(queryId, null, query, Integer.parseInt(searcher), null, relevants, null, null));
    }

    public String chooseRandomOwner() {
        final IntSet users = usersMap.keySet();
        Integer random = Main.random(users.size());
        IntIterator iterator = users.iterator();
        Integer searcher = null;
        for (int i = 0; i <= random; i++) {
            searcher = iterator.nextInt();
        }
        return String.valueOf(searcher);
    }

    protected Map<String, Double> findRelevants(String user) {
        final LastFMUser u = (LastFMUser) usersMap.get(Integer.parseInt(user)).getId();
        final Map<String, Double> map = new TreeMap<>();
        rootSearcher.setSimilarity(new ClassicSimilarity());
        for (Map.Entry<Track, Integer> e : u.getDocWeights().entrySet()) {
            int d;
            try {
                TopDocs results = rootSearcher.search(new TermQuery(new Term("id", String.valueOf(e.getKey().getId()))), 1);
                if (results.scoreDocs.length == 0) {
                    throw new RuntimeException();
                }
                d = results.scoreDocs[0].doc;
            } catch (IOException ex) {
                Logger.getLogger(LastFMRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            map.put(String.valueOf(d), e.getValue().doubleValue());
        }
        return map;
    }

    @Override
    protected void checkRelevancy(Query query) {
        PapersRetriever.setRelevancy(query, false);
    }

    @Override
    public Set<User> getPublishers(String docId) {
        return new TreeSet<>(tracksMap.get(docId).getUsers());
    }

    @Override
    public void skipQueryBatch(int n) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
