/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.evaluation.person;

import com.google.common.base.Objects;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.evaluation.Evaluator;
import ir.ac.ut.iis.person.paper.PapersInvertedRetriever;
import ir.ac.ut.iis.person.paper.PapersRetriever;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryExpander;
import ir.ac.ut.iis.person.query.QueryExpander.Profile;
import ir.ac.ut.iis.retrieval_tools.other.StopWordsExtractor;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
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
public class PERSONEvaluator implements Evaluator {

    private IndexSearcher searcher;
    private Profile profileCache;

    protected Map<String, Double> findRelevants(String docId, Integer year, Set<String> ignoredSelfCitations) {
        searcher.setSimilarity(new ClassicSimilarity());
        final Map<String, Double> map = new HashMap<>();
        String get;
        String[] authors;
        try {
            TopDocs results = searcher.search(new TermQuery(new Term("id", docId)), 1);
            get = searcher.getIndexReader().document(results.scoreDocs[0].doc).get("refs");
            authors = searcher.getIndexReader().document(results.scoreDocs[0].doc).get("authors").split(" ");
        } catch (IOException ex) {
            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        for (String r : get.split(" ")) {
            if (r.equals(docId)) {
                return null;
            }
            String n = null;
            try {
                TopDocs results = searcher.search(new TermQuery(new Term("id", r)), 1);
                if (results.totalHits != 0) {
                    n = String.valueOf(results.scoreDocs[0].doc);
                    if (Configs.yearFiltering) {
                        int refYear = Integer.parseInt(searcher.getIndexReader().document(results.scoreDocs[0].doc).get("year"));
                        if (refYear > year) {
                            continue;
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            if (n != null) {
                String[] refAuthors;
                try {
                    final Document document = searcher.getIndexReader().document(Integer.parseInt(n));
                    refAuthors = document.get("authors").split(" ");
                } catch (IOException ex) {
                    Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
                boolean check = true;
                if (Configs.ignoreSelfCitations) {
                    for (String s : refAuthors) {
                        for (String t : authors) {
                            if (t.equals(s)) {
                                check = false;
                                break;
                            }
//                            if (Configs.ignoreSelfCitations == 1) {
//                                break;
//                            }
                        }
                    }
                }
                if (check) {
                    map.put(n, 1.);
                } else {
                    ignoredSelfCitations.add(r);
                }
            }
        }
        if (Configs.ignoreSelfCitations) {
            for (String a : authors) {
                try {
                    TopDocs papers = this.searcher.search(new TermQuery(new Term("authors", a)), 1_000_000);
                    for (ScoreDoc td : papers.scoreDocs) {
                        final String document = searcher.getIndexReader().document(td.doc).get("id");
                        ignoredSelfCitations.add(document);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            ignoredSelfCitations.remove(docId);
            if (ignoredSelfCitations.size() > 1_024) {
                return null;
            }
        }

        if (map.size() <= 5) {
            return null;
        }
        return map;
    }

    @Override
    public Query convertToQuery(String queryDocId, Integer queryIndexId, String query, String searcher, int year, String authorsS) {
        final Map<String, Double> relevants;
        Set<String> ignoredSelfCitations = null;
        if (Configs.ignoreSelfCitations) {
            ignoredSelfCitations = new HashSet<>();
        }
        relevants = findRelevants(queryDocId, year, ignoredSelfCitations);

        if (relevants == null) {
            return null;
        }

        String[] authors = null;
        try {
            TopDocs papers = this.searcher.search(new TermQuery(new Term("authors", searcher)), 1_000_000);
            if (papers.totalHits <= Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers
                    || (papers.totalHits >= Configs.onlyQueriesWhoseAuthorHasLessThan_THIS_Papers && Configs.onlyQueriesWhoseAuthorHasLessThan_THIS_Papers != -1)) {
                return null;
            }
            authors = authorsS.split(" ");
        } catch (IOException ex) {
            Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        Set<String> authorsSet = authors == null ? null : new HashSet<>(Arrays.asList(authors));
        return new Query(queryDocId, queryIndexId, query, Integer.parseInt(searcher), year, relevants, authorsSet, ignoredSelfCitations);
    }

    @Override
    public Map<String, QueryExpander.WordStats> getProfile(int userId, Query query, int k) {
        StringBuilder profile = new StringBuilder();
        if (profileCache == null || !Objects.equal(profileCache.queryIndexId, query.getQueryIndexId()) || !(profileCache.searcher == userId)) {
            searcher.setSimilarity(new ClassicSimilarity());
            Set<String> keySet = null;
            if (Configs.dontUseAuthorsRelevantPapersInProfile) {
                keySet = query.getRelevants().keySet();
            }

            try {
                List<Integer> papers = getProfile(searcher, userId);

                for (Integer d : papers) {
                    if (Configs.ignoreQueryDocumentInQueryExpansion == false || !d.equals(query.getQueryIndexId())) {
                        if (Configs.dontUseAuthorsRelevantPapersInProfile == false || !keySet.contains(String.valueOf(d))) {
                            String content = searcher.getIndexReader().document(d).get("content");
                            profile.append(" ").append(content);
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }

            profileCache = new QueryExpander.Profile(userId, query.getQueryIndexId(), profile.toString());
        }
        String p = profileCache.profile;
        if (p.isEmpty()) {
            throw new IgnoreQueryEx("No profile to personalize");
        }
//        System.out.println("Profile: " + profile);
//        System.out.println("----------");
        return prepareProfileTerms(p, k);
    }

    protected List<Integer> getProfile(IndexSearcher searcher, int userId) throws IOException {
        TopDocs list = searcher.search(new TermQuery(new Term("authors", String.valueOf(userId))), 1_000_000);
        List<Integer> papers = new LinkedList<>();
        for (ScoreDoc d : list.scoreDocs) {
            papers.add(d.doc);
        }
        return papers;
    }

    private Map<String, QueryExpander.WordStats> prepareProfileTerms(String profile, int k) {
        Map<String, Double> w = new HashMap<>();

        List<String> tokenizeString = PapersRetriever.tokenizeString(profile);
        for (String s : tokenizeString) {
            Double get = w.get(s);
            if (get == null) {
                get = 0.;
            }
            get += 1;
            w.put(s, get);
        }

        Map<String, QueryExpander.WordStats> map = new HashMap<>();
        int numDocs = searcher.getIndexReader().numDocs();

        for (Map.Entry<String, Double> w1 : w.entrySet()) {
            Integer df = DatasetMain.getInstance().getDF(w1.getKey());
//            try {
//                df = searcher.getIndexReader().docFreq(new Term("content", w1.getKey()));
//            } catch (IOException ex) {
//                Logger.getLogger(QueryExpander.class.getName()).log(Level.SEVERE, null, ex);
//                throw new RuntimeException();
//            }
            if (df != null) {
                Double tf = w1.getValue();
                if (Configs.useLogTFInProfilesCreation) {
                    tf = Math.log(tf);
                }
                map.put(w1.getKey(), new QueryExpander.WordStats(tf * (Math.log(numDocs / (double) (df + 1)) + 1.0), (Math.log(numDocs / (double) (df + 1)) + 1.0)));
            }
        }

        Map<String, QueryExpander.WordStats> sortByValue = StopWordsExtractor.MapUtil.sortByValue(map);
        Map<String, QueryExpander.WordStats> result = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, QueryExpander.WordStats> w1 : sortByValue.entrySet()) {
            i++;
            if (i > k) {
                break;
            }
            result.put(w1.getKey(), w1.getValue());
        }

        return result;
    }

    @Override
    public String getName() {
        return "PERSON[" + Configs.personParameters() + "]";
    }

    @Override
    public void setSearcher(IndexSearcher searcher) {
        this.searcher = searcher;
    }

    @Override
    public void setBaselineSearcher(BasicSearcher baselineSearcher) {
    }

    @Override
    public boolean ignoreUnfoundRelevants() {
        return true;
    }

}
