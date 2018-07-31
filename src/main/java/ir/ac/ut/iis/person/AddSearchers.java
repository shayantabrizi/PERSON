/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person;

import static ir.ac.ut.iis.person.Main.outputPath;
import static ir.ac.ut.iis.person.Main.retriever;
import ir.ac.ut.iis.person.algorithms.aggregate.AggregateSearcher;
import ir.ac.ut.iis.person.algorithms.aggregate.MyValueSource;
import ir.ac.ut.iis.person.algorithms.campos.HRR;
import ir.ac.ut.iis.person.algorithms.campos.IHRR;
import ir.ac.ut.iis.person.algorithms.campos.IRR;
import ir.ac.ut.iis.person.algorithms.campos.SRR;
import ir.ac.ut.iis.person.algorithms.queries.MyLMQuery;
import ir.ac.ut.iis.person.algorithms.queries.MyWeiQuery;
import ir.ac.ut.iis.person.algorithms.searchers.BasicSearcher;
import ir.ac.ut.iis.person.algorithms.searchers.FeedbackExpander;
import ir.ac.ut.iis.person.algorithms.searchers.RandomSearcher;
import ir.ac.ut.iis.person.algorithms.searchers.SocialSearcher;
import ir.ac.ut.iis.person.algorithms.social_textual.citeseerx.CiteseerxSocialTextualValueSource;
import ir.ac.ut.iis.person.baselines.Binary_Log_No;
import ir.ac.ut.iis.person.baselines.Binary_Log_Yes;
import ir.ac.ut.iis.person.baselines.Binary_No_No;
import ir.ac.ut.iis.person.baselines.Binary_No_Yes;
import ir.ac.ut.iis.person.baselines.Log_Log_No;
import ir.ac.ut.iis.person.baselines.Log_Log_Yes;
import ir.ac.ut.iis.person.baselines.Log_No_No;
import ir.ac.ut.iis.person.baselines.Log_No_Yes;
import ir.ac.ut.iis.person.baselines.Raw_Log_No;
import ir.ac.ut.iis.person.baselines.Raw_Log_Yes;
import ir.ac.ut.iis.person.baselines.Raw_No_No;
import ir.ac.ut.iis.person.baselines.Raw_No_Yes;
import ir.ac.ut.iis.person.myretrieval.MyDummySimilarity;
import ir.ac.ut.iis.person.paper.TopicsReader;
import ir.ac.ut.iis.person.query.NormalizedQueryExpander;
import ir.ac.ut.iis.person.query.QueryConverter;
import ir.ac.ut.iis.person.query.QueryExpander;
import ir.ac.ut.iis.person.topics.InstanceClassifier;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author shayan
 */
public class AddSearchers {

    private static Similarity baseSimilarity;
    private static QueryConverter queryConverter;

    static {
        reinitialize();
    }

    public static void reinitialize() {
        AddSearchers.baseSimilarity = baseSimilarity();
        AddSearchers.queryConverter = new QueryConverter();
        Configs.datasetRoot = Configs.getDatasetRoot();
    }

