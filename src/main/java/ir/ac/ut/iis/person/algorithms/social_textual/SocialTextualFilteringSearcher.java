//package ir.ac.ut.iis.person.algorithms.social_textual;
//
///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//import ir.ac.ut.iis.person.Configs;
//import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
//import ir.ac.ut.iis.person.algorithms.social_textual.citeseerx.CiteseerxSocialTextualValueSource;
//import ir.ac.ut.iis.person.query.Query;
//import ir.ac.ut.iis.person.query.Query.Result;
//import ir.ac.ut.iis.person.query.QueryConverter;
//import ir.ac.ut.iis.retrieval_tools.other.StopWordsExtractor;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.similarities.Similarity;
//
///**
// *
// * @author shayan
// */
//public class SocialTextualFilteringSearcher extends BasicSearcher {
//
//    int numOfResults;
//
//    public SocialTextualFilteringSearcher(IndexSearcher searcher, String name, Similarity similarity, QueryConverter queryConverter, int numOfResults) {
//        super(searcher, name, similarity, queryConverter);
//        this.numOfResults = numOfResults;
//    }
//
//    @Override
//    public List<Query.Result> search(Query q, int numOfResults) {
//        List<Query.Result> search = super.search(q, numOfResults);
//        HashMap<Integer, Double> userTrackWeightSN;
//        try (CiteseerxSocialTextualValueSource myValueSource = new CiteseerxSocialTextualValueSource(q.getSearcher())) {
//            userTrackWeightSN = myValueSource.calcWeights(q.getSearcher(), Configs.socialTextualDegree);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        Map<String, Double> map = new TreeMap<>();
//        for (Result s : search) {
//            Integer i = s.getDocId();
//            Double get = userTrackWeightSN.get(i);
//            if (get == null) {
//                get = 0.;
//            }
//            map.put(String.valueOf(s.getDocId()), get);
//        }
//        Map<String, Double> sortByValue = StopWordsExtractor.MapUtil.sortByValue(map);
//        List<Result> list = new LinkedList<>();
//        for (Map.Entry<String, Double> e : sortByValue.entrySet()) {
//            list.add(new Result(Integer.valueOf(e.getKey())));
//        }
//        q.addResult(getName(), list);
//        return list;
//    }
//}
