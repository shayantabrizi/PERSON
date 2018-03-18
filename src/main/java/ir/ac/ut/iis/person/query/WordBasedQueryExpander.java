/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.query;

import ir.ac.ut.iis.person.Configs;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class WordBasedQueryExpander extends QueryConverter {

    Map<String, List<String>> map = new TreeMap<>();
    final Double weight;

    public WordBasedQueryExpander(Double weight) {
        super();
        this.weight = weight;
    }

    @Override
    public Map<String, Double> convert(Map<String, Double> words, int userId, Query query) {
        words = super.convert(words, userId, query);

        Map<String, Double> extended = new TreeMap<>();
        for (Map.Entry<String, Double> e : words.entrySet()) {
            List<String> get1 = map.get(e.getKey());
            if (get1 != null) {
                int i = 0;
                for (String w2 : get1) {
                    addAdditiveWeight(extended, w2, weight * e.getValue());
                    i++;
                    if (i == 10) {
                        break;
                    }
                }
            }
        }
        for (Map.Entry<String, Double> e : words.entrySet()) {
            addAdditiveWeight(extended, e.getKey(), e.getValue());
        }
        return extended;
    }

    public void readSimilarities() {
        System.out.println("Reading similarities started");
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(Configs.datasetRoot + "similar-words-pmi.txt")))) {
            while (sc.hasNextLine()) {
                String w1 = sc.nextLine().split(" ")[0];
                int parseInt = Integer.parseInt(sc.nextLine());
                List<String> list = new LinkedList<>();
                map.put(w1, list);
                for (int i = 0; i < parseInt; i++) {
                    String w2 = sc.nextLine().split("[\t| ]")[1];
                    list.add(w2);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WordBasedQueryExpander.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        System.out.println("Reading similarities finished");
    }

//    public static void main(String[] args) {
//        QueryExpander queryExpander = new QueryExpander(.3);
//        queryExpander.readSimilarities();
//        queryExpander.readSingulars();
//        String expand = queryExpander.expand("Graph clustering has been an essential part in many methods and thus its accuracy has a significant effect on many applications. In addition, exponential growth of real-world graphs such as social networks, biological networks and electrical circuits demands clustering algorithms with nearly-linear time and space complexity. In this paper we propose Personalized PageRank Clustering (PPC) that employs the inherent cluster exploratory property of random walks to reveal the clusters of a given graph. We combine random walks and modularity to precisely and efficiently reveal the clusters of a graph. PPC is a top-down algorithm so it can reveal inherent clusters of a graph more accurately than other nearly-linear approaches that are mainly bottom-up. It also gives a hierarchy of clusters that is useful in many applications. PPC has a linear time and space complexity and has been superior to most of the available clustering algorithms on many datasets. Furthermore, its top-down approach makes it a flexible solution for clustering problems with different requirements.");
//        System.out.println(expand);
//    }
}
