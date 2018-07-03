/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.social_textual.lastfm;

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
public class LastFMSocialTextualValueSource extends UserBasedValueSource {

    HashMap<Integer, Double> userWeights = new HashMap<>();
    final int MAX_USER_DEGREE = 26;  //max number of neighbours

    public LastFMSocialTextualValueSource(String database_name) {
        super(database_name);
    }

    //weights calculated using SN (social network) of the user
    @Override
    public HashMap<Integer, Double> calcWeights(int userId, int degree) {

        HashMap<Integer, Double> utSNMap = new HashMap<>();//(trackid, weight)

        HashMap<Integer, Integer> uuMap;
        uuMap = getFriends(userId, degree);
        Set<Map.Entry<Integer, Integer>> set = uuMap.entrySet();
        Iterator<Map.Entry<Integer, Integer>> i = set.iterator();
        while (i.hasNext()) {
            Map.Entry<Integer, Integer> me = i.next();
            int friendId = ((Number) me.getKey()).intValue();
            double friendWeight = 1.0 / ((Number) me.getValue()).intValue();
            HashMap<Integer, Double> utMap; //tracks for this friend
            utMap = userTrackWeightSimple(friendId);
            //iterate through tracks (docs) for this friend
            Set<Map.Entry<Integer, Double>> tracksSet = utMap.entrySet();
            Iterator<Map.Entry<Integer, Double>> j = tracksSet.iterator();
            while (j.hasNext()) {
                Map.Entry<Integer, Double> me2 = j.next();
                double trackWeight = ((Number) me2.getValue()).doubleValue(); //normalize?

                Double tt = userWeights.get(friendId);
                if (tt == null) {
                    tt = getUserWeight(friendId);
                    userWeights.put(friendId, tt);
                }
                double weight = trackWeight * friendWeight * tt; //check!
                double newWeight;
                if (utSNMap.containsKey(me2.getKey())) { //already in SN hashMap
                    double oldWeight = ((Number) utSNMap.get(me2.getKey())).doubleValue();
                    newWeight = oldWeight + weight;
                } else {  //first time we see this track
                    newWeight = weight;
                }
                //put it in the SN hash map
                utSNMap.put(me2.getKey(), newWeight);
            }
            //System.out.println(me.getKey() + " : " + me.getValue());
        }

        return utSNMap;
    }//userTrackWeightSN

    public HashMap<Integer, Integer> getFriends(int userId, int degree) {

        String query;
        Statement stmt;
        ResultSet rs;

        HashMap<Integer, Integer> uuMap = new HashMap<>();

        if (degree == 1) {

            query = " select distinct friend_id from small_friends where user_id = " + userId;

            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(query);

                int value;
                while (rs.next()) {
                    value = rs.getInt(1);
                    System.out.println(value + ",1");
                    uuMap.put(value, 1);
                } //end while
                stmt.close();
            } catch (SQLException e) {
                System.out.println("SQL Exception");
                e.printStackTrace();
            }
        }

