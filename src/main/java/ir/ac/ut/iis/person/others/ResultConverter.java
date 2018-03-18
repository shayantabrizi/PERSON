/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.others;

import ir.ac.ut.iis.person.Configs;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 *
 * @author shayan
 */
public class ResultConverter {

    public static void main(String[] args) {
        String fileName = "/home/shayan/NetBeansProjects/datasets/aminer/results/Exp-Cam-EP=PERSON[ISC=0,YF=true],RN=61,BS=LM,LMDMU=100.0,QF=title,MB=0,DUMPAS=true,NOR=100,SQC=true,SQ=13000,QC=2000,NDCGAT=50/result,OQWAHMTP=1,OQWAHLTP=-1,IQDIQE=true,ULTFIPC=false,UTFIDFWIC=false,DUARPIP=true";
        String sigFileName = "/home/shayan/NetBeansProjects/datasets/aminer/results/Exp-Cam-EP=PERSON[ISC=0,YF=true],RN=61,BS=LM,LMDMU=100.0,QF=title,MB=0,DUMPAS=true,NOR=100,SQC=true,SQ=13000,QC=2000,NDCGAT=50/result,OQWAHMTP=1,OQWAHLTP=-1,IQDIQE=true,ULTFIPC=false,UTFIDFWIC=false,DUARPIP=true-sig";
//        final KendallTauResult calcKendallTauB = ResultConverter.calcKendallTauB(Configs.datasetRoot + "/results/ASPIRE[tkr=150]-LM-SQ=200.0,QC=500.0,NOR=100,SQC=true,ISC=false,YF=false,OQWAHMTTP=0,PRA=0.15-t,Campos[ULTFIPC=false,UTFIDFWIC=false,DUARPIP=true,IQDIQE=true,full]",
//                Configs.datasetRoot + "/results/PERSON-LM-SQ=200.0,QC=500.0,NOR=100,SQC=true,ISC=false,YF=true,OQWAHMTTP=1,PRA=0.15-t,Campos[ULTFIPC=false,UTFIDFWIC=false,DUARPIP=true,IQDIQE=true,full]");
//        convertBasedOnResultsCount(fileName);
//        convertBasedOnIterationsCount(fileName);
        extractCamposStats(fileName, sigFileName);
//        extractIterationKendall(sigFileName);
//        extractResultsNumKendall(fileName);
//        System.out.println(calcKendallTauB.kendallTau + ", " + calcKendallTauB.z);
    }