    public static void addBaseline() {
//                QueryExpander queryExpander = new QueryExpander(.04);
//                queryExpander.readSimilarities();
        final BasicSearcher baselineSearcher = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-" + Configs.baseSimilarityName, baseSimilarity, queryConverter);
        Configs.evaluator.setBaselineSearcher(baselineSearcher);
        retriever.addSearcher(baselineSearcher);
//        final BasicSearcher baselineSearcher2 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-LM105", new LMDirichletSimilarity(105), queryConverter);
//        retriever.addSearcher(baselineSearcher2);
//        final BasicSearcher baselineSearcher3 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-LM-150", new LMDirichletSimilarity(150f), queryConverter);
//        retriever.addSearcher(baselineSearcher3);
//        final BasicSearcher baselineSearcher4 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-LM-100", new LMDirichletSimilarity(100f), queryConverter);
//        retriever.addSearcher(baselineSearcher4);
//        final BasicSearcher baselineSearcher5 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-LM-50", new LMDirichletSimilarity(50), queryConverter);
//        retriever.addSearcher(baselineSearcher5);
//        final BasicSearcher baselineSearcher6 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-LM-87", new LMDirichletSimilarity(87), queryConverter);
//        retriever.addSearcher(baselineSearcher6);
//        final BasicSearcher baselineSearcher7 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-MyLM-87", new MyDummySimilarity(Configs.lmDirichletMu, new MyLMQuery(87)), queryConverter);
//        retriever.addSearcher(baselineSearcher7);
//        final BasicSearcher baselineSearcher8 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-MyLM-50", new MyDummySimilarity(Configs.lmDirichletMu, new MyLMQuery(50)), queryConverter);
//        retriever.addSearcher(baselineSearcher8);
//        final BasicSearcher baselineSearcher9 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-MyLM-100", new MyDummySimilarity(Configs.lmDirichletMu, new MyLMQuery(100)), queryConverter);
//        retriever.addSearcher(baselineSearcher9);
//        final BasicSearcher baselineSearcher10 = new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-MyLM-200", new MyDummySimilarity(Configs.lmDirichletMu, new MyLMQuery(200)), queryConverter);
//        retriever.addSearcher(baselineSearcher10);
//                addAggregateSearchers(baseSimilarity, queryConverter);
//                addClusterBasedSearchers(baseSimilarity, queryConverter);
//                addOtherSearchers(baseSimilarity, queryConverter);
//                addMyWeightingSearchers(queryConverter);
//        addBaselineSearchers(queryConverter);
//        addAllCamposSearchers(baseSimilarity, "", queryConverter);
//                addTopicSearchers(baseSimilarity, queryConverter);
    }

    public static QueryConverter getQueryConverter() {
        return queryConverter;
    }

    public static Similarity getBaseSimilarity() {
        return baseSimilarity;
    }

    private static Similarity baseSimilarity() {
        Similarity bs;
        switch (Configs.baseSimilarityName) {
            case "LM":
                bs = new LMDirichletSimilarity(Configs.lmDirichletMu);
                break;
            case "MyLM":
                bs = new MyDummySimilarity(Configs.lmDirichletMu, new MyLMQuery((Configs.lmDirichletMu)));
                break;
            default:
                bs = new Log_Log_Yes();
                break;
        }
        return bs;
    }

