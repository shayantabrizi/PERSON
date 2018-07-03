/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.searchers;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.Main;
import ir.ac.ut.iis.person.algorithms.social_textual.citeseerx.CiteseerxSocialTextualValueSource;
import ir.ac.ut.iis.person.base.Searcher;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.Query.Result;
import ir.ac.ut.iis.person.query.QueryConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

/**
 *
 * @author shayan
 */
public class RandomNeighborSearcher extends Searcher {

    private final IndexSearcher searcher;
    private CacheResult cachedResult = null;

    public RandomNeighborSearcher(IndexSearcher searcher, String name, QueryConverter queryConverter) {
        super(name, queryConverter);
        this.searcher = searcher;
    }

    @Override
    public List<Result> search(Query q, int numOfResults) {
        CacheResult paperLists = getPaperLists(q, numOfResults, 2_000);
        List<Result> list = new LinkedList<>();
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < numOfResults; i++) {
            int docId;
            if (!paperLists.cachedProfile0.isEmpty()) {
                int rnd = Main.random(paperLists.cachedProfile0.size());
                docId = paperLists.cachedProfile0.get(rnd);
                for (Iterator<Integer> it = paperLists.cachedProfile0.iterator(); it.hasNext();) {
                    Integer next = it.next();
                    if (next.equals(docId)) {
                        it.remove();
                    }
                }
//                if (set.contains(docId)) {
//                    throw new RuntimeException();
//                }
            } else if (paperLists.cachedProfile1 != null) {
                paperLists.cachedProfile0 = paperLists.cachedProfile1;
                paperLists.cachedProfile1 = null;
                i--;
                continue;
            } else if (paperLists.cachedProfile2 != null) {
                paperLists.cachedProfile0 = paperLists.cachedProfile2;
                paperLists.cachedProfile2 = null;
                i--;
                continue;
            } else {
                docId = Main.random(searcher.getIndexReader().maxDoc());
            }

            if (Configs.yearFiltering || Configs.ignoreSelfCitations) {
                try {
                    Document document = searcher.getIndexReader().document(docId);
                    boolean check = false;
                    if (Configs.yearFiltering) {
                        if (Integer.parseInt(document.get("year")) > q.getYear()) {
                            check = true;
                        }
                    }
                    if (q.getIgnoredResults() != null) {
                        for (String s : q.getIgnoredResults()) {
                            if (s.equals(document.get("id"))) {
                                check = true;
                                break;
                            }
                        }
                    }
                    if (check) {
                        i--;
                        continue;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(RandomSearcher.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            if (set.contains(docId)) {
                i--;
                continue;
            }
            set.add(docId);
            list.add(new Result(docId));
        }
        q.addResult(getName(), list);
        return list;
    }

    public CacheResult getPaperLists(Query q, int numOfResults, int maxResults) {
        if (cachedResult == null || !(q.getSearcher() == cachedResult.cachedOwner)) {
            List<Integer> arr0 = new ArrayList<>();
            List<Integer> arr1 = new ArrayList<>();
            List<Integer> arr2 = new ArrayList<>();
            cachedResult = new CacheResult(q.getSearcher(), arr0, arr1, arr2);

            try (CiteseerxSocialTextualValueSource myValueSource = new CiteseerxSocialTextualValueSource(Configs.database_name)) {
                Set<Integer> uniqueDocs = new HashSet<>();

                boolean ch = false;
                CiteseerxSocialTextualValueSource.QueryResult temp;
                Map<Integer, Double> userTrackWeightSimple;
                if (!Configs.ignoreSelfCitations) {
                    temp = myValueSource.userTrackWeightSimple(q.getSearcher());
                    userTrackWeightSimple = temp.map;
                    for (Integer t : userTrackWeightSimple.keySet()) {
                        arr0.add(t);
                        uniqueDocs.add(t);
                    }
//                    if (uniqueDocs.size() >= numOfResults) {
//                        ch = true;
//                    }
                }

                Map<Integer, Double> friends = myValueSource.getFriends(q.getSearcher(), 2);
                Set<Integer> friends1 = new HashSet<>();
                Set<Integer> friends2 = new HashSet<>();
                for (Map.Entry<Integer, Double> e : friends.entrySet()) {
                    if (e.getValue().equals(1)) {
                        friends1.add(e.getKey());
                    } else {
                        friends2.add(e.getKey());
                    }
                }

                if (ch == false) {
                    for (Integer f : friends1) {
                        if (q.getAuthors() == null) {
                            if (Configs.ignoreSelfCitations && q.getAuthors().contains(String.valueOf(f))) {
                                continue;
                            }
                        }
                        temp = myValueSource.userTrackWeightSimple(f);
                        userTrackWeightSimple = temp.map;
                        for (Integer t : userTrackWeightSimple.keySet()) {
                            arr1.add(t);
                            uniqueDocs.add(t);
                        }
//                            if (uniqueDocs.size() >= numOfResults) {
//                                ch = true;
//                                break;
//                            }
                        if (arr0.size() + arr1.size() > maxResults) {
                            ch = true;
                            break;
                        }
                    }
                }
                if (ch == false) {
                    for (Integer f : friends2) {
                        temp = myValueSource.userTrackWeightSimple(f);
                        userTrackWeightSimple = temp.map;
                        for (Integer t : userTrackWeightSimple.keySet()) {
                            if (!uniqueDocs.contains(t)) {
                                arr2.add(t);
                            }
                        }
                        if (arr0.size() + arr1.size() + arr2.size() > maxResults) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new CacheResult(cachedResult.cachedOwner, new LinkedList<>(cachedResult.cachedProfile0), new LinkedList<>(cachedResult.cachedProfile1), new LinkedList<>(cachedResult.cachedProfile2));
    }

    @Override
    public String explain(ir.ac.ut.iis.person.query.Query q, int docId) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    public static class CacheResult {

        public int cachedOwner;
        public List<Integer> cachedProfile0;
        public List<Integer> cachedProfile1;
        public List<Integer> cachedProfile2;

        public CacheResult(int cachedOwner, List<Integer> cachedProfile0, List<Integer> cachedProfile1, List<Integer> cachedProfile2) {
            this.cachedOwner = cachedOwner;
            this.cachedProfile0 = cachedProfile0;
            this.cachedProfile1 = cachedProfile1;
            this.cachedProfile2 = cachedProfile2;
        }

    }

}
