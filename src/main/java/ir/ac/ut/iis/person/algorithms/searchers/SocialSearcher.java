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
import ir.ac.ut.iis.retrieval_tools.other.StopWordsExtractor;
import java.io.IOException;
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
public class SocialSearcher extends Searcher {

    private final IndexSearcher searcher;

    public SocialSearcher(IndexSearcher searcher, String name, QueryConverter queryConverter) {
        super(name, queryConverter);
        this.searcher = searcher;
    }

    @Override
    public List<Query.Result> search(Query q, int numOfResults) {
        List<Result> list = new LinkedList<>();
        Set<Integer> set = new HashSet<>();
        Map<Integer, Double> userTrackWeightSN;
        try (CiteseerxSocialTextualValueSource myValueSource = new CiteseerxSocialTextualValueSource(Configs.database_name)) {
            userTrackWeightSN = myValueSource.calcWeights(q.getSearcher(), 2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<Integer, Double> sortByValue = StopWordsExtractor.MapUtil.sortByValue(userTrackWeightSN);

        Iterator<Map.Entry<Integer, Double>> it = sortByValue.entrySet().iterator();
        for (int i = 0; i < numOfResults; i++) {
            int docId;
            if (it.hasNext()) {
                docId = it.next().getKey();

                if (set.contains(docId)) {
                    throw new RuntimeException();
                }
            } else {
                docId = Main.random(searcher.getIndexReader().maxDoc());
            }
            if (set.contains(docId)) {
                i--;
                continue;
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
            set.add(docId);
            list.add(new Result(docId));
        }
        q.addResult(getName(), list);
        return list;
    }

    @Override
    public String explain(ir.ac.ut.iis.person.query.Query q, int docId) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

}