        if (degree == 2) {

            query = " select distinct f1.friend_id, f2.friend_id from small_friends f1, small_friends f2 "
                    + " where f1.user_id = " + userId + " and f1.friend_id = f2.user_id";

            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(query);

                int value;
                while (rs.next()) {
                    value = rs.getInt(1);
                    //System.out.println(value + ",1");
                    uuMap.put(value, 1);

                    value = rs.getInt(2);
                    //System.out.println(value + ",2");
                    if (!uuMap.containsKey(value)) {
                        uuMap.put(value, 2);
                    }
                } //end while
                stmt.close();
            } catch (SQLException e) {
                System.out.println("SQL Exception");
                e.printStackTrace();
            }
        }

        if (degree == 3) {

            query = "select distinct f1.friend_id, f2.friend_id, f3.friend_id from small_friends "
                    + "f1, small_friends f2, small_friends f3 where f1.user_id = " + userId + " and f1.friend_id = " + "f2.user_id and f2.friend_id = f3.user_id ";

            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(query);

                int value;
                while (rs.next()) {
                    value = rs.getInt(1);
                    //                  System.out.println(value + ",1");
                    uuMap.put(value, 1);

                    value = rs.getInt(2);
                    //                System.out.println(value + ",2");
                    if (!uuMap.containsKey(value) || ((Number) uuMap.get(value)).intValue() == 3) {
                        uuMap.put(value, 2);
                    }

                    value = rs.getInt(3);
                    //              System.out.println(value + ",3");
                    if (!uuMap.containsKey(value)) {
                        uuMap.put(value, 3);
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
        uuMap.remove(userId);
        Set<Map.Entry<Integer, Integer>> set = uuMap.entrySet();
        Iterator<Map.Entry<Integer, Integer>> i = set.iterator();
        while (i.hasNext()) {
            Map.Entry<Integer, Integer> me = i.next();
            //           System.out.println(me.getKey() + " : " + me.getValue());
        }
        return uuMap;
    }//getFriends

    //put them in hashmao to simulate inverted lists (instead of user,track -> score)
    public HashMap<Integer, Double> userTrackWeightSimple(int userId) {

        HashMap<Integer, Double> utMap = new HashMap<>();
        String query = "select distinct track_id,playcount from small_user_track where user_id =" + userId + " order by playcount desc";

        Statement stmt;
        ResultSet rs;
        int maxPlayCount = -1; //for each user
        boolean firstRow = true;

        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            int id, palaycount;
            while (rs.next()) {
                id = rs.getInt(1);
                palaycount = rs.getInt(2);
                if (firstRow) {
                    maxPlayCount = palaycount;
                    firstRow = false;
                }
                double playCountNormWeight = palaycount / maxPlayCount;
                //    System.out.println(id + "," + playCountNormWeight);
                utMap.put(id, playCountNormWeight);

            } //end while
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }

        //writing the final set
//        System.out.println("-------tracks for user " + userId + "---------");
        Set<Map.Entry<Integer, Double>> set = utMap.entrySet();
        Iterator<Map.Entry<Integer, Double>> i = set.iterator();
        while (i.hasNext()) {
            Map.Entry<Integer, Double> me = i.next();
//            System.out.println(me.getKey() + " : " + me.getValue());
        }
        return utMap;
    }//userTrackWeightSimple

    //user weight (e.g. popularity,degree, idf,....)
    public double getUserWeight(int userId) {

        double weight = -1;

        String query = "select user_id,count(distinct friend_id) degree from small_friends where " + " user_id =" + userId + " group by user_id";

        Statement stmt;
        ResultSet rs;

        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            rs.next();
            weight = rs.getDouble(2);
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }
        weight /= MAX_USER_DEGREE;
        return Math.log(weight + 1);
    }//getUSerWeight

    public double[][] sort(HashMap map, int k) {  //sort the objects in an array (based on values) and return top k

        double[][] res = new double[k][2];

        for (int i = 0; i < k; i++) {

            res[i][0] = -1;
            res[i][1] = 9_999;
        }

        Set set = map.entrySet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            double value = ((Number) me.getValue()).doubleValue();
            int j = k - 1;
            double temp = res[j][1];
            if (value < temp) {
                continue;
            }
            while (value > temp && j > 0) {
                res[j][0] = res[j - 1][0];
                res[j][1] = res[j - 1][1];
                j--;
                temp = res[j][1];

            }//while
            res[j][0] = ((Number) me.getKey()).intValue();
            res[j][1] = value;

        }

        //print
        for (int i = 0; i < k; i++) {
            System.out.println((i + 1) + "-  [" + res[i][0] + "]  :  " + res[i][1]);
        }
        return res;
    }//sort

    public double[][] sortWithTag(HashMap map, int k, String tag) {
        //sort the objects(tracks) in an array (based on values(weights)) that have the tag and return top k

        double[][] res = new double[k][2];

        for (int i = 0; i < k; i++) {

            res[i][0] = -1;
            res[i][1] = 9_999;
        }

        Set set = map.entrySet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            double value = ((Number) me.getValue()).doubleValue();
            int trackId = ((Number) me.getKey()).intValue();
            if (numTagsInTrack(trackId, tag) == 0) {
                continue;
            }
            int j = k - 1;
            double temp = res[j][1];
            if (value < temp) {
                continue;
            }
            while (value > temp && j > 0) {
                res[j][0] = res[j - 1][0];
                res[j][1] = res[j - 1][1];
                j--;
                temp = res[j][1];

            }//while
            res[j][0] = ((Number) me.getKey()).intValue();
            res[j][1] = value;

        }

        //print
        for (int i = 0; i < k; i++) {
            System.out.println((i + 1) + "-  [" + res[i][0] + "]  :  " + res[i][1]);
        }
        return res;
    }//sortWithTag

    public void playcountRanking(int userID, int k) {

        String query = "select distinct track_id,playcount from small_user_track where user_id =" + userID + " order by playcount desc limit " + k;
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            String trackId, count;
            int i = 0;
            while (rs.next()) {
                trackId = rs.getString(1);
                count = rs.getString(2);
                System.out.println((i + 1) + "-  [" + trackId + "]  :  " + count);
                i++;
            } //end while
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }

    }//playcountRanking

    public void playcountRankingWithTag(int userID, int k, String tag) {

        String query = "select t.track_id,count(tag.name),playcount from small_tag_track t, small_tag tag,small_user_track u "
                + " where u.user_id = " + userID + " and u.track_id = t.track_id and "
                + "(tag.name REGEXP '^" + tag + "' || tag.name REGEXP ' " + tag + "') and  t.tag_id = tag.id "
                + "group by t.track_id order by playcount desc limit " + k;
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            String trackId, count;
            int i = 0;
            while (rs.next()) {
                trackId = rs.getString(1);
                count = rs.getString(3);
                System.out.println((i + 1) + "-  [" + trackId + "]  :  " + count);
                i++;
            } //end while
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }

    }//playcountRankingWithTag

    public int numTagsInTrack(int trackId, String tag) {

        int res = 0;

        String query = "select t.track_id,count(tag.name) from small_tag_track t, small_tag tag where track_id = " + trackId
                + " and (tag.name REGEXP '^" + tag + "' || tag.name REGEXP ' " + tag + "') and  t.tag_id = tag.id group by t.track_id";
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            boolean flag = rs.next();
            if (!flag) {
                return 0; //empty set
            }
            res = rs.getInt(2);

            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
        }
        return res;
    }//numTagsInTrack

    //global weight of tag (e.g. idf)
    public double getTagWeight(String tag) {

        double res = 0;

        //res = ln (1 + n / fk);
        // n : total number of tracks in the database (can define a final global variable at the begining of this class)
        // fk : total number of tracks containing this tag
        return res;
    }//getTageight

    //e.g. tf value
    public double getTagTrackWight(int trackId, String tag) {

        return Math.log(1 + getTagTrackWight(trackId, tag));

    }//getTagTrackWight

    @Override
    public String getName() {
        return "SocialTextual";
    }

}
