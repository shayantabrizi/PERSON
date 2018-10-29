/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person;

import ir.ac.ut.iis.person.base.Retriever;
import ir.ac.ut.iis.person.paper.TopicsProfileGenerator;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.store.LockObtainFailedException;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

/**
 *
 * @author Shayan
 */
public class Main {

    private static final Random RANDOM = new Random(Configs.randomSeed);
    public static Retriever retriever;
    public static String outputPath;
    public static int i;

    public static void main(String[] args) throws FileNotFoundException, CorruptIndexException, LockObtainFailedException, LockObtainFailedException, IOException, DOMException, ParserConfigurationException, SAXException, QueryNodeException, FileNotFoundException, FileNotFoundException, IOException {
//        createIndex(Configs.topicsName, Configs.profileTopicsDBTable, Configs.RunStage.CREATE_INDEXES);    
//        createIndex(Configs.topicsName, Configs.profileTopicsDBTable, Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP1);     
//        createIndex(Configs.topicsName, Configs.profileTopicsDBTable, Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP2);     
//        createIndex(Configs.topicsName, Configs.profileTopicsDBTable, Configs.RunStage.CREATE_QUERIES);     
//        createIndex(Configs.topicsName, Configs.profileTopicsDBTable, Configs.RunStage.CREATE_SOCIAL_TEXTUAL_DATABASE);     
//        expAxioms();
        expMoreAxioms();
//        expCampos();
//        expCamposTau();
//        expCompare();
//        expRobustness();
    }

    /* 
    * IMPORTANT: Run with at least -Xmx2500m
     */
    public static void expCampos() {
        Configs.ndcgAt = 50;
        Configs.useTFIDFWeightingInCampos = false;
        Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers = 1;
        try (PapersMain main = new PapersMain()) {
            main.main("Cam");
            AddSearchers.addBaseline();
            AddSearchers.addAllCamposSearchers("");
            retrieve();
        }
    }

    public static void expAxioms() {
        try (PapersMain main = new PapersMain()) {
            main.main("A");
            AddSearchers.addAxiomSearchers();
            retrieve();
        }
    }

    public static void expMoreAxioms() {
        Configs.baseSimilarityName = "MyLM";
        Configs.lmDirichletMu = 400;
        AddSearchers.reinitialize();
        try (PapersMain main = new PapersMain()) {
            main.main("MA");
            AddSearchers.addBaseline();
            AddSearchers.addFeedbackSearchers();
            AddSearchers.addTopicSearchers();
            retrieve();
        }
    }

    public static void expCamposTau() {
        Configs.ndcgAt = 50;
        Configs.topicsName = "100-AsymmetricAlpha-TFLogIDF-TFLogIDF";
        Configs.useTFIDFWeightingInCampos = true;

//        new EvaluatorComparator().tauAcrossMethods(true);
        new EvaluatorComparator().tauAcrossProfiles(false);
//        new EvaluatorComparator().tauAcrossCampos(true);
    }

    public static void expCompare() {
        Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers = 1;
        Configs.baseSimilarityName = "TF-IDF";
        Configs.ignoreSelfCitations = true;
        Configs.useSearchCaching = false;
        Configs.selfConsiderConstant = 0;
        AddSearchers.reinitialize();
        try (PapersMain main = new PapersMain()) {
            main.main("C");
            AddSearchers.addBaseline();
            AddSearchers.addOtherSearchers();
            AddSearchers.addAggregateSearchers(false);
            retrieve();
        }
    }

    public static void expRobustness() {
        Configs.baseSimilarityName = "TF-IDF";
        Configs.ignoreSelfCitations = true;
        Configs.useSearchCaching = false;
        try (PapersMain main = new PapersMain()) {
            main.main("R");
//            AddSearchers.addBaseline();
            AddSearchers.addAggregateSearchers(true);
            retrieve();
        }
    }

    public static Integer random(int max) {
        return Math.abs(RANDOM.nextInt() % max);
    }

    public static void retrieve() {
        Writer out = null;
        Writer perQueryOutput = null;

        retriever.skipQueryBatch(Configs.skipQueries);

        if (out == null) {
            try {
                out = new FileWriter(outputPath);
                perQueryOutput = new FileWriter(outputPath + ".query");
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        }
        System.out.println(outputPath);
        for (i = 0; i < Configs.queryCount; i++) {
            // @TODO: Shayan
//                        hier.node.getNodeId("0").getNodeId("1").calculateClosenessCenter();
//                        hier.doRetrieveLucene4(hier.node.getNodeId("0").getNodeId("1"), "C:\\Mohammad\\ut\\MS\\thesis\\citedata\\clustering\\shayan_maziar\\citedata\\topublish\\queries.txt", extractor);
            if (!retriever.hasNextQueryBatch()) {
                break;
            }
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "query num = {0}, results num = {1}", new Object[]{i, retriever.getProcessedResults().size()});
            if (!retriever.retrieve()) {
                continue;
            }

            retriever.writeResults(out, perQueryOutput);
        }

        retriever.writeSignificantTestData(outputPath + "-sig");
        try {
            DatasetMain.getInstance().close();
            out.close();
            perQueryOutput.close();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    // Run with -Xmx8000m
    public static void createIndex(String topicsName, String profileTopicsDBTable, Configs.RunStage runStage) {
        Configs.indexName = "index" + profileTopicsDBTable;
        if (!runStage.equals(Configs.RunStage.CREATE_INDEXES)) {
            Configs.loadGraph = true;
            Configs.profileTopicsDBTable = profileTopicsDBTable;
            Configs.topicsName = topicsName;
        }
        Configs.runStage = runStage;
        try (PapersMain main = new PapersMain()) {
            main.main("Ind");
        }
    }

}
