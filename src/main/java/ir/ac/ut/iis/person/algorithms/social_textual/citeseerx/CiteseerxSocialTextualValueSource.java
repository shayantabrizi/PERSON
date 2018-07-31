/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.social_textual.citeseerx;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.algorithms.aggregate.UserBasedValueSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author shayan (Based on a code provided by Ali Khodaei to us)
 */
public class CiteseerxSocialTextualValueSource extends UserBasedValueSource {

    private final boolean useFriendWeight = true;
    private final double selfConsiderConstant;

    HashMap<Integer, Double> userWeights = new HashMap<>();
    final int MAX_USER_DEGREE = 100;  //max number of neighbours

    public CiteseerxSocialTextualValueSource(String database_name) {
        super(database_name);
        selfConsiderConstant = Configs.selfConsiderConstant;
    }

    public CiteseerxSocialTextualValueSource(String database_name, double selfConsiderConstant) {
        super(database_name);
        this.selfConsiderConstant = selfConsiderConstant;
    }

    @Override
    public HashMap<Integer, Double> calcWeights(int userId, int degree) {

        HashMap<Integer, Double> utSNMap = new HashMap<>();//(trackid, weight)

        HashMap<Integer, Double> uuMap;
//        uuMap = getFriends(Main.random(500000), degree);
//        while (uuMap.size() < 50) {
//            uuMap = getFriends(Main.random(500000), degree);
//        }
        uuMap = getFriends(userId, degree);
//        if (uuMap.size() < 50) {
//            throw new IgnoreQueryEx("Not enough friends to personalize: " + uuMap.size());
//        }
//        System.out.println("Friends size: " + uuMap.size());
        Set<Map.Entry<Integer, Double>> set = uuMap.entrySet();
        Iterator<Map.Entry<Integer, Double>> i = set.iterator();
        Map<Integer, QueryResult> userTrackWeightSimple = userTrackWeightSimple(uuMap.keySet());
        while (i.hasNext()) {
            Map.Entry<Integer, Double> me = i.next();
            int friendId = me.getKey();
            QueryResult uMap = userTrackWeightSimple.get(friendId);
            double friendWeight = 1;
            if (useFriendWeight) {
                friendWeight = 1.0 / me.getValue();
            }
            Map<Integer, Double> utMap; //tracks for this friend
            utMap = uMap.map;
            //iterate through tracks (docs) for this friend
            Set<Map.Entry<Integer, Double>> tracksSet = utMap.entrySet(); 
            Iterator<Map.Entry<Integer, Double>> j = tracksSet.iterator();
            while (j.hasNext()) {
                Map.Entry<Integer, Double> me2 = j.next();
                double trackWeight = me2.getValue(); //normalize?
                double userWeight = uMap.friendsCoauthors;
                userWeight /= MAX_USER_DEGREE;
                userWeight = Math.min(userWeight, 1);
                double tt = Math.log(1 + userWeight);

                double weight = trackWeight * friendWeight * tt; //check!
                double newWeight = -1;
                if (utSNMap.containsKey(me2.getKey())) { //already in SN hashMap
                    double oldWeight = utSNMap.get(me2.getKey());
                    newWeight = oldWeight + weight;
                } else {  //first time we see this track
                    newWeight = weight;
                }
                //put it in the SN hash map
                utSNMap.put(me2.getKey(), newWeight);
            }
            //System.out.println(me.getKey() + " : " + me.getValue());
        }

//        for (Object e: utSNMap.entrySet()) {
//            Entry<Integer, Double> ee = (Entry<Integer, Double>) e;
//            System.out.println(userId + " " + ee.getKey() + " " + ee.getValue());
//        }
//        System.out.println("Size: " + utSNMap.size());
        return utSNMap;
    }//userTrackWeightSN

    public HashMap<Integer, Double> getFriends(int userId, int degree) {

        String query;
        Statement stmt;
        ResultSet rs;

        HashMap<Integer, Double> uuMap = new HashMap<>();

        if (degree == 2) {

            query = " select distinct f1.uid2, f2.uid2 from coauthors f1, coauthors f2 "
                    + " where f1.uid1 = " + userId + " and f1.uid2 = f2.uid1";

            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(query);

                int value;
                while (rs.next()) {
                    value = rs.getInt(1);
                    //System.out.println(value + ",1");
                    uuMap.put(value, 1.);

                    value = rs.getInt(2);
                    //System.out.println(value + ",2");
                    if (!uuMap.containsKey(value)) {
                        uuMap.put(value, 2.);
                    }
                } //end while
                stmt.close();
            } catch (SQLException e) {
                System.out.println("SQL Exception");
                e.printStackTrace();
            }
        }

