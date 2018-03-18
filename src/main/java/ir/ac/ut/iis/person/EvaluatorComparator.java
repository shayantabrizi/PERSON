/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person;

import ir.ac.ut.iis.person.algorithms.campos.HRR;
import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.base.Retriever;
import ir.ac.ut.iis.person.base.Searcher;
import ir.ac.ut.iis.person.evaluation.person.PERSONEvaluator;
import ir.ac.ut.iis.person.others.ResultConverter;
import ir.ac.ut.iis.person.query.NormalizedQueryExpander;
import ir.ac.ut.iis.person.query.QueryExpander;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 *
 * @author shayan
 */
public class EvaluatorComparator {

    public Retriever retriever;
    public String outputFolder;

    public void tauAcrossMethods(boolean self) {
        final String name = "methods_" + self + "_" + Configs.ASPIRECoefficient;
        outputFolder = Configs.datasetRoot + "results/EC/methods/self=" + self + "," + Configs.commonParametersWithoutEvaluator() + "," + Configs.aspireParameters() + "," + Configs.personParameters() + "/";
        File file = new File(outputFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFolder + "tau"))) {
            int[] k = {5, 10, 20, 40};
            double[] p0 = {.33, .66, .99};
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp1;
                    if (!self) {
                        Configs.evaluator = new PERSONEvaluator();
                        Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers = 1;
                        Configs.yearFiltering = true;
                        tauTemp1 = tauTempAcrossMethods(name + "_", k[i], p0[j], self);
                    } else {
                        Configs.initializeWithASPIRE();
                        Configs.skipQueries = 0;
                        tauTemp1 = tauTempAcrossMethods(name + "_", k[i], p0[j], self);
                    }
                    Configs.initializeWithASPIRE();
                    if (self) {
                        Configs.skipQueries = 13_000;//Configs.queryCount * 6;
                    }
                    Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp2 = tauTempAcrossMethods(name + "_", k[i], p0[j], self);
                    writer.write(calcResult(tauTemp1, tauTemp2, "k=" + k[i] + ",p0=" + p0[j]));
                    writer.flush();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected String calcResult(Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp1, Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp2, final String prefix) {
        final StringBuilder output = new StringBuilder(prefix).append(":\n");
        perMeasureOutput(output, "ndcg", "NDCG@50", tauTemp1, tauTemp2);
        perMeasureOutput(output, "map", "AP", tauTemp1, tauTemp2);
        perMeasureOutput(output, "mrr", "MRR", tauTemp1, tauTemp2);
        perMeasureOutput(output, "merr", "ERR", tauTemp1, tauTemp2);
        perMeasureOutput(output, "pAt10", "P@10", tauTemp1, tauTemp2);
        return output.toString();
    }

    protected void perMeasureOutput(StringBuilder output, String statsName, String riName, Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp1, Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp2) {
        Map<String, Double> vals1 = extractNDCGMap(statsName, tauTemp1);
        Map<String, Double> vals2 = extractNDCGMap(statsName, tauTemp2);
        int bestIndex = bestIndex(vals1, vals2);
        ResultConverter.KendallTauResult calcKendallTau = ResultConverter.calcKenallTauB(vals1, vals2);
        int bestIndexRI = bestIndex(tauTemp1.getValue().get(riName), tauTemp2.getValue().get(riName));
        ResultConverter.KendallTauResult calcKendallTauRI = ResultConverter.calcKenallTauB(tauTemp1.getValue().get(riName), tauTemp2.getValue().get(riName));
        output.append("\t").append(statsName).append(":").append(calcKendallTau.kendallTau).append(",").append(calcKendallTau.pValue).append(",").append(calcKendallTau.z).append(",").append(calcKendallTau.difference).append(",").append(bestIndex)
                .append(";;;").append(calcKendallTauRI.kendallTau).append(",").append(calcKendallTauRI.pValue).append(",").append(calcKendallTauRI.z).append(",").append(calcKendallTauRI.difference).append(",").append(bestIndexRI).append("\n");
    }

    protected Map<String, Double> extractNDCGMap(String measureName, Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp1) {
        Map<String, Double> ndcgs = new HashMap<>();
        Field field;
        try {
            field = ResultConverter.Stats.class.getDeclaredField(measureName);
        } catch (NoSuchFieldException | SecurityException ex) {
            Logger.getLogger(EvaluatorComparator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        field.setAccessible(true);
        for (Map.Entry<String, ResultConverter.Stats> e : tauTemp1.getKey().entrySet()) {
            double num;
            try {
                Object get = field.get(e.getValue());
                if (measureName.equals("ndcg")) {
                    num = ((Double[]) get)[Configs.ndcgAt - 1];
                } else {
                    num = (double) get;
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(EvaluatorComparator.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            ndcgs.put(e.getKey(), num);
        }
        return ndcgs;
    }

    public void tauAcrossProfiles(boolean self) {
        outputFolder = Configs.datasetRoot + "results/EC/profiles/self=" + self + "," + Configs.commonParametersWithoutEvaluator() + "," + Configs.aspireParameters() + "," + Configs.personParameters() + "/";
        File file = new File(outputFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFolder + "tau"))) {
//            String[] arr = {"IHRR", "IRR", "PHRR", "QE", "NQE", "HRR", "SRR"};
            String[] arr = {"QE", "NQE", "HRR", "SRR", "IRR", "IHRR", "PHRR"};
//            String[] arr = {"IHRR", "IRR", "PHRR"};
            for (int i = 2; i < arr.length; i++) {
                Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp1;
                final String name = "profiles_" + self + "_" + Configs.ASPIRECoefficient;
                if (!self) {
                    Configs.evaluator = new PERSONEvaluator();
                    Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers = 1;
                    Configs.yearFiltering = true;
                    tauTemp1 = tauTempAcrossProfiles(name + "_", arr[i], self);
                } else {
                    Configs.initializeWithASPIRE();
                    Configs.skipQueries = 0;
                    tauTemp1 = tauTempAcrossProfiles(name + "_", arr[i], self);
                }
                Configs.initializeWithASPIRE();
                if (self) {
                    Configs.skipQueries = 13_000;//Configs.queryCount * 6;
                }
                Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp2 = tauTempAcrossProfiles(name + "_", arr[i], self);
                writer.write(calcResult(tauTemp1, tauTemp2, "method=" + arr[i]));
                writer.flush();
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void tauAcrossCampos(boolean self) {
        outputFolder = Configs.datasetRoot + "results/EC/campos/self=" + self + "," + Configs.commonParametersWithoutEvaluator() + "," + Configs.aspireParameters() + "," + Configs.personParameters() + "/";
        File file = new File(outputFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFolder + "tau"))) {
            Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp1;
            final String name = "campos_" + self + "_" + Configs.ASPIRECoefficient;
            if (!self) {
                Configs.evaluator = new PERSONEvaluator();
                Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers = 1;
                Configs.yearFiltering = true;
                tauTemp1 = tauTempAcrossCampos(name + "_", self);
            } else {
                Configs.initializeWithASPIRE();
                Configs.skipQueries = 0;
                tauTemp1 = tauTempAcrossCampos(name + "_", self);
            }
            Configs.initializeWithASPIRE();
            if (self) {
                Configs.skipQueries = 13_000;// Configs.queryCount * 6;
            }
            Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTemp2 = tauTempAcrossCampos(name + "_", self);
            writer.write(calcResult(tauTemp1, tauTemp2, "campos"));
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected Map<String, Double[]> conv(Map<String, Double> tauTemp) {
        Map<String, Double[]> conv = new HashMap<>();
        for (Map.Entry<String, Double> e : tauTemp.entrySet()) {
            Double[] d = new Double[100];
            d[Configs.ndcgAt - 1] = e.getValue();
            conv.put(e.getKey(), d);
        }
        return conv;
    }

    private int bestIndex(Map<String, Double> tauTemp1, Map<String, Double> baseline) {
        double max2 = Double.NEGATIVE_INFINITY;
        String maxInd2 = null;
        for (Map.Entry<String, Double> e : baseline.entrySet()) {
            if (e.getValue() > max2) {
                max2 = e.getValue();
                maxInd2 = e.getKey();
            }
        }

        int biggerCnt = 0;
        for (Map.Entry<String, Double> e : tauTemp1.entrySet()) {
            if (e.getValue() > tauTemp1.get(maxInd2)) {
                biggerCnt++;
            }
        }
        return biggerCnt + 1;
    }

    protected Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTempAcrossCampos(String prefix, boolean self) {
        new PapersMain().main(prefix, outputFolder + Configs.evaluator.getName());
        this.retriever = Main.retriever;
        AddSearchers.addBaseline();
        AddSearchers.addAllCamposSearchers(prefix);
        Main.retrieve();
        return calcStats();
    }

    protected Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> calcStats() throws NumberFormatException {
        Map<String, ResultConverter.Stats> calc = ResultConverter.readStats(Main.outputPath);
        Map<String, Map<String, Double>> ri = ResultConverter.calcRI(Main.outputPath + "-sig");
        return new Pair<>(calc, ri);
    }

    protected Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTempAcrossMethods(String prefix, int k, double p0, boolean self) {
        new PapersMain().main(prefix, outputFolder + Configs.evaluator.getName() + "_k=" + k + "_p0=" + p0);
        this.retriever = Main.retriever;
        AddSearchers.addBaseline();
        AddSearchers.addCamposSearchers(k, prefix + k + "_" + p0 + "_", p0);
        Main.retrieve();
        return calcStats();
    }

    protected Pair<Map<String, ResultConverter.Stats>, Map<String, Map<String, Double>>> tauTempAcrossProfiles(String prefix, String method, boolean self) {
        new PapersMain().main(prefix, outputFolder + Configs.evaluator.getName() + "_method=" + method);
        this.retriever = Main.retriever;
        AddSearchers.addBaseline();
        int[] k = {5, 10, 20, 40};
        double[] p0 = {.33, .66, .99};
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                NormalizedQueryExpander normalizedQueryExpander = new NormalizedQueryExpander(k[i], Configs.evaluator, p0[j]);
                switch (method) {
                    case "QE":
                        QueryExpander queryExpander = new QueryExpander(k[i], Configs.evaluator);
                        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), prefix + k[i] + "_" + p0[j] + "_1_QE", AddSearchers.getBaseSimilarity(), queryExpander));
                        break;
                    case "NQE":
                        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), prefix + k[i] + "_" + p0[j] + "_2_NQE", AddSearchers.getBaseSimilarity(), normalizedQueryExpander));
                        break;
                    case "HRR":
                    case "SRR":
                    case "IRR":
                    case "IHRR":
                        try {
                            retriever.addSearcher((Searcher) Class.forName("ir.ac.ut.iis.taval.algorithms.campos." + method).getConstructors()[0].newInstance(DatasetMain.getInstance().getIndexSearcher(), prefix + k[i] + "_" + p0[j] + "_3_" + method, AddSearchers.getBaseSimilarity(), AddSearchers.getQueryConverter(), normalizedQueryExpander));
                        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                            throw new RuntimeException();
                        }
                        break;
                    case "PHRR":
                        NormalizedQueryExpander normalizedQueryExpanderOriginalIgnored = new NormalizedQueryExpander(DatasetMain.getInstance().getIndexSearcher(), k[i], Configs.evaluator, p0[j], true);
                        retriever.addSearcher(new HRR(DatasetMain.getInstance().getIndexSearcher(), prefix + k[i] + "_" + p0[j] + "_7_PHRR", AddSearchers.getBaseSimilarity(), AddSearchers.getQueryConverter(), normalizedQueryExpanderOriginalIgnored));
                        break;
                }
            }
        }
        Main.retrieve();
        return calcStats();
    }

}