    public static void addTopicSearchers() {
        outputPath += "," + Configs.topicParameters();
        TopicsReader.WordStats readTopicWords = new TopicsReader().readTopicsWords(Configs.datasetRoot + "topics/" + Configs.topicsName + "/model.mallet", Configs.datasetRoot + "topics/" + Configs.topicsName + "/word-topic-counts.mallet");
        double[] alphas = TopicsReader.readAlphas(Configs.datasetRoot + "topics/" + Configs.topicsName + "/topic-keys.mallet", 100);
//        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-WeiSimilarity-.90", new MyDummySimilarity(Configs.lmDirichletMu, new MyWeiQuery(Configs.lmDirichletMu, .9f, DatasetMain.getInstance().getIndexReader(), readTopicWords, alphas, TopicsReader.readBeta())), queryConverter));
//        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-WeiSimilarity-.85", new MyDummySimilarity(Configs.lmDirichletMu, new MyWeiQuery(Configs.lmDirichletMu, .85f, DatasetMain.getInstance().getIndexReader(), readTopicWords, alphas, TopicsReader.readBeta())), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-WeiSimilarity-.80", new MyDummySimilarity(Configs.lmDirichletMu, new MyWeiQuery(Configs.lmDirichletMu, .8f, DatasetMain.getInstance().getIndexReader(), readTopicWords, alphas, readTopicWords.getBeta())), queryConverter));
//        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-WeiSimilarity-.75", new MyDummySimilarity(Configs.lmDirichletMu, new MyWeiQuery(Configs.lmDirichletMu, .75f, DatasetMain.getInstance().getIndexReader(), readTopicWords, alphas, TopicsReader.readBeta())), queryConverter));

        InstanceClassifier ic = new InstanceClassifier();
//        addTopicSearcher(new JSSearcherAuthor(1.f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new KLDocSearcher(1.f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new KLDocQuery(1.f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new KLSearcherDoc(1.f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new KLQueryDoc(1.f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
        float constantWeight = 0f;
//        addTopicSearcher(new CosineSearcherAuthor(1f - constantWeight), .25f, .8f, constantWeight, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new CosineSearcherDoc(1.f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1f - constantWeight, .1f), .25f, .8f, constantWeight, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1.f, 1f), 1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1.f, 1f), 1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1.f, 1f), 1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1.f, .005f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherDoc(1.f, .005f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1.f, .05f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherDoc(1.f, .05f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1.f, .01f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherDoc(1.f, .01f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherAuthor(1.f, .001f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        addTopicSearcher(new MyCosineSearcherDoc(1.f, 0.001f), .1f, .8f, ic, readTopicWords, alphas, queryConverter);
//        WeiValueSource weiValueSource = new WeiValueSource();
//        retriever.addSearcher(new WeiAggregateSearcher(weiValueSource, hiers[0].getRootNode().getSearcher(), "Agg", new LMJelinekMercerSimilarity(.7f), queryConverter));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.8-0", baseSimilarity, queryConverter, .8f, .2f));
//        topicsValueSource = new TopicsValueSource(hiers[0].getRootNode().getSearcher(), Main.database_name, ic, .33, 0);
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "05-topicsSearcher-.6-.33", baseSimilarity, queryConverter, .6f, .4f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "06-topicsSearcher-.8-.33", baseSimilarity, queryConverter, .8f, .2f));
//        topicsValueSource = new TopicsValueSource(hiers[0].getRootNode().getSearcher(), Main.database_name, ic, .5, 0);
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "07-topicsSearcher-.6-.5", baseSimilarity, queryConverter, .6f, .4f));
//        TopicsValueSource topicsValueSource = new TopicsValueSource(hiers[0].getRootNode().getSearcher(), Main.database_name, ic, 0., 0, 0, 0);
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.8-0-0-0-0", baseSimilarity, queryConverter, .8f, .2f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.6-0-0-0-0", baseSimilarity, queryConverter, .6f, .4f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.4-0-0-0-0", baseSimilarity, queryConverter, .4f, .6f));
//        topicsValueSource = new TopicsValueSource(hiers[0].getRootNode().getSearcher(), Main.database_name, ic, 1, 0, 0, 0);
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.6-1-0-0-0", baseSimilarity, queryConverter, .6f, .4f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.8-1-0-0-0", baseSimilarity, queryConverter, .8f, .2f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.4-1-0-0-0", baseSimilarity, queryConverter, .4f, .6f));
//        topicsValueSource = new TopicsValueSource(hiers[0].getRootNode().getSearcher(), Main.database_name, ic, 0., 1, 0, 0);
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.8-0-1-0-0", baseSimilarity, queryConverter, .8f, .2f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.9-0-1-0-0", baseSimilarity, queryConverter, .9f, .1f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.6-0-1-0-0", baseSimilarity, queryConverter, .6f, .4f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.4-0-1-0-0", baseSimilarity, queryConverter, .4f, .6f));
//        topicsValueSource = new TopicsValueSource(hiers[0].getRootNode().getSearcher(), Main.database_name, ic, 0., 0, 1, 0);
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.8-0-0-1-0", baseSimilarity, queryConverter, .8f, .2f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.9-0-0-1-0", baseSimilarity, queryConverter, .9f, .1f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.6-0-0-1-0", baseSimilarity, queryConverter, .6f, .4f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.4-0-0-1-0", baseSimilarity, queryConverter, .4f, .6f));
//        topicsValueSource = new TopicsValueSource(hiers[0].getRootNode().getSearcher(), Main.database_name, ic, 0., 0, 0, 1);
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.8-0-0-0-1", baseSimilarity, queryConverter, .8f, .2f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.9-0-0-0-1", baseSimilarity, queryConverter, .9f, .1f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.6-0-0-0-1", baseSimilarity, queryConverter, .6f, .4f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.4-0-0-0-1", baseSimilarity, queryConverter, .4f, .6f));
//        retriever.addSearcher(new AggregateSearcher(topicsValueSource, hiers[0].getRootNode().getSearcher(), "04-topicsSearcher-.2-0-0-0-1", baseSimilarity, queryConverter, .2f, .8f));
    }

    protected static void addMyWeightingSearchers(QueryConverter queryConverter) {
//        QueryWeighter queryWeighter_general = new QueryWeighter(hiers[0]);
//        queryWeighter_general.setParentConverter(queryConverter);
//        
//        QueryWeighter queryWeighter_web = new QueryWeighter(hiers[1]);
//        queryWeighter_web.setParentConverter(queryConverter);
//        QueryWeighter queryWeighter_verification = new QueryWeighter(hier3);
//        queryWeighter_verification.setParentConverter(queryConverter);
//        
//        QueryWeighter queryWeighter_general_expanded = new QueryWeighter(hiers[0]);
//        queryWeighter_general_expanded.setParentConverter(queryExpander);
//        
//        QueryWeighter queryWeighter_web_expanded = new QueryWeighter(hier2);
//        queryWeighter_web_expanded.setParentConverter(queryExpander);
//        
//        QueryWeighter queryWeighter_verification_expanded = new QueryWeighter(hier2);
//        queryWeighter_verification_expanded.setParentConverter(queryExpander);
//                UserLMSearcher userLMSearcher_general = new UserLMSearcher(hier, "UserLM-general", new CiteseerxUserHierarchySimilarity(hier.node, 500), extractor, queryConverter);
//                retriever.addSearcher(userLMSearcher_general);
//                UserLMSearcher userTFIDFSearcher_general = new UserLMSearcher(hier, "UserTFIDF-general", new TFIDFCiteseerxUserHierarchySimilarity(hier.node), extractor, queryConverter);
//                retriever.addSearcher(userTFIDFSearcher_general);
//                UserLMSearcher userLMSearcher_web = new UserLMSearcher(hier2, "UserLM-web", new CiteseerxUserHierarchySimilarity(hier2.node, 500), extractor, queryConverter);
//                retriever.addSearcher(userLMSearcher_web);
//                UserLMSearcher userTFIDFSearcher_web = new UserLMSearcher(hier2, "UserTFIDF-web", new TFIDFCiteseerxUserHierarchySimilarity(hier2.node), extractor, queryConverter);
//                retriever.addSearcher(userTFIDFSearcher_web);
//                retriever.addSearcher(new BasicSearcher(hiers[0].getRootNode().getSearcher(), "13-User-general", baseSimilarity, queryWeighter_general));

//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserTFIDF-verification-new", new DefaultSimilarity(), queryWeighter_verification));
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserLM-verification-new", new LMDirichletSimilarity(400), queryWeighter_verification));
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "TFIDF-expanded", new DefaultSimilarity(), queryExpander));
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearch"er(), "LM-expanded", new LMDirichletSimilarity(400), queryExpander));
//                UserLMSearcher userLMSearcher_general_expanded = new UserLMSearcher(hier, "UserLM-general-expanded", new CiteseerxUserHierarchySimilarity(hier.node, 500), extractor, queryExpander);
//                retriever.addSearcher(userLMSearcher_general_expanded);
//                UserLMSearcher userTFIDFSearcher_general_expanded = new UserLMSearcher(hier, "UserTFIDF-general-expanded", new TFIDFCiteseerxUserHierarchySimilarity(hier.node), extractor, queryExpander);
//                retriever.addSearcher(userTFIDFSearcher_general_expanded);
//                UserLMSearcher userLMSearcher_web_expanded = new UserLMSearcher(hier2, "UserLM-web-expanded", new CiteseerxUserHierarchySimilarity(hier2.node, 500), extractor, queryExpander);
//                retriever.addSearcher(userLMSearcher_web_expanded);
//                UserLMSearcher userTFIDFSearcher_web_expanded = new UserLMSearcher(hier2, "UserTFIDF-web-expanded", new TFIDFCiteseerxUserHierarchySimilarity(hier2.node), extractor, queryExpander);
//                retriever.addSearcher(userTFIDFSearcher_web_expanded);
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserTFIDF-general-expanded-new", new DefaultSimilarity(), queryWeighter_general_expanded));
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserLM-general-expanded-new", new LMDirichletSimilarity(400), queryWeighter_general_expanded));        
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserTFIDF-web-expanded-new", new DefaultSimilarity(), queryWeighter_web_expanded));
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserLM-web-expanded-new", new LMDirichletSimilarity(400), queryWeighter_web_expanded));
//
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserTFIDF-verification-expanded-new", new DefaultSimilarity(), queryWeighter_verification_expanded));
//                retriever.addSearcher(new BasicSearcher(hier.node.getSearcher(), "UserLM-verification-expanded-new", new LMDirichletSimilarity(400), queryWeighter_verification_expanded));
    }

    public static void addOtherSearchers() {
        outputPath += "," + Configs.socialTextualParameters();
        retriever.addSearcher(new RandomSearcher(DatasetMain.getInstance().getIndexSearcher(), "31-RandomSearcher", queryConverter));
//        final RandomNeighborSearcher randomNeighborSearcher = new RandomNeighborSearcher(Main.DatasetMain.getInstance().getIndexSearcher(), "32-RandomNeighborSearcher", queryConverter);
//        retriever.addSearcher(randomNeighborSearcher);
        retriever.addSearcher(new SocialSearcher(DatasetMain.getInstance().getIndexSearcher(), "33-SocialSearcher", queryConverter));
//        retriever.addSearcher(new RandomNeighborFilteringSearcher(randomNeighborSearcher, DatasetMain.getInstance().getIndexSearcher(), "15-RandomNeighborFilteringSearcher-f", baseSimilarity, queryConverter, Configs.considerFriendsOfFriendsInRandomNeighborFilteringSearcher));
//        retriever.addSearcher(new RandomNeighborFilteringSearcher(randomNeighborSearcher, DatasetMain.getInstance().getIndexSearcher(), "15-RandomNeighborFilteringSearcher-t", baseSimilarity, queryConverter, !Configs.considerFriendsOfFriendsInRandomNeighborFilteringSearcher));
//        retriever.addSearcher(new FilteringSearcher(hiers[0].getRootNode().getSearcher(), "30-FilteringSearcherLM", new LMDirichletSimilarity(), queryConverter, Main.numOfResults));
//                List<LeafReaderContext> leaves = hiers[0].getRootNode().getIndexReader().leaves();
//                NumericDocValues norms = null;
//                for (LeafReaderContext ctx : leaves) {
//                    norms = ctx.reader().getNormValues("content");
//                }
//                retriever.addSearcher(new BasicSearcher(hiers[0].getRootNode().getSearcher(), "19-BM25_No_No", new org.apache.lucene.search.similarities.BM25Similarity(), queryConverter));
//                retriever.addSearcher(new BasicSearcher(hiers[0].getRootNode().getSearcher(), "19-BM25_No_No", new BM25_No_No(129.92435f, 1.2f, .75f, norms), queryConverter));

        outputPath += "," + Configs.profileParameters();
        NormalizedQueryExpander normalizedQueryExpander = new NormalizedQueryExpander(20, Configs.evaluator, .66);
        retriever.addSearcher(new IRR(DatasetMain.getInstance().getIndexSearcher(), "5_IRR", baseSimilarity, queryConverter, normalizedQueryExpander));
        NormalizedQueryExpander normalizedQueryExpanderOriginalIgnored = new NormalizedQueryExpander(DatasetMain.getInstance().getIndexSearcher(), 20, Configs.evaluator, .66, true);
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "20_.66-8_ProfileOnly", baseSimilarity, normalizedQueryExpanderOriginalIgnored));
    }

    public static void addAggregateSearchers(boolean all) {
        outputPath += "," + Configs.socialTextualParameters();

//        Map<Integer, Integer> docIdMap = generateDocIdMap(hiers[0].getRootNode().getSearcher().getIndexReader());
        MyValueSource myValueSource = new CiteseerxSocialTextualValueSource(Configs.database_name, Configs.selfConsiderConstant);
//                MyValueSource myValueSourceNoPenalization = new CiteseerxSocialTextualValueSource(database_name, docIdMap);
//                ((CiteseerxSocialTextualValueSource) myValueSourceNoPenalization).setPenalizeMultipleAuthors(false);
//                MyValueSource myValueSourceNoUseUserWeight = new CiteseerxSocialTextualValueSource(database_name, docIdMap);
//                ((CiteseerxSocialTextualValueSource) myValueSourceNoUseUserWeight).setUseUserWeights(false);
//                MyValueSource myValueSourceNoUseFriendWeight = new CiteseerxSocialTextualValueSource(database_name, docIdMap);
//                ((CiteseerxSocialTextualValueSource) myValueSourceNoUseFriendWeight).setUseFriendWeight(false);
//        retriever.addSearcher(new AggregateSearcher(myValueSource, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.05", baseSimilarity, queryConverter, .05f, .95f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.10", baseSimilarity, queryConverter, .10f, .90f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.20", baseSimilarity, queryConverter, .20f, .80f));
//                retriever.addSearcher(new AggregateSearcher(myValueSource, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.25", baseSimilarity, queryConverter, .25f, .75f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.40", baseSimilarity, queryConverter, .40f, .60f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), "AggregateTFIDF-.50", baseSimilarity, queryConverter, .50f, .50f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), "AggregateTFIDF-.55", baseSimilarity, queryConverter, .55f, .45f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), "AggregateTFIDF-.60", baseSimilarity, queryConverter, .60f, .40f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), "AggregateTFIDF-.65", baseSimilarity, queryConverter, .65f, .35f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), "AggregateTFIDF-.55", baseSimilarity, queryConverter, .55f, .45f));
//        retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), "AggregateTFIDF-.65", baseSimilarity, queryConverter, .65f, .35f));
        if (all) {
            retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), myValueSource.getName(), new ClassicSimilarity(), queryConverter, .85f, .15f));
            retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), myValueSource.getName(), new ClassicSimilarity(), queryConverter, .75f, .25f));
            retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), myValueSource.getName(), new ClassicSimilarity(), queryConverter, .7f, .3f));
        }
        retriever.addSearcher(new AggregateSearcher(myValueSource, DatasetMain.getInstance().getIndexSearcher(), myValueSource.getName(), new ClassicSimilarity(), queryConverter, .8f, .2f));
