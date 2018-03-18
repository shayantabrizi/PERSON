/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.base;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.Main;
import ir.ac.ut.iis.person.algorithms.aggregate.MyCustomScoreProvider;
import ir.ac.ut.iis.person.evaluation.Evaluator;
import ir.ac.ut.iis.person.hierarchy.User;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.QueryBatch;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

/**
 *
 * @author shayan
 */
public abstract class Retriever implements Closeable {

    protected QueryBatch result = null;
    protected List<Result> processedResults = new ArrayList<>();
    private final String name;
    protected final List<Searcher> searchers = new LinkedList<>();
    protected Writer traceWriter = null;
    protected final Evaluator evaluator;

    public Retriever(String name, Evaluator evaluator) {
        this.name = name;
        this.evaluator = evaluator;
    }

    public void setTraceWriter(String fileName) {
        try {
            traceWriter = new FileWriter(fileName);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public void addSearcher(Searcher searcher) {
        this.searchers.add(searcher);
    }

    public boolean retrieve() {
        try {
            QueryBatch queries = nextQueryBatch();
            for (Searcher s : searchers) {
                System.out.println(s.getName());
                createQueryAndSearch(queries, s);
                MyCustomScoreProvider.printStatistics();
                MyCustomScoreProvider.resetStatistics();
//                if (entry.getValue().size() < Main.numOfResults) {
//                    throw new IgnoreQueryEx("small resultset: " + s.getName() + ": " + entry.getValue().size());
//                }
            }

//            if (queries.properQueryCount() == 0) {
//                throw new IgnoreQueryEx();
//            }
            for (Query q : queries) {
                checkRelevancy(q);
            }

            writeLogs(queries);
            if (queries.properQueryCount() > 0) {
                result = queries;
                return true;
            } else {
                return false;
            }

        } catch (IgnoreQueryEx ex) {
//            ex.printStackTrace();
            System.out.println("query ignored: " + ex.getMessage());
//            ex.printStackTrace();
            return false;
        } catch (IOException | QueryNodeException ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
        //out.close();
    }

    protected abstract void checkRelevancy(Query query);

    public abstract QueryBatch nextQueryBatch();

    public abstract void skipQueryBatch(int n);

    public abstract boolean hasNextQueryBatch();

    public void writeSignificantTestData(String fileName) {
        Map<String, int[]> countsMap = new HashMap<>();
        Map<String, double[]> ndcgMap = new HashMap<>();
//            Map<String, double[]> precisionMap = new TreeMap<>();
        Map<String, Double> map = new HashMap<>();
        Map<String, Double> mmrr = new HashMap<>();
        Map<String, Double> mpAt10 = new HashMap<>();
        Map<String, List<Double>> apList = new HashMap<>();
        Map<String, List<Double>> pAt10List = new HashMap<>();
        Map<String, Integer> relevantsDistribution = new HashMap<>();
        Map<String, Double> merr = new HashMap<>();
        Map<String, List<Double>> errList = new HashMap<>();
        Map<String, List<Double>> ndcgList = new HashMap<>();
        Map<String, List<Double>> mmrrList = new HashMap<>();
        Map<String, String> otherStats = new HashMap<>();

        calcStats(mmrr, mpAt10, pAt10List, countsMap, ndcgMap, map, apList, merr, errList, ndcgList, mmrrList, relevantsDistribution, otherStats);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileName))) {
            writer.write("AP:\n");
            writeStats(apList, writer);
            writer.write("P@10:\n");
            writeStats(pAt10List, writer);
            writer.write("ERR:\n");
            writeStats(errList, writer);
            writer.write("MRR:\n");
            writeStats(mmrrList, writer);
            writer.write("NDCG@" + Configs.ndcgAt + ":\n");
            writeStats(ndcgList, writer);
            writer.write("Other Stats:\n");
            for (Map.Entry<String, String> e : otherStats.entrySet()) {
                writer.write(e.getKey() + ": " + e.getValue() + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(Retriever.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeStats(Map<String, List<Double>> apList, final Writer writer) throws IOException {
        for (Map.Entry<String, List<Double>> e : apList.entrySet()) {
            StringBuilder sb = new StringBuilder(e.getKey()).append(',');
            for (Object d : e.getValue()) {
                sb.append(d).append(',');
            }
            sb.append("\n");
            writer.write(sb.toString());
        }
        writer.write("\n\n");
    }

    public void writeResults(Writer out, Writer perQueryOutput) {
        try {
            Map<String, int[]> countsMap = new HashMap<>();
            Map<String, double[]> ndcgMap = new HashMap<>();
//            Map<String, double[]> precisionMap = new HashMap<>();
            Map<String, Double> map = new HashMap<>();
            Map<String, Double> mmrr = new HashMap<>();
            Map<String, Double> mpAt10 = new HashMap<>();
            Map<String, List<Double>> apList = new HashMap<>();
            Map<String, List<Double>> pAt10List = new HashMap<>();
            Map<String, Integer> relevantsDistribution = new HashMap<>();
            Map<String, Double> merr = new HashMap<>();
            Map<String, List<Double>> errList = new HashMap<>();
            Map<String, List<Double>> ndcgList = new HashMap<>();
            Map<String, String> otherStats = new HashMap<>();
            Map<String, List<Double>> mmrrList = new HashMap<>();
            final Result calcStats = calcStats(mmrr, mpAt10, pAt10List, countsMap, ndcgMap, map, apList, merr, errList, ndcgList, mmrrList, relevantsDistribution, otherStats);
            if (calcStats == null) {
                return;
            }

            Query firstQuery = calcStats.qb.iterator().next();
            perQueryOutput.write(firstQuery.getQueryId() + " " + firstQuery.getSearcher() + " " + firstQuery.getRelevants().size() + " " + firstQuery.getRelevants().size() + ":");
            for (Map.Entry<String, QueryBatch.Stats> e : calcStats.perSearcherStatss.entrySet()) {
                perQueryOutput.write(e.getKey() + "=" + e.getValue().ndcg[Configs.ndcgAt - 1] + ",");
            }
            perQueryOutput.write("\n");

            final int validResultsCount = apList.values().iterator().next().size();

//            for (Map.Entry<String, Integer> e : relevantsDistribution.entrySet()) {
//                System.out.println(e.getKey() + " " + e.getValue());
//            }
            out.write((Main.i + 1) + " " + validResultsCount + " " + processedResults.size() + "\n");
            for (Map.Entry<String, int[]> e : countsMap.entrySet()) {
                out.write(e.getKey() + "\n");
                int[] count = new int[Configs.numOfResults];
                count[0] = e.getValue()[0];
                for (int i = 0; i < Configs.numOfResults; i++) {
                    if (i > 0) {
                        count[i] += count[i - 1] + e.getValue()[i];
                    }
                    out.write(count[i] + ",");
                }
                out.write("\n");
                double[] get = ndcgMap.get(e.getKey());
                for (int i = 0; i < Configs.numOfResults; i++) {
                    out.write(get[i] / validResultsCount + ",");
                }
                out.write("\n");
                out.write("CNT: " + count[Configs.numOfResults - 1] + "\n");
                out.write("MAP: " + Double.toString(map.get(e.getKey()) / validResultsCount) + "\n");
                out.write("MRR: " + Double.toString(mmrr.get(e.getKey()) / validResultsCount) + "\n");
                out.write("ERR: " + Double.toString(merr.get(e.getKey()) / validResultsCount) + "\n");
                out.write("p10: " + Double.toString(mpAt10.get(e.getKey()) / validResultsCount) + "\n");
            }

            out.write("-------------------------------------------------------------------\n");
            out.flush();
            perQueryOutput.flush();
        } catch (IOException ex) {
            Logger.getLogger(Retriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private Result calcStats(Map<String, Double> mmrr, Map<String, Double> mpAt10, Map<String, List<Double>> pAt10List, Map<String, int[]> countsMap, Map<String, double[]> ndcgMap, Map<String, Double> map, Map<String, List<Double>> apList, Map<String, Double> merr, Map<String, List<Double>> errList, Map<String, List<Double>> ndcgList, Map<String, List<Double>> mmrrList, Map<String, Integer> relevantsDistribution, Map<String, String> otherStats) {
        boolean check = true;
        Result res = null;
        if (result != null) {
            res = calcCurrentStats(result);
            result = null;
            if (!res.perSearcherStatss.isEmpty()) {
                processedResults.add(res);
            } else {
                check = false;
            }
        }

        int totalRelevants = 0;
        int relevantsRetrieved = 0;
        double relevantsRetrievedRatio = 0;

        for (Result qb : processedResults) {
            totalRelevants += qb.totalRelevants;
            relevantsRetrieved += qb.relevantsRetrieved;
            relevantsRetrievedRatio += ((double) qb.relevantsRetrieved) / qb.totalRelevants;

            String s = "";
            for (String k : qb.perSearcherStatss.keySet()) {
                QueryBatch.Stats stats = qb.perSearcherStatss.get(k);

                Double tmp = mmrr.get(k);
                if (tmp == null) {
                    tmp = 0.;
                }
                tmp += stats.rr;
                mmrr.put(k, tmp);
                Double tmp2 = mpAt10.get(k);
                if (tmp2 == null) {
                    tmp2 = 0.;
                }
                tmp2 += stats.pAt10;
                mpAt10.put(k, tmp2);
                List<Double> g = pAt10List.get(k);
                if (g == null) {
                    g = new LinkedList<>();
                }
                g.add(stats.pAt10);
                pAt10List.put(k, g);

                int[] get = countsMap.get(k);
                double[] ndcg = ndcgMap.get(k);

                if (get == null) {
                    get = new int[Configs.numOfResults];
                    ndcg = new double[Configs.numOfResults];
                    for (int i = 0; i < Configs.numOfResults; i++) {
                        get[i] = 0;
                        ndcg[i] = 0;
                    }
                    countsMap.put(k, get);
                    ndcgMap.put(k, ndcg);
//                        precisionMap.put(e.getKey(), precision);
                }

                for (int i = 0; i < Configs.numOfResults; i++) {
                    get[i] += stats.counts[i];
                }
                Double get1 = map.get(k);
                if (get1 == null) {
                    get1 = 0.;
                }
                map.put(k, get1 + stats.ap);

                List<Double> gg = apList.get(k);
                if (gg == null) {
                    gg = new LinkedList<>();
                }
                gg.add(stats.ap);
                apList.put(k, gg);

                List<Double> mm = mmrrList.get(k);
                if (mm == null) {
                    mm = new LinkedList<>();
                }
                mm.add(stats.rr);
                mmrrList.put(k, mm);

                List<Double> nl = ndcgList.get(k);
                if (nl == null) {
                    nl = new LinkedList<>();
                }
                nl.add(stats.ndcg[Configs.ndcgAt - 1]);
                ndcgList.put(k, nl);

                Double merrV = merr.get(k);
                if (merrV == null) {
                    merrV = 0.;
                }
                merr.put(k, merrV + stats.err);

                List<Double> errListV = errList.get(k);
                if (errListV == null) {
                    errListV = new LinkedList<>();
                }
                errListV.add(stats.err);
                errList.put(k, errListV);

                for (int i = 0; i < Configs.numOfResults; i++) {
                    ndcg[i] += stats.ndcg[i];
                }

                s += "-" + stats.rels;
            }
            Integer get = relevantsDistribution.get(s);
            if (get == null) {
                get = 0;
            }
            relevantsDistribution.put(s, get + 1);
        }

        otherStats.put("globalRelevantsRetrievedRatio", String.valueOf(((double) relevantsRetrieved) / totalRelevants));
        otherStats.put("localRelevantsRetrievedRatio", String.valueOf(relevantsRetrievedRatio / processedResults.size()));

        return check ? res : null;
    }

    private int numOfRelevants(Query q) {
        if (evaluator.ignoreUnfoundRelevants()) {
            return q.getFoundRelevants().size();
        } else {
            return q.getRelevants().size();
        }
    }

    protected Result calcCurrentStats(QueryBatch qb) {
        Map<String, QueryBatch.Stats> statss = new HashMap<>();
        Map<String, List<Double>> aggregatedList = new HashMap<>();
        Map<String, Double> tmrr = new HashMap<>();
        Map<String, Double> tpAt10 = new HashMap<>();
        int totalRelevants = 0;
        int retrievedRelevants = 0;
        boolean verbose = false;

        for (Query q : qb) {
            if (q.getFoundRelevants().isEmpty()) {
                continue;
            }
            if (q.isIgnored()) {
                continue;
            }
            List<Query.Result> r1 = null;
            List<Query.Result> r2 = null;
            for (Map.Entry<String, List<Query.Result>> r : q.getResults().entrySet()) {
                if (r1 != null) {
                    r2 = r.getValue();
                } else {
                    r1 = r.getValue();
                }
            }
            totalRelevants += q.getRelevants().keySet().size();
            retrievedRelevants += q.getFoundRelevants().size();
            if (verbose) {
                System.out.println("Total relevants: " + q.getRelevants().size());
                System.out.println("Found relevants: " + q.getFoundRelevants().size());
            }
            Map<Integer, Integer> numberOfSearchersFoundEachRelevant = new HashMap<>();
            Map<String, Integer> numberOfRelevantsEachSearcherFound = new HashMap<>();
            Map<String, Integer> averageLocattionOfRelevantsEachSearcherFound = new HashMap<>();
            for (Map.Entry<String, List<Query.Result>> r : q.getResults().entrySet()) {
                List<Double> get1 = aggregatedList.get(r.getKey());
                Double mrr;
                Double pAt10;
                if (get1 == null) {
                    get1 = new LinkedList<>();
                    for (int j = 0; j < Configs.numOfResults; j++) {
                        get1.add(0.);
                    }
                    mrr = 0.;
                    pAt10 = 0.;
                } else {
                    mrr = tmrr.get(r.getKey());
                    pAt10 = tpAt10.get(r.getKey());
                }

                int firstRelevant = 0;
                int k = 0;
                for (Query.Result d : r.getValue()) {
                    if (verbose) {
                        if (d.getRelevancy() > 0) {
                            Integer get = numberOfSearchersFoundEachRelevant.get(d.getDocId());
                            if (get == null) {
                                get = 0;
                            }
                            get++;
                            numberOfSearchersFoundEachRelevant.put(d.getDocId(), get);
                            Integer get2 = numberOfRelevantsEachSearcherFound.get(r.getKey());
                            if (get2 == null) {
                                get2 = 0;
                            }
                            get2++;
                            numberOfRelevantsEachSearcherFound.put(r.getKey(), get2);
                            Integer get3 = averageLocattionOfRelevantsEachSearcherFound.get(r.getKey());
                            if (get3 == null) {
                                get3 = 0;
                            }
                            get3 += (k + 1);
                            averageLocattionOfRelevantsEachSearcherFound.put(r.getKey(), get3);
                        }
                    }
                    if (k < 10) {
                        pAt10 += d.getRelevancy();
                    }
                    get1.set(k, get1.get(k) + d.getRelevancy());
                    k++;
                    if (d.getRelevancy().equals(1.)) {
                        firstRelevant = k;
                    }
                }
                mrr += firstRelevant == 0 ? 0 : 1. / firstRelevant;
                aggregatedList.put(r.getKey(), get1);
                tmrr.put(r.getKey(), mrr);
                tpAt10.put(r.getKey(), pAt10);
            }
            for (Map.Entry<Integer, Integer> e : numberOfSearchersFoundEachRelevant.entrySet()) {
                System.out.println("Num: " + e.getKey() + ": " + e.getValue());
            }
            if (verbose) {
                double stdev = 0;
                double average = 0;
                for (Map.Entry<String, Integer> e : numberOfRelevantsEachSearcherFound.entrySet()) {
                    System.out.println("Num2: " + e.getKey() + ": " + e.getValue());
                    stdev += e.getValue() * e.getValue();
                    average += e.getValue();
//                System.out.println("Num3: " + e.getKey() + ": " + ((double) e.getValue())/e.getValue());
                }
                average /= numberOfRelevantsEachSearcherFound.size();
                stdev = Math.sqrt(((double) 1) / (numberOfRelevantsEachSearcherFound.size() - 1) * stdev - ((double) numberOfRelevantsEachSearcherFound.size()) / (numberOfRelevantsEachSearcherFound.size() - 1) * average * average);
                System.out.println("STAT: " + stdev / average + ", STDEV:" + stdev + ",AVERAGE:" + average);
            }
        }

        for (String k : aggregatedList.keySet()) {
            QueryBatch.Stats stats = new QueryBatch.Stats();
            statss.put(k, stats);
            Double mrr = tmrr.get(k);
            mrr /= qb.properQueryCount();
            stats.rr = mrr;
//                tmrr.put(k, mrr);
            Double pAt10 = tpAt10.get(k);

            final double pAt10Temp = pAt10 / 10 / qb.properQueryCount();
            stats.pAt10 = pAt10Temp;

//                    double[] precision = ndcgMap.get(e.getKey());
            double[] currentNDCG = new double[Configs.numOfResults];
            double[] currentCounts = new double[Configs.numOfResults];
            double[] baselineNDCG = new double[Configs.numOfResults];
            List<Double> result = aggregatedList.get(k);
            for (int j = 0; j < Configs.numOfResults - result.size(); j++) {
                result.add(0.);
            }

            Double[] toArray = result.toArray(new Double[result.size()]);
            currentNDCG[0] = toArray[0];
            baselineNDCG[0] = 1;
            double err = 0;
            double errP = 1;
            double ap = 0;
            int rels = 0;
            double maxAP = 0;
            final int relevantsCount = numOfRelevants(qb.iterator().next());
            for (int i = 0; i < Configs.numOfResults; i++) {
                if (i < relevantsCount) {
                    maxAP += ((double) i + 1) / (i + 1 + Configs.mapBias);
                }
                double R = (Math.pow(2, toArray[i]) - 1) / 2;
                err += errP * R / (i + 1);
                errP *= (1 - R);
                if (toArray[i].equals(1.)) {
                    rels++;
                    ap += ((double) rels) / (i + 1 + Configs.mapBias);
                }
                if (i > 0) {
                    currentNDCG[i] = currentNDCG[i - 1] + toArray[i] / (Math.log(i + 1) / Math.log(2));
                    baselineNDCG[i] = baselineNDCG[i - 1] + (i < relevantsCount ? 1 : 0) / (Math.log(i + 1) / Math.log(2));
                }
                currentCounts[i] = toArray[i] / qb.properQueryCount();
            }
            stats.counts = currentCounts;
            ap /= relevantsCount;
            maxAP /= relevantsCount;
            stats.ap = ap / maxAP;
            stats.err = err;

            for (int i = 0; i < Configs.numOfResults; i++) {
                currentNDCG[i] /= baselineNDCG[i];
            }
            stats.ndcg = currentNDCG;

            stats.rels = rels;
        }
//        if (statss.get("_05_66_7_PHRR").counts[0] == 1 && statss.get("02-LM").counts[0] != 1) {
//            Query q = qb.iterator().next();
//            org.apache.lucene.document.Document d_LM;
//            org.apache.lucene.document.Document d_HRR;
//            try {
//                d_LM = Main.hiers[0].getRootNode().getIndexReader().document(q.getResults().get("02-LM").get(0).getDocId());
//                d_HRR = Main.hiers[0].getRootNode().getIndexReader().document(q.getResults().get("_05_66_7_PHRR").get(0).getDocId());
//            } catch (IOException ex) {
//                Logger.getLogger(Retriever.class.getName()).log(Level.SEVERE, null, ex);
//                throw new RuntimeException();
//            }
//            System.out.println("TTTTEEEESSSSTTT\n" + q.getQuery() + "\n" + q.getSearcher() + "\n" + d_LM.get("content") + "\n" + d_HRR.get("content"));
//        }
        return new Result(statss, totalRelevants, retrievedRelevants, qb);
    }

    protected void createQueryAndSearch(QueryBatch qb, Searcher s) throws QueryNodeException, IOException {
        for (Query q : qb) {
            System.out.println(q.getQueryId() + " " + q.getSearcher());
            s.search(q, Configs.numOfResults + 1);
        }
    }

    public String getName() {
        return name;
    }

    public List<Searcher> getSearchers() {
        return Collections.unmodifiableList(searchers);
    }

    protected String prepareQuery(String get) {
        get = get.trim();
        get = get.replace('+', ' ');
        get = get.replace('-', ' ');
        get = get.replaceAll("&&", " ");
        get = get.replaceAll("\\|\\|", " ");
        get = get.replace('!', ' ');
        get = get.replace('(', ' ');
        get = get.replace(')', ' ');
        get = get.replace('{', ' ');
        get = get.replace('}', ' ');
        get = get.replace('[', ' ');
        get = get.replace(']', ' ');
        get = get.replace('^', ' ');
        get = get.replace('\"', ' ');
        get = get.replace('~', ' ');
        get = get.replace('*', ' ');
        get = get.replace('?', ' ');
        get = get.replace(':', ' ');
        get = get.replace('\\', ' ');
        get = get.replace('/', ' ');
        get = get.replaceAll("\\b(and|or|not)\\b", " ");
        if (get.startsWith("and ") || get.startsWith("or ") || get.startsWith("not ")
                || get.endsWith(" and") || get.endsWith(" or") || get.endsWith(" not")) {
            throw new RuntimeException();
        }
        return get;
    }

    protected void writeLogs(QueryBatch qb) {

    }

    public List<Result> getProcessedResults() {
        return Collections.unmodifiableList(processedResults);
    }

    public abstract Set<User> getPublishers(String docId);

    protected static class Result {

        Map<String, QueryBatch.Stats> perSearcherStatss;
        int totalRelevants;
        int relevantsRetrieved;
        QueryBatch qb;

        Result(Map<String, QueryBatch.Stats> perSearcherStatss, int totalRelevants, int relevantsRetrieved, QueryBatch qb) {
            this.perSearcherStatss = perSearcherStatss;
            this.totalRelevants = totalRelevants;
            this.relevantsRetrieved = relevantsRetrieved;
            this.qb = qb;
        }

    }

    @Override
    public void close() throws IOException {
        if (traceWriter != null) {
            traceWriter.close();
        }
    }

}
