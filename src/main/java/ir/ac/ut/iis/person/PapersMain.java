/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person;

import ir.ac.ut.iis.person.datasets.citeseerx.CiteseerxUserFactory;
import ir.ac.ut.iis.person.datasets.citeseerx.PapersExtractor;
import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import ir.ac.ut.iis.person.paper.PapersRetriever;
import ir.ac.ut.iis.person.paper.TopicsProfileGenerator;
import ir.ac.ut.iis.person.paper.TopicsReader;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class PapersMain extends DatasetMain {

    public PapersMain() {
    }

    public void main(String prefix) {
        final String folderName = Configs.datasetRoot + "results/" + (prefix == null ? "" : (prefix + "-")) + Configs.commonParameters();
        File file = new File(folderName);
        if (!file.exists()) {
            file.mkdirs();
        }
        main("", folderName + "/result");
    }

    public void main(String prefix, String outputPath) {
        DatasetMain.setInstance(this);
        uf = new CiteseerxUserFactory();
        if (Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP1)
                || Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP2)
                || Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP3)
                || Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES)
                || Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_PPRs)) {
            createIndexes(null, Configs.datasetRoot + "clusters/general.tree", Configs.datasetRoot + "index", Configs.datasetRoot + "index/general", true);
            return;
        }
        initialize(prefix, outputPath);
        if (Configs.runStage.equals(Configs.RunStage.CREATE_SOCIAL_TEXTUAL_DATABASE)) {
            new PapersExtractor().createSocialTextualSQLScripts(Configs.datasetRoot + "papers_giant.sql", Configs.datasetRoot + "coauthors_giant.sql");
        }
//        System.exit(0);
//        new PageRankCacher(Main.database_name, String.valueOf(Main.pagerankAlpha)).cache();
//        new PageRankCacher(datasetName).loadPPRs();
    }

    @Override
    protected Hierarchy<?> loadHierarchy() {
        return loadHierarchy(Configs.datasetRoot + Configs.graphFile, Configs.clustersFileName == null ? null : Configs.datasetRoot + "clusters/" + Configs.clustersFileName + ".tree", Configs.clustersFileName, false, false, false, false);
    }

    @Override
    public Hierarchy loadHierarchy(String graphFile, String clustersFile, String name, boolean ignoreLastWeight, boolean addNodesAsClusters, boolean loadAsFlatHierarchy, boolean isMultiLayer) {
        Hierarchy hier;
        try {
            hier = new Hierarchy<>(name);
            hier.load(false, null, null, Configs.loadGraph ? clustersFile : null, addNodesAsClusters, loadAsFlatHierarchy, isMultiLayer);
//            hiers[1] = new Hierarchy(Main.datasetRoot+"clusters/web_20.tree", "web_20");
//            hiers[1].load(false, Main.datasetRoot+"index/web_20-only", hiers[0]);
//                Hierarchy hier3 = new Hierarchy(Main.datasetRoot+"clusters/verification.tree", "verification");
//                hier3.load(create, Main.datasetRoot+"index", Main.datasetRoot+"index/verification-new", true);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        if (Configs.loadGraph) {
            hier.readGraph(graphFile, ignoreLastWeight, isMultiLayer);
        }
        return hier;
    }

    public void initialize(String prefix, String outputPath) {
        Main.outputPath = outputPath;
        setIndexSearcher(openSearcher());

        Configs.evaluator.setSearcher(indexSearcher);
        String name = prefix + Configs.commonParameters();
        Main.retriever = new PapersRetriever(name, Configs.evaluator, indexSearcher, Configs.datasetRoot + "queries/" + Configs.queryFile);

//        ((PapersRetriever) Main.retriever).calcDatasetStats(Configs.datasetRoot + "datasetStats.txt");
//        ((PapersRetriever) Main.retriever).createSimpleQueries(Configs.datasetRoot + "queries/general-queries.txt");
        if (Configs.runStage.equals(Configs.RunStage.CREATE_QUERIES)) {
            ((PapersRetriever) Main.retriever).createSimpleQueries(Configs.datasetRoot + "queries/test.txt", null);
//            ((PapersRetriever) Main.retriever).createMultidisciplinaryQueries(Configs.datasetRoot + "queries/multidisciplinary.txt");
        }
//        ((PapersRetriever) Main.retriever).createMultidisciplinaryQueries(Configs.datasetRoot + "queries/multidisciplinary-queries.txt");
//        System.exit(0);
//        retriever = new PapersInvertedRetriever(hiers[0].getRootNode().getSearcher(), hiers[0].getRootNode().getIndexReader(), "inverted-retriever.txt", Main.datasetRoot+"queries/inverted-general-queries.txt");
//        ((PapersInvertedRetriever)retriever).createSimpleQueries("inverted-general-queries.txt");
//        retriever.setTraceWriter("searchTraces.log");
//                Retriever retriever = new CiteseerxRandomRetriever(hier.node.getSearcher(), hier.node.getIndexReader(), "retriever.txt", Main.datasetRoot+"queries/queries-web.txt");
//                Retriever retriever = new InvertedCiteseerxRetriever(hier.node.getSearcher(), hier.node.getIndexReader(), "invertedRetriever.txt", "invertedQueries.txt");
//                ((CiteseerxRetriever) retriever).createQueries("queries-web.txt", "Web searching and information discovery");
//                ((InvertedCiteseerxRetriever) retriever).createQueries("invertedQueries.txt", "Web searching and information discovery");
//                System.exit(0);
    }

    protected void createIndexes(String topic, String tree, String rootIndex, String index, boolean makeRootIndex) {
        try {
            Hierarchy hier = new Hierarchy<>("temp");
            hier.load(true, rootIndex, index, !makeRootIndex ? tree : null, false, false, false);

            PapersExtractor extractor = new PapersExtractor();

            Map<String, float[]> topics = null;
            Map<String, TopicsReader.DocTopics> docTopics = null;
            if (Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP1)
                    || Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP2)
                    || Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP3)) {
                topics = new TopicsReader().readComposition(Configs.datasetRoot + "topics/" + Configs.topicsName + "/doc-topics.mallet");
                docTopics = new TopicsReader().readDocTopics(Configs.datasetRoot + "topics/" + Configs.topicsName + "/model.mallet");
            }

            if (makeRootIndex) {
                extractor.doParseAndIndex(hier, null, true, topics, docTopics);
            } else {
                extractor.doParseAndIndex(hier, topic, false, topics, docTopics);
            }

            hier.getRootNode().close();
            if (Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP1)) {
                initialize(null, null);
                try (TopicsProfileGenerator tpf = new TopicsProfileGenerator(Configs.database_name, Configs.useDirichletEstimationForAuthorTopics)) {
                    tpf.generateTopicsProfile(new ObjectOpenHashSet<>(loadHierarchy().getUserNodeMapping().values()), DatasetMain.getInstance().getIndexSearcher(), topics);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