//                retriever.addSearcher(new AggregateSearcher(myValueSourceNoPenalization, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.25-myValueSourceNoPenalization", new DefaultSimilarity(), queryConverter, .25f, .75f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(myValueSourceNoUseUserWeight, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.25-myValueSourceNoUseUserWeight", new DefaultSimilarity(), queryConverter, .25f, .75f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(myValueSourceNoUseFriendWeight, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.25-myValueSourceNoUseFriendWeight", new DefaultSimilarity(), queryConverter, .25f, .75f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(myValueSource, hiers[0].getRootNode().getSearcher(), "AggregateLM-.25", new LMDirichletSimilarity(100), queryConverter, .25f, .75f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(myValueSource, hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.8", new Log_Log_No(), queryConverter, .8f, .2f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(hiers[0].getRootNode().getSearcher(), "14-AggregateTFIDF-.55", new Log_Log_Norm(), queryConverter, .55f, .45f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(hiers[0].getRootNode().getSearcher(), "AggregateTFIDF-.25", new DefaultSimilarity(), queryConverter, .25f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(hiers[0].getRootNode().getSearcher(), "AggregateLM-1.0", new LMDirichletSimilarity(400), queryConverter, 1f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(hiers[0].getRootNode().getSearcher(), "29-AggregateLM-2.5", new LMDirichletSimilarity(400), queryConverter, 2.5f, docIdMap));
//                retriever.addSearcher(new AggregateSearcher(hiers[0].getRootNode().getSearcher(), "AggregateLM-5.0", new LMDirichletSimilarity(400), queryConverter, 5.0f, docIdMap));
    }

    protected static void addAxiomSearchers() {
        outputPath += ",Baselines";
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Binary_No_Yes", new Binary_No_Yes(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Log_Log_Yes", new Log_Log_Yes(), queryConverter));
//        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Log_Log_No_FB", new Log_Log_No(), queryConverter, new FeedbackExpander(DatasetMain.getInstance().getIndexSearcher().getIndexReader(), true, .2, 4, 25)));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Binary_Log_Yes", new Binary_Log_Yes(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Log_No_Yes", new Log_No_Yes(), queryConverter));
//        retriever.addSearcher(new BasicSearcher(hiers[0].getRootNode().getSearcher(), "01-Log_Raw_No", new Log_Raw_No(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Raw_Log_Yes", new Raw_Log_Yes(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Raw_No_Yes", new Raw_No_Yes(), queryConverter));
//        retriever.addSearcher(new BasicSearcher(hiers[0].getRootNode().getSearcher(), "01-Raw_Raw_No", new Raw_Raw_No(), queryConverter));

        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Binary_No_No", new Binary_No_No(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Log_Log_No", new Log_Log_No(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Binary_Log_No", new Binary_Log_No(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Log_No_No", new Log_No_No(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Raw_Log_No", new Raw_Log_No(), queryConverter));
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "01-Raw_No_No", new Raw_No_No(), queryConverter));
    }

    public static void addAllCamposSearchers(String prefix) {
        outputPath += "," + Configs.profileParameters();

        addCamposSearchers(5, prefix + "_05_33_", .33);
        addCamposSearchers(5, prefix + "_05_66_", .66);
        addCamposSearchers(5, prefix + "_05_99_", .99);
        addCamposSearchers(10, prefix + "_10_33_", .33);
        addCamposSearchers(10, prefix + "_10_66_", .66);
        addCamposSearchers(10, prefix + "_10_99_", .99);
        addCamposSearchers(20, prefix + "_20_33_", .33);
        addCamposSearchers(20, prefix + "_20_66_", .66);
        addCamposSearchers(20, prefix + "_20_99_", .99);
        addCamposSearchers(40, prefix + "_40_33_", .33);
        addCamposSearchers(40, prefix + "_40_66_", .66);
        addCamposSearchers(40, prefix + "_40_99_", .99);
    }

    protected static void addCamposSearchers(Integer numOfKeywords, String prefix, Double weight) {
        QueryExpander queryExpander = new QueryExpander(numOfKeywords, Configs.evaluator);
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), prefix + "1_QE", baseSimilarity, queryExpander));
        NormalizedQueryExpander normalizedQueryExpander = new NormalizedQueryExpander(numOfKeywords, Configs.evaluator, weight);
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), prefix + "2_NQE", baseSimilarity, normalizedQueryExpander));
        retriever.addSearcher(new HRR(DatasetMain.getInstance().getIndexSearcher(), prefix + "3_HRR", baseSimilarity, queryConverter, normalizedQueryExpander));
        retriever.addSearcher(new SRR(DatasetMain.getInstance().getIndexSearcher(), prefix + "4_SRR", baseSimilarity, queryConverter, normalizedQueryExpander));
        retriever.addSearcher(new IRR(DatasetMain.getInstance().getIndexSearcher(), prefix + "5_IRR", baseSimilarity, queryConverter, normalizedQueryExpander));
        retriever.addSearcher(new IHRR(DatasetMain.getInstance().getIndexSearcher(), prefix + "6_IHRR", baseSimilarity, queryConverter, normalizedQueryExpander));
        NormalizedQueryExpander normalizedQueryExpanderOriginalIgnored = new NormalizedQueryExpander(DatasetMain.getInstance().getIndexSearcher(), numOfKeywords, Configs.evaluator, weight, true);
        retriever.addSearcher(new HRR(DatasetMain.getInstance().getIndexSearcher(), prefix + "7_PHRR", baseSimilarity, queryConverter, normalizedQueryExpanderOriginalIgnored));
