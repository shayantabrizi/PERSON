/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import ir.ac.ut.iis.person.topics.InstanceClassifier;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author shayan
 */
public class TopicsReader {

    public Map<String, float[]> readComposition(String compositionFileName) {
        System.out.println("Reading topics started");
        Map<String, float[]> map = new HashMap<>();
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(compositionFileName)))) {
            sc.nextLine();
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                String[] split = nextLine.split("\t");
                float[] topics = new float[(split.length - 2) / 2];
                for (int i = 2; i < split.length; i += 2) {
                    topics[Integer.valueOf(split[i])] = Float.valueOf(split[i + 1]);
                }
                map.put(split[1], topics);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TopicsReader.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        System.out.println("Reading topics finished");
        return map;
    }

    public static double[] readAlphas(String inputFileName, int numOfTopics) throws RuntimeException {
        double[] alphas = new double[numOfTopics];
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(inputFileName)))) {
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                String[] split = nextLine.split("\t");
                alphas[Integer.parseInt(split[0])] = Double.parseDouble(split[1]);
            }
        } catch (IOException ex) {
            Logger.getLogger(TopicsReader.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return alphas;
    }

    public Map<String, DocTopics> readDocTopics(String modelFileName) {
        System.out.println("Reading model started");
        ParallelTopicModel readModel = InstanceClassifier.readModel(modelFileName);
        Map<String, DocTopics> map = new HashMap<>();
        int j = 0;
        for (cc.mallet.topics.TopicAssignment a : readModel.getData()) {
            float[] topics = new float[readModel.getNumTopics()];
//            short[] arr = new short[readModel.getNumTopics()];
//            for (Iterator it = a.topicSequence.iterator(); it.hasNext();) {
//                Label next = (Label) it.next();
//                arr[next.getIndex()]++;
//            }
            double[] topicProbabilities = readModel.getTopicProbabilities(j);
            j++;
            for (int i = 0; i < readModel.getNumTopics(); i++) {
                topics[i] = (float) topicProbabilities[i];
            }
//            map.put((String) a.instance.getName(), new DocTopics((short) a.topicSequence.size(), arr, topics));
            map.put((String) a.instance.getName(), new DocTopics((short) 0, null, topics));
        }

        System.out.println("Reading model finished");
        return map;
    }

    public WordStats readTopicsWords(String modelFileName, String countsFileName) {
        System.out.println("Reading model started");
        ParallelTopicModel readModel = InstanceClassifier.readModel(modelFileName);
        Alphabet alphabet = readModel.getAlphabet();
        final ArrayList<TreeSet<IDSorter>> sortedWords = readModel.getSortedWords();
        Map<BytesRef, float[]> tt = new TreeMap<>();
        for (int i = 0; i < sortedWords.size(); i++) {
            TreeSet<IDSorter> get = sortedWords.get(i);
            double total = 0;
            for (IDSorter next : get) {
                total += next.getWeight();
            }
            total += readModel.numTypes * readModel.beta;

            for (Iterator it = alphabet.iterator(); it.hasNext();) {
                final BytesRef bytesRef = new BytesRef((CharSequence) it.next());
                float[] get1 = tt.get(bytesRef);
                if (get1 == null) {
                    get1 = new float[readModel.numTopics];
                    tt.put(bytesRef, get1);
                }
                get1[i] = (float) (readModel.beta / total);
            }
            for (IDSorter next : get) {
                final BytesRef bytesRef = new BytesRef((CharSequence) alphabet.lookupObject(next.getID()));
                float[] get1 = tt.get(bytesRef);
                get1[i] += (float) (next.getWeight() / total);
            }
        }
//        Map<BytesRef, int[]> map = new HashMap<>();
//        int[] topicCounts = new int[readModel.numTopics];
//        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(countsFileName)))) {
//            while (sc.hasNextLine()) {
//                String nextLine = sc.nextLine();
//                String[] split = nextLine.split(" ");
//                int[] topics = new int[readModel.numTopics];
//                for (int i = 2; i < split.length; i++) {
//                    String[] split1 = split[i].split(":");
//                    final int parseInt = Integer.parseInt(split1[0]);
//                    final int parseInt1 = Integer.parseInt(split1[1]);
//                    topics[parseInt] = parseInt1;
//                    topicCounts[parseInt] += parseInt1;
//                }
//                map.put(new BytesRef(split[1].getBytes()), topics);
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(TopicsReader.class.getName()).log(Level.SEVERE, null, ex);
//            throw new RuntimeException();
//        }
        System.out.println("Reading model finished");
//        return new WordStats(map, tt, topicCounts, readModel.beta);
        return new WordStats(null, tt, null, readModel.beta);
    }

    public static class DocTopics implements Serializable {

//        public short docLength;
//        public short[] topicCounts;
        public float[] topics;

        public DocTopics(short docLength, short[] topicCounts, float[] topics) {
//            this.docLength = docLength;
//            this.topicCounts = topicCounts;
            this.topics = topics;
        }

    }

    public static class WordStats {

//        public Map<BytesRef, int[]> wordCounts;
        public Map<BytesRef, float[]> wordTopics;
//        public int[] topicCounts;
        public double beta;

        public WordStats(Map<BytesRef, int[]> wordCounts, Map<BytesRef, float[]> wordTopics, int[] topicCounts, double beta) {
//            this.wordCounts = wordCounts;
            this.wordTopics = wordTopics;
//            this.topicCounts = topicCounts;
            this.beta = beta;
        }

        public double getBeta() {
            return beta;
        }
    }

}
