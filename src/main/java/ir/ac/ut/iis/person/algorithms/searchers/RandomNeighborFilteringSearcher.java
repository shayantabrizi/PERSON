/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.searchers;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.Query.Result;
import ir.ac.ut.iis.person.query.QueryConverter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public class RandomNeighborFilteringSearcher extends BasicSearcher {

    private final RandomNeighborSearcher rns;
    private final boolean considerFriendsOfFriends;

    public RandomNeighborFilteringSearcher(RandomNeighborSearcher rns, IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, boolean considerFriendsOfFriends) {
        super(searcher, name, similarity, queryConverter);
        this.rns = rns;
        this.considerFriendsOfFriends = considerFriendsOfFriends;
    }

    @Override
    public List<Query.Result> doSearch(Query q, int numOfResults) {
        List<Query.Result> search = super.doSearch(q, Math.round(Math.min(numOfResults, numOfResults * Configs.ratioOfCandidateToResults - 2)));
        Set<Integer> set = new HashSet<>();
        for (Query.Result r : search) {
            set.add(r.getDocId());
        }

        RandomNeighborSearcher.CacheResult paperLists = rns.getPaperLists(q, 10_000, 10_000);

        List<Result> list = new LinkedList<>();
        for (Integer e : paperLists.cachedProfile0) {
            if (list.size() == numOfResults) {
                break;
            }
            if (set.remove(e)) {
                list.add(new Result(e));
            }
        }

        for (Integer e : paperLists.cachedProfile1) {
            if (list.size() == numOfResults) {
                break;
            }
            if (set.remove(e)) {
                list.add(new Result(e));
            }
        }

        if (considerFriendsOfFriends) {
            for (Integer e : paperLists.cachedProfile2) {
                if (list.size() == numOfResults) {
                    break;
                }
                if (set.remove(e)) {
                    list.add(new Result(e));
                }
            }
        }
        for (Result e : search) {
            if (list.size() == numOfResults) {
                break;
            }
            if (set.contains(e.getDocId())) {
                list.add(e);
            }
        }

        q.addResult(getName(), list);
        return list;
    }
}