        if (degree == 3) {

            query = " select distinct f1.uid2, f2.uid2, f3.uid2 from coauthors f1, coauthors f2, coauthors f3"
                    + " where f1.uid1 = " + userId + " and f1.uid2 = f2.uid1 and f2.uid2=f3.uid1";

            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(query);

                int value;
                while (rs.next()) {
                    value = rs.getInt(1);
                    //                  System.out.println(value + ",1");
                    uuMap.put(value, 1.);

                    value = rs.getInt(2);
                    final Double get = uuMap.get(value);
                    //                System.out.println(value + ",2");
                    if (get == null || get == 3.) {
                        uuMap.put(value, 2.);
                    }

                    value = rs.getInt(3);
                    //              System.out.println(value + ",3");
                    if (!uuMap.containsKey(value)) {
                        uuMap.put(value, 3.);
                    }
                } //end while
                stmt.close();
            } catch (SQLException e) {
                System.out.println("SQL Exception");
                e.printStackTrace();
            }
        }//degree == 3

        //writing the final set
//        System.out.println("-------friends for user " + userId + " , degree = " + degree + "---------");
        if (selfConsiderConstant == 0.) {
            uuMap.remove(userId);
        } else {
            uuMap.put(userId, selfConsiderConstant);
        }
        return uuMap;
    }//getFriends

    public QueryResult userTrackWeightSimple(int userId) {

        HashMap<Integer, Double> utMap = new HashMap<>();
        String query = "select paperId,numOfAuthors,numberOfAuthorsCoauthors from UserPapers where uid =" + userId;

        Statement stmt;
        ResultSet rs;

        int friendsCoauthors = 0;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            int id, palaycount;
            while (rs.next()) {
                id = rs.getInt(1);
                palaycount = rs.getInt(2);
                double playCountNormWeight = 1. / Math.sqrt(palaycount);
                //    System.out.println(id + "," + playCountNormWeight);
                friendsCoauthors = rs.getInt(3);
                utMap.put(id, playCountNormWeight);

            } //end while
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }

        //writing the final set
//        System.out.println("-------tracks for user " + userId + "---------");
        return new QueryResult(utMap, friendsCoauthors);
    }//userTrackWeightSimple

    //put them in hashmap to simulate inverted lists (instead of user,track -> score)
    public Map<Integer, QueryResult> userTrackWeightSimple(Set<Integer> friends) {

        StringBuilder sb = new StringBuilder();
        for (Integer i : friends) {
            sb.append(",").append(i);
        }
        sb.deleteCharAt(0);
        String query = "select uid,paperId,numOfAuthors,numberOfAuthorsCoauthors from UserPapers where uid in (" + sb.toString() + ")";

        Statement stmt;
        ResultSet rs;
        Map<Integer, QueryResult> map = new HashMap<>();
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                final int uid = rs.getInt(1);
                QueryResult get = map.get(uid);
                if (get == null) {
                    get = new QueryResult(new HashMap<>(), rs.getInt(4));
                    map.put(uid, get);
                }
                int id, numOfAuthors;
                id = rs.getInt(2);
                numOfAuthors = rs.getInt(3);
                double contributionInPaper = 1.;
                if (Configs.penalizeMultipleAuthorsInSocialTextual) {
                    contributionInPaper /= Math.sqrt(numOfAuthors);
                }
                //    System.out.println(id + "," + playCountNormWeight);
                get.map.put(id, contributionInPaper);
            } //end while
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }

        //writing the final set
//        System.out.println("-------tracks for user " + userId + "---------");
        return map;
    }//userTrackWeightSimple
    //user weight (e.g. popularity,degree, idf,....)

    public double getUserWeight(int userId) {

        double weight = -1;

        String query = "select count(*) from coauthors where " + " uid1 =" + userId;

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            rs.next();
            weight = rs.getDouble(1);
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }
        weight /= MAX_USER_DEGREE;
        weight = Math.min(weight, 1);
        return Math.log(1 + weight);
    }//getUSerWeight

    @Override
    public String getName() {
        return "SocialTextual-" + selfConsiderConstant;
    }

    public static class QueryResult {

        public Map<Integer, Double> map;
        public Integer friendsCoauthors;

        public QueryResult(Map<Integer, Double> map, Integer friendsCoauthors) {
            this.map = map;
            this.friendsCoauthors = friendsCoauthors;
        }

    }

}