    protected static void convertBasedOnResultsCount(String fileName) throws NumberFormatException {
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("output.csv")))) {
            StringBuilder sb = new StringBuilder();
            String s = null;
            try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(fileName)))) {
                while (sc.hasNextLine()) {
                    String nextLine = sc.nextLine();
                    if (nextLine.equals("-------------------------------------------------------------------")) {
                        s = sb.toString();
                        sb = new StringBuilder();
                    } else {
                        sb.append(nextLine).append("\n");
                    }
                }
            }
            Map<String, Integer[]> counts = new TreeMap<>();
            Map<String, Double[]> ndcgs = new TreeMap<>();
            Map<String, Double> maps = new TreeMap<>();
            Map<String, Double> mrrs = new TreeMap<>();
            Map<String, Double> merrs = new TreeMap<>();
            Map<String, Double> pAt10s = new TreeMap<>();
            try (Scanner sc = new Scanner(new StringReader(s))) {
                sc.nextLine();
                while (sc.hasNextLine()) {
                    String kind = sc.nextLine();
                    String[] count = sc.nextLine().split(",");
                    String[] ndcg = sc.nextLine().split(",");
                    String cnt = sc.nextLine();
                    String map = sc.nextLine().replace("MAP: ", "");
                    String mrr = sc.nextLine().replace("MRR: ", "");
                    String merr = sc.nextLine().replace("ERR: ", "");
                    String pAt10 = sc.nextLine().replace("p10: ", "");
                    final Integer[] c = new Integer[Configs.numOfResults];
                    counts.put(kind, c);
                    final Double[] n = new Double[Configs.numOfResults];
                    ndcgs.put(kind, n);
                    for (int i = 0; i < Configs.numOfResults; i++) {
                        c[i] = (int) Double.parseDouble(count[i]);
                        n[i] = Double.parseDouble(ndcg[i]);
                    }
                    maps.put(kind, Double.parseDouble(map));
                    mrrs.put(kind, Double.parseDouble(mrr));
                    merrs.put(kind, Double.parseDouble(merr));
                    pAt10s.put(kind, Double.parseDouble(pAt10));
                }
            }
            String[] kinds = ndcgs.keySet().toArray(new String[0]);
            writer.write("counts:\n");
            writer.write(",");
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n");
            for (int i = 0; i < Configs.numOfResults; i++) {
                writer.write((i + 1) + ",");
                for (String kind : kinds) {
                    writer.write(counts.get(kind)[i] + ",");
                }
                writer.write("\n");
            }

            writer.write("\n");
            writer.write("ndcgs:\n");
            writer.write(",");
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n");
            for (int i = 0; i < Configs.numOfResults; i++) {
                writer.write((i + 1) + ",");
                for (String kind : kinds) {
                    writer.write(ndcgs.get(kind)[i] + ",");
                }
                writer.write("\n");
            }
            writer.write("\n");
            writer.write(",");
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n");
            writer.write("MAP,");
            for (String kind : kinds) {
                writer.write(maps.get(kind) + ",");
            }
            writer.write("\n");
            writer.write("MRR,");
            for (String kind : kinds) {
                writer.write(mrrs.get(kind) + ",");
            }
            writer.write("\n");
            writer.write("ERR,");
            for (String kind : kinds) {
                writer.write(merrs.get(kind) + ",");
            }
            writer.write("\n");
            writer.write("P@10,");
            for (String kind : kinds) {
                writer.write(pAt10s.get(kind) + ",");
            }
            writer.write("\n");
        } catch (IOException ex) {
            Logger.getLogger(ResultConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    protected static void convertBasedOnIterationsCount(String fileName) throws NumberFormatException {
        Map<String, List<Double>> maps = new TreeMap<>();
        Map<String, List<Double>> mrrs = new TreeMap<>();
        Map<String, List<Double>> merrs = new TreeMap<>();
        Map<String, List<Double>> pAt10s = new TreeMap<>();
        Map<String, List<Double>> NDCGs = new TreeMap<>();

        int num = 0;
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("output.csv")))) {
            StringBuilder sb = new StringBuilder();
            try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(fileName)))) {
                String nextLine = sc.nextLine();
                while (sc.hasNextLine()) {
                    String kind = sc.nextLine();
                    if (kind.equals("-------------------------------------------------------------------")) {
                        num++;
                        if (sc.hasNextLine()) {
                            sc.nextLine();
                        }
                        continue;
                    }
                    String[] count = sc.nextLine().split(",");
                    String[] ndcg = sc.nextLine().split(",");
                    String cnt = sc.nextLine();
                    String map = sc.nextLine().replace("MAP: ", "");
                    String mrr = sc.nextLine().replace("MRR: ", "");
                    String merr = sc.nextLine().replace("ERR: ", "");
                    String pAt10 = sc.nextLine().replace("p10: ", "");

                    addToMap(maps, kind, map);
                    addToMap(merrs, kind, merr);
                    addToMap(pAt10s, kind, pAt10);
                    addToMap(NDCGs, kind, ndcg[Configs.ndcgAt - 1]);
                }
            }

            writer.write("map,");
            String[] kinds = maps.keySet().toArray(new String[0]);
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n");
            for (int i = 0; i < num; i++) {
                writer.write((i + 1) + ",");
                for (String kind : kinds) {
                    writer.write(maps.get(kind).get(i) + ",");
                }
                writer.write("\n");
            }

            writer.write("\nerr,");
            kinds = merrs.keySet().toArray(new String[0]);
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n");
            for (int i = 0; i < num; i++) {
                writer.write((i + 1) + ",");
                for (String kind : kinds) {
                    writer.write(merrs.get(kind).get(i) + ",");
                }
                writer.write("\n");
            }

            writer.write("\nNDCG,");
            kinds = NDCGs.keySet().toArray(new String[0]);
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n");
            for (int i = 0; i < num; i++) {
                writer.write((i + 1) + ",");
                for (String kind : kinds) {
                    writer.write(NDCGs.get(kind).get(i) + ",");
                }
                writer.write("\n");
            }

            writer.write("\np@10,");
            kinds = pAt10s.keySet().toArray(new String[0]);
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n");
            for (int i = 0; i < num; i++) {
                writer.write((i + 1) + ",");
                for (String kind : kinds) {
                    writer.write(pAt10s.get(kind).get(i) + ",");
                }
                writer.write("\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(ResultConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected static void extractCamposStats(String fileName, String sigFileName) throws NumberFormatException {
        Configs.ndcgAt = 50;
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("output-ndcgAt=" + Configs.ndcgAt + ".csv")))) {
            Map<String, Stats> statss = readStats(fileName);
            String[] kinds = statss.keySet().toArray(new String[0]);
            writer.write("ndcg@" + Configs.ndcgAt + ":\n");
            writer.write(",");
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n,");
            for (String kind : kinds) {
                writer.write(statss.get(kind).ndcg[Configs.ndcgAt - 1] + ",");
            }
            writer.write("\n");

            Map<String, Map<String, Double>> RIs = calcRI(sigFileName);

            writer.write("RI:\n");
            writer.write(",");
            for (String kind : kinds) {
                writer.write(kind + ",");
            }
            writer.write("\n,");
            for (String kind : kinds) {
                writer.write(RIs.get("NDCG@50").get(kind) + ",");
            }
        } catch (IOException ex) {
            Logger.getLogger(ResultConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Map<String, Map<String, Double>> calcRI(String sigFileName) throws NumberFormatException {
        String baselineName = "02-" + Configs.baseSimilarityName;
        Map<String, Map<String, List<Double>>> statss = readSigStats(sigFileName);

        Map<String, Map<String, Double>> RIs = new HashMap<>();
        for (Map.Entry<String, Map<String, List<Double>>> m : statss.entrySet()) {
            Map<String, Double> RI = new HashMap<>();
            List<Double> baseline = m.getValue().get(baselineName);
            for (Map.Entry<String, List<Double>> e : m.getValue().entrySet()) {
                if (!e.getKey().equals(baselineName)) {
                    List<Double> value = e.getValue();
                    int nPlus = 0;
                    int nMinus = 0;
                    for (int i = 0; i < value.size(); i++) {
                        if (value.get(i) < baseline.get(i)) {
                            nMinus++;
                        } else if (value.get(i) > baseline.get(i)) {
                            nPlus++;
                        }
                    }
                    Double RIVal = ((double) (nPlus - nMinus)) / value.size();
                    RI.put(e.getKey(), RIVal);
                }
            }
            RIs.put(m.getKey(), RI);
        }
        return RIs;
    }

    protected static Map<String, Map<String, List<Double>>> readSigStats(String sigFileName) throws RuntimeException {
        Map<String, Map<String, List<Double>>> statss = new HashMap<>();
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(sigFileName)))) {
            while (sc.hasNextLine()) {
                String measure = sc.nextLine();
                measure = measure.replaceAll(":", "");
                if (measure.equals("Other Stats")) {
                    break;
                }
                String s = sc.nextLine();
                Map<String, List<Double>> map = new HashMap<>();
                while (!s.isEmpty()) {
                    List<Double> list = new LinkedList<>();
                    String[] split = s.split(",");
                    for (int i = 1; i < split.length; i++) {
                        list.add(Double.valueOf(split[i]));
                    }
                    map.put(split[0], list);
                    s = sc.nextLine();
                }
                statss.put(measure, map);
                sc.nextLine();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ResultConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return statss;
    }

    public static Map<String, Stats> readStats(String fileName) {
        StringBuilder sb = new StringBuilder();
        String s = null;
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(fileName)))) {
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                if (nextLine.equals("-------------------------------------------------------------------")) {
                    s = sb.toString();
                    sb = new StringBuilder();
                } else {
                    sb.append(nextLine).append("\n");
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ResultConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        Map<String, Stats> statss = new TreeMap<>();
        try (Scanner sc = new Scanner(new StringReader(s))) {
            sc.nextLine();
            while (sc.hasNextLine()) {
                String kind = sc.nextLine();
                String[] count = sc.nextLine().split(",");
                String[] ndcg = sc.nextLine().split(",");
                String cnt = sc.nextLine();
                String map = sc.nextLine().replace("MAP: ", "");
                String mrr = sc.nextLine().replace("MRR: ", "");
                String merr = sc.nextLine().replace("ERR: ", "");
                String pAt10 = sc.nextLine().replace("p10: ", "");
                final Integer[] c = new Integer[Configs.numOfResults];
                final Double[] n = new Double[Configs.numOfResults];
                for (int i = 0; i < Configs.numOfResults; i++) {
                    c[i] = (int) Double.parseDouble(count[i]);
                    n[i] = Double.parseDouble(ndcg[i]);
                }
                Stats stats = new Stats(c, n, Double.parseDouble(map), Double.parseDouble(mrr), Double.parseDouble(merr), Double.parseDouble(pAt10));
                statss.put(kind, stats);
            }
        }
        return statss;
    }

    protected static void addToMap(Map<String, List<Double>> maps, String kind, String map) throws NumberFormatException {
        if (maps != null) {
            List<Double> get = maps.get(kind);
            if (get == null) {
                get = new LinkedList<>();
                maps.put(kind, get);
            }
            maps.get(kind).add(Double.parseDouble(map));
        }
    }

    private static KendallTauResult calcKendallTauB(String fileName1, String fileName2) {
        Double[] arr1 = {0.367865829179183, 0.367865829179183, 0.367865829179183, 0.372699000066455, 0.372699000066455, 0.372699000066455, 0.370532019529533, 0.370532019529533, 0.370532019529533, 0.3696996261622, 0.3696996261622, 0.3696996261622};
        Map<String, Double> ndcgs1 = conv(arr1);
        Double[] arr2 = {0.473462063539992, 0.473462063539992, 0.473462063539992, 0.479708491282099, 0.479708491282099, 0.479708491282099, 0.485870617677623, 0.485870617677623, 0.485870617677623, 0.469481786236313, 0.469481786236313, 0.469481786236313};
        Map<String, Double> ndcgs2 = conv(arr2);

//        Map<String, Double[]> ndcgs1 = calc(fileName1);
//        Map<String, Double[]> ndcgs2 = calc(fileName2);
        return calcKenallTauB(ndcgs1, ndcgs2);
    }

    public static KendallTauResult calcKenallTauB(Map<String, Double> ndcgs1, Map<String, Double> ndcgs2) {
        int concordants = 0;
        int discordants = 0;
        int Ty = 0;
        int Ty1 = 0;
        int Ty2 = 0;
        int Tx = 0;
        int Tx1 = 0;
        int Tx2 = 0;
        Map<Double, Integer> TiesX = new HashMap<>();
        Map<Double, Integer> TiesY = new HashMap<>();
        for (String k1 : ndcgs1.keySet()) {
            final Double v1 = ndcgs1.get(k1);
            Integer get = TiesX.get(v1);
            if (get == null) {
                get = 0;
            }
            get++;
            TiesX.put(v1, get);
            final Double v2 = ndcgs2.get(k1);
            Integer get2 = TiesY.get(v2);
            if (get2 == null) {
                get2 = 0;
            }
            get2++;
            TiesY.put(v2, get2);
            for (String k2 : ndcgs1.keySet()) {
                if (k1.compareTo(k2) < 0) {
                    if (ndcgs1.get(k1) < ndcgs1.get(k2)
                            && ndcgs2.get(k1) < ndcgs2.get(k2)
                            || ndcgs1.get(k1) > ndcgs1.get(k2)
                            && ndcgs2.get(k1) > ndcgs2.get(k2)) {
                        concordants++;
                    }
                    if (ndcgs1.get(k1) < ndcgs1.get(k2)
                            && ndcgs2.get(k1) > ndcgs2.get(k2)
                            || ndcgs1.get(k1) > ndcgs1.get(k2)
                            && ndcgs2.get(k1) < ndcgs2.get(k2)) {
                        discordants++;
                    }
                    if (ndcgs1.get(k1).equals(ndcgs1.get(k2))) {
                        Tx++;
                    }
                    if (ndcgs2.get(k1).equals(ndcgs2.get(k2))) {
                        Ty++;
                    }
                }
            }
        }
        Tx *= 2;
        Ty *= 2;

        for (Map.Entry<Double, Integer> e : TiesX.entrySet()) {
            Tx1 += (e.getValue() * e.getValue() - e.getValue()) * (e.getValue() - 2);
            Tx2 += (e.getValue() * e.getValue() - e.getValue()) * (2 * e.getValue() + 5);
        }
        for (Map.Entry<Double, Integer> e : TiesY.entrySet()) {
            Ty1 += (e.getValue() * e.getValue() - e.getValue()) * (e.getValue() - 2);
            Ty2 += (e.getValue() * e.getValue() - e.getValue()) * (2 * e.getValue() + 5);
        }

        int n = ndcgs1.size();
        double tau = 2 * (concordants - discordants) / Math.sqrt((n * (n - 1) - Tx) * (n * (n - 1) - Ty));

        int delta = Integer.MIN_VALUE;       // To show a problem
        if (concordants - discordants > 0) {
            delta = -1;
        } else if (concordants - discordants < 0) {
            delta = 1;
        } else {
            System.out.println("A Problem!!!!");
        }

        double sigma2 = ((double) ((n * n - n) * (2 * n + 5) - Tx2 - Ty2)) / 18 + ((double) (Tx1 * Tx2)) / (9 * (n * n - n) * (n - 2)) + ((double) (Tx * Ty)) / (2 * (n * n - n));

        double z = (((double) concordants - discordants) + delta) / Math.sqrt(sigma2);
        // @TODO: Sare tabdile z be p value motmaen nistam. zemne inke inja Gaussian farz shode ke agar andaze koochik bashe shayad behtar bashe bar asase critical value ha hesab beshe.
        final NormalDistribution standardNormal = new NormalDistribution(0, 1);
        return new KendallTauResult(tau, z, 1 - standardNormal.cumulativeProbability(z), concordants - discordants);
    }

    private static void extractIterationKendall(String sigFileName) {
        Map<String, Map<String, List<Double>>> readSigStats = readSigStats(sigFileName);

        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("iteration_kendall.txt")))) {
            for (Map.Entry<String, Map<String, List<Double>>> e : readSigStats.entrySet()) {
                writer.write(e.getKey() + ":\n");
                Map<String, Double[]> conv = new HashMap<>();
                int i = 0;
                for (Map.Entry<String, List<Double>> m : e.getValue().entrySet()) {
                    Double[] arr = new Double[m.getValue().size()];
                    i = 0;
                    for (Double d : m.getValue()) {
                        arr[i] = d;
                        if (i > 0) {
                            arr[i] += arr[i - 1];
                        }
                        i++;
                    }
                    for (int j = 0; j < arr.length; j++) {
                        arr[j] /= (j + 1);
                    }
                    conv.put(m.getKey(), arr);
                }

                System.out.println(i);
                Map<String, Double> baseMap = new HashMap<>();
                for (Map.Entry<String, Double[]> m : conv.entrySet()) {
                    baseMap.put(m.getKey(), m.getValue()[2_208]);
                }
                for (int j = 0; j < 2_208; j++) {
                    Map<String, Double> compMap = new HashMap<>();
                    for (Map.Entry<String, Double[]> m : conv.entrySet()) {
                        compMap.put(m.getKey(), m.getValue()[j]);
                    }
                    KendallTauResult k = calcKenallTauB(baseMap, compMap);
//                    writer.write(k.kendallTau + "," + k.pValue + "," + k.z + "," + k.difference + "\n");
                    writer.write(k.kendallTau + ",");
                }
                writer.write("\n\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(ResultConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private static void extractResultsNumKendall(String fileName) {
        Map<String, Stats> readStats = readStats(fileName);
        int size = readStats.entrySet().iterator().next().getValue().ndcg.length;

        Map<String, Double> baseMap = new HashMap<>();
        for (Map.Entry<String, Stats> m : readStats.entrySet()) {
            baseMap.put(m.getKey(), m.getValue().ndcg[size - 1]);
        }
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("resultsNum_kendall.txt")))) {
            for (int k = 0; k < size; k++) {
                Map<String, Double> compMap = new HashMap<>();
                for (Map.Entry<String, Stats> e : readStats.entrySet()) {
                    compMap.put(e.getKey(), e.getValue().ndcg[k]);
                }
                KendallTauResult kendall = calcKenallTauB(baseMap, compMap);
//                    writer.write(k.kendallTau + "," + k.pValue + "," + k.z + "," + k.difference + "\n");
                writer.write(kendall.kendallTau + ",");

            }

        } catch (IOException ex) {
            Logger.getLogger(ResultConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public static class KendallTauResult {

        public double kendallTau;
        public double z;
        public double pValue;
        public int difference;

        public KendallTauResult(double kendallTau, double z, double pValue, int difference) {
            this.kendallTau = kendallTau;
            this.z = z;
            this.pValue = pValue;
            this.difference = difference;
        }

    }

    private static Map<String, Double> conv(Double[] arr) {
        Map<String, Double> map = new HashMap<>();
        for (int i = 0; i < arr.length; i++) {
            map.put(String.valueOf(i), arr[i]);
        }
        return map;
    }

    public static class Stats {

        public Integer[] count;
        public Double[] ndcg;
        public double map;
        public double mrr;
        public double merr;
        public double pAt10;

        public Stats(Integer[] count, Double[] ndcg, double map, double mrr, double merr, double pAt10) {
            this.count = count;
            this.ndcg = ndcg;
            this.map = map;
            this.mrr = mrr;
            this.merr = merr;
            this.pAt10 = pAt10;
        }

    }

}