//        retriever.addSearcher(new BasicSearcher(hiers[0].getRootNode().getSearcher(), prefix + "8_ProfileOnly", new LMDirichletSimilarity(400), normalizedQueryExpanderOriginalIgnored));
    }

    public static void addFeedbackSearchers() {
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 1, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 2, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 3, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 4, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .2, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .2, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .2, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .2, 4, 25, 10);

//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 1, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 2, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 3, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .05, 4, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .1, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .1, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .1, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .1, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .15, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .15, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .15, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .15, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .2, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .2, 2, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .2, 3, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .2, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .025, 1, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 1, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 4, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 4, 100, 10);
        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 4, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 4, 100, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .15, 1, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .2, 1, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 1, 100, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 1, 50, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 1, 50, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .05, 1, 15, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 2, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 3, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 4, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 5, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 6, 25, 15);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 1, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 1, 25, 5);
//        addFeedbackSearcher(baseSimilarity, queryConverter, false, .1, 1, 25, 20);
//
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .1, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .1, 4, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .1, 4, 100, 10);
        addFeedbackSearcher(baseSimilarity, queryConverter, true, .15, 4, 25, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .15, 4, 50, 10);
//        addFeedbackSearcher(baseSimilarity, queryConverter, true, .15, 4, 100, 10);
    }

    protected static void addFeedbackSearcher(Similarity baseSimilarity, QueryConverter queryConverter, boolean useDocScores, double beta, double formulaC, int numOfTerms, int feedbackDocumentCount) {
        FeedbackExpander feedbackExpander = new FeedbackExpander(DatasetMain.getInstance().getIndexSearcher().getIndexReader(), useDocScores, beta, formulaC, numOfTerms, feedbackDocumentCount);
        retriever.addSearcher(new BasicSearcher(DatasetMain.getInstance().getIndexSearcher(), "02-" + Configs.baseSimilarityName + "-" + feedbackExpander.getName(), baseSimilarity, queryConverter, feedbackExpander));
    }

    private AddSearchers() {
    }

}
