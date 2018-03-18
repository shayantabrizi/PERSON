/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.evaluation.aspire;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.evaluation.Evaluator;
import ir.ac.ut.iis.person.paper.PapersInvertedRetriever;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryConverter;
import ir.ac.ut.iis.person.query.QueryExpander;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author shayan
 */
public class ASPIREEvaluator implements Evaluator {

    private final Map<Integer, List<Integer>> clusters = new HashMap<>();
    private final Map<String, Map<String, QueryExpander.WordStats>> profiles = new HashMap<>();
    private IndexSearcher searcher;
    private BasicSearcher baselineSearcher;
    private final int topkRel;
    private static final Set<String> badDocuments = new HashSet<>();

    public ASPIREEvaluator(String clustersFileName, String profilesFileName, int topkRel) {
        if (Configs.badDocumentsFileName != null) {
            try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(Configs.datasetRoot + "topics/" + Configs.topicsName + "/" + Configs.badDocumentsFileName)))) {
                while (sc.hasNextLine()) {
                    String nextLine = sc.nextLine();
                    badDocuments.add(nextLine);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ASPIREEvaluator.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }
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
            throw new RuntimeException(ex);
        }
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(profilesFileName)))) {
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                String[] split = nextLine.split(":");
                String[] split1 = split[1].split(";");
                Map<String, QueryExpander.WordStats> w = new HashMap<>();
                for (String split11 : split1) {
                    String[] split2 = split11.split(" ");
                    final double parseDouble = Double.parseDouble(split2[1]);
                    w.put(split2[0], new QueryExpander.WordStats(parseDouble, parseDouble));
                }
                profiles.put(split[0], w);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(QueryExpander.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        this.topkRel = topkRel;
    }

    private String updateSearcher(String queryDocId) {
        for (Map.Entry<Integer, List<Integer>> e : clusters.entrySet()) {
            if (e.getValue().contains(Integer.parseInt(queryDocId))) {
                return e.getKey().toString();
            }
        }
        throw new RuntimeException();
    }

    private Map<String, Double> findRelevants(Query query) {
        List<Integer> get = clusters.get(query.getSearcher());
        Map<String, Double> idMap = new HashMap<>();
        try {
            QueryConverter.ParsedQuery convertedQuery = baselineSearcher.getQueryConverter().run(query);
            TopDocs rets = baselineSearcher.doSearch(convertedQuery, query, topkRel).td;

            for (ScoreDoc d : rets.scoreDocs) {
                Document document = searcher.getIndexReader().document(d.doc);
                if (Configs.yearFiltering) {
                    if (Integer.parseInt(document.get("year")) > query.getYear()) {
                        throw new RuntimeException();
                    }
                }
                if (get.contains(Integer.parseInt(document.get("id")))) {
                    idMap.put(String.valueOf(d.doc), 1.);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return idMap;
    }

    @Override
    public Query convertToQuery(String queryDocId, Integer queryIndexId, String query, String searcher, int year, String authorsS) {
        if (badDocuments.contains(queryDocId)) {
            return null;
        }
        final Map<String, Double> relevants;
        searcher = updateSearcher(queryDocId);
        relevants = findRelevants(new Query(queryDocId, queryIndexId, query, Integer.parseInt(searcher), year, null, null, null));
        return new Query(queryDocId, queryIndexId, query, Integer.parseInt(searcher), year, relevants, null, null);
    }

    @Override
    public Map<String, QueryExpander.WordStats> getProfile(int userId, Query query, int k) {
        return profiles.get(userId);
    }

    @Override
    public String getName() {
        return "ASPIRE[" + Configs.aspireParameters() + "]";
    }

    @Override
    public void setSearcher(IndexSearcher searcher) {
        this.searcher = searcher;
    }

    @Override
    public void setBaselineSearcher(BasicSearcher baselineSearcher) {
        this.baselineSearcher = baselineSearcher;
    }

    @Override
    public boolean ignoreUnfoundRelevants() {
        return false;
    }

}
